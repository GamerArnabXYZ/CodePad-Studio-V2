package com.arnix.codepad.studio.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnix.codepad.studio.engine.ScreenRecorderEngine
import com.arnix.codepad.studio.ui.components.InAppRecordingFloatingPill
import com.arnix.codepad.studio.ui.components.SavedRecordingSuccessDialog
import com.arnix.codepad.studio.ui.components.RecordingAudioSourceBottomSheet
import com.arnix.codepad.studio.ui.components.CustomAudioBgmSheet
import com.arnix.codepad.studio.ui.components.RecordingLayoutCustomizerSheet
import com.arnix.codepad.studio.ui.components.RecordingLayoutDivider
import com.arnix.codepad.studio.model.BgmConfig
import com.arnix.codepad.studio.model.RecordingLayoutConfig
import com.arnix.codepad.studio.model.RecordingLayoutMode
import com.arnix.codepad.studio.model.RecDividerStyle
import com.arnix.codepad.studio.model.RecRevealTransition
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.GraphicEq
import com.arnix.codepad.studio.model.RecordingAudioSource
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.engine.SyntaxHighlighter
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.ui.components.CreatorEndCtaAnimationOverlay
import com.arnix.codepad.studio.ui.components.CreatorWatermarkBadge
import com.arnix.codepad.studio.ui.components.NativeDomPreviewPanel
import com.arnix.codepad.studio.ui.components.RecordingCountdownOverlay
import com.arnix.codepad.studio.ui.components.ViralHookHeaderBanner
import com.arnix.codepad.studio.ui.theme.EditorColorPalette
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.LocalEditorPalette
import com.arnix.codepad.studio.ui.theme.uiSp
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import com.arnix.codepad.studio.viewmodel.CtaDisplayTiming
import com.arnix.codepad.studio.viewmodel.EndCtaPosition
import com.arnix.codepad.studio.viewmodel.EndCtaStyle
import com.arnix.codepad.studio.viewmodel.ShortsBackdropPreset
import com.arnix.codepad.studio.viewmodel.StudioModule
import com.arnix.codepad.studio.viewmodel.StudioUiState
import com.arnix.codepad.studio.viewmodel.StudioViewModel
import com.arnix.codepad.studio.viewmodel.ViralHookPosition
import com.arnix.codepad.studio.viewmodel.WatermarkPosition
import com.arnix.codepad.studio.viewmodel.WatermarkStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private val RealisticSlideEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/**
 * Module 2: Auto Write Studio — Realistic, Cinema-Grade Video Recording Engine for TikTok & YT Shorts.
 * - Instant per-character Live Output updates
 * - Multi-position & Multi-style Creator Handle Watermark (Top, Bottom, Footer, Center Overlay, Corner Overlay)
 * - Customizable Recording End Auto-Subscribe / Like / Comment Animation
 * - Realistic 680ms Horizontal Slide Transition (`index.html` → `style.css` → `script.js`)
 */
