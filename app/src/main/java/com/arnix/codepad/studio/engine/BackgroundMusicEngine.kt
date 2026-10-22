package com.arnix.codepad.studio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.arnix.codepad.studio.model.BgmConfig
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Engine for customizable background audio / music during AutoWrite sessions and Screen Recordings.
 *
 * Supports:
 * - User-imported audio files (MP3, WAV, AAC, M4A) with custom start offsets and duration windows.
 * - Procedural synthesized ambient, lo-fi, and synthwave beats for instant zero-dependency preview.
 * - Precision timing cuts tailored for shorts (15s, 20s, 30s, 45s, 60s).
 * - Smooth fade-out in final 1.5 seconds.
 * - Streams over AudioAttributes.USAGE_MEDIA so Android 10+ internal audio capture captures both
 *   typing sound and background music directly into screen recordings.
 */
object BackgroundMusicEngine {

    private const val TAG = "BackgroundMusicEngine"
    private const val SAMPLE_RATE = 22050

    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var playerJob: Job? = null
    private var previewJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var isSynthActive = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress = _previewProgress.asStateFlow()

    private val _previewElapsedSec = MutableStateFlow(0)
    val previewElapsedSec = _previewElapsedSec.asStateFlow()

    private val _previewTotalSec = MutableStateFlow(30)
    val previewTotalSec = _previewTotalSec.asStateFlow()

    /**
     * Imports a user-selected audio Uri, copies it to app cache storage, and probes duration.
     */
    fun importAudioFromUri(context: Context, uri: Uri): Pair<String, Int>? {
        return try {
            val resolver = context.contentResolver
            val bgmDir = File(context.cacheDir, "bgm").apply { mkdirs() }
            val destFile = File(bgmDir, "custom_bgm_${System.currentTimeMillis()}.mp3")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            var durationSeconds = 180
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(destFile.absolutePath)
                val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationMsStr?.toLongOrNull() ?: 180_000L
                durationSeconds = (durationMs / 1000L).toInt().coerceAtLeast(5)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to probe audio duration: ${e.message}")
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }

            Pair(destFile.absolutePath, durationSeconds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom audio", e)
            null
        }
    }

    /**
     * Starts background music playback for a live session or recording.
     */
    @Synchronized
    fun startPlayback(context: Context, config: BgmConfig, target: AutoDurationTarget) {
        if (!config.isEnabled) {
            stopPlayback()
            return
        }
        stopPlayback()

        val clipDuration = config.getEffectiveDuration(target)
        val startOffset = config.startOffsetSeconds
        _isPlaying.value = true

        if (config.isCustomFile && config.customAudioPath != null && File(config.customAudioPath).exists()) {
            playCustomFile(context, config, startOffset, clipDuration, isPreview = false)
        } else {
            playPresetSynth(config, clipDuration, isPreview = false)
        }
    }

    /**
     * Previews a segment in the settings/trimmer sheet.
     */
    @Synchronized
    fun startPreview(context: Context, config: BgmConfig, target: AutoDurationTarget) {
        stopPlayback()

        val clipDuration = config.getEffectiveDuration(target)
        val startOffset = config.startOffsetSeconds
        _previewElapsedSec.value = 0
        _previewTotalSec.value = clipDuration
        _previewProgress.value = 0f
        _isPlaying.value = true

        if (config.isCustomFile && config.customAudioPath != null && File(config.customAudioPath).exists()) {
            playCustomFile(context, config, startOffset, clipDuration, isPreview = true)
        } else {
            playPresetSynth(config, clipDuration, isPreview = true)
        }
    }

