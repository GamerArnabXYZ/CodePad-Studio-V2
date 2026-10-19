package com.arnix.codepad.studio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class TypingSoundProfile(val index: Int, val label: String, val shortLabel: String) {
    OFF(0, "Silent (Mute)", "Mute"),
    MECH_THOCK(1, "Mechanical Thock", "Thock"),
    CRISP_CLICK(2, "Crisp Tactile Click", "Click"),
    SOFT_ASMR(3, "Soft ASMR Tap", "ASMR");

    companion object {
        fun fromIndex(index: Int): TypingSoundProfile =
            entries.firstOrNull { it.index == index } ?: OFF
    }
}

/**
 * High-Performance, Zero-External-Codec Mechanical Keyboard Audio Engine.
 *
 * Designed specifically for clean ASMR audio during code typing:
 * - Direct 16-bit PCM AudioTrack streaming: zero dependency on Stagefright / NuMediaExtractor,
 *   completely eliminating "Failed to query component interface" errors in emulators.
 * - Distinct, authentic switch profiles (Mechanical Thock, Spacebar Stabilizer, Tactile Blue Click, Soft ASMR).
 * - Humanized cadence spacing: enforces a randomized micro-gap so ultra-fast auto-write doesn't
 *   produce a machine-gun wall of noise.
 * - Organic pitch variation (+-3.5%) mimicking human finger pressure differences.
 */
object TypingSoundEngine {

    private const val SAMPLE_RATE = 22050
    private val rng = Random(42)

    @Volatile
    private var lastPlayTimeMs = 0L

    @Volatile
    var isMutedForRecording: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var isInitialized = false

    // Pre-synthesized 16-bit PCM acoustic waveforms
    private val bufferThock: ShortArray by lazy { generateThockSound(isSpacebar = false) }
    private val bufferThockEnter: ShortArray by lazy { generateThockSound(isSpacebar = true) }
    private val bufferCrispClick: ShortArray by lazy { generateClickSound() }
    private val bufferSoftAsmr: ShortArray by lazy { generateAsmrSound() }