@Composable
fun AutoWriteStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val editorPalette = LocalEditorPalette.current
    val context = LocalContext.current
    val recorderState by ScreenRecorderEngine.state.collectAsStateWithLifecycle()

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    var pendingCaptureToken by remember { mutableStateOf<Pair<Int, Intent>?>(null) }
    var isCleanRecordRequested by remember { mutableStateOf(false) }
    var isAudioSourceSheetVisible by remember { mutableStateOf(false) }
    var isBgmSheetVisible by remember { mutableStateOf(false) }
    var isLayoutSheetVisible by remember { mutableStateOf(false) }

    val launchServiceWithToken: (Pair<Int, Intent>) -> Unit = { token ->
        val metrics = context.resources.displayMetrics
        ScreenRecorderEngine.startServiceRecording(
            context = context,
            resultCode = token.first,
            data = token.second,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            audioEnabled = uiState.recorderEnableAudio,
            audioSource = uiState.recordingAudioSource,
            fps60 = uiState.recorderPrefer60Fps,
            is1080p = uiState.recorderPrefer1080p
        )
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val token = Pair(result.resultCode, data)
            pendingCaptureToken = token

            if (isCleanRecordRequested) {
                isCleanRecordRequested = false
                viewModel.startCleanShortsRecording(
                    onLaunchInAppRecorder = {
                        launchServiceWithToken(token)
                    }
                )
            } else {
                launchServiceWithToken(token)
            }
        } else {
            isCleanRecordRequested = false
            Toast.makeText(context, "Screen recording was cancelled", Toast.LENGTH_SHORT).show()
            ScreenRecorderEngine.updateState { it.copy(isPreparing = false, isRecording = false) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        try {
            val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
            if (captureIntent != null) {
                screenCaptureLauncher.launch(captureIntent)
            }
        } catch (e: Exception) {
            ScreenRecorderEngine.updateState { it.copy(error = "Screen recorder unavailable: ${e.message}") }
        }
    }

    val startInAppRecording: () -> Unit = {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val needsAudio = uiState.recordingAudioSource != RecordingAudioSource.SILENT
        if (needsAudio) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            try {
                val intent = mediaProjectionManager?.createScreenCaptureIntent()
                if (intent != null) {
                    screenCaptureLauncher.launch(intent)
                } else {
                    Toast.makeText(context, "Screen recorder not available on this device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                ScreenRecorderEngine.updateState { it.copy(error = "Screen recorder unavailable: ${e.message}") }
            }
        }
    }

    LaunchedEffect(recorderState.error) {
        val err = recorderState.error
        if (!err.isNullOrBlank()) {
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            ScreenRecorderEngine.clearError()
        }
    }

    LaunchedEffect(recorderState.isRecording) {
        if (!recorderState.isRecording) {
            pendingCaptureToken = null
        }
    }

    // Auto-stop recording only when both code autowrite and the post-typing preview showcase have finished
    LaunchedEffect(uiState.autoWriteFinishedNonce) {
        if (uiState.autoWriteFinishedNonce > 0L && recorderState.isRecording && !uiState.isPreviewShowcaseActive) {
            delay(500L)
            ScreenRecorderEngine.stopServiceRecording(context)
            if (uiState.isRecordingMode) {
                viewModel.exitRecordingMode(context)
            }
        }
    }

    LaunchedEffect(uiState.isAutoWritePlaying, uiState.isPreviewShowcaseActive, recorderState.isRecording) {
        if (!uiState.isAutoWritePlaying &&
            !uiState.isPreviewShowcaseActive &&
            recorderState.isRecording &&
            uiState.typedCharCount > 0 &&
            uiState.autoWriteFullCode.isNotEmpty() &&
            uiState.typedCharCount >= uiState.autoWriteFullCode.length
        ) {
            val isFinalTab = !uiState.isAutoChainTabs || uiState.autoWriteTab == CodeTab.SCRIPT_JS ||
                (uiState.autoWriteTab == CodeTab.STYLE_CSS && uiState.jsCode.isBlank()) ||
                (uiState.autoWriteTab == CodeTab.INDEX_HTML && uiState.cssCode.isBlank() && uiState.jsCode.isBlank())

            if (isFinalTab && uiState.autoWriteFinishedNonce > 0L) {
                delay(500L)
                if (recorderState.isRecording) {
                    ScreenRecorderEngine.stopServiceRecording(context)
                    if (uiState.isRecordingMode) {
                        viewModel.exitRecordingMode(context)
                    }
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        viewModel.onAutoWriteFinished = {
            if (ScreenRecorderEngine.state.value.isRecording) {
                coroutineScope.launch {
                    delay(500L)
                    ScreenRecorderEngine.stopServiceRecording(context)
                    if (viewModel.uiState.value.isRecordingMode) {
                        viewModel.exitRecordingMode(context)
                    }
                }
            }
        }
        onDispose {
            viewModel.onAutoWriteFinished = null
        }
    }

    val fontSizeSp = uiState.autoWriteFontSize
    val lineHeightSp = fontSizeSp * 1.45f

    BackHandler {
        when {
            uiState.isRecordingMode -> viewModel.exitRecordingMode(context)
            uiState.isFloatingPreviewOpen -> viewModel.handleDoubleTapGesture()
            else -> viewModel.selectModule(StudioModule.CODEPAD_IDE)
        }
    }

    val typedCountByTab = remember {
        mutableMapOf(
            CodeTab.INDEX_HTML to 0,
            CodeTab.STYLE_CSS to 0,
            CodeTab.SCRIPT_JS to 0
        )
    }
    if (uiState.autoWriteTab == CodeTab.INDEX_HTML && uiState.typedCharCount == 0) {
        typedCountByTab[CodeTab.STYLE_CSS] = 0
        typedCountByTab[CodeTab.SCRIPT_JS] = 0
    }
    typedCountByTab[uiState.autoWriteTab] = uiState.typedCharCount

    val isActivelyTyping = uiState.isAutoWritePlaying &&
        uiState.typedCharCount > 0 &&
        uiState.typedCharCount < uiState.autoWriteFullCode.length

    val sharedCodeTextStyle = remember(fontSizeSp, editorPalette.plainText) {
        TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = lineHeightSp.sp,
            color = editorPalette.plainText,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    }

    val backdropPreset = remember(uiState.backdropPresetIndex) {
        ShortsBackdropPreset.fromIndex(uiState.backdropPresetIndex)
    }

    val stageModifier = if (backdropPreset == ShortsBackdropPreset.CLEAN_M3) {
        Modifier.background(MaterialTheme.colorScheme.background)
    } else {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(backdropPreset.startColorHex),
                    Color(backdropPreset.endColorHex)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(stageModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (uiState.isRecordingMode) 0.dp else 10.dp,
                    vertical = if (uiState.isRecordingMode) 0.dp else 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (uiState.isRecordingMode) 0.dp else 8.dp)
        ) {
            // Optional Zero-Editing Viral Hook Title Banner at Top Stage (Above Code Window) - hidden in clean recording mode
            if (!uiState.isRecordingMode &&
                uiState.showViralHookBanner &&
                uiState.viralHookTitle.isNotBlank() &&
                uiState.viralHookPosition == ViralHookPosition.TOP_STAGE
            ) {
                ViralHookHeaderBanner(
                    badgeText = uiState.viralHookBadge,
                    titleText = uiState.viralHookTitle,
                    style = uiState.viralHookStyle,
                    alignment = uiState.viralHookAlignment,
                    transparentBg = uiState.viralHookTransparentBg,
                    showBorder = uiState.viralHookShowBorder,
                    showBadge = uiState.viralHookShowBadge,
                    accentIndex = uiState.viralHookAccentIndex,
                    sizeScale = uiState.viralHookSizeScale,
                    onClick = {}
                )
            }

            // Optional Top Stage Creator Handle Banner (Top Stage Placement) - hidden in clean recording mode
            if (!uiState.isRecordingMode && uiState.watermarkPosition == WatermarkPosition.TOP_STAGE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CreatorWatermarkBadge(
                        handle = uiState.creatorHandle,
                        style = uiState.watermarkStyle,
                        isOverlay = false,
                        onClick = {},
                        opacity = uiState.watermarkOpacity,
                        sizeScale = uiState.watermarkSizeScale
                    )
                }
            }

            val layoutConfig = uiState.recordingLayoutConfig
            val stageCornerRadius = if (uiState.isRecordingMode) RoundedCornerShape(layoutConfig.cornerRadiusDp.dp) else RoundedCornerShape(16.dp)

            // Reusable pane for Code Editor Window
            val renderEditorPane: @Composable (Modifier) -> Unit = { paneModifier ->
                ElevatedCard(
                    modifier = paneModifier.fillMaxWidth(),
                    shape = stageCornerRadius,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (uiState.isRecordingMode) 0.dp else 6.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = editorPalette.editorBackground
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        StudioCodeWindowHeader(
                            projectName = uiState.projectName,
                            activeTab = uiState.autoWriteTab,
                            progress = uiState.autoWriteProgress,
                            isAutoChain = uiState.isAutoChainTabs,
                            isRecordingMode = uiState.isRecordingMode,
                            layoutMode = layoutConfig.layoutMode,
                            onOpenLayoutSheet = { isLayoutSheetVisible = true },
                            onSelectTab = { viewModel.selectAutoWriteTab(it) },
                            onExitRecordingMode = { viewModel.exitRecordingMode() }
                        )

                    SmoothAutoWriteProgressBar(
                        targetProgress = uiState.autoWriteProgress,
                        trackColor = editorPalette.divider
                    )

                    // Outer Gesture Box + Realistic 680ms Slide Transition Viewport + Overlays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(editorPalette.editorBackground)
                            .pointerInput(Unit) {
                                var tapCount = 0
                                var tapDispatchJob: Job? = null
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downPos = down.position
                                    val touchSlop = viewConfiguration.touchSlop
                                    var draggedBeyondSlop = false
                                    var upCaptured = false

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Final)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if ((change.position - downPos).getDistance() > touchSlop || change.isConsumed) {
                                            draggedBeyondSlop = true
                                            break
                                        }
                                        if (!change.pressed) {
                                            upCaptured = true
                                            break
                                        }
                                    }

                                    if (draggedBeyondSlop) {
                                        tapDispatchJob?.cancel()
                                        tapCount = 0
                                    } else if (upCaptured) {
                                        tapCount++
                                        tapDispatchJob?.cancel()
                                        val capturedCount = tapCount
                                        tapDispatchJob = coroutineScope.launch {
                                            delay(260L)
                                            when (capturedCount) {
                                                1 -> viewModel.handleSingleTapGesture()
                                                2 -> viewModel.handleDoubleTapGesture()
                                                else -> viewModel.handleTripleTapGesture(context)
                                            }
                                            tapCount = 0
                                        }
                                    }
                                }
                            }
                            .testTag("autowrite_gesture_surface")
                    ) {
                        AnimatedContent(
                            targetState = uiState.autoWriteTab,
                            transitionSpec = {
                                val isForward = targetState.index >= initialState.index
                                if (isForward) {
                                    (slideInHorizontally(
                                        animationSpec = tween(
                                            durationMillis = 680,
                                            easing = RealisticSlideEasing
                                        ),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    ) + scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(
                                            durationMillis = 680,
                                            easing = RealisticSlideEasing
                                        )
                                    ) + fadeIn(
                                        animationSpec = tween(durationMillis = 480)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(
                                                durationMillis = 680,
                                                easing = RealisticSlideEasing
                                            ),
                                            targetOffsetX = { fullWidth -> -(fullWidth * 0.72f).roundToInt() }
                                        ) + scaleOut(
                                            targetScale = 0.94f,
                                            animationSpec = tween(
                                                durationMillis = 680,
                                                easing = RealisticSlideEasing
                                            )
                                        ) + fadeOut(
                                            animationSpec = tween(durationMillis = 520)
                                        )
                                    )
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = tween(
                                            durationMillis = 680,
                                            easing = RealisticSlideEasing
                                        ),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    ) + scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(
                                            durationMillis = 680,
                                            easing = RealisticSlideEasing
                                        )
                                    ) + fadeIn(
                                        animationSpec = tween(durationMillis = 480)
                                    )).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(
                                                durationMillis = 680,
                                                easing = RealisticSlideEasing
                                            ),
                                            targetOffsetX = { fullWidth -> (fullWidth * 0.72f).roundToInt() }
                                        ) + scaleOut(
                                            targetScale = 0.94f,
                                            animationSpec = tween(
                                                durationMillis = 680,
                                                easing = RealisticSlideEasing
                                            )
                                        ) + fadeOut(
                                            animationSpec = tween(durationMillis = 520)
                                        )
                                    )
                                }
                            },
                            label = "autoWriteTabSlideTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { tabForPane ->
                            val isCurrentTabPane = tabForPane == uiState.autoWriteTab
                            val paneScrollState = rememberScrollState()

                            val fullTabCode = when (tabForPane) {
                                CodeTab.INDEX_HTML -> uiState.htmlCode
                                CodeTab.STYLE_CSS -> uiState.cssCode
                                CodeTab.SCRIPT_JS -> uiState.jsCode
                            }

                            val effectiveTypedCount = if (isCurrentTabPane) {
                                uiState.typedCharCount
                            } else {
                                (typedCountByTab[tabForPane] ?: fullTabCode.length).coerceAtLeast(
                                    if (tabForPane.index < uiState.autoWriteTab.index) fullTabCode.length else 0
                                )
                            }

                            LaunchedEffect(isCurrentTabPane, effectiveTypedCount == 0) {
                                if (isCurrentTabPane && effectiveTypedCount == 0) {
                                    if (paneScrollState.value > 0) {
                                        paneScrollState.dispatchRawDelta(-paneScrollState.value.toFloat())
                                    }
                                    paneScrollState.scrollTo(0)
                                }
                            }

                            LaunchedEffect(isCurrentTabPane, uiState.isAutoWritePlaying) {
                                if (!isCurrentTabPane || !uiState.isAutoWritePlaying) return@LaunchedEffect
                                while (true) {
                                    if (!paneScrollState.isScrollInProgress) {
                                        val target = paneScrollState.maxValue
                                        val current = paneScrollState.value
                                        val diff = target - current
                                        if (diff > 0) {
                                            withFrameNanos { }
                                            val step = max(2.5f, diff * 0.28f).coerceAtMost(diff.toFloat())
                                            paneScrollState.dispatchRawDelta(step)
                                        } else {
                                            delay(32L)
                                        }
                                    } else {
                                        delay(32L)
                                    }
                                }
                            }

                            LaunchedEffect(isCurrentTabPane, uiState.isFloatingPreviewOpen) {
                                if (isCurrentTabPane && uiState.isFloatingPreviewOpen) {
                                    repeat(16) {
                                        delay(18L)
                                        val diff = paneScrollState.maxValue - paneScrollState.value
                                        if (!paneScrollState.isScrollInProgress && diff > 0) {
                                            paneScrollState.dispatchRawDelta(diff.toFloat())
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(isCurrentTabPane, uiState.previewSectionHeightDp) {
                                val diff = paneScrollState.maxValue - paneScrollState.value
                                if (isCurrentTabPane &&
                                    uiState.isFloatingPreviewOpen &&
                                    !paneScrollState.isScrollInProgress &&
                                    diff > 0
                                ) {
                                    paneScrollState.dispatchRawDelta(diff.toFloat())
                                }
                            }

                            // O(1) Precomputed Syntax-Highlighted Lines for Large Codebases
                            val precomputedCode = remember(fullTabCode, tabForPane, editorPalette) {
                                SyntaxHighlighter.precomputeLines(
                                    code = fullTabCode,
                                    tab = tabForPane,
                                    palette = editorPalette
                                )
                            }

                            val (completedLines, activeLineText) = remember(precomputedCode, effectiveTypedCount) {
                                precomputedCode.sliceAt(effectiveTypedCount)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(editorPalette.editorBackground)
                                    .border(
                                        width = 0.5.dp,
                                        color = editorPalette.divider.copy(alpha = 0.55f)
                                    )
                                    .drawBehind {
                                        if (uiState.autoWriteShowGutter) {
                                            val gutterWidthPx = 42.dp.toPx()
                                            val dividerWidthPx = 1.dp.toPx()
                                            drawRect(
                                                color = editorPalette.gutterBackground,
                                                topLeft = Offset.Zero,
                                                size = Size(width = gutterWidthPx, height = size.height)
                                            )
                                            drawRect(
                                                color = editorPalette.divider,
                                                topLeft = Offset(x = gutterWidthPx, y = 0f),
                                                size = Size(width = dividerWidthPx, height = size.height)
                                            )
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(paneScrollState)
                                        .padding(top = 10.dp, bottom = 52.dp)
                                ) {
                                    // Completed lines have 100% immutable inputs and skip recomposition in O(1)
                                    completedLines.forEachIndexed { idx, lineText ->
                                        key(idx) {
                                            CompletedAutoWriteCodeLineRow(
                                                lineNum = idx + 1,
                                                lineText = lineText,
                                                showGutter = uiState.autoWriteShowGutter,
                                                sharedCodeTextStyle = sharedCodeTextStyle,
                                                editorPalette = editorPalette
                                            )
                                        }
                                    }

                                    // Only the single active line at the bottom recomposes during typing / cursor blink
                                    val activeLineNumber = completedLines.size + 1
                                    key(activeLineNumber) {
                                        ActiveAutoWriteCodeLineRow(
                                            lineNum = activeLineNumber,
                                            lineText = activeLineText,
                                            isCurrentTabPane = isCurrentTabPane,
                                            isActivelyTyping = isActivelyTyping && isCurrentTabPane,
                                            showGutter = uiState.autoWriteShowGutter,
                                            sharedCodeTextStyle = sharedCodeTextStyle,
                                            editorPalette = editorPalette
                                        )
                                    }
                                }
                            }
                        }

                        // Screen Center Overlay Watermark ("Screen par overlay jaisa")
                        if (uiState.watermarkPosition == WatermarkPosition.CENTER_OVERLAY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CreatorWatermarkBadge(
                                    handle = uiState.creatorHandle,
                                    style = uiState.watermarkStyle,
                                    isOverlay = true,
                                    onClick = {},
                                    opacity = uiState.watermarkOpacity,
                                    sizeScale = uiState.watermarkSizeScale
                                )
                            }
                        }

                        // Screen Corner Overlay Watermark (Bottom-Right)
                        if (uiState.watermarkPosition == WatermarkPosition.CORNER_OVERLAY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 14.dp, bottom = 14.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                CreatorWatermarkBadge(
                                    handle = uiState.creatorHandle,
                                    style = uiState.watermarkStyle,
                                    isOverlay = false,
                                    onClick = {},
                                    opacity = uiState.watermarkOpacity,
                                    sizeScale = uiState.watermarkSizeScale
                                )
                            }
                        }

                        // Screen Top-Right Overlay Watermark
                        if (uiState.watermarkPosition == WatermarkPosition.TOP_RIGHT_OVERLAY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 14.dp, top = 12.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                CreatorWatermarkBadge(
                                    handle = uiState.creatorHandle,
                                    style = uiState.watermarkStyle,
                                    isOverlay = false,
                                    onClick = {},
                                    opacity = uiState.watermarkOpacity,
                                    sizeScale = uiState.watermarkSizeScale
                                )
                            }
                        }

                        // Optional Viral Hook Banner inside Code Window (Top or Bottom Overlay)
                        if (uiState.showViralHookBanner &&
                            uiState.viralHookTitle.isNotBlank() &&
                            (uiState.viralHookPosition == ViralHookPosition.CODE_TOP_OVERLAY ||
                                uiState.viralHookPosition == ViralHookPosition.CODE_BOTTOM_OVERLAY)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(
                                        if (uiState.viralHookPosition == ViralHookPosition.CODE_TOP_OVERLAY) {
                                            Alignment.TopCenter
                                        } else {
                                            Alignment.BottomCenter
                                        }
                                    )
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                ViralHookHeaderBanner(
                                    badgeText = uiState.viralHookBadge,
                                    titleText = uiState.viralHookTitle,
                                    style = uiState.viralHookStyle,
                                    alignment = uiState.viralHookAlignment,
                                    transparentBg = uiState.viralHookTransparentBg,
                                    showBorder = uiState.viralHookShowBorder,
                                    showBadge = uiState.viralHookShowBadge,
                                    accentIndex = uiState.viralHookAccentIndex,
                                    sizeScale = uiState.viralHookSizeScale,
                                    onClick = {}
                                )
                            }
                        }

                        // Customizable Recording Like ❤️ • Comment 💬 • Subscribe 🔔 Overlay inside Code Window
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.shouldDisplayCtaOverlay &&
                                uiState.endCtaPosition != EndCtaPosition.STAGE_BOTTOM_BAR,
                            enter = fadeIn(tween(260)) + scaleIn(
                                initialScale = 0.88f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ),
                            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.9f),
                            modifier = Modifier
                                .align(
                                    when {
                                        uiState.endCtaStyle == EndCtaStyle.SIDE_RAIL ||
                                            uiState.endCtaPosition == EndCtaPosition.SIDE_RIGHT_OVERLAY -> Alignment.CenterEnd
                                        uiState.endCtaPosition == EndCtaPosition.CODE_TOP_OVERLAY -> Alignment.TopCenter
                                        uiState.endCtaPosition == EndCtaPosition.CODE_CENTER_OVERLAY -> Alignment.Center
                                        else -> Alignment.BottomCenter
                                    }
                                )
                                .padding(12.dp)
                        ) {
                            CreatorEndCtaAnimationOverlay(
                                creatorHandle = uiState.creatorHandle,
                                ctaMessage = uiState.endCtaMessage,
                                style = uiState.endCtaStyle,
                                transparentBg = uiState.ctaTransparentBg,
                                showBorder = uiState.ctaShowBorder,
                                showHandle = uiState.ctaShowHandle,
                                accentIndex = uiState.ctaAccentIndex,
                                sizeScale = uiState.ctaSizeScale,
                                showLike = uiState.ctaShowLike,
                                showComment = uiState.ctaShowComment,
                                showSubscribe = uiState.ctaShowSubscribe,
                                likeLabel = uiState.ctaLikeLabel,
                                commentLabel = uiState.ctaCommentLabel,
                                subscribeLabel = uiState.ctaSubscribeLabel,
                                subscribedLabel = uiState.ctaSubscribedLabel,
                                loopAnimation = uiState.ctaDisplayTiming == CtaDisplayTiming.ALWAYS_VISIBLE,
                                onDismiss = {
                                    if (uiState.isRecordingMode) {
                                        viewModel.dismissEndCta()
                                    } else {
                                        viewModel.setCreatorCustomizeOpen(true)
                                    }
                                }
                            )
                        }

                        // Sleek 3-2-1 Recording Pre-Roll Countdown Overlay + Start Delay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.recordingCountdownSeconds > 0 || uiState.isRecordingStartingDelay,
                            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
                            exit = fadeOut(tween(120)) + scaleOut(targetScale = 1.08f),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            RecordingCountdownOverlay(
                                secondsLeft = uiState.recordingCountdownSeconds,
                                isStartingDelay = uiState.isRecordingStartingDelay,
                                onSkipNow = { viewModel.skipRecordingCountdownNow() }
                            )
                        }
                    }

                    HorizontalDivider(color = editorPalette.divider)

                    val metrics = uiState.sequenceMetrics

                    StudioWindowWatermarkFooter(
                        creatorHandle = uiState.creatorHandle,
                        showHandleInFooter = uiState.watermarkPosition == WatermarkPosition.FOOTER_BAR,
                        watermarkStyle = uiState.watermarkStyle,
                        activeTab = uiState.autoWriteTab,
                        currentLine = uiState.autoWriteCurrentLine,
                        typedChars = uiState.typedCharCount,
                        totalChars = uiState.autoWriteFullCode.length,
                        speedLevel = uiState.speedLevel,
                        isTurboMode = uiState.isTurboMode,
                        autoDurationTarget = uiState.autoDurationTarget,
                        estimatedSeconds = metrics.estimatedTotalSeconds,
                        isPlaying = uiState.isAutoWritePlaying
                    )
                }
            }
        }

        // Reusable pane for Live Web DOM Preview Window
        val renderPreviewPane: @Composable (Modifier) -> Unit = { paneModifier ->
            ElevatedCard(
                modifier = paneModifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var tapCount = 0
                        var tapDispatchJob: Job? = null
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            val touchSlop = viewConfiguration.touchSlop
                            var draggedBeyondSlop = false
                            var upCaptured = false

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if ((change.position - downPos).getDistance() > touchSlop || change.isConsumed) {
                                    draggedBeyondSlop = true
                                    break
                                }
                                if (!change.pressed) {
                                    upCaptured = true
                                    break
                                }
                            }

                            if (draggedBeyondSlop) {
                                tapDispatchJob?.cancel()
                                tapCount = 0
                            } else if (upCaptured) {
                                tapCount++
                                tapDispatchJob?.cancel()
                                val capturedCount = tapCount
                                tapDispatchJob = coroutineScope.launch {
                                    delay(260L)
                                    when (capturedCount) {
                                        2 -> viewModel.handleDoubleTapGesture()
                                        3 -> viewModel.handleTripleTapGesture(context)
                                    }
                                    tapCount = 0
                                }
                            }
                        }
                    }
                    .testTag("floating_live_preview_window"),
                shape = stageCornerRadius,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (uiState.isRecordingMode) 0.dp else 6.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NativeDomPreviewPanel(
                        domNodes = uiState.autoWriteDomNodes,
                        bodyStyle = uiState.autoWriteBodyStyle,
                        onDomButtonClick = { fn -> viewModel.onNativeDomButtonClicked(fn) },
                        onRefreshRunJs = { viewModel.forceReloadPreview() },
                        htmlCode = uiState.autoWriteEffectiveHtml,
                        cssCode = uiState.autoWriteEffectiveCss,
                        jsCode = uiState.autoWriteEffectiveJs,
                        runJs = !uiState.isAutoWritePlaying || uiState.autoWriteEffectiveJs.isNotBlank() || uiState.isPreviewShowcaseActive,
                        isProgressiveAutoWrite = uiState.isAutoWritePlaying,
                        previewScaleMode = uiState.previewScaleMode,
                        previewReloadNonce = uiState.previewReloadNonce,
                        onCycleScaleMode = { viewModel.cyclePreviewScaleMode() },
                        onToggleExpandHeight = { viewModel.togglePreviewExpandedHeight() },
                        onWebConsoleLog = { level, msg, src, stack ->
                            viewModel.onWebConsoleMessage(level, msg, src, stack)
                        },
                        onWebConsoleClear = { viewModel.onWebConsoleClear() },
                        onClose = if (uiState.isRecordingMode) null else ({ viewModel.handleDoubleTapGesture() }),
                        isRecordingMode = uiState.isRecordingMode,
                        autoScrollPreview = uiState.isAutoWritePlaying,
                        maxBodyHeight = if (uiState.isRecordingMode) 4000.dp else uiState.previewSectionHeightDp.dp,
                        onResizeDeltaDp = { deltaDp ->
                            viewModel.adjustPreviewSectionHeight(deltaDp)
                        }
                    )

                    if (uiState.isPreviewShowcaseActive && uiState.recordingLayoutConfig.showPreviewShowcaseBadge) {
                        PreviewShowcaseBadge(
                            remainingSeconds = uiState.previewShowcaseRemainingSec,
                            totalSeconds = uiState.previewShowcaseTotalSec,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        )
                    }
                }
            }
        }

        // 4 Built-In Recording Layouts (Ultra Customizable: Editor Only, Split Top/Bottom, Preview Top, End Fullscreen Reveal)
        if (uiState.isRecordingMode) {
            when (layoutConfig.layoutMode) {
                RecordingLayoutMode.EDITOR_ONLY -> {
                    renderEditorPane(Modifier.weight(1f))
                }
                RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM -> {
                    renderEditorPane(Modifier.weight(layoutConfig.editorWeight))
                    RecordingLayoutDivider(style = layoutConfig.dividerStyle)
                    renderPreviewPane(Modifier.weight(layoutConfig.previewWeight))
                }
                RecordingLayoutMode.PREVIEW_TOP_EDITOR_BOTTOM -> {
                    renderPreviewPane(Modifier.weight(layoutConfig.previewWeight))
                    RecordingLayoutDivider(style = layoutConfig.dividerStyle)
                    renderEditorPane(Modifier.weight(layoutConfig.editorWeight))
                }
                RecordingLayoutMode.EDITOR_THEN_FULL_PREVIEW_AT_END -> {
                    if (uiState.isEndFullscreenPreviewRevealed) {
                        AnimatedVisibility(
                            visible = true,
                            enter = when (layoutConfig.revealTransition) {
                                RecRevealTransition.SMOOTH_CROSSFADE -> fadeIn(tween(400))
                                RecRevealTransition.SLIDE_UP -> slideInVertically(tween(420, easing = RealisticSlideEasing)) { it } + fadeIn()
                                RecRevealTransition.EXPAND_AND_SCALE -> expandVertically(tween(450, easing = RealisticSlideEasing)) + fadeIn(tween(300))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            renderPreviewPane(Modifier.fillMaxSize())
                        }
                    } else {
                        renderEditorPane(Modifier.weight(1f))
                    }
                }
            }
        } else {
            // Non-recording mode: respects layout mode if preview top or standard editor top
            if (layoutConfig.layoutMode == RecordingLayoutMode.PREVIEW_TOP_EDITOR_BOTTOM && uiState.isFloatingPreviewOpen) {
                AnimatedVisibility(
                    visible = uiState.isFloatingPreviewOpen,
                    enter = expandVertically(animationSpec = tween(260, easing = RealisticSlideEasing), expandFrom = Alignment.Top) + fadeIn(tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220, easing = RealisticSlideEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(160))
                ) {
                    renderPreviewPane(Modifier.fillMaxWidth())
                }
                if (uiState.isFloatingPreviewOpen) {
                    RecordingLayoutDivider(style = layoutConfig.dividerStyle)
                }
                renderEditorPane(Modifier.weight(1f))
            } else {
                renderEditorPane(Modifier.weight(if (uiState.isFloatingPreviewOpen) 0.52f else 1f))
                if (uiState.isFloatingPreviewOpen) {
                    RecordingLayoutDivider(style = layoutConfig.dividerStyle)
                }
                AnimatedVisibility(
                    visible = uiState.isFloatingPreviewOpen,
                    enter = expandVertically(animationSpec = tween(260, easing = RealisticSlideEasing), expandFrom = Alignment.Top) + fadeIn(tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220, easing = RealisticSlideEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(160))
                ) {
                    renderPreviewPane(Modifier.fillMaxWidth())
                }
            }
        }

            // Optional Viral Hook Banner at Bottom Stage (Below Code Window) - hidden in clean recording mode
            if (!uiState.isRecordingMode &&
                uiState.showViralHookBanner &&
                uiState.viralHookTitle.isNotBlank() &&
                uiState.viralHookPosition == ViralHookPosition.BOTTOM_STAGE
            ) {
                ViralHookHeaderBanner(
                    badgeText = uiState.viralHookBadge,
                    titleText = uiState.viralHookTitle,
                    style = uiState.viralHookStyle,
                    alignment = uiState.viralHookAlignment,
                    transparentBg = uiState.viralHookTransparentBg,
                    showBorder = uiState.viralHookShowBorder,
                    showBadge = uiState.viralHookShowBadge,
                    accentIndex = uiState.viralHookAccentIndex,
                    sizeScale = uiState.viralHookSizeScale,
                    onClick = {}
                )
            }

            // Optional Dedicated Stage Bottom Like • Comment • Subscribe Bar (Below Code Window) - hidden in clean recording mode
            AnimatedVisibility(
                visible = !uiState.isRecordingMode &&
                    uiState.shouldDisplayCtaOverlay &&
                    uiState.endCtaPosition == EndCtaPosition.STAGE_BOTTOM_BAR,
                enter = expandVertically() + fadeIn(tween(220)),
                exit = shrinkVertically() + fadeOut(tween(160))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CreatorEndCtaAnimationOverlay(
                        creatorHandle = uiState.creatorHandle,
                        ctaMessage = uiState.endCtaMessage,
                        style = uiState.endCtaStyle,
                        transparentBg = uiState.ctaTransparentBg,
                        showBorder = uiState.ctaShowBorder,
                        showHandle = uiState.ctaShowHandle,
                        accentIndex = uiState.ctaAccentIndex,
                        sizeScale = uiState.ctaSizeScale,
                        showLike = uiState.ctaShowLike,
                        showComment = uiState.ctaShowComment,
                        showSubscribe = uiState.ctaShowSubscribe,
                        likeLabel = uiState.ctaLikeLabel,
                        commentLabel = uiState.ctaCommentLabel,
                        subscribeLabel = uiState.ctaSubscribeLabel,
                        subscribedLabel = uiState.ctaSubscribedLabel,
                        loopAnimation = uiState.ctaDisplayTiming == CtaDisplayTiming.ALWAYS_VISIBLE,
                        onDismiss = {
                            if (uiState.isRecordingMode) {
                                viewModel.dismissEndCta()
                            }
                        }
                    )
                }
            }

            // Optional Bottom Stage Creator Handle Banner (Bottom Stage Placement) - hidden in clean recording mode
            if (!uiState.isRecordingMode && uiState.watermarkPosition == WatermarkPosition.BOTTOM_STAGE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CreatorWatermarkBadge(
                        handle = uiState.creatorHandle,
                        style = uiState.watermarkStyle,
                        isOverlay = false,
                        onClick = {},
                        opacity = uiState.watermarkOpacity,
                        sizeScale = uiState.watermarkSizeScale
                    )
                }
            }

            // 3. Creator Control Deck (Hidden in Clean Recording Mode)
            AnimatedVisibility(
                visible = !uiState.isRecordingMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val metrics = uiState.sequenceMetrics
                CreatorControlDeck(
                    isPlaying = uiState.isAutoWritePlaying,
                    speedLevel = uiState.speedLevel,
                    isTurboMode = uiState.isTurboMode,
                    autoDurationTarget = uiState.autoDurationTarget,
                    typingSoundProfile = uiState.typingSoundProfile,
                    recordingAudioSource = uiState.recordingAudioSource,
                    estimatedSeconds = metrics.estimatedTotalSeconds,
                    isFloatingPreviewOpen = uiState.isFloatingPreviewOpen,
                    recordingLayoutConfig = uiState.recordingLayoutConfig,
                    onOpenLayoutSheet = { isLayoutSheetVisible = true },
                    onSelectDurationTarget = { viewModel.selectAutoDurationTarget(it) },
                    onTogglePlayPause = { viewModel.handleSingleTapGesture() },
                    onStartCleanRecord = {
                        if (!recorderState.isRecording) {
                            isCleanRecordRequested = true
                            startInAppRecording()
                        }
                    },
                    onRestart = { viewModel.restartAutoWrite() },
                    onSkipNearEnd = { viewModel.skipNearCompletionForDemo() },
                    onSpeedChange = { viewModel.setSpeedLevel(it) },
                    onToggleTurbo = { viewModel.toggleTurboMode() },
                    onCycleTypingSound = { viewModel.cycleTypingSoundProfile() },
                    onOpenAudioSourceSheet = { isAudioSourceSheetVisible = true },
                    bgmConfig = uiState.bgmConfig,
                    onOpenBgmSheet = { isBgmSheetVisible = true },
                    onToggleFloatingPreview = { viewModel.handleDoubleTapGesture() },
                    isScreenRecording = recorderState.isRecording,
                    onToggleInAppRecord = {
                        if (recorderState.isRecording) {
                            ScreenRecorderEngine.stopServiceRecording(context)
                        } else {
                            isCleanRecordRequested = false
                            startInAppRecording()
                        }
                    }
                )
            }
        }

        // Floating Live In-App Screen Recording Pill (ONLY visible when NOT in Clean Recording Mode, so rec timer never appears on video)
        if (!uiState.isRecordingMode && recorderState.isRecording) {
            InAppRecordingFloatingPill(
                durationSeconds = recorderState.durationSeconds,
                isRecording = recorderState.isRecording,
                onStopClick = { ScreenRecorderEngine.stopServiceRecording(context) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )
        }

        // Post-Recording Success Dialog with Play and Share Actions
        if (recorderState.lastSavedRecording != null) {
            SavedRecordingSuccessDialog(
                recordingInfo = recorderState.lastSavedRecording,
                onPlay = { uri -> ScreenRecorderEngine.playRecordedVideo(context, uri) },
                onShare = { uri, name -> ScreenRecorderEngine.shareRecordedVideo(context, uri, name) },
                onDismiss = { ScreenRecorderEngine.clearLastSavedRecording() }
            )
        }

        // Sound Recording Options Bottom Sheet (System Only / Mic Only / Both / Silent)
        if (isAudioSourceSheetVisible) {
            RecordingAudioSourceBottomSheet(
                selectedSource = uiState.recordingAudioSource,
                onSelectSource = { source ->
                    viewModel.setRecordingAudioSource(source)
                },
                onDismiss = { isAudioSourceSheetVisible = false },
                onOpenCustomBgm = { isBgmSheetVisible = true }
            )
        }

        // Custom Background Music & Audio Timing Customization Sheet (15s, 20s, 30s, etc.)
        if (isBgmSheetVisible) {
            CustomAudioBgmSheet(
                bgmConfig = uiState.bgmConfig,
                activeDurationTarget = uiState.autoDurationTarget,
                onUpdateConfig = { updated ->
                    viewModel.updateBgmConfig(updated)
                },
                onImportAudio = { uri, name ->
                    viewModel.importCustomBgmFile(context, uri, name)
                },
                onDismiss = { isBgmSheetVisible = false }
            )
        }

        // Recording Screen Layouts Sheet (Editor Only, Split Top/Bottom, Preview Top, End Fullscreen Reveal)
        if (isLayoutSheetVisible || uiState.isRecordingLayoutSheetOpen) {
            RecordingLayoutCustomizerSheet(
                config = uiState.recordingLayoutConfig,
                onUpdateConfig = { updated ->
                    viewModel.updateRecordingLayoutConfig(updated)
                },
                onDismiss = {
                    isLayoutSheetVisible = false
                    viewModel.setRecordingLayoutSheetOpen(false)
                }
            )
        }
    }
}