    private fun playCustomFile(
        context: Context,
        config: BgmConfig,
        startOffsetSec: Int,
        clipDurationSec: Int,
        isPreview: Boolean
    ) {
        try {
            val file = File(config.customAudioPath!!)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                val startMs = (startOffsetSec * 1000).coerceAtMost(duration.coerceAtLeast(0))
                seekTo(startMs)
                val targetVol = config.volume.coerceIn(0.05f, 1.0f)
                setVolume(targetVol, targetVol)
                start()
            }
            mediaPlayer = mp

            val job = engineScope.launch {
                val startTimeMs = System.currentTimeMillis()
                val totalDurationMs = clipDurationSec * 1000L

                while (isActive && mp.isPlaying) {
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtMost(clipDurationSec)

                    if (isPreview) {
                        _previewElapsedSec.value = elapsedSec
                        _previewProgress.value = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    }

                    // Apply smooth 1.5s fade out if enabled
                    if (config.isFadeOutEnabled && totalDurationMs - elapsedMs in 0..1500L) {
                        val fadeFraction = ((totalDurationMs - elapsedMs).toFloat() / 1500f).coerceIn(0f, 1f)
                        val vol = config.volume * fadeFraction
                        try { mp.setVolume(vol, vol) } catch (_: Exception) {}
                    }

                    if (elapsedMs >= totalDurationMs) {
                        if (config.isLoopEnabled && !isPreview) {
                            val startMs = (startOffsetSec * 1000).coerceAtMost(mp.duration.coerceAtLeast(0))
                            try {
                                mp.seekTo(startMs)
                                mp.setVolume(config.volume, config.volume)
                            } catch (_: Exception) {}
                            // restart loop count
                            playCustomFile(context, config, startOffsetSec, clipDurationSec, isPreview)
                            return@launch
                        } else {
                            break
                        }
                    }
                    delay(80L)
                }

                stopPlayback()
            }

            if (isPreview) previewJob = job else playerJob = job
        } catch (e: Exception) {
            Log.e(TAG, "Error playing custom audio file", e)
            stopPlayback()
        }
    }

    private fun playPresetSynth(
        config: BgmConfig,
        clipDurationSec: Int,
        isPreview: Boolean
    ) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synthTrack = track
            isSynthActive = true
            track.play()

            val job = engineScope.launch {
                val startTimeMs = System.currentTimeMillis()
                val totalDurationMs = clipDurationSec * 1000L
                val pcmChunk = ShortArray(1024)
                var phase = 0.0
                var step = 0
                val volume = config.volume.coerceIn(0.05f, 1.0f)

                while (isActive && isSynthActive) {
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtMost(clipDurationSec)

                    if (isPreview) {
                        _previewElapsedSec.value = elapsedSec
                        _previewProgress.value = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    }

                    // Procedural waveform synthesis based on preset ID
                    val fadeFactor = if (config.isFadeOutEnabled && totalDurationMs - elapsedMs in 0..1500L) {
                        ((totalDurationMs - elapsedMs).toFloat() / 1500f).coerceIn(0f, 1f)
                    } else 1.0f

                    synthesizePresetBlock(config.selectedTrackId, pcmChunk, step, phase, volume * fadeFactor)
                    phase += (1024.0 / SAMPLE_RATE) * 2.0 * PI
                    step++

                    try {
                        track.write(pcmChunk, 0, pcmChunk.size)
                    } catch (_: Exception) {
                        break
                    }

                    if (elapsedMs >= totalDurationMs) {
                        if (config.isLoopEnabled && !isPreview) {
                            // Loop synth sequence
                            playPresetSynth(config, clipDurationSec, isPreview)
                            return@launch
                        } else {
                            break
                        }
                    }
                }

                stopPlayback()
            }

            if (isPreview) previewJob = job else playerJob = job
        } catch (e: Exception) {
            Log.e(TAG, "Error playing preset synthesizer audio", e)
            stopPlayback()
        }
    }

    private fun synthesizePresetBlock(
        trackId: String,
        out: ShortArray,
        step: Int,
        basePhase: Double,
        vol: Float
    ) {
        val baseFreq = when (trackId) {
            "preset_synthwave" -> 110.0 // A2
            "preset_ambient" -> 130.81  // C3 (Warm 432Hz harmonious)
            "preset_tech" -> 146.83     // D3
            else -> 174.61              // F3 (Lo-fi warm chord base)
        }

        val beatDurationSteps = 16
        val barBeat = (step / beatDurationSteps) % 4
        val chordFreq = when (barBeat) {
            0 -> baseFreq
            1 -> baseFreq * 1.2
            2 -> baseFreq * 1.5
            else -> baseFreq * 1.33
        }

        val len = out.size
        for (i in 0 until len) {
            val t = (step * len + i).toDouble() / SAMPLE_RATE.toDouble()
            val wave = when (trackId) {
                "preset_ambient" -> {
                    // Gentle soothing binaural pad
                    0.55 * sin(2.0 * PI * chordFreq * t) +
                        0.30 * sin(2.0 * PI * (chordFreq * 1.5) * t + 0.5) +
                        0.15 * sin(2.0 * PI * (chordFreq * 2.0) * t + 1.0)
                }
                "preset_synthwave" -> {
                    // 80s driving pulse
                    val saw = (2.0 * ((t * chordFreq) % 1.0) - 1.0) * 0.4
                    val sub = 0.5 * sin(2.0 * PI * (chordFreq / 2.0) * t)
                    saw + sub
                }
                "preset_tech" -> {
                    // Tech beat with hi-hat sizzle
                    val synth = 0.5 * sin(2.0 * PI * chordFreq * t)
                    val noise = if ((step % 8) == 0 && i < 200) (Random.nextDouble(-0.3, 0.3)) else 0.0
                    synth + noise
                }
                else -> {
                    // Lo-fi warm chill chords
                    val fundamental = 0.6 * sin(2.0 * PI * chordFreq * t)
                    val third = 0.3 * sin(2.0 * PI * (chordFreq * 1.25) * t)
                    val fifth = 0.2 * sin(2.0 * PI * (chordFreq * 1.5) * t)
                    fundamental + third + fifth
                }
            }
            val sample = (wave * 32767.0 * vol * 0.45).coerceIn(-32767.0, 32767.0).toInt().toShort()
            out[i] = sample
        }
    }

    /**
     * Stops all background music playback and resets preview trackers.
     */
    @Synchronized
    fun stopPlayback() {
        _isPlaying.value = false
        playerJob?.cancel()
        previewJob?.cancel()
        playerJob = null
        previewJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Throwable) {}
        mediaPlayer = null

        isSynthActive = false
        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (_: Throwable) {}
        synthTrack = null

        _previewProgress.value = 0f
        _previewElapsedSec.value = 0
    }
}