    @Synchronized
    fun initialize(context: Context? = null) {
        if (audioTrack != null) return
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build().apply {
                        runCatching { play() }
                    }
            } catch (_: Throwable) {
                // Fallback to legacy AudioTrack constructor for Android API 24/25 compatibility
                try {
                    @Suppress("DEPRECATION")
                    audioTrack = AudioTrack(
                        android.media.AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufSize * 2,
                        AudioTrack.MODE_STREAM
                    ).apply {
                        runCatching { play() }
                    }
                } catch (_: Throwable) {
                }
            }
            isInitialized = (audioTrack != null)
        } catch (_: Throwable) {
            // Graceful fallback for mock / headless test environments
        }
    }

    /**
     * Plays a single authentic mechanical keystroke sound.
     */
    @Synchronized
    fun playKeyTick(
        profile: TypingSoundProfile,
        typedChar: Char?,
        isManualTest: Boolean = false,
        volume: Float = 0.85f,
        minGapMs: Long = 75L,
        pitchVariation: Boolean = true,
        pitchMultiplier: Float = 1.0f,
        stabilizerResonance: Boolean = true
    ) {
        if (profile == TypingSoundProfile.OFF || volume <= 0.01f || isMutedForRecording) return

        val now = SystemClock.uptimeMillis()

        // Humanized Cadence Spacing:
        // Even at 100+ chars/second, real keyboard recording must breathe with natural spacing.
        val effectiveMinGap = if (isManualTest) 35L else (minGapMs + rng.nextLong(35L))
        if (now - lastPlayTimeMs < effectiveMinGap) return
        lastPlayTimeMs = now

        val track = audioTrack ?: run {
            initialize()
            audioTrack
        } ?: return

        val isDeepKey = stabilizerResonance && (typedChar == '\n' || typedChar == ' ' || typedChar == '}' || typedChar == '>')

        val rawBuffer = when (profile) {
            TypingSoundProfile.OFF -> return
            TypingSoundProfile.MECH_THOCK -> if (isDeepKey) bufferThockEnter else bufferThock
            TypingSoundProfile.CRISP_CLICK -> bufferCrispClick
            TypingSoundProfile.SOFT_ASMR -> bufferSoftAsmr
        }

        // Apply pitch variation and master volume with pitch multiplier
        val basePitch = if (pitchVariation) (0.97f + rng.nextFloat() * 0.06f) else 1.0f
        val effectivePitch = (basePitch * pitchMultiplier.coerceIn(0.70f, 1.40f)).coerceIn(0.5f, 2.0f)
        val outBuffer = processPcmAudio(rawBuffer, volume.coerceIn(0.1f, 1.0f), effectivePitch)

        try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            track.write(outBuffer, 0, outBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
        } catch (_: Throwable) {
        }
    }

    private fun processPcmAudio(input: ShortArray, volume: Float, pitchFactor: Float): ShortArray {
        val outLen = (input.size / pitchFactor).toInt().coerceAtLeast(1)
        val result = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcIdx = (i * pitchFactor).toInt().coerceIn(0, input.size - 1)
            val sample = (input[srcIdx] * volume).toInt()
            result[i] = sample.coerceIn(-32767, 32767).toShort()
        }
        return result
    }

    /**
     * Mechanical Thock: 140Hz hollow acoustic cavity resonance with subtle 2.2kHz snap.
     */
    private fun generateThockSound(isSpacebar: Boolean): ShortArray {
        val durationMs = if (isSpacebar) 52 else 40
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(sampleCount)
        val fundFreq = if (isSpacebar) 72.0 else 135.0
        val damping = if (isSpacebar) 42.0 else 58.0

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * damping)

            // Click transient (0-4ms)
            val click = if (t < 0.004) {
                sin(2.0 * PI * 2200.0 * t) * (1.0 - (t / 0.004)) * 0.4
            } else {
                0.0
            }

            // Resonant switch housing acoustic body
            val body = sin(2.0 * PI * fundFreq * t) * 0.75 +
                sin(2.0 * PI * (fundFreq * 2.05) * t) * 0.35 +
                sin(2.0 * PI * (fundFreq * 3.1) * t) * 0.15

            val sample = (click + body * env) * 0.85
            buffer[i] = (sample * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Crisp Tactile Click: Cherry Blue style high-frequency mechanical click leaf snap.
     */
    private fun generateClickSound(): ShortArray {
        val durationMs = 34
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(sampleCount)

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 110.0)

            // Primary switch click leaf snap (0-3ms at 3100Hz)
            val click1 = if (t < 0.003) sin(2.0 * PI * 3100.0 * t) * 0.8 else 0.0

            // Secondary leaf bounce (2-5ms at 2700Hz)
            val click2 = if (t in 0.002..0.005) sin(2.0 * PI * 2700.0 * (t - 0.002)) * 0.55 else 0.0

            // Light plastic bottom-out body
            val thud = sin(2.0 * PI * 380.0 * t) * 0.28 * exp(-t * 60.0)

            val sample = (click1 + click2 + thud) * env * 0.88
            buffer[i] = (sample * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Soft ASMR Tap: Rounded Gaussian-damped lubed silent linear switch tap.
     */
    private fun generateAsmrSound(): ShortArray {
        val durationMs = 36
        val sampleCount = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(sampleCount)

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            // Smooth Gaussian bell-curve envelope
            val dt = t - 0.004
            val env = exp(-(dt * dt) / 0.00014) * exp(-t * 45.0)

            val tone = sin(2.0 * PI * 430.0 * t) * 0.75 +
                sin(2.0 * PI * 860.0 * t) * 0.25

            val sample = tone * env * 0.82
            buffer[i] = (sample * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    @Synchronized
    fun resetCadence() {
        lastPlayTimeMs = 0L
    }

    @Synchronized
    fun stopAll() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Throwable) {
        }
        lastPlayTimeMs = 0L
    }

    @Synchronized
    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
        isInitialized = false
    }
}