/**
 * Floating badge displayed during the post-typing preview showcase period.
 * Shows viewers and creator how many seconds the live output preview is being showcased.
 */
@Composable
private fun PreviewShowcaseBadge(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 8.dp,
        modifier = modifier.testTag("preview_showcase_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Text(
                text = "LIVE OUTPUT SHOWCASE",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.uiSp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "${remainingSeconds}s",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.uiSp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                )
            }
        }
    }
}

@Composable
private fun SmoothAutoWriteProgressBar(
    targetProgress: Float,
    trackColor: Color
) {
    val smoothProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "smoothProgress"
    )
    LinearProgressIndicator(
        progress = { smoothProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = trackColor
    )
}

@Composable
private fun CompletedAutoWriteCodeLineRow(
    lineNum: Int,
    lineText: AnnotatedString,
    showGutter: Boolean,
    sharedCodeTextStyle: TextStyle,
    editorPalette: EditorColorPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (showGutter) {
            Text(
                text = lineNum.toString(),
                style = sharedCodeTextStyle.copy(
                    fontWeight = FontWeight.Normal,
                    color = editorPalette.gutterText,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .width(42.dp)
                    .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }

        Text(
            text = lineText,
            style = sharedCodeTextStyle,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (showGutter) 11.dp else 14.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
        )
    }
}

@Composable
private fun ActiveAutoWriteCodeLineRow(
    lineNum: Int,
    lineText: AnnotatedString,
    isCurrentTabPane: Boolean,
    isActivelyTyping: Boolean,
    showGutter: Boolean,
    sharedCodeTextStyle: TextStyle,
    editorPalette: EditorColorPalette
) {
    val cursorInfinite = rememberInfiniteTransition(label = "activeLineCursor")
    val blinkingCursorAlpha by cursorInfinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    val cursorAlpha = if (isActivelyTyping) 1.0f else blinkingCursorAlpha
    val activeBg = editorPalette.activeLineBackground
    val dividerColor = editorPalette.divider

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = activeBg, size = size)
                if (showGutter) {
                    val gutterWidthPx = 42.dp.toPx()
                    val dividerWidthPx = 1.dp.toPx()
                    drawRect(
                        color = dividerColor,
                        topLeft = Offset(x = gutterWidthPx, y = 0f),
                        size = Size(width = dividerWidthPx, height = size.height)
                    )
                }
            },
        verticalAlignment = Alignment.Top
    ) {
        if (showGutter) {
            Text(
                text = lineNum.toString(),
                style = sharedCodeTextStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = editorPalette.gutterActiveText,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .width(42.dp)
                    .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }

        val renderedLine = if (isCurrentTabPane) {
            buildAnnotatedString {
                append(lineText)
                withStyle(
                    SpanStyle(
                        color = editorPalette.cursor.copy(alpha = cursorAlpha),
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("▎")
                }
            }
        } else {
            lineText
        }

        Text(
            text = renderedLine,
            style = sharedCodeTextStyle,
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
                .then(
                    if (isCurrentTabPane) Modifier.testTag("autowrite_code_text")
                    else Modifier
                )
        )
    }
}

@Composable
private fun StudioCodeWindowHeader(
    projectName: String,
    activeTab: CodeTab,
    progress: Float,
    isAutoChain: Boolean,
    isRecordingMode: Boolean,
    layoutMode: RecordingLayoutMode? = null,
    onOpenLayoutSheet: (() -> Unit)? = null,
    onSelectTab: (CodeTab) -> Unit,
    onExitRecordingMode: () -> Unit
) {
    val palette = LocalEditorPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.gutterBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5F56))
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFBD2E))
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF27C93F))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.plainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isRecordingMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (layoutMode != null && onOpenLayoutSheet != null) {
                        Surface(
                            onClick = onOpenLayoutSheet,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.testTag("rec_header_layout_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = layoutMode.shortLabel,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onExitRecordingMode,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Recording Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = palette.divider.copy(alpha = 0.6f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CodeTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val pillBgColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 550, easing = RealisticSlideEasing),
                    label = "tabPillBg_${tab.extension}"
                )
                val pillTextColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        palette.gutterText
                    },
                    animationSpec = tween(durationMillis = 550, easing = RealisticSlideEasing),
                    label = "tabPillText_${tab.extension}"
                )

                Surface(
                    onClick = { onSelectTab(tab) },
                    shape = RoundedCornerShape(6.dp),
                    color = pillBgColor,
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    } else null,
                    modifier = Modifier.testTag("autowrite_tab_${tab.extension}")
                ) {
                    Text(
                        text = tab.fileName,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.uiSp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = pillTextColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioWindowWatermarkFooter(
    creatorHandle: String,
    showHandleInFooter: Boolean,
    watermarkStyle: WatermarkStyle,
    activeTab: CodeTab,
    currentLine: Int,
    typedChars: Int,
    totalChars: Int,
    speedLevel: Int,
    isTurboMode: Boolean,
    autoDurationTarget: AutoDurationTarget,
    estimatedSeconds: Int,
    isPlaying: Boolean
) {
    val palette = LocalEditorPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.gutterBackground)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showHandleInFooter) {
                CreatorWatermarkBadge(
                    handle = creatorHandle,
                    style = watermarkStyle,
                    isOverlay = false,
                    onClick = {}
                )
            } else {
                Text(
                    text = activeTab.fileName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.uiSp,
                    color = palette.gutterText
                )
            }

            if (isPlaying) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.18f),
                    border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "WRITING",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.5.uiSp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$typedChars/$totalChars",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.5.uiSp,
                color = palette.gutterText.copy(alpha = 0.85f)
            )

            Text(
                text = "Ln $currentLine",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.uiSp,
                color = palette.gutterText
            )

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = autoDurationTarget.shortLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.uiSp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                )
            }
        }
    }
}

