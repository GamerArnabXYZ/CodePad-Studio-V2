package com.arnix.codepad.studio.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnix.codepad.studio.data.CodeProjectEntity
import com.arnix.codepad.studio.data.CodeProjectRepository
import com.arnix.codepad.studio.data.CodeRevisionSnapshot
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.data.ProjectPreset
import com.arnix.codepad.studio.ui.components.CodeSnippet
import com.arnix.codepad.studio.engine.CodeFormatter
import com.arnix.codepad.studio.engine.ComputedNodeStyle
import com.arnix.codepad.studio.engine.ConsoleLogEntry
import com.arnix.codepad.studio.engine.ExportFormat
import com.arnix.codepad.studio.engine.ExportResult
import com.arnix.codepad.studio.engine.JsFunctionDef
import com.arnix.codepad.studio.engine.LogLevel
import com.arnix.codepad.studio.engine.NativeDomNode
import com.arnix.codepad.studio.engine.NativeLiveEngine
import com.arnix.codepad.studio.engine.PreviewScaleMode
import com.arnix.codepad.studio.engine.BackgroundMusicEngine
import com.arnix.codepad.studio.engine.ProjectExportEngine
import com.arnix.codepad.studio.engine.ScreenRecorderEngine
import com.arnix.codepad.studio.engine.TypingSoundEngine
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.engine.WebSandboxEngine
import com.arnix.codepad.studio.model.BgmConfig
import com.arnix.codepad.studio.model.RecordingAudioSource
import com.arnix.codepad.studio.model.RecordingLayoutConfig
import com.arnix.codepad.studio.model.RecordingLayoutMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class StudioModule {
    CODEPAD_IDE,
    AUTO_WRITE_STUDIO,
    MORE
}

enum class ShortsBackdropPreset(
    val index: Int,
    val label: String,
    val startColorHex: Long,
    val endColorHex: Long
) {
    CLEAN_M3(0, "Material Default", 0xFF000000, 0xFF000000),
    MIDNIGHT_SLATE(1, "Midnight Studio", 0xFF0F172A, 0xFF1E293B),
    ROYAL_INDIGO(2, "Royal Indigo", 0xFF1E1B4B, 0xFF312E81),
    EMERALD_TEAL(3, "Emerald Craft", 0xFF064E3B, 0xFF0F766E),
    SUNSET_DUSK(4, "Sunset Dusk", 0xFF4C1D95, 0xFF9D174D);

    companion object {
        fun fromIndex(index: Int): ShortsBackdropPreset =
            entries.firstOrNull { it.index == index } ?: CLEAN_M3
    }
}

enum class WatermarkPosition(val label: String) {
    FOOTER_BAR("Window Footer"),
    TOP_STAGE("Screen Top"),
    BOTTOM_STAGE("Screen Bottom"),
    CENTER_OVERLAY("Center Overlay"),
    CORNER_OVERLAY("Bottom-Right Overlay"),
    TOP_RIGHT_OVERLAY("Top-Right Overlay"),
    HIDDEN("Hidden")
}

enum class WatermarkStyle(val label: String) {
    PILL_BADGE("Modern Pill"),
    GLASS_OUTLINE("Glass Verified"),
    CREATOR_GRADIENT("Neon Gradient"),
    CYBER_TERMINAL("Terminal Tag"),
    MINIMAL_MONO("Minimal Text")
}

enum class EndCtaStyle(val label: String) {
    BOTTOM_BAR("Floating Pill Bar"),
    SUBSCRIBE_CARD("Subscribe Card"),
    TRANSPARENT_BAR("Transparent Floating Bar"),
    MINIMAL_CHIPS("Floating Action Chips"),
    SIDE_RAIL("Vertical Side Rail"),
    CYBER_NEON_BAR("Cyber Neon Bar")
}

enum class ViralHookStyle(val label: String) {
    NEON_PILL("Neon Pill Header"),
    GLASS_BANNER("Glass Hook Card"),
    TRANSPARENT_CLEAN("Transparent Floating"),
    CYBER_TERMINAL("Cyber Terminal Box"),
    GRADIENT_RIBBON("Gradient Ribbon"),
    MINIMAL_HEADER("Clean Minimal")
}

enum class AutoDurationTarget(
    val seconds: Int,
    val shortLabel: String,
    val fullLabel: String
) {
    SEC_15(15, "15s", "15s Blitz"),
    SEC_20(20, "20s", "20s Hook"),
    SEC_30(30, "30s", "30s Reel"),
    SEC_45(45, "45s", "45s Short"),
    SEC_60(60, "1m", "1m Shorts (<60s)"),
    MANUAL(0, "Manual", "Manual Speed")
}

data class TypingSequenceMetrics(
    val totalChars: Int,
    val effectiveSteps: Int,
    val weightedUnits: Float,
    val recommendedSpeedLevel: Int,
    val requiresTurboBurst: Boolean,
    val estimatedTotalSeconds: Int
)

object AutoSpeedOptimizer {

    fun countStepsAndWeight(code: String, startIndex: Int = 0): Pair<Int, Float> {
        val len = code.length
        var idx = startIndex.coerceIn(0, len)
        var steps = 0
        var weighted = 0f

        while (idx < len) {
            val ch = code[idx]
            val mult = when {
                ch == '\n' -> 1.75f
                ch == ';' || ch == '}' || ch == '>' -> 1.35f
                ch == '{' || ch == '(' || ch == ':' || ch == '=' -> 1.18f
                ch.isLetterOrDigit() -> 0.85f
                else -> 1.0f
            }
            steps++
            weighted += mult
            idx++
            if (ch == '\n') {
                while (idx < len && (code[idx] == ' ' || code[idx] == '\t')) {
                    idx++
                }
            }
        }
        return steps to weighted
    }

    fun analyze(
        htmlCode: String,
        cssCode: String,
        jsCode: String,
        activeTab: CodeTab,
        isAutoChain: Boolean,
        showEndCta: Boolean,
        target: AutoDurationTarget,
        manualSpeedLevel: Int,
        manualTurbo: Boolean,
        hasPreviewShowcase: Boolean = false,
        showcaseDurationMs: Long = 0L
    ): TypingSequenceMetrics {
        val codes = if (isAutoChain) {
            listOf(htmlCode, cssCode, jsCode)
        } else {
            val single = when (activeTab) {
                CodeTab.INDEX_HTML -> htmlCode
                CodeTab.STYLE_CSS -> cssCode
                CodeTab.SCRIPT_JS -> jsCode
            }
            listOf(single)
        }

        var totalChars = 0
        var totalSteps = 0
        var totalWeighted = 0f
        codes.forEach { c ->
            totalChars += c.length
            val (s, w) = countStepsAndWeight(c, 0)
            totalSteps += s
            totalWeighted += w
        }

        val transitionCount = if (isAutoChain) 2 else 0
        val perTransitionMs = if (target == AutoDurationTarget.SEC_15 || target == AutoDurationTarget.SEC_20) {
            1080L
        } else {
            1400L
        }
        val transitionOverheadMs = transitionCount * perTransitionMs
        val outroOverheadMs = if (showEndCta) {
            if (target == AutoDurationTarget.SEC_15 || target == AutoDurationTarget.SEC_20) 1300L else 2000L
        } else {
            400L
        }
        val effectiveShowcaseMs = if (hasPreviewShowcase) showcaseDurationMs else 0L

        if (target == AutoDurationTarget.MANUAL) {
            val level = manualSpeedLevel.coerceIn(1, 20)
            val baseDelay = max(10f, 108f - (level * 4.8f))
            val effectiveDelay = if (manualTurbo) max(6f, baseDelay / 2f) else baseDelay
            val typingMs = (totalWeighted * effectiveDelay).toLong()
            val estSec = max(1, ((typingMs + transitionOverheadMs + outroOverheadMs + effectiveShowcaseMs) / 1000f).roundToInt())
            return TypingSequenceMetrics(
                totalChars = totalChars,
                effectiveSteps = totalSteps,
                weightedUnits = totalWeighted,
                recommendedSpeedLevel = level,
                requiresTurboBurst = manualTurbo,
                estimatedTotalSeconds = estSec
            )
        }

        val targetTotalMs = target.seconds * 1000L
        val availableTypingMs = max(
            2500L,
            targetTotalMs - transitionOverheadMs - outroOverheadMs - effectiveShowcaseMs
        )
        val rawBaseDelayMs = availableTypingMs.toFloat() / max(1f, totalWeighted)

        // Cap max delay per unit so short snippets still type at a crisp creator pace
        val maxNaturalDelayMs = when (target) {
            AutoDurationTarget.SEC_15 -> 14f
            AutoDurationTarget.SEC_20 -> 18f
            AutoDurationTarget.SEC_30 -> 26f
            AutoDurationTarget.SEC_45 -> 34f
            AutoDurationTarget.SEC_60 -> 40f
            AutoDurationTarget.MANUAL -> 40f
        }

        val clampedBaseDelay = min(rawBaseDelayMs, maxNaturalDelayMs)
        val recommendedLevel = ((108f - clampedBaseDelay) / 4.8f).roundToInt().coerceIn(1, 20)
        val needsBurst = rawBaseDelayMs < 11f
        val actualTypingMs = (totalWeighted * clampedBaseDelay).toLong().coerceAtMost(availableTypingMs)
        val totalEstMs = actualTypingMs + transitionOverheadMs + outroOverheadMs + effectiveShowcaseMs
        val estSec = max(
            1,
            min(
                target.seconds,
                (totalEstMs / 1000f).roundToInt()
            )
        )

        return TypingSequenceMetrics(
            totalChars = totalChars,
            effectiveSteps = totalSteps,
            weightedUnits = totalWeighted,
            recommendedSpeedLevel = recommendedLevel,
            requiresTurboBurst = needsBurst,
            estimatedTotalSeconds = estSec
        )
    }
}

