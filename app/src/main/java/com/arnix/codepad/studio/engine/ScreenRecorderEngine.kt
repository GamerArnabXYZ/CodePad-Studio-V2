package com.arnix.codepad.studio.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.arnix.codepad.studio.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedRecordingInfo(
    val filePath: String,
    val fileUri: Uri,
    val fileName: String,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScreenRecorderState(
    val isRecording: Boolean = false,
    val isPreparing: Boolean = false,
    val durationSeconds: Int = 0,
    val currentFilePath: String? = null,
    val lastSavedRecording: SavedRecordingInfo? = null,
    val isAudioEnabled: Boolean = false,
    val targetFps: Int = 30,
    val videoQualityLabel: String = "1080p FHD",
    val error: String? = null
)

object ScreenRecorderEngine {
    private const val TAG = "ScreenRecorderEngine"
    private val _state = MutableStateFlow(ScreenRecorderState())
    val state: StateFlow<ScreenRecorderState> = _state.asStateFlow()

    // Preferences
    var isAutoRecordOnShortsStart: Boolean = false
    var recordAudio: Boolean = false
    var prefer60Fps: Boolean = true
    var prefer1080p: Boolean = true

    fun setAutoRecordOnShorts(enabled: Boolean) {
        isAutoRecordOnShortsStart = enabled
    }

    fun setRecordAudioPref(enabled: Boolean) {
        recordAudio = enabled
        _state.update { it.copy(isAudioEnabled = enabled) }
    }

    fun setPrefer60FpsPref(enabled: Boolean) {
        prefer60Fps = enabled
        _state.update { it.copy(targetFps = if (enabled) 60 else 30) }
    }

    fun setPrefer1080pPref(enabled: Boolean) {
        prefer1080p = enabled
        _state.update { it.copy(videoQualityLabel = if (enabled) "1080p FHD" else "720p HD") }
    }

    fun clearLastSavedRecording() {
        _state.update { it.copy(lastSavedRecording = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    internal fun updateState(update: (ScreenRecorderState) -> ScreenRecorderState) {
        _state.update(update)
    }

    fun startServiceRecording(
        context: Context,
        resultCode: Int,
        data: Intent,
        width: Int,
        height: Int,
        densityDpi: Int,
        audioEnabled: Boolean = recordAudio,
        fps60: Boolean = prefer60Fps,
        is1080p: Boolean = prefer1080p
    ) {
        // Audio permission check safety
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val effectiveRecordAudio = audioEnabled && hasAudioPermission

        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecordingService.EXTRA_WIDTH, width)
            putExtra(ScreenRecordingService.EXTRA_HEIGHT, height)
            putExtra(ScreenRecordingService.EXTRA_DENSITY, densityDpi)
            putExtra(ScreenRecordingService.EXTRA_RECORD_AUDIO, effectiveRecordAudio)
            putExtra(ScreenRecordingService.EXTRA_FPS, if (fps60) 60 else 30)
            putExtra(ScreenRecordingService.EXTRA_BITRATE, if (is1080p) 4_500_000 else 2_500_000)
            putExtra(ScreenRecordingService.EXTRA_PREFER_1080P, is1080p)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start ScreenRecordingService", e)
            _state.update { it.copy(error = "Cannot start recorder: ${e.message}") }
        }
    }

    fun stopServiceRecording(context: Context) {
        try {
            val intent = Intent(context, ScreenRecordingService::class.java).apply {
                action = ScreenRecordingService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot stop ScreenRecordingService", e)
        }
    }

    fun playRecordedVideo(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Play CodePad Recording"))
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
        }
    }

    fun shareRecordedVideo(context: Context, uri: Uri, projectName: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$projectName - CodePad Studio Recording")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out my coding creation recorded with CodePad Studio! #CodePad #WebDev #Shorts"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Recording"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing video", e)
        }
    }
}

class ScreenRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.arnix.codepad.studio.ACTION_START_RECORDING"
        const val ACTION_STOP = "com.arnix.codepad.studio.ACTION_STOP_RECORDING"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_DENSITY = "extra_density"
        const val EXTRA_RECORD_AUDIO = "extra_record_audio"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_PREFER_1080P = "extra_prefer_1080p"

        private const val CHANNEL_ID = "codepad_screen_recording_channel"
        private const val NOTIFICATION_ID = 4099
        private const val TAG = "ScreenRecordingService"

        fun resolveCodePadStorageDir(context: Context): File {
            val root = Environment.getExternalStorageDirectory() // /storage/emulated/0
            // Prioritize user's preferred folders outside Android/data:
            // 1. /storage/emulated/0/CodePad/
            // 2. /storage/emulated/0/Record/CodePad/
            // 3. /storage/emulated/0/Movies/CodePad/
            val candidates = listOf(
                File(root, "CodePad"),
                File(root, "Record/CodePad"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CodePad"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "CodePad")
            )

            for (dir in candidates) {
                try {
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    if (dir.exists() && dir.canWrite()) {
                        val testFile = File(dir, ".probe_${System.currentTimeMillis()}")
                        if (testFile.createNewFile()) {
                            testFile.delete()
                            Log.i(TAG, "Selected target recording directory: ${dir.absolutePath}")
                            return dir
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Directory probe failed for ${dir.absolutePath}: ${e.message}")
                }
            }

            // Fallback: public movies or root CodePad
            val publicMovies = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CodePad")
            try { publicMovies.mkdirs() } catch (_: Exception) {}
            return if (publicMovies.exists() && publicMovies.canWrite()) publicMovies else File(root, "CodePad")
        }
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var recordedSeconds = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val width = intent.getIntExtra(EXTRA_WIDTH, 720)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1280)
                val density = intent.getIntExtra(EXTRA_DENSITY, 320)
                val recordAudio = intent.getBooleanExtra(EXTRA_RECORD_AUDIO, false)
                val fps = intent.getIntExtra(EXTRA_FPS, 30)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 3_000_000)
                val prefer1080p = intent.getBooleanExtra(EXTRA_PREFER_1080P, false)

                if (resultCode != 0 && resultData != null) {
                    // CRITICAL FOR ANDROID 14+ (API 34+):
                    // Must obtain MediaProjection token BEFORE calling startForeground with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    val projection = runCatching {
                        mediaProjectionManager?.getMediaProjection(resultCode, resultData)
                    }.getOrNull()

                    val initialNotification = buildNotification(0)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                NOTIFICATION_ID,
                                initialNotification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                            )
                        } else {
                            startForeground(NOTIFICATION_ID, initialNotification)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "startForeground mediaProjection failed, falling back to standard FGS", e)
                        try {
                            startForeground(NOTIFICATION_ID, initialNotification)
                        } catch (fgsEx: Exception) {
                            Log.e(TAG, "Fatal startForeground error", fgsEx)
                        }
                    }

                    if (projection != null) {
                        mediaProjection = projection
                        startRecording(projection, width, height, density, recordAudio, fps, bitrate, prefer1080p)
                    } else {
                        Log.e(TAG, "Failed to acquire MediaProjection token")
                        ScreenRecorderEngine.updateState { it.copy(error = "Screen recording permission was not acquired.") }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    ScreenRecorderEngine.updateState { it.copy(error = "Recording permission was denied.") }
                    val notification = buildNotification(0)
                    try { startForeground(NOTIFICATION_ID, notification) } catch (_: Exception) {}
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                val notification = buildNotification(0)
                try { startForeground(NOTIFICATION_ID, notification) } catch (_: Exception) {}
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "In-App Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status during CodePad Studio video recording."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(seconds: Int): Notification {
        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mins = seconds / 60
        val secs = seconds % 60
        val timeStr = String.format(Locale.US, "%02d:%02d", mins, secs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording CodePad Studio ($timeStr)")
            .setContentText("Capturing high-definition video of your coding session.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startRecording(
        projection: MediaProjection,
        width: Int,
        height: Int,
        density: Int,
        recordAudio: Boolean,
        fps: Int,
        bitrate: Int,
        prefer1080p: Boolean
    ) {
        try {
            ScreenRecorderEngine.updateState { it.copy(isPreparing = true, error = null) }

            val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")

            // 1. Calculate sanitized dimensions: strictly divisible by 16 for H.264 profile stability
            val rawW = if (width > 0) width else 720
            val rawH = if (height > 0) height else 1280
            val aspectRatio = rawH.toFloat() / rawW.toFloat().coerceAtLeast(1f)

            var targetW = if (isEmulator || !prefer1080p) 720 else 1080
            var targetH = (targetW * aspectRatio).toInt()

            // Align to multiple of 16
            targetW = (targetW / 16) * 16
            targetH = (targetH / 16) * 16

            if (targetW < 320) targetW = 720
            if (targetH < 320) targetH = 1280

            // 2. Prepare output file in /storage/emulated/0/CodePad/ or /storage/emulated/0/Record/CodePad/
            val targetDir = resolveCodePadStorageDir(this)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "CodePad_Rec_$timeStamp.mp4"
            val directFile = File(targetDir, fileName)

            var activeOutputFile = directFile
            var useStaging = false

            try {
                if (!targetDir.exists()) targetDir.mkdirs()
                val probe = File(targetDir, ".probe_${System.currentTimeMillis()}")
                if (probe.createNewFile()) {
                    probe.delete()
                    activeOutputFile = directFile
                } else {
                    useStaging = true
                }
            } catch (_: Exception) {
                useStaging = true
            }

            if (useStaging) {
                val stagingDir = File(cacheDir, "recordings_staging").apply { mkdirs() }
                activeOutputFile = File(stagingDir, fileName)
                Log.d(TAG, "Using staging cache file for recording: ${activeOutputFile.absolutePath}")
            } else {
                Log.d(TAG, "Using direct destination file for recording: ${activeOutputFile.absolutePath}")
            }
            currentOutputFile = activeOutputFile
            val file = activeOutputFile

            // 3. Configure MediaRecorder safely with multi-tier fallback
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = recorder

            recorder.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                stopRecording()
            }

            var audioConfigured = false
            val shouldAttemptAudio = recordAudio && !isEmulator

            val prepareSuccess = runCatching {
                if (shouldAttemptAudio) {
                    try {
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                        audioConfigured = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Microphone audio not available on this device", e)
                        audioConfigured = false
                    }
                }
                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (audioConfigured) {
                    try {
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        recorder.setAudioEncodingBitRate(128_000)
                        recorder.setAudioSamplingRate(44100)
                    } catch (_: Exception) {
                        audioConfigured = false
                    }
                }
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                recorder.setVideoSize(targetW, targetH)
                recorder.setVideoFrameRate(fps.coerceIn(24, 60))
                recorder.setVideoEncodingBitRate(if (isEmulator) 2_000_000 else bitrate.coerceIn(1_500_000, 6_000_000))
                recorder.setOutputFile(file.absolutePath)
                recorder.prepare()
                true
            }.getOrElse { prepareEx ->
                Log.w(TAG, "Direct prepare failed: ${prepareEx.message}")
                false
            }

            if (!prepareSuccess) {
                Log.w(TAG, "Tier 1 prepare failed, attempting safe fallback with staging file...")
                try {
                    recorder.reset()
                } catch (_: Throwable) {}
                targetW = 720
                targetH = 1280
                audioConfigured = false
                val stagingDir = File(cacheDir, "recordings_staging").apply { mkdirs() }
                val fallbackFile = File(stagingDir, fileName)
                currentOutputFile = fallbackFile

                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                recorder.setVideoSize(720, 1280)
                recorder.setVideoFrameRate(30)
                recorder.setVideoEncodingBitRate(2_000_000)
                recorder.setOutputFile(fallbackFile.absolutePath)
                recorder.prepare()
            }

            // 4. CRITICAL FOR ANDROID 14+ (API 34+): Register MediaProjection.Callback before createVirtualDisplay
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system")
                    stopRecording()
                }
            }
            projectionCallback = callback
            val mainHandler = Handler(Looper.getMainLooper())
            projection.registerCallback(callback, mainHandler)

            // 5. Create VirtualDisplay piping to recorder surface
            virtualDisplay = projection.createVirtualDisplay(
                "CodePadScreenRecorder",
                targetW,
                targetH,
                density.coerceAtLeast(160),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                mainHandler
            )

            try {
                recorder.start()
            } catch (startEx: Exception) {
                Log.e(TAG, "recorder.start() failed", startEx)
                ScreenRecorderEngine.updateState {
                    it.copy(
                        isRecording = false,
                        isPreparing = false,
                        error = "Device encoder error: ${startEx.localizedMessage ?: "Cannot start video encoder"}"
                    )
                }
                cleanup()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            recordedSeconds = 0

            ScreenRecorderEngine.updateState {
                it.copy(
                    isRecording = true,
                    isPreparing = false,
                    durationSeconds = 0,
                    currentFilePath = file.absolutePath,
                    isAudioEnabled = audioConfigured,
                    targetFps = if (isEmulator) 30 else fps
                )
            }

            // 6. Start duration timer
            timerJob?.cancel()
            timerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000L)
                    recordedSeconds++
                    ScreenRecorderEngine.updateState { it.copy(durationSeconds = recordedSeconds) }
                    try {
                        val updatedNotification = buildNotification(recordedSeconds)
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(NOTIFICATION_ID, updatedNotification)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            ScreenRecorderEngine.updateState {
                it.copy(
                    isRecording = false,
                    isPreparing = false,
                    error = "Recording failed: ${e.localizedMessage ?: "Codec initialization error"}"
                )
            }
            cleanup()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        val duration = recordedSeconds
        val file = currentOutputFile

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        }

        cleanup()

        if (file != null && file.exists() && file.length() > 2048) {
            var finalFile = file
            val targetDir = resolveCodePadStorageDir(applicationContext)
            val preferredFile = File(targetDir, file.name)

            // Transfer to target /storage/emulated/0/CodePad/ or /storage/emulated/0/Record/CodePad/
            if (file.absolutePath != preferredFile.absolutePath) {
                try {
                    if (!targetDir.exists()) targetDir.mkdirs()
                    file.copyTo(preferredFile, overwrite = true)
                    file.delete()
                    finalFile = preferredFile
                    Log.i(TAG, "Successfully moved video to external folder: ${finalFile.absolutePath}")
                } catch (copyEx: Exception) {
                    Log.w(TAG, "Direct copy failed, saving via MediaStore: ${copyEx.message}")
                    val exported = exportVideoToMediaStore(applicationContext, file, file.name)
                    if (exported != null) {
                        finalFile = exported
                    }
                }
            }

            // Index file into Android MediaStore so it appears in device Gallery
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(finalFile.absolutePath),
                arrayOf("video/mp4")
            ) { _, uri ->
                val contentUri = uri ?: try {
                    FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.fileprovider",
                        finalFile
                    )
                } catch (_: Exception) {
                    Uri.fromFile(finalFile)
                }

                val savedInfo = SavedRecordingInfo(
                    filePath = finalFile.absolutePath,
                    fileUri = contentUri,
                    fileName = finalFile.name,
                    durationSeconds = duration,
                    fileSizeBytes = finalFile.length(),
                    width = 1080,
                    height = 1920
                )
                ScreenRecorderEngine.updateState {
                    it.copy(
                        isRecording = false,
                        isPreparing = false,
                        durationSeconds = 0,
                        currentFilePath = null,
                        lastSavedRecording = savedInfo
                    )
                }
            }
        } else {
            ScreenRecorderEngine.updateState {
                it.copy(
                    isRecording = false,
                    isPreparing = false,
                    durationSeconds = 0,
                    currentFilePath = null
                )
            }
        }
    }

    private fun exportVideoToMediaStore(context: Context, sourceFile: File, displayName: String): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CodePad")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                val publicFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "CodePad/$displayName"
                )
                sourceFile.delete()
                return publicFile
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting video to MediaStore", e)
            }
        }
        return null
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (_: Throwable) {}

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (_: Throwable) {}

        try {
            projectionCallback?.let { mediaProjection?.unregisterCallback(it) }
            projectionCallback = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }
}