@Composable
private fun CreatorControlDeck(
    isPlaying: Boolean,
    speedLevel: Int,
    isTurboMode: Boolean,
    autoDurationTarget: AutoDurationTarget,
    typingSoundProfile: TypingSoundProfile,
    recordingAudioSource: RecordingAudioSource,
    estimatedSeconds: Int,
    isFloatingPreviewOpen: Boolean,
    recordingLayoutConfig: RecordingLayoutConfig? = null,
    onOpenLayoutSheet: (() -> Unit)? = null,
    onSelectDurationTarget: (AutoDurationTarget) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStartCleanRecord: () -> Unit,
    onRestart: () -> Unit,
    onSkipNearEnd: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleTurbo: () -> Unit,
    onCycleTypingSound: () -> Unit,
    onOpenAudioSourceSheet: () -> Unit,
    bgmConfig: BgmConfig,
    onOpenBgmSheet: () -> Unit,
    onToggleFloatingPreview: () -> Unit,
    isScreenRecording: Boolean = false,
    onToggleInAppRecord: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Lv.$speedLevel/20",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 4.dp)
                )

                FilterChip(
                    selected = typingSoundProfile != TypingSoundProfile.OFF,
                    onClick = onCycleTypingSound,
                    label = { Text(typingSoundProfile.shortLabel, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (typingSoundProfile != TypingSoundProfile.OFF) {
                                Icons.Default.VolumeUp
                            } else {
                                Icons.Default.VolumeOff
                            },
                            contentDescription = "ASMR Typing Sound",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("typing_sound_button")
                )

                // Recording Audio Source Chip (System / Mic / Both / Silent)
                FilterChip(
                    selected = recordingAudioSource != RecordingAudioSource.SILENT,
                    onClick = onOpenAudioSourceSheet,
                    label = { Text("Sound: ${recordingAudioSource.shortLabel}", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (recordingAudioSource) {
                                RecordingAudioSource.SYSTEM_ONLY -> Icons.Default.Headphones
                                RecordingAudioSource.MIC_ONLY -> Icons.Default.Mic
                                RecordingAudioSource.SYSTEM_AND_MIC -> Icons.Default.GraphicEq
                                RecordingAudioSource.SILENT -> Icons.Default.VolumeOff
                            },
                            contentDescription = "Recording Audio Source: ${recordingAudioSource.title}",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("recording_audio_source_button")
                )

                // Custom Background Music & Timing Customization Chip
                FilterChip(
                    selected = bgmConfig.isEnabled,
                    onClick = onOpenBgmSheet,
                    label = {
                        val text = if (bgmConfig.isEnabled) {
                            val duration = bgmConfig.getEffectiveDuration(autoDurationTarget)
                            "BGM: ${bgmConfig.trackTitle.take(8)} (${duration}s)"
                        } else {
                            "BGM: Off"
                        }
                        Text(text, style = MaterialTheme.typography.labelSmall)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Background Music",
                            tint = if (bgmConfig.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("bgm_config_chip_button")
                )

                FilterChip(
                    selected = isTurboMode,
                    onClick = onToggleTurbo,
                    label = { Text("2×", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Turbo Mode",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("autowrite_turbo_button")
                )

                if (recordingLayoutConfig != null && onOpenLayoutSheet != null) {
                    FilterChip(
                        selected = false,
                        onClick = onOpenLayoutSheet,
                        label = { Text("Layout: ${recordingLayoutConfig.layoutMode.shortLabel}", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Recording Screen Layout",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("autowrite_rec_layout_button")
                    )
                }
            }

            // Quick Duration Selector Row (15s, 20s, 30s, 45s, 1m, Manual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Duration:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AutoDurationTarget.entries.forEach { target ->
                    val isSelected = target == autoDurationTarget
                    Surface(
                        onClick = { onSelectDurationTarget(target) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier.testTag("duration_preset_${target.shortLabel.lowercase()}")
                    ) {
                        Text(
                            text = target.shortLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.uiSp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Slider(
                value = speedLevel.toFloat(),
                onValueChange = { onSpeedChange(it.roundToInt()) },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("autowrite_speed_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTogglePlayPause,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("autowrite_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onStartCleanRecord,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("autowrite_recording_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Clean Recording Mode",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Record",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onToggleInAppRecord,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("autowrite_screen_record_button")
                ) {
                    Icon(
                        imageVector = if (isScreenRecording) Icons.Filled.Stop else Icons.Filled.Videocam,
                        contentDescription = "In-App Video Recording",
                        tint = if (isScreenRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("autowrite_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSkipNearEnd,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("autowrite_skip_near_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Skip Near End",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onToggleFloatingPreview,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("autowrite_floating_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "Bottom Preview Section (2x Tap)",
                        tint = if (isFloatingPreviewOpen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Tap: 1× Play/Pause • 2× Bottom Preview (Drag ↕ Resize) • 3× Record",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 9.5.uiSp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}
