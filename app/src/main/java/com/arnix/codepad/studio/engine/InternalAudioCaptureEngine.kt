package com.arnix.codepad.studio.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Captures clean internal playback audio (typing sounds, system playback) via Android 10+
 * AudioPlaybackCaptureConfiguration and encodes to AAC.
 */
class InternalAudioCaptureEngine(
    private val projection: MediaProjection,
    private val stagingAudioFile: File
) {
    companion object {
        private const val TAG = "InternalAudioCapture"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
        private const val BIT_RATE = 128_000

        fun mergeVideoAndAudio(videoFile: File, audioFile: File, destinationFile: File): Boolean {
            if (!audioFile.exists() || audioFile.length() < 256) return false
            var videoExtractor: MediaExtractor? = null
            var audioExtractor: MediaExtractor? = null
            var muxer: MediaMuxer? = null
            return try {
                videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
                audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
                muxer = MediaMuxer(destinationFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                var videoTrackIndex = -1
                for (i in 0 until videoExtractor.trackCount) {
                    val format = videoExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        videoTrackIndex = muxer.addTrack(format)
                        videoExtractor.selectTrack(i)
                        break
                    }
                }

                var audioTrackIndex = -1
                for (i in 0 until audioExtractor.trackCount) {
                    val format = audioExtractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = muxer.addTrack(format)
                        audioExtractor.selectTrack(i)
                        break
                    }
                }

                if (videoTrackIndex < 0) return false
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                    bufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                    videoExtractor.advance()
                }

                if (audioTrackIndex >= 0) {
                    while (true) {
                        bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                        if (bufferInfo.size < 0) break
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }

                Log.i(TAG, "Successfully merged video and internal audio into: ${destinationFile.absolutePath}")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Error merging video and internal audio", e)
                false
            } finally {
                try { muxer?.stop() } catch (_: Throwable) {}
                try { muxer?.release() } catch (_: Throwable) {}
                try { videoExtractor?.release() } catch (_: Throwable) {}
                try { audioExtractor?.release() } catch (_: Throwable) {}
            }
        }
    }

    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var audioTrackIndex = -1
    private var muxerStarted = false

    fun start(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "AudioPlaybackCapture requires Android 10 (API 29)+")
            return false
        }

        return try {
            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding)
            val bufferSize = (minBufSize * 4).coerceAtLeast(8192)

            val audioFormat = AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(channelConfig)
                .build()

            val record = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                record.release()
                return false
            }

            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            }

            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            stagingAudioFile.parentFile?.mkdirs()
            val muxer = MediaMuxer(stagingAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            audioRecord = record
            mediaCodec = codec
            mediaMuxer = muxer
            isRunning.set(true)
            muxerStarted = false
            audioTrackIndex = -1

            record.startRecording()

            workerThread = Thread({
                recordAndEncodeLoop(record, codec, muxer, bufferSize)
            }, "InternalAudioWorker").apply { start() }

            Log.i(TAG, "Internal audio capture engine started successfully")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start internal audio capture", e)
            stop()
            false
        }
    }

    private fun recordAndEncodeLoop(
        record: AudioRecord,
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferSize: Int
    ) {
        val pcmBuffer = ByteArray(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            while (isRunning.get()) {
                val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
                if (bytesRead > 0) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(pcmBuffer, 0, bytesRead)
                            val presentationTimeUs = System.nanoTime() / 1000L
                            codec.queueInputBuffer(inputIndex, 0, bytesRead, presentationTimeUs, 0)
                        }
                    }
                }

                drainEncoder(codec, muxer, bufferInfo, endOfStream = false)
            }

            // Signal End of Stream
            val inputIndex = codec.dequeueInputBuffer(10_000L)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, System.nanoTime() / 1000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(codec, muxer, bufferInfo, endOfStream = true)
        } catch (e: Throwable) {
            Log.w(TAG, "Audio loop exception: ${e.message}")
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean
    ) {
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, if (endOfStream) 20_000L else 0L)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break else break
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) {
                    Log.w(TAG, "Audio format changed twice")
                } else {
                    val newFormat = codec.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                }
            } else if (outputIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outputIndex)
                if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            workerThread?.join(1000L)
        } catch (_: Exception) {}
        workerThread = null

        try { audioRecord?.stop() } catch (_: Throwable) {}
        try { audioRecord?.release() } catch (_: Throwable) {}
        audioRecord = null

        try { mediaCodec?.stop() } catch (_: Throwable) {}
        try { mediaCodec?.release() } catch (_: Throwable) {}
        mediaCodec = null

        try {
            if (muxerStarted) {
                mediaMuxer?.stop()
            }
        } catch (_: Throwable) {}
        try { mediaMuxer?.release() } catch (_: Throwable) {}
        mediaMuxer = null
        muxerStarted = false
    }
}