data class StudioUiState(
    val activeModule: StudioModule = StudioModule.CODEPAD_IDE,
    val projectName: String = "Portfolio Card",
    val activeTab: CodeTab = CodeTab.INDEX_HTML,
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    val editorFontSize: Float = 11.5f,
    val autoWriteFontSize: Float = 11f,
    val uiFontScale: Float = 0.92f,
    val isDarkTheme: Boolean = false,
    val isSaved: Boolean = true,
    val lastSavedTimeText: String = "Saved",
    val currentLine: Int = 1,
    val currentColumn: Int = 1,
    val isPreviewVisible: Boolean = false,
    val isConsoleVisible: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val isExportSheetOpen: Boolean = false,
    val latestExportResult: ExportResult? = null,
    // Native Live Preview & Console Subsystem
    val domNodes: List<NativeDomNode> = emptyList(),
    val bodyStyle: ComputedNodeStyle = ComputedNodeStyle(),
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val consoleFilter: LogLevel? = null,
    val previewScaleMode: PreviewScaleMode = PreviewScaleMode.AUTO_FIT,
    val previewReloadNonce: Long = 0L,
    // Auto Write Studio & Creator Customization
    val autoWriteTab: CodeTab = CodeTab.INDEX_HTML,
    val isAutoWritePlaying: Boolean = false,
    val typedCharCount: Int = 0,
    val speedLevel: Int = 14, // 1..20
    val isTurboMode: Boolean = false,
    val autoDurationTarget: AutoDurationTarget = AutoDurationTarget.SEC_60,
    val isFloatingPreviewOpen: Boolean = false,
    val isRecordingMode: Boolean = false,
    val creatorHandle: String = "@codewitharnix",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.FOOTER_BAR,
    val watermarkStyle: WatermarkStyle = WatermarkStyle.PILL_BADGE,
    val watermarkOpacity: Float = 0.85f,
    val watermarkSizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    val showEndCtaAnimation: Boolean = true,
    val endCtaStyle: EndCtaStyle = EndCtaStyle.SUBSCRIBE_CARD,
    val endCtaPosition: EndCtaPosition = EndCtaPosition.CODE_BOTTOM_OVERLAY,
    val ctaDisplayTiming: CtaDisplayTiming = CtaDisplayTiming.AT_END_100,
    val ctaTransparentBg: Boolean = false,
    val ctaShowBorder: Boolean = true,
    val ctaShowHandle: Boolean = true,
    val ctaAccentIndex: Int = 1,
    val ctaSizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    val endCtaMessage: String = "Like, Comment & Subscribe for more!",
    val ctaShowLike: Boolean = true,
    val ctaShowComment: Boolean = true,
    val ctaShowSubscribe: Boolean = true,
    val ctaLikeLabel: String = "Like",
    val ctaCommentLabel: String = "Comment",
    val ctaSubscribeLabel: String = "SUBSCRIBE",
    val ctaSubscribedLabel: String = "SUBSCRIBED ✓",
    val isEndCtaActive: Boolean = false,
    val isAutoChainTabs: Boolean = true,
    val autoRevealPreviewOnFinish: Boolean = false,
    val backdropPresetIndex: Int = 1,
    val showViralHookBanner: Boolean = true,
    val viralHookBadge: String = "🔥 HTML • CSS • JS",
    val viralHookTitle: String = "Build a Developer Portfolio Card",
    val viralHookStyle: ViralHookStyle = ViralHookStyle.NEON_PILL,
    val viralHookPosition: ViralHookPosition = ViralHookPosition.TOP_STAGE,
    val viralHookAlignment: ViralHookAlignment = ViralHookAlignment.START,
    val viralHookTransparentBg: Boolean = false,
    val viralHookShowBorder: Boolean = true,
    val viralHookShowBadge: Boolean = true,
    val viralHookAccentIndex: Int = 0,
    val viralHookSizeScale: OverlaySizeScale = OverlaySizeScale.STANDARD,
    val typingSoundProfile: TypingSoundProfile = TypingSoundProfile.MECH_THOCK,
    val typingSoundVolume: Float = 0.85f,
    val typingSoundMinGapMs: Long = 75L,
    val typingSoundPitchVar: Boolean = true,
    val autoWriteShowGutter: Boolean = true,
    val useRecordingCountdown: Boolean = true,
    val recordingCountdownSeconds: Int = 0,
    val keepPreviewOpenInRecord: Boolean = true,
    val isCreatorCustomizeOpen: Boolean = false,
    val isViralTemplatesSheetOpen: Boolean = false,
    val previewSectionHeightDp: Float = 195f,
    val autoWriteDomNodes: List<NativeDomNode> = emptyList(),
    val autoWriteBodyStyle: ComputedNodeStyle = ComputedNodeStyle(),
    val autoWriteEffectiveHtml: String = "",
    val autoWriteEffectiveCss: String = "",
    val autoWriteEffectiveJs: String = "",
    val autoWriteCurrentLine: Int = 1,
    val autoWriteCurrentColumn: Int = 1,
    val sequenceMetrics: TypingSequenceMetrics = TypingSequenceMetrics(
        totalChars = 0,
        effectiveSteps = 0,
        weightedUnits = 0f,
        recommendedSpeedLevel = 14,
        requiresTurboBurst = false,
        estimatedTotalSeconds = 1
    ),
    val isInitialized: Boolean = false,
    val isOnboardingOpen: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    // Find & Replace Suite
    val isFindReplaceOpen: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val isFindMatchCase: Boolean = false,
    val isFindUseRegex: Boolean = false,
    val findCurrentMatchIndex: Int = 0,
    val findTotalMatches: Int = 0,
    val activeMatchRange: Pair<Int, Int>? = null,
    val isAutoClosePairs: Boolean = true,
    // CSS Color Picker
    val isColorPickerOpen: Boolean = false,
    val initialColorHex: String = "#38BDF8",
    // Component & Snippet Store
    val isSnippetStoreOpen: Boolean = false,
    // Revision Snapshots
    val isRevisionSheetOpen: Boolean = false,
    val snapshots: List<CodeRevisionSnapshot> = emptyList(),
    // Audio Acoustic Customizer
    val typingSoundPitchMultiplier: Float = 1.0f,
    val typingSoundStabilizerResonance: Boolean = true,
    // Start Delay after Countdown & In-App Recording
    val recordingStartDelayMs: Long = 1000L,
    val isRecordingStartingDelay: Boolean = false,
    val isAutoRecordInApp: Boolean = true,
    val isSavedVideoDialogOpen: Boolean = false,
    val recorderPrefer1080p: Boolean = true,
    val recorderPrefer60Fps: Boolean = true,
    val recorderEnableAudio: Boolean = true,
    val recordingAudioSource: RecordingAudioSource = RecordingAudioSource.SYSTEM_ONLY,
    val bgmConfig: BgmConfig = BgmConfig(),
    val recordingLayoutConfig: RecordingLayoutConfig = RecordingLayoutConfig(),
    val isRecordingLayoutSheetOpen: Boolean = false,
    val isEndFullscreenPreviewRevealed: Boolean = false,
    val autoWriteFinishedNonce: Long = 0L,
    val isPreviewShowcaseActive: Boolean = false,
    val previewShowcaseRemainingSec: Int = 0,
    val previewShowcaseTotalSec: Int = 0
) {
    val activeCode: String
        get() = when (activeTab) {
            CodeTab.INDEX_HTML -> htmlCode
            CodeTab.STYLE_CSS -> cssCode
            CodeTab.SCRIPT_JS -> jsCode
        }

    val autoWriteFullCode: String
        get() = when (autoWriteTab) {
            CodeTab.INDEX_HTML -> htmlCode
            CodeTab.STYLE_CSS -> cssCode
            CodeTab.SCRIPT_JS -> jsCode
        }

    val autoWriteVisibleCode: String
        get() {
            val full = autoWriteFullCode
            if (full.isEmpty()) return ""
            return full.substring(0, typedCharCount.coerceIn(0, full.length))
        }

    val autoWriteProgress: Float
        get() {
            val total = autoWriteFullCode.length
            if (total == 0) return 1f
            return (typedCharCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }

    val overallSequenceProgress: Float
        get() {
            if (!isAutoChainTabs) return autoWriteProgress
            val totalLen = (htmlCode.length + cssCode.length + jsCode.length).coerceAtLeast(1)
            val completedBeforeTab = when (autoWriteTab) {
                CodeTab.INDEX_HTML -> 0
                CodeTab.STYLE_CSS -> htmlCode.length
                CodeTab.SCRIPT_JS -> htmlCode.length + cssCode.length
            }
            val currentTyped = typedCharCount.coerceIn(0, autoWriteFullCode.length)
            return ((completedBeforeTab + currentTyped).toFloat() / totalLen.toFloat()).coerceIn(0f, 1f)
        }

    val shouldDisplayCtaOverlay: Boolean
        get() {
            if (!showEndCtaAnimation) return false
            if (isEndCtaActive) return true
            return when (ctaDisplayTiming) {
                CtaDisplayTiming.AT_END_100 -> false
                CtaDisplayTiming.ALWAYS_VISIBLE -> true
                CtaDisplayTiming.FROM_50_PERCENT -> overallSequenceProgress >= 0.50f
                CtaDisplayTiming.LAST_20_PERCENT -> overallSequenceProgress >= 0.80f
            }
        }

    val errorCount: Int
        get() = consoleLogs.count { it.level == LogLevel.ERROR }

    val canSplitEmbeddedHtml: Boolean
        get() = WebSandboxEngine.hasEmbeddedStyleOrScript(htmlCode)
}

fun computeLineAndColumn(code: String, charCount: Int): Pair<Int, Int> {
    val limit = charCount.coerceIn(0, code.length)
    var line = 1
    var col = 1
    for (i in 0 until limit) {
        if (code[i] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
    }
    return line to col
}

class StudioViewModel(
    private val repository: CodeProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var liveJsDebounceJob: Job? = null
    private var typewriterJob: Job? = null
    private var countdownJob: Job? = null
    var onAutoWriteFinished: (() -> Unit)? = null
    private var appContext: Context? = null

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
    }
    private var isWebViewEngineAttached: Boolean = false
    private var lastAutoPreviewHtml: String = "__UNSET__"
    private var lastAutoPreviewCss: String = "__UNSET__"
    private var lastAutoPreviewJs: String = "__UNSET__"
    private var lastAutoPreviewTimeMs: Long = 0L

    private var runtimeFunctions: Map<String, JsFunctionDef> = emptyMap()
    private val runtimeVariables = mutableMapOf<String, String>()
    private val domTextOverrides = mutableMapOf<String, String>()
    private val domColorOverrides = mutableMapOf<String, Color>()
    private val domBgOverrides = mutableMapOf<String, Color>()

    private fun clearRuntimeDomState() {
        domTextOverrides.clear()
        domColorOverrides.clear()
        domBgOverrides.clear()
        runtimeVariables.clear()
    }

    init {
        viewModelScope.launch {
            runCatching {
                val initial = repository.getOrInitProject()
            val initTab = CodeTab.fromIndex(initial.activeTabIndex)
            val savedWatermarkPos = WatermarkPosition.entries.getOrElse(initial.watermarkPositionOrdinal) {
                WatermarkPosition.FOOTER_BAR
            }
            val savedWatermarkStyle = WatermarkStyle.entries.getOrElse(initial.watermarkStyleOrdinal) {
                WatermarkStyle.PILL_BADGE
            }
            val savedDurationTarget = AutoDurationTarget.entries.getOrElse(initial.autoDurationTargetOrdinal) {
                AutoDurationTarget.SEC_60
            }
            val savedEndCtaStyle = EndCtaStyle.entries.getOrElse(initial.endCtaStyleOrdinal) {
                EndCtaStyle.SUBSCRIBE_CARD
            }
            val savedEndCtaPosition = EndCtaPosition.entries.getOrElse(initial.endCtaPositionOrdinal) {
                EndCtaPosition.CODE_BOTTOM_OVERLAY
            }
            val savedCtaTiming = CtaDisplayTiming.entries.getOrElse(initial.ctaDisplayTimingOrdinal) {
                CtaDisplayTiming.AT_END_100
            }
            val savedCtaSizeScale = OverlaySizeScale.fromIndex(initial.ctaSizeScaleIndex)
            val savedHookStyle = ViralHookStyle.entries.getOrElse(initial.viralHookStyleOrdinal) {
                ViralHookStyle.NEON_PILL
            }
            val savedHookPosition = ViralHookPosition.entries.getOrElse(initial.viralHookPositionOrdinal) {
                ViralHookPosition.TOP_STAGE
            }
            val savedHookAlignment = ViralHookAlignment.entries.getOrElse(initial.viralHookAlignmentOrdinal) {
                ViralHookAlignment.START
            }
            val savedHookSizeScale = OverlaySizeScale.fromIndex(initial.viralHookSizeScaleIndex)
            val savedSoundProfile = TypingSoundProfile.fromIndex(initial.typingSoundProfileIndex)

            _uiState.update {
                it.copy(
                    projectName = initial.projectName,
                    htmlCode = initial.htmlCode,
                    cssCode = initial.cssCode,
                    jsCode = initial.jsCode,
                    editorFontSize = initial.editorFontSize.coerceIn(8f, 22f),
                    autoWriteFontSize = initial.autoWriteFontSize.coerceIn(8f, 22f),
                    uiFontScale = initial.uiFontScale.coerceIn(0.70f, 1.35f),
                    isDarkTheme = initial.isDarkTheme,
                    activeTab = initTab,
                    autoWriteTab = initTab,
                    creatorHandle = initial.creatorHandle.ifBlank { "@codewitharnix" },
                    watermarkPosition = savedWatermarkPos,
                    watermarkStyle = savedWatermarkStyle,
                    autoDurationTarget = savedDurationTarget,
                    speedLevel = initial.speedLevel.coerceIn(1, 20),
                    isTurboMode = initial.isTurboMode,
                    previewSectionHeightDp = initial.previewSectionHeightDp.coerceIn(85f, 460f),
                    showEndCtaAnimation = initial.showEndCtaAnimation,
                    endCtaStyle = savedEndCtaStyle,
                    endCtaPosition = savedEndCtaPosition,
                    ctaDisplayTiming = savedCtaTiming,
                    ctaTransparentBg = initial.ctaTransparentBg,
                    ctaShowBorder = initial.ctaShowBorder,
                    ctaShowHandle = initial.ctaShowHandle,
                    ctaAccentIndex = initial.ctaAccentIndex,
                    ctaSizeScale = savedCtaSizeScale,
                    endCtaMessage = initial.endCtaMessage,
                    ctaShowLike = initial.ctaShowLike,
                    ctaShowComment = initial.ctaShowComment,
                    ctaShowSubscribe = initial.ctaShowSubscribe,
                    ctaLikeLabel = initial.ctaLikeLabel.ifBlank { "Like" },
                    ctaCommentLabel = initial.ctaCommentLabel.ifBlank { "Comment" },
                    ctaSubscribeLabel = initial.ctaSubscribeLabel.ifBlank { "SUBSCRIBE" },
                    ctaSubscribedLabel = initial.ctaSubscribedLabel.ifBlank { "SUBSCRIBED ✓" },
                    isAutoChainTabs = initial.isAutoChainTabs,
                    autoRevealPreviewOnFinish = initial.autoRevealPreviewOnFinish,
                    backdropPresetIndex = initial.backdropPresetIndex,
                    showViralHookBanner = initial.showViralHookBanner,
                    viralHookBadge = initial.viralHookBadge.ifBlank { "🔥 HTML • CSS • JS" },
                    viralHookTitle = initial.viralHookTitle.ifBlank { initial.projectName },
                    viralHookStyle = savedHookStyle,
                    viralHookPosition = savedHookPosition,
                    viralHookAlignment = savedHookAlignment,
                    viralHookTransparentBg = initial.viralHookTransparentBg,
                    viralHookShowBorder = initial.viralHookShowBorder,
                    viralHookShowBadge = initial.viralHookShowBadge,
                    viralHookAccentIndex = initial.viralHookAccentIndex,
                    viralHookSizeScale = savedHookSizeScale,
                    typingSoundProfile = savedSoundProfile,
                    useRecordingCountdown = initial.useRecordingCountdown,
                    keepPreviewOpenInRecord = initial.keepPreviewOpenInRecord,
                    hasCompletedOnboarding = initial.hasCompletedOnboarding,
                    isOnboardingOpen = !initial.hasCompletedOnboarding,
                    isSaved = true,
                    isInitialized = true
                )
            }
            syncAutoSpeedMetricsIfNeeded()
            rebuildNativePreview(runTopLevelJs = true, logSystemBoot = true)
            updateProgressiveAutoWritePreview()
            }
        }
        viewModelScope.launch {
            repository.snapshotsFlow.collect { snapList ->
                _uiState.update { it.copy(snapshots = snapList) }
            }
        }
    }

    fun toggleTheme() {
        val nextDark = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = nextDark) }
        triggerDebouncedAutoSave()
    }

    fun setDarkTheme(dark: Boolean) {
        _uiState.update { it.copy(isDarkTheme = dark) }
        triggerDebouncedAutoSave()
    }

    fun openOnboardingGuide() {
        _uiState.update { it.copy(isOnboardingOpen = true) }
    }

    fun dismissOnboardingGuide(markCompleted: Boolean = true) {
        _uiState.update {
            it.copy(
                isOnboardingOpen = false,
                hasCompletedOnboarding = it.hasCompletedOnboarding || markCompleted
            )
        }
        if (markCompleted) {
            triggerDebouncedAutoSave()
        }
    }

    fun selectModule(module: StudioModule) {
        _uiState.update { state ->
            val syncTab = if (module == StudioModule.AUTO_WRITE_STUDIO) state.activeTab else state.autoWriteTab
            state.copy(
                activeModule = module,
                autoWriteTab = syncTab
            )
        }
        if (module == StudioModule.AUTO_WRITE_STUDIO && _uiState.value.typedCharCount == 0) {
            startOrResumeAutoWrite()
        } else if (module == StudioModule.CODEPAD_IDE || module == StudioModule.MORE) {
            pauseAutoWrite()
        }
    }

    fun selectTab(tab: CodeTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                currentLine = 1,
                currentColumn = 1
            )
        }
        triggerDebouncedAutoSave()
    }

    fun onCodeChanged(newCode: String, cursorOffset: Int) {
        val sanitized = WebSandboxEngine.sanitizeCode(newCode)
        val prevCode = _uiState.value.activeCode
        val activeTab = _uiState.value.activeTab
        val isBulkPaste = abs(sanitized.length - prevCode.length) > 8

        val (line, col) = calculateLineAndColumn(sanitized, cursorOffset)
        _uiState.update { state ->
            when (state.activeTab) {
                CodeTab.INDEX_HTML -> state.copy(
                    htmlCode = sanitized,
                    isSaved = false,
                    currentLine = line,
                    currentColumn = col
                )
                CodeTab.STYLE_CSS -> state.copy(
                    cssCode = sanitized,
                    isSaved = false,
                    currentLine = line,
                    currentColumn = col
                )
                CodeTab.SCRIPT_JS -> state.copy(
                    jsCode = sanitized,
                    isSaved = false,
                    currentLine = line,
                    currentColumn = col
                )
            }
        }
        syncAutoSpeedMetricsIfNeeded()
        updateProgressiveAutoWritePreview()

        when {
            activeTab == CodeTab.STYLE_CSS -> {
                // CSS edits apply at 60fps without needing to re-run top-level JS or clear console
                rebuildNativePreview(runTopLevelJs = false, logSystemBoot = false, clearPreviousRuntimeLogs = false)
            }
            isBulkPaste -> {
                // User pasted a code snippet: run immediately and clear stale logs from old code
                liveJsDebounceJob?.cancel()
                _uiState.update { it.copy(previewReloadNonce = it.previewReloadNonce + 1L) }
                rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = true)
            }
            else -> {
                // Single-character typing: update DOM structure immediately, debounce JS execution by 300ms
                // so incomplete lines while typing never spam false SyntaxErrors in the Console.
                rebuildNativePreview(runTopLevelJs = false, logSystemBoot = false, clearPreviousRuntimeLogs = false)
                liveJsDebounceJob?.cancel()
                liveJsDebounceJob = viewModelScope.launch {
                    delay(300L)
                    val latestJs = _uiState.value.jsCode
                    if (WebSandboxEngine.isJsSyntacticallyComplete(latestJs)) {
                        _uiState.update { it.copy(previewReloadNonce = it.previewReloadNonce + 1L) }
                        rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = true)
                    }
                }
            }
        }
        triggerDebouncedAutoSave()
    }

    fun onCursorPositionChanged(code: String, cursorOffset: Int) {
        val (line, col) = calculateLineAndColumn(code, cursorOffset)
        _uiState.update {
            if (it.currentLine == line && it.currentColumn == col) it
            else it.copy(currentLine = line, currentColumn = col)
        }
    }

    private fun calculateLineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 1 to 1
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var lastNewLineIdx = -1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                lastNewLineIdx = i
            }
        }
        val col = safeOffset - lastNewLineIdx
        return line to col
    }

    private fun buildProjectEntity(current: StudioUiState): CodeProjectEntity {
        return CodeProjectEntity(
            id = 1,
            projectName = current.projectName,
            htmlCode = current.htmlCode,
            cssCode = current.cssCode,
            jsCode = current.jsCode,
            editorFontSize = current.editorFontSize,
            autoWriteFontSize = current.autoWriteFontSize,
            uiFontScale = current.uiFontScale,
            activeTabIndex = current.activeTab.index,
            isDarkTheme = current.isDarkTheme,
            creatorHandle = current.creatorHandle,
            watermarkPositionOrdinal = current.watermarkPosition.ordinal,
            watermarkStyleOrdinal = current.watermarkStyle.ordinal,
            autoDurationTargetOrdinal = current.autoDurationTarget.ordinal,
            speedLevel = current.speedLevel,
            isTurboMode = current.isTurboMode,
            previewSectionHeightDp = current.previewSectionHeightDp,
            showEndCtaAnimation = current.showEndCtaAnimation,
            endCtaStyleOrdinal = current.endCtaStyle.ordinal,
            endCtaPositionOrdinal = current.endCtaPosition.ordinal,
            ctaDisplayTimingOrdinal = current.ctaDisplayTiming.ordinal,
            ctaTransparentBg = current.ctaTransparentBg,
            ctaShowBorder = current.ctaShowBorder,
            ctaShowHandle = current.ctaShowHandle,
            ctaAccentIndex = current.ctaAccentIndex,
            ctaSizeScaleIndex = current.ctaSizeScale.index,
            endCtaMessage = current.endCtaMessage,
            ctaShowLike = current.ctaShowLike,
            ctaShowComment = current.ctaShowComment,
            ctaShowSubscribe = current.ctaShowSubscribe,
            ctaLikeLabel = current.ctaLikeLabel,
            ctaCommentLabel = current.ctaCommentLabel,
            ctaSubscribeLabel = current.ctaSubscribeLabel,
            ctaSubscribedLabel = current.ctaSubscribedLabel,
            isAutoChainTabs = current.isAutoChainTabs,
            autoRevealPreviewOnFinish = current.autoRevealPreviewOnFinish,
            backdropPresetIndex = current.backdropPresetIndex,
            showViralHookBanner = current.showViralHookBanner,
            viralHookBadge = current.viralHookBadge,
            viralHookTitle = current.viralHookTitle,
            viralHookStyleOrdinal = current.viralHookStyle.ordinal,
            viralHookPositionOrdinal = current.viralHookPosition.ordinal,
            viralHookAlignmentOrdinal = current.viralHookAlignment.ordinal,
            viralHookTransparentBg = current.viralHookTransparentBg,
            viralHookShowBorder = current.viralHookShowBorder,
            viralHookShowBadge = current.viralHookShowBadge,
            viralHookAccentIndex = current.viralHookAccentIndex,
            viralHookSizeScaleIndex = current.viralHookSizeScale.index,
            typingSoundProfileIndex = current.typingSoundProfile.index,
            useRecordingCountdown = current.useRecordingCountdown,
            keepPreviewOpenInRecord = current.keepPreviewOpenInRecord,
            hasCompletedOnboarding = current.hasCompletedOnboarding,
            lastSavedTimestamp = System.currentTimeMillis()
        )
    }

    private fun triggerDebouncedAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500L)
            val entity = buildProjectEntity(_uiState.value)
            repository.saveProject(entity)
            _uiState.update {
                it.copy(
                    isSaved = true,
                    lastSavedTimeText = "Saved"
                )
            }
        }
    }

    fun flushPendingAutoSave() {
        autoSaveJob?.cancel()
        val snapshotState = _uiState.value
        if (!snapshotState.isInitialized) return
        viewModelScope.launch {
            repository.saveProject(buildProjectEntity(snapshotState))
            _uiState.update {
                it.copy(
                    isSaved = true,
                    lastSavedTimeText = "Saved"
                )
            }
        }
    }

    fun updateProjectName(newName: String) {
        val trimmed = newName.trim().ifEmpty { "Untitled Project" }
        _uiState.update {
            it.copy(
                projectName = trimmed,
                isSaved = false,
                isRenameDialogOpen = false
            )
        }
        appendConsoleLog(
            LogLevel.SYSTEM,
            "Project renamed to '$trimmed'",
            "workspace"
        )
        triggerDebouncedAutoSave()
    }

    fun updateEditorFontSize(newSize: Float) {
        val clamped = newSize.coerceIn(8f, 22f)
        _uiState.update {
            it.copy(
                editorFontSize = clamped,
                isSaved = false
            )
        }
        triggerDebouncedAutoSave()
    }

    fun updateAutoWriteFontSize(newSize: Float) {
        val clamped = newSize.coerceIn(8f, 22f)
        _uiState.update {
            it.copy(
                autoWriteFontSize = clamped,
                isSaved = false
            )
        }
        triggerDebouncedAutoSave()
    }

    fun updateUiFontScale(newScale: Float) {
        val clamped = newScale.coerceIn(0.70f, 1.35f)
        _uiState.update {
            it.copy(
                uiFontScale = clamped,
                isSaved = false
            )
        }
        triggerDebouncedAutoSave()
    }

    fun applyFontSizePreset(editorSize: Float, autoWriteSize: Float, uiScale: Float) {
        _uiState.update {
            it.copy(
                editorFontSize = editorSize.coerceIn(8f, 22f),
                autoWriteFontSize = autoWriteSize.coerceIn(8f, 22f),
                uiFontScale = uiScale.coerceIn(0.70f, 1.35f),
                isSaved = false
            )
        }
        triggerDebouncedAutoSave()
    }

    fun formatActiveTabCode() {
        val state = _uiState.value
        val formatted = CodeFormatter.format(state.activeCode, state.activeTab)
        val totalLines = max(1, formatted.count { it == '\n' } + 1)
        _uiState.update { current ->
            val safeLine = current.currentLine.coerceIn(1, totalLines)
            when (current.activeTab) {
                CodeTab.INDEX_HTML -> current.copy(htmlCode = formatted, currentLine = safeLine, isSaved = false)
                CodeTab.STYLE_CSS -> current.copy(cssCode = formatted, currentLine = safeLine, isSaved = false)
                CodeTab.SCRIPT_JS -> current.copy(jsCode = formatted, currentLine = safeLine, isSaved = false)
            }
        }
        appendConsoleLog(
            LogLevel.SYSTEM,
            "Formatted ${state.activeTab.fileName} ($totalLines lines)",
            "Formatter"
        )
        syncAutoSpeedMetricsIfNeeded()
        rebuildNativePreview(runTopLevelJs = false, logSystemBoot = false)
        updateProgressiveAutoWritePreview(force = true)
        triggerDebouncedAutoSave()
    }

    fun loadPreset(preset: ProjectPreset) {
        typewriterJob?.cancel()
        countdownJob?.cancel()
        liveJsDebounceJob?.cancel()
        clearRuntimeDomState()
        _uiState.update {
            it.copy(
                projectName = preset.name,
                htmlCode = preset.html,
                cssCode = preset.css,
                jsCode = preset.js,
                viralHookBadge = preset.hookBadge,
                viralHookTitle = preset.hookTitle,
                autoWriteTab = CodeTab.INDEX_HTML,
                isSaved = false,
                typedCharCount = 0,
                autoWriteCurrentLine = 1,
                autoWriteCurrentColumn = 1,
                isEndCtaActive = false,
                isViralTemplatesSheetOpen = false,
                consoleLogs = emptyList(),
                previewReloadNonce = it.previewReloadNonce + 1L
            )
        }
        appendConsoleLog(
            LogLevel.SYSTEM,
            "Loaded template '${preset.name}'",
            "Templates"
        )
        syncAutoSpeedMetricsIfNeeded()
        rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = false)
        updateProgressiveAutoWritePreview(force = true)
        triggerDebouncedAutoSave()
        if (_uiState.value.activeModule == StudioModule.AUTO_WRITE_STUDIO) {
            startOrResumeAutoWrite()
        }
    }

    fun splitEmbeddedHtmlIntoTabs() {
        val state = _uiState.value
        val splitResult = WebSandboxEngine.splitEmbeddedHtml(
            htmlCode = state.htmlCode,
            existingCss = state.cssCode,
            existingJs = state.jsCode
        )
        if (!splitResult.didSplit) return

        liveJsDebounceJob?.cancel()
        clearRuntimeDomState()
        _uiState.update {
            it.copy(
                htmlCode = splitResult.cleanedHtml,
                cssCode = splitResult.extractedCss,
                jsCode = splitResult.extractedJs,
                currentLine = 1,
                currentColumn = 1,
                isSaved = false,
                consoleLogs = emptyList(),
                previewReloadNonce = it.previewReloadNonce + 1L
            )
        }
        appendConsoleLog(
            LogLevel.SYSTEM,
            "Extracted inline <style> & <script> into style.css and script.js",
            "Splitter"
        )
        syncAutoSpeedMetricsIfNeeded()
        rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = false)
        updateProgressiveAutoWritePreview(force = true)
        triggerDebouncedAutoSave()
    }

    fun clearActiveTabCode() {
        liveJsDebounceJob?.cancel()
        val tab = _uiState.value.activeTab
        _uiState.update { state ->
            when (tab) {
                CodeTab.INDEX_HTML -> state.copy(
                    htmlCode = "",
                    currentLine = 1,
                    currentColumn = 1,
                    isSaved = false,
                    previewReloadNonce = state.previewReloadNonce + 1L
                )
                CodeTab.STYLE_CSS -> state.copy(
                    cssCode = "",
                    currentLine = 1,
                    currentColumn = 1,
                    isSaved = false,
                    previewReloadNonce = state.previewReloadNonce + 1L
                )
                CodeTab.SCRIPT_JS -> state.copy(
                    jsCode = "",
                    currentLine = 1,
                    currentColumn = 1,
                    isSaved = false,
                    previewReloadNonce = state.previewReloadNonce + 1L
                )
            }
        }
        syncAutoSpeedMetricsIfNeeded()
        rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = true)
        updateProgressiveAutoWritePreview(force = true)
        triggerDebouncedAutoSave()
    }

    fun clearAllTabsForNewSnippet() {
        typewriterJob?.cancel()
        countdownJob?.cancel()
        liveJsDebounceJob?.cancel()
        clearRuntimeDomState()
        _uiState.update {
            it.copy(
                htmlCode = "",
                cssCode = "",
                jsCode = "",
                currentLine = 1,
                currentColumn = 1,
                typedCharCount = 0,
                autoWriteCurrentLine = 1,
                autoWriteCurrentColumn = 1,
                isAutoWritePlaying = false,
                isSaved = false,
                consoleLogs = emptyList(),
                previewReloadNonce = it.previewReloadNonce + 1L
            )
        }
        appendConsoleLog(
            LogLevel.SYSTEM,
            "Cleared HTML, CSS & JS tabs for a new code snippet.",
            "Workspace"
        )
        syncAutoSpeedMetricsIfNeeded()
        rebuildNativePreview(runTopLevelJs = false, logSystemBoot = false, clearPreviousRuntimeLogs = false)
        updateProgressiveAutoWritePreview(force = true)
        triggerDebouncedAutoSave()
    }

    fun cyclePreviewScaleMode() {
        _uiState.update {
            it.copy(previewScaleMode = it.previewScaleMode.next())
        }
    }

    fun togglePreviewExpandedHeight() {
        _uiState.update { current ->
            val nextHeight = if (current.previewSectionHeightDp < 300f) 360f else 195f
            current.copy(previewSectionHeightDp = nextHeight)
        }
        triggerDebouncedAutoSave()
    }

    fun forceReloadPreview() {
        liveJsDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                consoleLogs = emptyList(),
                previewReloadNonce = it.previewReloadNonce + 1L
            )
        }
        rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false, clearPreviousRuntimeLogs = true)
        updateProgressiveAutoWritePreview(force = true)
    }

    fun setWebViewEngineAttached(attached: Boolean) {
        isWebViewEngineAttached = attached
    }

    fun onWebConsoleMessage(
        level: LogLevel,
        message: String,
        sourceLocation: String = "script.js",
        stackTrace: String? = null
    ) {
        val trimmedMsg = message.trim()
        if (trimmedMsg.isEmpty()) return
        _uiState.update { current ->
            val lastEntry = current.consoleLogs.lastOrNull()
            // Avoid rapid duplicate identical console messages within 40ms
            if (lastEntry != null &&
                lastEntry.level == level &&
                lastEntry.message == trimmedMsg &&
                (System.nanoTime() - lastEntry.id) < 40_000_000L
            ) {
                current
            } else {
                current.copy(
                    consoleLogs = (current.consoleLogs + ConsoleLogEntry(
                        level = level,
                        message = trimmedMsg,
                        sourceLocation = sourceLocation.ifBlank { "script.js" },
                        stackTrace = stackTrace?.takeIf { it.isNotBlank() }
                    )).takeLast(120)
                )
            }
        }
    }

    fun onWebConsoleClear() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun rebuildNativePreview(
        runTopLevelJs: Boolean = true,
        logSystemBoot: Boolean = false,
        clearPreviousRuntimeLogs: Boolean = runTopLevelJs
    ) {
        val state = _uiState.value
        if (runTopLevelJs) {
            clearRuntimeDomState()
        }
        val snapshot = NativeLiveEngine.evaluateProject(
            html = state.htmlCode,
            css = state.cssCode,
            js = state.jsCode,
            existingVariables = runtimeVariables,
            domTextOverrides = domTextOverrides,
            domColorOverrides = domColorOverrides,
            domBgOverrides = domBgOverrides,
            runTopLevelJs = runTopLevelJs
        )
        runtimeFunctions = snapshot.functions
        runtimeVariables.putAll(snapshot.variables)

        _uiState.update { current ->
            val baseLogs = if (clearPreviousRuntimeLogs) {
                current.consoleLogs.filter { it.level == LogLevel.SYSTEM }.takeLast(4)
            } else {
                current.consoleLogs
            }
            val updatedLogs = buildList {
                addAll(baseLogs)
                if (logSystemBoot && baseLogs.isEmpty()) {
                    add(
                        ConsoleLogEntry(
                            level = LogLevel.SYSTEM,
                            message = "Live Web & Native preview ready.",
                            sourceLocation = "system"
                        )
                    )
                }
                // When the Chromium WebView engine is attached, V8 emits authoritative console logs directly
                if (runTopLevelJs && !isWebViewEngineAttached) {
                    addAll(snapshot.logs)
                }
            }.takeLast(120)

            current.copy(
                domNodes = snapshot.rootNodes,
                bodyStyle = snapshot.bodyStyle,
                consoleLogs = updatedLogs
            )
        }
    }

    private fun updateProgressiveAutoWritePreview(force: Boolean = false) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        if (!force && state.isAutoWritePlaying) {
            val code = state.autoWriteFullCode
            val idx = (state.typedCharCount - 1).coerceIn(0, max(0, code.length - 1))
            val lastChar = if (code.isNotEmpty()) code[idx] else ' '
            val isStructuralChar = lastChar == '>' || lastChar == ';' || lastChar == '}' || lastChar == '\n'
            // Throttle non-structural updates to ~35 FPS (28ms) during ultra-fast burst typing on large code
            if (!isStructuralChar && (now - lastAutoPreviewTimeMs) < 28L) {
                return
            }
        }

        val visible = state.autoWriteVisibleCode

        val rawHtml = when (state.autoWriteTab) {
            CodeTab.INDEX_HTML -> visible
            CodeTab.STYLE_CSS, CodeTab.SCRIPT_JS -> state.htmlCode
        }
        val effectiveHtml = NativeLiveEngine.normalizeProgressiveHtml(rawHtml)
        val effectiveCss = when (state.autoWriteTab) {
            CodeTab.INDEX_HTML -> if (state.isAutoChainTabs) "" else state.cssCode
            CodeTab.STYLE_CSS -> visible
            CodeTab.SCRIPT_JS -> state.cssCode
        }
        val effectiveJs = when (state.autoWriteTab) {
            CodeTab.INDEX_HTML, CodeTab.STYLE_CSS -> ""
            CodeTab.SCRIPT_JS -> {
                if (state.typedCharCount >= state.jsCode.length) {
                    visible
                } else {
                    // Only run a progressive JS prefix when all braces/parens/strings are balanced
                    val lastNewline = visible.lastIndexOf('\n')
                    val candidate = if (lastNewline >= 0) visible.substring(0, lastNewline) else ""
                    if (candidate.isNotBlank() && WebSandboxEngine.isJsSyntacticallyComplete(candidate)) {
                        candidate
                    } else {
                        lastAutoPreviewJs.takeIf { it != "__UNSET__" } ?: ""
                    }
                }
            }
        }

        if (!force &&
            effectiveHtml == lastAutoPreviewHtml &&
            effectiveCss == lastAutoPreviewCss &&
            effectiveJs == lastAutoPreviewJs
        ) {
            return
        }

        lastAutoPreviewHtml = effectiveHtml
        lastAutoPreviewCss = effectiveCss
        lastAutoPreviewJs = effectiveJs
        lastAutoPreviewTimeMs = now

        val runJsNow = state.autoWriteTab == CodeTab.SCRIPT_JS && effectiveJs.isNotBlank()

        val progressiveSnapshot = NativeLiveEngine.evaluateProject(
            html = effectiveHtml,
            css = effectiveCss,
            js = effectiveJs,
            existingVariables = runtimeVariables,
            domTextOverrides = domTextOverrides,
            domColorOverrides = domColorOverrides,
            domBgOverrides = domBgOverrides,
            runTopLevelJs = runJsNow
        )

        if (state.autoWriteTab == CodeTab.SCRIPT_JS && progressiveSnapshot.functions.isNotEmpty()) {
            runtimeFunctions = progressiveSnapshot.functions
        }

        _uiState.update {
            it.copy(
                autoWriteDomNodes = progressiveSnapshot.rootNodes,
                autoWriteBodyStyle = progressiveSnapshot.bodyStyle,
                autoWriteEffectiveHtml = effectiveHtml,
                autoWriteEffectiveCss = effectiveCss,
                autoWriteEffectiveJs = effectiveJs
            )
        }
    }

    fun onNativeDomButtonClicked(onClickAttr: String) {
        val state = _uiState.value
        val cleanName = onClickAttr.substringBefore("(").trim()
        if (!runtimeFunctions.containsKey(cleanName) && state.jsCode.isNotBlank()) {
            val preload = NativeLiveEngine.evaluateProject(
                html = state.htmlCode,
                css = state.cssCode,
                js = state.jsCode,
                existingVariables = runtimeVariables,
                domTextOverrides = domTextOverrides,
                domColorOverrides = domColorOverrides,
                domBgOverrides = domBgOverrides,
                runTopLevelJs = runtimeVariables.isEmpty()
            )
            runtimeFunctions = preload.functions
            runtimeVariables.putAll(preload.variables)
        }

        val newLogs = NativeLiveEngine.invokeJsFunction(
            functionName = onClickAttr,
            functions = runtimeFunctions,
            variables = runtimeVariables,
            textOverrides = domTextOverrides,
            colorOverrides = domColorOverrides,
            bgOverrides = domBgOverrides
        )
        val refreshed = NativeLiveEngine.evaluateProject(
            html = state.htmlCode,
            css = state.cssCode,
            js = state.jsCode,
            existingVariables = runtimeVariables,
            domTextOverrides = domTextOverrides,
            domColorOverrides = domColorOverrides,
            domBgOverrides = domBgOverrides,
            runTopLevelJs = false
        )
        val hasError = newLogs.any { it.level == LogLevel.ERROR }
        _uiState.update { current ->
            val isIde = current.activeModule == StudioModule.CODEPAD_IDE
            current.copy(
                domNodes = refreshed.rootNodes,
                bodyStyle = refreshed.bodyStyle,
                autoWriteDomNodes = refreshed.rootNodes,
                autoWriteBodyStyle = refreshed.bodyStyle,
                consoleLogs = (current.consoleLogs + newLogs).takeLast(120),
                isConsoleVisible = if (hasError && isIde) true else current.isConsoleVisible,
                isPreviewVisible = if (hasError && isIde) false else current.isPreviewVisible
            )
        }
    }

    fun triggerSimulatedStackTraceError() {
        appendConsoleLog(
            level = LogLevel.ERROR,
            message = "Uncaught ReferenceError: undefinedVariable is not defined",
            sourceLocation = "script.js:18:5",
            stackTrace = """
                ReferenceError: undefinedVariable is not defined
                    at handleAction (script.js:18:5)
                    at HTMLButtonElement.onclick (index.html:12:9)
            """.trimIndent()
        )
        _uiState.update { it.copy(isConsoleVisible = true, isPreviewVisible = false) }
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun setConsoleFilter(level: LogLevel?) {
        _uiState.update { it.copy(consoleFilter = level) }
    }

    fun togglePreview() {
        _uiState.update {
            val next = !it.isPreviewVisible
            it.copy(
                isPreviewVisible = next,
                isConsoleVisible = if (next) false else it.isConsoleVisible
            )
        }
    }

    fun toggleConsole() {
        _uiState.update {
            val next = !it.isConsoleVisible
            it.copy(
                isConsoleVisible = next,
                isPreviewVisible = if (next) false else it.isPreviewVisible
            )
        }
    }

    fun setSettingsSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsSheetOpen = open) }
    }

    fun setRenameDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isRenameDialogOpen = open) }
    }

    fun setExportSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isExportSheetOpen = open) }
    }

    fun dismissExportResult() {
        _uiState.update { it.copy(latestExportResult = null) }
    }

    fun exportToAppStorage(context: Context, format: ExportFormat) {
        val appContext = context.applicationContext
        val s = _uiState.value
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    ProjectExportEngine.exportToLocalAppDirectory(
                        context = appContext,
                        projectName = s.projectName,
                        htmlCode = s.htmlCode,
                        cssCode = s.cssCode,
                        jsCode = s.jsCode,
                        format = format
                    )
                }
                _uiState.update {
                    it.copy(
                        latestExportResult = res,
                        isExportSheetOpen = false
                    )
                }
                appendConsoleLog(
                    LogLevel.SYSTEM,
                    "Exported ${res.fileName} (${res.sizeBytes} bytes)",
                    "Export"
                )
            } catch (t: Throwable) {
                appendConsoleLog(
                    LogLevel.ERROR,
                    "Export failed: ${t.message}",
                    "Export",
                    t.stackTraceToString()
                )
            }
        }
    }

    fun exportToUserUri(context: Context, uri: Uri, format: ExportFormat) {
        val appContext = context.applicationContext
        val s = _uiState.value
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    ProjectExportEngine.writeToDocumentUri(
                        context = appContext,
                        uri = uri,
                        projectName = s.projectName,
                        htmlCode = s.htmlCode,
                        cssCode = s.cssCode,
                        jsCode = s.jsCode,
                        format = format
                    )
                }
                _uiState.update {
                    it.copy(
                        latestExportResult = res,
                        isExportSheetOpen = false
                    )
                }
                appendConsoleLog(
                    LogLevel.SYSTEM,
                    "Saved ${res.fileName} (${res.sizeBytes} bytes)",
                    "Export"
                )
            } catch (t: Throwable) {
                appendConsoleLog(
                    LogLevel.ERROR,
                    "Save failed: ${t.message}",
                    "Export",
                    t.stackTraceToString()
                )
            }
        }
    }

    private fun appendConsoleLog(
        level: LogLevel,
        message: String,
        sourceLocation: String,
        stackTrace: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                consoleLogs = (state.consoleLogs + ConsoleLogEntry(
                    level = level,
                    message = message,
                    sourceLocation = sourceLocation,
                    stackTrace = stackTrace
                )).takeLast(120)
            )
        }
    }

    // =========================================================================
    // MODULE 2: AUTO WRITE STUDIO (CLEAN SILENT ANIMATION ENGINE - NO POPUPS)
    // =========================================================================

    fun selectAutoWriteTab(tab: CodeTab) {
        typewriterJob?.cancel()
        if (tab == CodeTab.INDEX_HTML) {
            clearRuntimeDomState()
        }
        _uiState.update {
            it.copy(
                autoWriteTab = tab,
                typedCharCount = 0,
                autoWriteCurrentLine = 1,
                autoWriteCurrentColumn = 1,
                isAutoWritePlaying = false,
                isEndCtaActive = false
            )
        }
        syncAutoSpeedMetricsIfNeeded()
        updateProgressiveAutoWritePreview(force = true)
        startOrResumeAutoWrite()
    }

    fun selectAutoDurationTarget(target: AutoDurationTarget) {
        _uiState.update { it.copy(autoDurationTarget = target) }
        syncAutoSpeedMetricsIfNeeded()
        triggerDebouncedAutoSave()
    }

    private fun syncAutoSpeedMetricsIfNeeded() {
        _uiState.update { current ->
            val hasPreviewShowcase = current.isFloatingPreviewOpen ||
                current.recordingLayoutConfig.layoutMode != RecordingLayoutMode.EDITOR_ONLY ||
                current.autoRevealPreviewOnFinish
            val showcaseMs = if (hasPreviewShowcase) {
                current.recordingLayoutConfig.getEffectiveShowcaseDurationMs(current.autoDurationTarget)
            } else {
                0L
            }

            val metrics = AutoSpeedOptimizer.analyze(
                htmlCode = current.htmlCode,
                cssCode = current.cssCode,
                jsCode = current.jsCode,
                activeTab = current.autoWriteTab,
                isAutoChain = current.isAutoChainTabs,
                showEndCta = current.showEndCtaAnimation,
                target = current.autoDurationTarget,
                manualSpeedLevel = current.speedLevel,
                manualTurbo = current.isTurboMode,
                hasPreviewShowcase = hasPreviewShowcase,
                showcaseDurationMs = showcaseMs
            )
            if (current.autoDurationTarget == AutoDurationTarget.MANUAL) {
                current.copy(sequenceMetrics = metrics)
            } else {
                current.copy(
                    speedLevel = metrics.recommendedSpeedLevel,
                    isTurboMode = metrics.requiresTurboBurst,
                    sequenceMetrics = metrics
                )
            }
        }
    }

    fun setSpeedLevel(level: Int) {
        val clamped = level.coerceIn(1, 20)
        _uiState.update {
            it.copy(
                speedLevel = clamped,
                autoDurationTarget = AutoDurationTarget.MANUAL
            )
        }
        syncAutoSpeedMetricsIfNeeded()
        triggerDebouncedAutoSave()
    }

    fun toggleTurboMode() {
        _uiState.update {
            it.copy(
                isTurboMode = !it.isTurboMode,
                autoDurationTarget = AutoDurationTarget.MANUAL
            )
        }
        syncAutoSpeedMetricsIfNeeded()
        triggerDebouncedAutoSave()
    }

    fun updateCreatorHandle(handle: String) {
        _uiState.update { it.copy(creatorHandle = handle.trim().ifEmpty { "@creator" }) }
        triggerDebouncedAutoSave()
    }

    fun selectWatermarkPosition(position: WatermarkPosition) {
        _uiState.update { it.copy(watermarkPosition = position) }
        triggerDebouncedAutoSave()
    }

    fun selectWatermarkStyle(style: WatermarkStyle) {
        _uiState.update { it.copy(watermarkStyle = style) }
        triggerDebouncedAutoSave()
    }

    fun updateWatermarkOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.15f, 1.0f)
        _uiState.update { it.copy(watermarkOpacity = clamped) }
        triggerDebouncedAutoSave()
    }

    fun selectWatermarkSizeScale(scale: OverlaySizeScale) {
        _uiState.update { it.copy(watermarkSizeScale = scale) }
        triggerDebouncedAutoSave()
    }

    fun toggleShowEndCtaAnimation() {
        _uiState.update {
            val next = !it.showEndCtaAnimation
            it.copy(
                showEndCtaAnimation = next,
                isEndCtaActive = if (!next) false else it.isEndCtaActive
            )
        }
        syncAutoSpeedMetricsIfNeeded()
        triggerDebouncedAutoSave()
    }

    fun selectEndCtaStyle(style: EndCtaStyle) {
        _uiState.update {
            it.copy(
                endCtaStyle = style,
                ctaTransparentBg = if (style == EndCtaStyle.TRANSPARENT_BAR || style == EndCtaStyle.MINIMAL_CHIPS) {
                    true
                } else {
                    it.ctaTransparentBg
                }
            )
        }
        triggerDebouncedAutoSave()
    }

    fun selectEndCtaPosition(position: EndCtaPosition) {
        _uiState.update { it.copy(endCtaPosition = position) }
        triggerDebouncedAutoSave()
    }

    fun selectCtaDisplayTiming(timing: CtaDisplayTiming) {
        _uiState.update { it.copy(ctaDisplayTiming = timing) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaTransparentBg() {
        _uiState.update { it.copy(ctaTransparentBg = !it.ctaTransparentBg) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaShowBorder() {
        _uiState.update { it.copy(ctaShowBorder = !it.ctaShowBorder) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaShowHandle() {
        _uiState.update { it.copy(ctaShowHandle = !it.ctaShowHandle) }
        triggerDebouncedAutoSave()
    }

    fun selectCtaAccentIndex(index: Int) {
        _uiState.update { it.copy(ctaAccentIndex = index) }
        triggerDebouncedAutoSave()
    }

    fun selectCtaSizeScale(scale: OverlaySizeScale) {
        _uiState.update { it.copy(ctaSizeScale = scale) }
        triggerDebouncedAutoSave()
    }

    fun updateEndCtaMessage(message: String) {
        _uiState.update { it.copy(endCtaMessage = message) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaLike() {
        _uiState.update { it.copy(ctaShowLike = !it.ctaShowLike) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaComment() {
        _uiState.update { it.copy(ctaShowComment = !it.ctaShowComment) }
        triggerDebouncedAutoSave()
    }

    fun toggleCtaSubscribe() {
        _uiState.update { it.copy(ctaShowSubscribe = !it.ctaShowSubscribe) }
        triggerDebouncedAutoSave()
    }

    fun updateCtaLikeLabel(label: String) {
        _uiState.update { it.copy(ctaLikeLabel = label) }
        triggerDebouncedAutoSave()
    }

    fun updateCtaCommentLabel(label: String) {
        _uiState.update { it.copy(ctaCommentLabel = label) }
        triggerDebouncedAutoSave()
    }

    fun updateCtaSubscribeLabel(label: String) {
        _uiState.update { it.copy(ctaSubscribeLabel = label) }
        triggerDebouncedAutoSave()
    }

    fun updateCtaSubscribedLabel(label: String) {
        _uiState.update { it.copy(ctaSubscribedLabel = label) }
        triggerDebouncedAutoSave()
    }

    fun applyCtaPlatformPreset(preset: CtaPlatformQuickPreset) {
        _uiState.update {
            it.copy(
                ctaLikeLabel = preset.likeLabel,
                ctaCommentLabel = preset.commentLabel,
                ctaSubscribeLabel = preset.subscribeLabel,
                ctaSubscribedLabel = preset.subscribedLabel,
                endCtaMessage = preset.message
            )
        }
        triggerDebouncedAutoSave()
    }

    fun triggerEndCtaPreview() {
        _uiState.update {
            it.copy(
                showEndCtaAnimation = true,
                isEndCtaActive = true,
                isCreatorCustomizeOpen = false
            )
        }
    }

    fun dismissEndCta() {
        _uiState.update { it.copy(isEndCtaActive = false) }
    }

    fun toggleAutoChainTabs() {
        _uiState.update { it.copy(isAutoChainTabs = !it.isAutoChainTabs) }
        syncAutoSpeedMetricsIfNeeded()
        triggerDebouncedAutoSave()
    }

    fun toggleAutoRevealPreview() {
        _uiState.update { it.copy(autoRevealPreviewOnFinish = !it.autoRevealPreviewOnFinish) }
        triggerDebouncedAutoSave()
    }

    fun selectBackdropPreset(index: Int) {
        _uiState.update { it.copy(backdropPresetIndex = index) }
        triggerDebouncedAutoSave()
    }

    fun toggleShowViralHookBanner() {
        _uiState.update { it.copy(showViralHookBanner = !it.showViralHookBanner) }
        triggerDebouncedAutoSave()
    }

    fun updateViralHookBadge(badge: String) {
        _uiState.update { it.copy(viralHookBadge = badge) }
        triggerDebouncedAutoSave()
    }

    fun updateViralHookTitle(title: String) {
        _uiState.update { it.copy(viralHookTitle = title) }
        triggerDebouncedAutoSave()
    }

    fun selectViralHookStyle(style: ViralHookStyle) {
        _uiState.update {
            it.copy(
                viralHookStyle = style,
                viralHookTransparentBg = if (style == ViralHookStyle.TRANSPARENT_CLEAN) {
                    true
                } else {
                    it.viralHookTransparentBg
                }
            )
        }
        triggerDebouncedAutoSave()
    }

    fun selectViralHookPosition(position: ViralHookPosition) {
        _uiState.update { it.copy(viralHookPosition = position) }
        triggerDebouncedAutoSave()
    }

    fun selectViralHookAlignment(alignment: ViralHookAlignment) {
        _uiState.update { it.copy(viralHookAlignment = alignment) }
        triggerDebouncedAutoSave()
    }

    fun toggleViralHookTransparentBg() {
        _uiState.update { it.copy(viralHookTransparentBg = !it.viralHookTransparentBg) }
        triggerDebouncedAutoSave()
    }

    fun toggleViralHookShowBorder() {
        _uiState.update { it.copy(viralHookShowBorder = !it.viralHookShowBorder) }
        triggerDebouncedAutoSave()
    }

    fun toggleViralHookShowBadge() {
        _uiState.update { it.copy(viralHookShowBadge = !it.viralHookShowBadge) }
        triggerDebouncedAutoSave()
    }

    fun selectViralHookAccentIndex(index: Int) {
        _uiState.update { it.copy(viralHookAccentIndex = index) }
        triggerDebouncedAutoSave()
    }

    fun selectViralHookSizeScale(scale: OverlaySizeScale) {
        _uiState.update { it.copy(viralHookSizeScale = scale) }
        triggerDebouncedAutoSave()
    }

    fun applyViralHookQuickPreset(preset: ViralHookQuickPreset) {
        _uiState.update {
            it.copy(
                viralHookBadge = preset.badge,
                viralHookTitle = preset.title,
                showViralHookBanner = true
            )
        }
        triggerDebouncedAutoSave()
    }

    fun selectTypingSoundProfile(profile: TypingSoundProfile) {
        _uiState.update { it.copy(typingSoundProfile = profile) }
        if (profile == TypingSoundProfile.OFF) {
            TypingSoundEngine.release()
        } else {
            TypingSoundEngine.playKeyTick(
                profile = profile,
                typedChar = 'a',
                isManualTest = true,
                volume = _uiState.value.typingSoundVolume,
                minGapMs = _uiState.value.typingSoundMinGapMs,
                pitchVariation = _uiState.value.typingSoundPitchVar,
                pitchMultiplier = _uiState.value.typingSoundPitchMultiplier,
                stabilizerResonance = _uiState.value.typingSoundStabilizerResonance
            )
        }
        triggerDebouncedAutoSave()
    }

    fun cycleTypingSoundProfile() {
        val entries = TypingSoundProfile.entries
        val nextIdx = (_uiState.value.typingSoundProfile.ordinal + 1) % entries.size
        val nextProfile = entries[nextIdx]
        selectTypingSoundProfile(nextProfile)
    }

    fun updateTypingSoundVolume(volume: Float) {
        val clamped = volume.coerceIn(0.1f, 1.0f)
        _uiState.update { it.copy(typingSoundVolume = clamped) }
        triggerDebouncedAutoSave()
    }

    fun updateTypingSoundMinGap(gapMs: Long) {
        val clamped = gapMs.coerceIn(40L, 200L)
        _uiState.update { it.copy(typingSoundMinGapMs = clamped) }
        triggerDebouncedAutoSave()
    }

    fun toggleTypingSoundPitchVar() {
        _uiState.update { it.copy(typingSoundPitchVar = !it.typingSoundPitchVar) }
        triggerDebouncedAutoSave()
    }

    fun toggleAutoWriteShowGutter() {
        _uiState.update { it.copy(autoWriteShowGutter = !it.autoWriteShowGutter) }
        triggerDebouncedAutoSave()
    }

    fun toggleUseRecordingCountdown() {
        _uiState.update { it.copy(useRecordingCountdown = !it.useRecordingCountdown) }
        triggerDebouncedAutoSave()
    }

    fun toggleKeepPreviewOpenInRecord() {
        _uiState.update { it.copy(keepPreviewOpenInRecord = !it.keepPreviewOpenInRecord) }
        triggerDebouncedAutoSave()
    }

    fun setCreatorCustomizeOpen(open: Boolean) {
        _uiState.update { it.copy(isCreatorCustomizeOpen = open) }
    }

    fun setViralTemplatesSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isViralTemplatesSheetOpen = open) }
    }

    fun adjustPreviewSectionHeight(deltaDp: Float) {
        _uiState.update { current ->
            val updated = (current.previewSectionHeightDp + deltaDp).coerceIn(85f, 460f)
            current.copy(previewSectionHeightDp = updated)
        }
        triggerDebouncedAutoSave()
    }

    fun setPreviewSectionHeight(heightDp: Float) {
        _uiState.update { current ->
            current.copy(previewSectionHeightDp = heightDp.coerceIn(85f, 460f))
        }
        triggerDebouncedAutoSave()
    }

    private var pendingInAppRecorderLaunch: (() -> Unit)? = null

    /**
     * Enters Clean Recording Mode and starts typing cleanly from 0% (with 3-2-1 countdown followed by start delay).
     * NOTE: Screen recording ONLY starts AFTER countdown and start delay have finished, so neither the
     * countdown overlay nor the start delay appear on the recorded video.
     */
    fun startCleanShortsRecording(onLaunchInAppRecorder: (() -> Unit)? = null) {
        typewriterJob?.cancel()
        countdownJob?.cancel()
        pendingInAppRecorderLaunch = onLaunchInAppRecorder
        clearRuntimeDomState()
        val useCountdown = _uiState.value.useRecordingCountdown
        val startDelay = _uiState.value.recordingStartDelayMs
        _uiState.update {
            it.copy(
                isRecordingMode = true,
                isCreatorCustomizeOpen = false,
                isViralTemplatesSheetOpen = false,
                isAutoWritePlaying = false,
                isEndCtaActive = false,
                isFloatingPreviewOpen = when (it.recordingLayoutConfig.layoutMode) {
                    RecordingLayoutMode.EDITOR_ONLY -> false
                    RecordingLayoutMode.EDITOR_THEN_FULL_PREVIEW_AT_END -> false
                    else -> true
                },
                isEndFullscreenPreviewRevealed = false,
                recordingCountdownSeconds = if (useCountdown) 3 else 0,
                isRecordingStartingDelay = false,
                typedCharCount = 0,
                autoWriteCurrentLine = 1,
                autoWriteCurrentColumn = 1,
                autoWriteTab = if (it.isAutoChainTabs) CodeTab.INDEX_HTML else it.autoWriteTab
            )
        }
        updateProgressiveAutoWritePreview(force = true)

        if (useCountdown) {
            countdownJob = viewModelScope.launch {
                for (sec in 3 downTo 1) {
                    _uiState.update { it.copy(recordingCountdownSeconds = sec, isRecordingStartingDelay = false) }
                    delay(850L)
                }
                // Post-Countdown Start Delay (e.g. 1.0s breathing pause so video starts cleanly)
                if (startDelay > 0) {
                    _uiState.update { it.copy(recordingCountdownSeconds = 0, isRecordingStartingDelay = true) }
                    delay(startDelay)
                }
                _uiState.update { it.copy(recordingCountdownSeconds = 0, isRecordingStartingDelay = false) }

                // Screen recording starts after countdown and delay
                if (!ScreenRecorderEngine.state.value.isRecording) {
                    pendingInAppRecorderLaunch?.invoke()
                }
                pendingInAppRecorderLaunch = null

                startOrResumeAutoWrite()
            }
        } else {
            if (startDelay > 0) {
                countdownJob = viewModelScope.launch {
                    _uiState.update { it.copy(isRecordingStartingDelay = true) }
                    delay(startDelay)
                    _uiState.update { it.copy(isRecordingStartingDelay = false) }

                    if (!ScreenRecorderEngine.state.value.isRecording) {
                        pendingInAppRecorderLaunch?.invoke()
                    }
                    pendingInAppRecorderLaunch = null

                    startOrResumeAutoWrite()
                }
            } else {
                if (!ScreenRecorderEngine.state.value.isRecording) {
                    pendingInAppRecorderLaunch?.invoke()
                }
                pendingInAppRecorderLaunch = null
                startOrResumeAutoWrite()
            }
        }
    }

    fun skipRecordingCountdownNow() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                recordingCountdownSeconds = 0,
                isRecordingStartingDelay = false
            )
        }
        if (_uiState.value.isAutoRecordInApp && !ScreenRecorderEngine.state.value.isRecording) {
            pendingInAppRecorderLaunch?.invoke()
        }
        pendingInAppRecorderLaunch = null
        startOrResumeAutoWrite()
    }

    fun setRecordingStartDelayMs(delayMs: Long) {
        _uiState.update { it.copy(recordingStartDelayMs = delayMs.coerceIn(0L, 5000L)) }
        triggerDebouncedAutoSave()
    }

    fun setAutoRecordInApp(enabled: Boolean) {
        _uiState.update { it.copy(isAutoRecordInApp = enabled) }
        ScreenRecorderEngine.setAutoRecordOnShorts(enabled)
        triggerDebouncedAutoSave()
    }

    fun setSavedVideoDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSavedVideoDialogOpen = isOpen) }
    }

    fun setRecorderPrefer1080p(is1080p: Boolean) {
        _uiState.update { it.copy(recorderPrefer1080p = is1080p) }
        ScreenRecorderEngine.setPrefer1080pPref(is1080p)
        triggerDebouncedAutoSave()
    }

    fun setRecorderPrefer60Fps(is60Fps: Boolean) {
        _uiState.update { it.copy(recorderPrefer60Fps = is60Fps) }
        ScreenRecorderEngine.setPrefer60FpsPref(is60Fps)
        triggerDebouncedAutoSave()
    }

    fun setRecorderEnableAudio(enabled: Boolean) {
        val newSource = if (enabled) RecordingAudioSource.SYSTEM_ONLY else RecordingAudioSource.SILENT
        setRecordingAudioSource(newSource)
    }

    fun setRecordingAudioSource(source: RecordingAudioSource) {
        _uiState.update {
            it.copy(
                recordingAudioSource = source,
                recorderEnableAudio = source != RecordingAudioSource.SILENT
            )
        }
        ScreenRecorderEngine.setRecordAudioPref(source != RecordingAudioSource.SILENT)
        triggerDebouncedAutoSave()
    }

    fun cycleRecordingAudioSource() {
        val current = _uiState.value.recordingAudioSource
        val all = RecordingAudioSource.entries
        val nextIndex = (all.indexOf(current) + 1) % all.size
        setRecordingAudioSource(all[nextIndex])
    }

    fun updateBgmConfig(config: BgmConfig) {
        _uiState.update { it.copy(bgmConfig = config) }
    }

    fun toggleBgm(enabled: Boolean, context: Context? = null) {
        val updated = _uiState.value.bgmConfig.copy(isEnabled = enabled)
        _uiState.update { it.copy(bgmConfig = updated) }
        if (!enabled) {
            BackgroundMusicEngine.stopPlayback()
        } else if (_uiState.value.isAutoWritePlaying && context != null) {
            BackgroundMusicEngine.startPlayback(context, updated, _uiState.value.autoDurationTarget)
        }
    }

    fun importCustomBgmFile(context: Context, uri: Uri, fileName: String) {
        val imported = BackgroundMusicEngine.importAudioFromUri(context, uri)
        if (imported != null) {
            val (path, durationSec) = imported
            val updated = _uiState.value.bgmConfig.copy(
                selectedTrackId = "custom_file",
                trackTitle = fileName.ifBlank { "Custom Audio Track" },
                customAudioPath = path,
                customAudioFileName = fileName,
                customAudioDurationSeconds = durationSec,
                startOffsetSeconds = 0,
                isEnabled = true
            )
            _uiState.update { it.copy(bgmConfig = updated) }
        }
    }

    fun setRecordingLayoutMode(mode: RecordingLayoutMode) {
        val updated = _uiState.value.recordingLayoutConfig.copy(layoutMode = mode)
        _uiState.update { it.copy(recordingLayoutConfig = updated) }
        syncAutoSpeedMetricsIfNeeded()
    }

    fun updateRecordingLayoutConfig(config: RecordingLayoutConfig) {
        _uiState.update { it.copy(recordingLayoutConfig = config) }
        syncAutoSpeedMetricsIfNeeded()
    }

    fun setRecordingLayoutSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isRecordingLayoutSheetOpen = open) }
    }

    fun setRecordingSplitRatio(editorWeight: Float) {
        val clamped = editorWeight.coerceIn(0.20f, 0.80f)
        val updated = _uiState.value.recordingLayoutConfig.copy(editorWeight = clamped)
        _uiState.update { it.copy(recordingLayoutConfig = updated) }
        syncAutoSpeedMetricsIfNeeded()
    }

    fun setEndFullscreenPreviewRevealed(revealed: Boolean) {
        _uiState.update { it.copy(isEndFullscreenPreviewRevealed = revealed) }
    }

    fun handleSingleTapGesture() {
        val state = _uiState.value
        if (state.isAutoWritePlaying) {
            pauseAutoWrite()
        } else {
            if (state.typedCharCount >= state.autoWriteFullCode.length) {
                clearRuntimeDomState()
                _uiState.update {
                    it.copy(
                        autoWriteTab = if (it.isAutoChainTabs && it.autoWriteTab == CodeTab.SCRIPT_JS) {
                            CodeTab.INDEX_HTML
                        } else {
                            it.autoWriteTab
                        },
                        typedCharCount = 0,
                        autoWriteCurrentLine = 1,
                        autoWriteCurrentColumn = 1,
                        isEndCtaActive = false
                    )
                }
                updateProgressiveAutoWritePreview(force = true)
            } else {
                _uiState.update { it.copy(isEndCtaActive = false) }
            }
            startOrResumeAutoWrite()
        }
    }

    fun handleDoubleTapGesture() {
        val nextPreview = !_uiState.value.isFloatingPreviewOpen
        _uiState.update { it.copy(isFloatingPreviewOpen = nextPreview) }
        if (nextPreview) {
            updateProgressiveAutoWritePreview(force = true)
        }
    }

    fun handleTripleTapGesture(context: Context? = null) {
        val enteringRecord = !_uiState.value.isRecordingMode
        if (enteringRecord) {
            startCleanShortsRecording()
        } else {
            exitRecordingMode(context)
        }
    }

    fun exitRecordingMode(context: Context? = null) {
        countdownJob?.cancel()
        showcaseJob?.cancel()
        BackgroundMusicEngine.stopPlayback()
        _uiState.update {
            it.copy(
                isRecordingMode = false,
                recordingCountdownSeconds = 0,
                isRecordingStartingDelay = false,
                isEndFullscreenPreviewRevealed = false,
                isPreviewShowcaseActive = false,
                previewShowcaseRemainingSec = 0
            )
        }
        if (context != null && ScreenRecorderEngine.state.value.isRecording) {
            ScreenRecorderEngine.stopServiceRecording(context)
        }
    }

    fun restartAutoWrite() {
        showcaseJob?.cancel()
        typewriterJob?.cancel()
        clearRuntimeDomState()
        _uiState.update {
            it.copy(
                typedCharCount = 0,
                autoWriteCurrentLine = 1,
                autoWriteCurrentColumn = 1,
                isAutoWritePlaying = false,
                isEndCtaActive = false,
                isEndFullscreenPreviewRevealed = false,
                isPreviewShowcaseActive = false,
                previewShowcaseRemainingSec = 0
            )
        }
        updateProgressiveAutoWritePreview(force = true)
        startOrResumeAutoWrite()
    }

    fun skipNearCompletionForDemo() {
        val fullCode = _uiState.value.autoWriteFullCode
        val total = fullCode.length
        if (total <= 0) return
        val target = max(0, (total * 0.94f).roundToInt())
        val (line, col) = computeLineAndColumn(fullCode, target)
        _uiState.update {
            it.copy(
                typedCharCount = target,
                autoWriteCurrentLine = line,
                autoWriteCurrentColumn = col,
                isEndCtaActive = false
            )
        }
        updateProgressiveAutoWritePreview(force = true)
        if (!_uiState.value.isAutoWritePlaying) {
            startOrResumeAutoWrite()
        }
    }

    fun startOrResumeAutoWrite(context: Context? = null) {
        typewriterJob?.cancel()
        TypingSoundEngine.resetCadence()
        val ctx = context ?: appContext
        if (ctx != null && _uiState.value.bgmConfig.isEnabled) {
            BackgroundMusicEngine.startPlayback(ctx, _uiState.value.bgmConfig, _uiState.value.autoDurationTarget)
        }
        syncAutoSpeedMetricsIfNeeded()
        _uiState.update { state ->
            val (line, col) = computeLineAndColumn(state.autoWriteFullCode, state.typedCharCount)
            state.copy(
                isAutoWritePlaying = true,
                autoWriteCurrentLine = line,
                autoWriteCurrentColumn = col,
                isEndCtaActive = false
            )
        }

        typewriterJob = viewModelScope.launch {
            var lastTargetMode = _uiState.value.autoDurationTarget
            var lastAutoChain = _uiState.value.isAutoChainTabs
            var remainingSteps = countRemainingSequenceSteps(_uiState.value)
            var typingBudgetDeadlineWallMs = computeWallDeadlineMs(_uiState.value, remainingSteps)

            while (_uiState.value.isAutoWritePlaying) {
                val state = _uiState.value
                if (state.autoDurationTarget != lastTargetMode || state.isAutoChainTabs != lastAutoChain) {
                    lastTargetMode = state.autoDurationTarget
                    lastAutoChain = state.isAutoChainTabs
                    remainingSteps = countRemainingSequenceSteps(state)
                    typingBudgetDeadlineWallMs = computeWallDeadlineMs(state, remainingSteps)
                }

                val fullCode = state.autoWriteFullCode
                val fullLength = fullCode.length

                if (state.typedCharCount >= fullLength) {
                    val nextChainTab = if (!state.isAutoChainTabs) {
                        null
                    } else {
                        when (state.autoWriteTab) {
                            CodeTab.INDEX_HTML -> when {
                                state.cssCode.isNotBlank() -> CodeTab.STYLE_CSS
                                state.jsCode.isNotBlank() -> CodeTab.SCRIPT_JS
                                else -> null
                            }
                            CodeTab.STYLE_CSS -> if (state.jsCode.isNotBlank()) CodeTab.SCRIPT_JS else null
                            CodeTab.SCRIPT_JS -> null
                        }
                    }

                    if (nextChainTab != null) {
                        updateProgressiveAutoWritePreview(force = true)
                        val isShortPreset = state.autoDurationTarget == AutoDurationTarget.SEC_15 ||
                            state.autoDurationTarget == AutoDurationTarget.SEC_20
                        val preSlidePause = if (isShortPreset) 400L else 600L
                        val slideWait = if (isShortPreset) 680L else 800L

                        // 1. Natural pause at 100% of current file so the viewer sees the completed code
                        delay(preSlidePause)
                        _uiState.update {
                            it.copy(
                                autoWriteTab = nextChainTab,
                                typedCharCount = 0,
                                autoWriteCurrentLine = 1,
                                autoWriteCurrentColumn = 1
                            )
                        }
                        remainingSteps = countRemainingSequenceSteps(_uiState.value)
                        updateProgressiveAutoWritePreview(force = true)
                        // 2. Allow the 680ms horizontal slide transition to complete
                        delay(slideWait)
                        continue
                    } else {
                        updateProgressiveAutoWritePreview(force = true)
                        TypingSoundEngine.stopAll()
                        val state = _uiState.value
                        val hasPreview = state.isFloatingPreviewOpen ||
                            state.recordingLayoutConfig.layoutMode != RecordingLayoutMode.EDITOR_ONLY ||
                            state.autoRevealPreviewOnFinish
                        val isRevealMode = state.recordingLayoutConfig.layoutMode == RecordingLayoutMode.EDITOR_THEN_FULL_PREVIEW_AT_END

                        val showcaseMs = if (hasPreview) {
                            state.recordingLayoutConfig.getEffectiveShowcaseDurationMs(state.autoDurationTarget)
                        } else {
                            0L
                        }

                        _uiState.update {
                            it.copy(
                                isAutoWritePlaying = false,
                                isFloatingPreviewOpen = if (it.autoRevealPreviewOnFinish || isRevealMode) true else it.isFloatingPreviewOpen,
                                isEndCtaActive = it.showEndCtaAnimation
                            )
                        }

                        if (isRevealMode) {
                            val delayMs = state.recordingLayoutConfig.revealDelayMs
                            viewModelScope.launch {
                                delay(delayMs)
                                _uiState.update { it.copy(isEndFullscreenPreviewRevealed = true) }
                            }
                        }

                        if (hasPreview && showcaseMs > 0L) {
                            startPreviewShowcase(showcaseMs)
                        } else {
                            BackgroundMusicEngine.stopPlayback()
                            _uiState.update { it.copy(autoWriteFinishedNonce = System.currentTimeMillis()) }
                            onAutoWriteFinished?.invoke()
                        }
                        break
                    }
                }

                val currentChar = fullCode.getOrNull(state.typedCharCount)
                val cadenceMultiplier = when {
                    currentChar == null -> 1.0f
                    currentChar == '\n' -> 1.75f
                    currentChar == ';' || currentChar == '}' || currentChar == '>' -> 1.35f
                    currentChar == '{' || currentChar == '(' || currentChar == ':' || currentChar == '=' -> 1.18f
                    currentChar.isLetterOrDigit() -> 0.85f
                    else -> 1.0f
                }

                var stepsToAdvance = 1
                val effectiveDelayMs: Long
                // Real-time dynamic pacing calculation
                if (state.autoDurationTarget != AutoDurationTarget.MANUAL) {
                    val remSteps = max(1, remainingSteps)
                    val remTransitions = countRemainingTransitions(state)
                    val isShortPreset = state.autoDurationTarget == AutoDurationTarget.SEC_15 ||
                        state.autoDurationTarget == AutoDurationTarget.SEC_20
                    val perTransitionMs = if (isShortPreset) 1080L else 1400L
                    val remainingTypingWallMs = max(
                        50L,
                        typingBudgetDeadlineWallMs - System.currentTimeMillis() - (remTransitions * perTransitionMs)
                    )
                    val idealMsPerStep = remainingTypingWallMs.toFloat() / remSteps.toFloat()
                    val targetMsPerStep = idealMsPerStep * cadenceMultiplier

                    if (targetMsPerStep < 18.0f) {
                        // Batch multiple chars per frame when code is long so typing animation doesn't lag
                        stepsToAdvance = ceil(18.0f / max(1.0f, targetMsPerStep)).toInt().coerceIn(1, 14)
                        effectiveDelayMs = 18L
                    } else {
                        stepsToAdvance = 1
                        effectiveDelayMs = targetMsPerStep.toLong().coerceIn(18L, 95L)
                    }
                } else {
                    val level = state.speedLevel.coerceIn(1, 20)
                    val baseDelayMs = max(18L, (115L - (level * 4.8f)).toLong())
                    val rawDelay = (baseDelayMs * cadenceMultiplier).toLong().coerceAtLeast(18L)
                    if (state.isTurboMode) {
                        stepsToAdvance = 2
                        effectiveDelayMs = max(18L, rawDelay / 2L)
                    } else {
                        stepsToAdvance = 1
                        effectiveDelayMs = rawDelay
                    }
                }

                delay(effectiveDelayMs)

                var actualAdvancedSteps = 0
                var lastTypedChar: Char? = null
                _uiState.update { current ->
                    val code = current.autoWriteFullCode
                    val len = code.length
                    var nextIndex = current.typedCharCount
                    var line = current.autoWriteCurrentLine
                    var col = current.autoWriteCurrentColumn
                    var advanced = 0

                    while (nextIndex < len && advanced < stepsToAdvance) {
                        val ch = code[nextIndex]
                        lastTypedChar = ch
                        nextIndex++
                        if (ch == '\n') {
                            line++
                            col = 1
                            while (nextIndex < len && (code[nextIndex] == ' ' || code[nextIndex] == '\t')) {
                                nextIndex++
                                col++
                            }
                        } else {
                            col++
                        }
                        advanced++
                    }
                    actualAdvancedSteps = advanced

                    current.copy(
                        typedCharCount = nextIndex.coerceAtMost(len),
                        autoWriteCurrentLine = line,
                        autoWriteCurrentColumn = col
                    )
                }
                remainingSteps = max(1, remainingSteps - actualAdvancedSteps)

                // Synthesized ASMR Mechanical Key Tick - cadence-controlled with randomized intervals
                val currentSnapshot = _uiState.value
                val soundProfile = currentSnapshot.typingSoundProfile
                if (soundProfile != TypingSoundProfile.OFF && actualAdvancedSteps > 0) {
                    TypingSoundEngine.playKeyTick(
                        profile = soundProfile,
                        typedChar = lastTypedChar,
                        isManualTest = false,
                        volume = currentSnapshot.typingSoundVolume,
                        minGapMs = currentSnapshot.typingSoundMinGapMs,
                        pitchVariation = currentSnapshot.typingSoundPitchVar,
                        pitchMultiplier = currentSnapshot.typingSoundPitchMultiplier,
                        stabilizerResonance = currentSnapshot.typingSoundStabilizerResonance
                    )
                }

                // Real-time Live Preview update (deduplicated & frame-paced for large code)
                updateProgressiveAutoWritePreview(force = false)
            }
        }
    }

    private fun countRemainingTransitions(state: StudioUiState): Int {
        if (!state.isAutoChainTabs) return 0
        return when (state.autoWriteTab) {
            CodeTab.INDEX_HTML -> (if (state.cssCode.isNotBlank()) 1 else 0) + (if (state.jsCode.isNotBlank()) 1 else 0)
            CodeTab.STYLE_CSS -> if (state.jsCode.isNotBlank()) 1 else 0
            CodeTab.SCRIPT_JS -> 0
        }
    }

    private fun countRemainingSequenceSteps(state: StudioUiState): Int {
        val currentRem = AutoSpeedOptimizer.countStepsAndWeight(
            state.autoWriteFullCode,
            state.typedCharCount
        ).first
        if (!state.isAutoChainTabs) return max(1, currentRem)

        val upcomingSteps = when (state.autoWriteTab) {
            CodeTab.INDEX_HTML -> {
                AutoSpeedOptimizer.countStepsAndWeight(state.cssCode, 0).first +
                    AutoSpeedOptimizer.countStepsAndWeight(state.jsCode, 0).first
            }
            CodeTab.STYLE_CSS -> {
                AutoSpeedOptimizer.countStepsAndWeight(state.jsCode, 0).first
            }
            CodeTab.SCRIPT_JS -> 0
        }
        return max(1, currentRem + upcomingSteps)
    }

    private fun computeWallDeadlineMs(state: StudioUiState, remSteps: Int = countRemainingSequenceSteps(state)): Long {
        val now = System.currentTimeMillis()
        val target = state.autoDurationTarget
        if (target == AutoDurationTarget.MANUAL) return now + 60_000L

        val totalSteps = max(1, state.sequenceMetrics.effectiveSteps)
        val remFraction = (remSteps.toFloat() / totalSteps.toFloat()).coerceIn(0.05f, 1.0f)

        val isShortPreset = target == AutoDurationTarget.SEC_15 || target == AutoDurationTarget.SEC_20
        val outroReserveMs = if (state.showEndCtaAnimation) {
            if (isShortPreset) 1300L else 2000L
        } else {
            400L
        }
        val hasPreview = state.isFloatingPreviewOpen ||
            state.recordingLayoutConfig.layoutMode != RecordingLayoutMode.EDITOR_ONLY ||
            state.autoRevealPreviewOnFinish
        val showcaseReserveMs = if (hasPreview) {
            state.recordingLayoutConfig.getEffectiveShowcaseDurationMs(target)
        } else {
            0L
        }
        val usableTargetMs = max(2500L, (target.seconds * 1000L) - outroReserveMs - showcaseReserveMs)
        return now + (usableTargetMs * remFraction).toLong()
    }

    private var showcaseJob: Job? = null

    private fun startPreviewShowcase(showcaseMs: Long) {
        showcaseJob?.cancel()
        showcaseJob = viewModelScope.launch {
            val totalSec = max(1, (showcaseMs / 1000f).roundToInt())
            _uiState.update {
                it.copy(
                    isPreviewShowcaseActive = true,
                    previewShowcaseTotalSec = totalSec,
                    previewShowcaseRemainingSec = totalSec
                )
            }

            // Ensure preview DOM has top-level JS active so animations, canvas, timers run during showcase
            rebuildNativePreview(runTopLevelJs = true, logSystemBoot = false)

            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val remainingMs = showcaseMs - elapsed
                if (remainingMs <= 0L) break

                val remSec = max(1, ceil(remainingMs / 1000f).toInt())
                _uiState.update { it.copy(previewShowcaseRemainingSec = remSec) }
                delay(200L)
            }

            // Showcase completed cleanly!
            _uiState.update {
                it.copy(
                    isPreviewShowcaseActive = false,
                    previewShowcaseRemainingSec = 0,
                    autoWriteFinishedNonce = System.currentTimeMillis()
                )
            }
            BackgroundMusicEngine.stopPlayback()
            onAutoWriteFinished?.invoke()
        }
    }

    fun pauseAutoWrite() {
        typewriterJob?.cancel()
        showcaseJob?.cancel()
        TypingSoundEngine.stopAll()
        BackgroundMusicEngine.stopPlayback()
        _uiState.update {
            it.copy(
                isAutoWritePlaying = false,
                isPreviewShowcaseActive = false,
                previewShowcaseRemainingSec = 0
            )
        }
    }

    // --- Productivity Suite: Undo / Redo ---
    private val undoHistory = mutableMapOf<CodeTab, ArrayDeque<String>>()
    private val redoHistory = mutableMapOf<CodeTab, ArrayDeque<String>>()

    fun pushUndoCheckpoint(tab: CodeTab, code: String) {
        val stack = undoHistory.getOrPut(tab) { ArrayDeque() }
        if (stack.lastOrNull() != code) {
            stack.addLast(code)
            if (stack.size > 35) stack.removeFirst()
        }
        redoHistory[tab]?.clear()
    }

    fun undoActiveTabCode(): Pair<String, Int>? {
        val tab = _uiState.value.activeTab
        val stack = undoHistory[tab] ?: return null
        if (stack.isEmpty()) return null
        val currentCode = _uiState.value.activeCode
        val redoStack = redoHistory.getOrPut(tab) { ArrayDeque() }
        redoStack.addLast(currentCode)
        val prev = stack.removeLast()
        applyTabCodeDirectly(tab, prev)
        return prev to prev.length.coerceAtMost(300)
    }

    fun redoActiveTabCode(): Pair<String, Int>? {
        val tab = _uiState.value.activeTab
        val redoStack = redoHistory[tab] ?: return null
        if (redoStack.isEmpty()) return null
        val currentCode = _uiState.value.activeCode
        val undoStack = undoHistory.getOrPut(tab) { ArrayDeque() }
        undoStack.addLast(currentCode)
        val next = redoStack.removeLast()
        applyTabCodeDirectly(tab, next)
        return next to next.length.coerceAtMost(300)
    }

    private fun applyTabCodeDirectly(tab: CodeTab, code: String) {
        _uiState.update { state ->
            when (tab) {
                CodeTab.INDEX_HTML -> state.copy(htmlCode = code, isSaved = false)
                CodeTab.STYLE_CSS -> state.copy(cssCode = code, isSaved = false)
                CodeTab.SCRIPT_JS -> state.copy(jsCode = code, isSaved = false)
            }
        }
        triggerDebouncedAutoSave()
        rebuildNativePreview(runTopLevelJs = true)
        updateProgressiveAutoWritePreview()
    }

    // --- Productivity Suite: In-Code Find & Replace ---
    fun toggleFindReplace(open: Boolean? = null) {
        val next = open ?: !_uiState.value.isFindReplaceOpen
        _uiState.update { it.copy(isFindReplaceOpen = next) }
        if (next) updateFindMatches()
    }

    fun setFindQuery(query: String) {
        _uiState.update { it.copy(findQuery = query) }
        updateFindMatches()
    }

    fun setReplaceQuery(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun toggleFindMatchCase() {
        _uiState.update { it.copy(isFindMatchCase = !it.isFindMatchCase) }
        updateFindMatches()
    }

    fun toggleFindUseRegex() {
        _uiState.update { it.copy(isFindUseRegex = !it.isFindUseRegex) }
        updateFindMatches()
    }

    private fun getFindMatches(code: String, query: String, matchCase: Boolean, useRegex: Boolean): List<IntRange> {
        if (query.isEmpty()) return emptyList()
        return try {
            if (useRegex) {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                val regex = Regex(query, options)
                regex.findAll(code).map { it.range }.toList()
            } else {
                val matches = mutableListOf<IntRange>()
                var idx = 0
                while (idx < code.length) {
                    val found = code.indexOf(query, idx, ignoreCase = !matchCase)
                    if (found == -1) break
                    matches.add(found until (found + query.length))
                    idx = found + max(1, query.length)
                }
                matches
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun updateFindMatches() {
        val state = _uiState.value
        val code = state.activeCode
        val matches = getFindMatches(code, state.findQuery, state.isFindMatchCase, state.isFindUseRegex)
        val currentIdx = if (matches.isEmpty()) 0 else state.findCurrentMatchIndex.coerceIn(0, matches.size - 1)
        val activeRange = if (matches.isNotEmpty()) matches[currentIdx].first to matches[currentIdx].last + 1 else null
        _uiState.update {
            it.copy(
                findTotalMatches = matches.size,
                findCurrentMatchIndex = currentIdx,
                activeMatchRange = activeRange
            )
        }
    }

    fun findNextMatch(): Pair<Int, Int>? {
        val state = _uiState.value
        val matches = getFindMatches(state.activeCode, state.findQuery, state.isFindMatchCase, state.isFindUseRegex)
        if (matches.isEmpty()) return null
        val nextIdx = (state.findCurrentMatchIndex + 1) % matches.size
        val range = matches[nextIdx].first to matches[nextIdx].last + 1
        _uiState.update {
            it.copy(
                findCurrentMatchIndex = nextIdx,
                activeMatchRange = range
            )
        }
        return range
    }

    fun findPreviousMatch(): Pair<Int, Int>? {
        val state = _uiState.value
        val matches = getFindMatches(state.activeCode, state.findQuery, state.isFindMatchCase, state.isFindUseRegex)
        if (matches.isEmpty()) return null
        val prevIdx = if (state.findCurrentMatchIndex - 1 < 0) matches.size - 1 else state.findCurrentMatchIndex - 1
        val range = matches[prevIdx].first to matches[prevIdx].last + 1
        _uiState.update {
            it.copy(
                findCurrentMatchIndex = prevIdx,
                activeMatchRange = range
            )
        }
        return range
    }

    fun replaceCurrentMatch(): Pair<String, Pair<Int, Int>>? {
        val state = _uiState.value
        val code = state.activeCode
        val matches = getFindMatches(code, state.findQuery, state.isFindMatchCase, state.isFindUseRegex)
        if (matches.isEmpty()) return null
        val idx = state.findCurrentMatchIndex.coerceIn(0, matches.size - 1)
        val targetRange = matches[idx]
        pushUndoCheckpoint(state.activeTab, code)
        val newCode = code.replaceRange(targetRange.first, targetRange.last + 1, state.replaceQuery)
        val newCursor = targetRange.first + state.replaceQuery.length
        onCodeChanged(newCode, newCursor)
        updateFindMatches()
        return newCode to (targetRange.first to newCursor)
    }

    fun replaceAllMatches(): String? {
        val state = _uiState.value
        val code = state.activeCode
        if (state.findQuery.isEmpty()) return null
        pushUndoCheckpoint(state.activeTab, code)
        val newCode = try {
            if (state.isFindUseRegex) {
                val options = if (state.isFindMatchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                code.replace(Regex(state.findQuery, options), state.replaceQuery)
            } else {
                code.replace(state.findQuery, state.replaceQuery, ignoreCase = !state.isFindMatchCase)
            }
        } catch (_: Throwable) {
            code
        }
        onCodeChanged(newCode, 0)
        updateFindMatches()
        return newCode
    }

    // --- Auto-closing pairs toggle ---
    fun toggleAutoClosePairs() {
        _uiState.update { it.copy(isAutoClosePairs = !it.isAutoClosePairs) }
    }

    // --- CSS Color Picker ---
    fun setColorPickerOpen(open: Boolean, initialHex: String = "#38BDF8") {
        _uiState.update { it.copy(isColorPickerOpen = open, initialColorHex = initialHex) }
    }

    // --- Component & Snippet Store ---
    fun setSnippetStoreOpen(open: Boolean) {
        _uiState.update { it.copy(isSnippetStoreOpen = open) }
    }

    fun insertSnippet(snippet: CodeSnippet) {
        val currentCode = when (snippet.targetTab) {
            CodeTab.INDEX_HTML -> _uiState.value.htmlCode
            CodeTab.STYLE_CSS -> _uiState.value.cssCode
            CodeTab.SCRIPT_JS -> _uiState.value.jsCode
        }
        pushUndoCheckpoint(snippet.targetTab, currentCode)
        val updatedCode = if (currentCode.isBlank()) {
            snippet.codeSnippet
        } else {
            currentCode + "\n\n" + snippet.codeSnippet
        }
        _uiState.update { state ->
            when (snippet.targetTab) {
                CodeTab.INDEX_HTML -> state.copy(htmlCode = updatedCode, isSaved = false, activeTab = CodeTab.INDEX_HTML)
                CodeTab.STYLE_CSS -> state.copy(cssCode = updatedCode, isSaved = false, activeTab = CodeTab.STYLE_CSS)
                CodeTab.SCRIPT_JS -> state.copy(jsCode = updatedCode, isSaved = false, activeTab = CodeTab.SCRIPT_JS)
            }
        }
        triggerDebouncedAutoSave()
        rebuildNativePreview(runTopLevelJs = true)
        updateProgressiveAutoWritePreview()
    }

    // --- Local Code Revision Snapshots ---
    fun setRevisionSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isRevisionSheetOpen = open) }
    }

    fun createRevisionSnapshot(tag: String) {
        viewModelScope.launch {
            val state = _uiState.value
            repository.saveSnapshot(
                tag = tag.ifBlank { "Manual Snapshot" },
                html = state.htmlCode,
                css = state.cssCode,
                js = state.jsCode
            )
        }
    }

    fun restoreRevisionSnapshot(snapshot: CodeRevisionSnapshot) {
        val state = _uiState.value
        pushUndoCheckpoint(CodeTab.INDEX_HTML, state.htmlCode)
        pushUndoCheckpoint(CodeTab.STYLE_CSS, state.cssCode)
        pushUndoCheckpoint(CodeTab.SCRIPT_JS, state.jsCode)
        _uiState.update {
            it.copy(
                htmlCode = snapshot.htmlCode,
                cssCode = snapshot.cssCode,
                jsCode = snapshot.jsCode,
                isSaved = false
            )
        }
        triggerDebouncedAutoSave()
        rebuildNativePreview(runTopLevelJs = true)
        updateProgressiveAutoWritePreview()
    }

    fun deleteRevisionSnapshot(id: Long) {
        viewModelScope.launch {
            repository.deleteSnapshot(id)
        }
    }

    fun clearAllRevisionSnapshots() {
        viewModelScope.launch {
            repository.clearAllSnapshots()
        }
    }

    // --- Audio Acoustic Customizer ---
    fun setTypingSoundPitchMultiplier(pitch: Float) {
        _uiState.update { it.copy(typingSoundPitchMultiplier = pitch.coerceIn(0.70f, 1.40f)) }
        TypingSoundEngine.playKeyTick(
            profile = _uiState.value.typingSoundProfile,
            typedChar = 'a',
            isManualTest = true,
            volume = _uiState.value.typingSoundVolume,
            pitchMultiplier = pitch,
            stabilizerResonance = _uiState.value.typingSoundStabilizerResonance
        )
    }

    fun setTypingSoundStabilizerResonance(enabled: Boolean) {
        _uiState.update { it.copy(typingSoundStabilizerResonance = enabled) }
        TypingSoundEngine.playKeyTick(
            profile = _uiState.value.typingSoundProfile,
            typedChar = '\n',
            isManualTest = true,
            volume = _uiState.value.typingSoundVolume,
            pitchMultiplier = _uiState.value.typingSoundPitchMultiplier,
            stabilizerResonance = enabled
        )
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        liveJsDebounceJob?.cancel()
        typewriterJob?.cancel()
        countdownJob?.cancel()
        TypingSoundEngine.release()
        super.onCleared()
    }

    companion object {
        fun provideFactory(repository: CodeProjectRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudioViewModel(repository) as T
                }
            }
        }
    }
}
