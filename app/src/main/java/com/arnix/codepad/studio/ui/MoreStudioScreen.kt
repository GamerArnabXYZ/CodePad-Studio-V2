package com.arnix.codepad.studio.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.data.ProjectPreset
import com.arnix.codepad.studio.data.ProjectTemplates
import com.arnix.codepad.studio.engine.ExportFormat
import com.arnix.codepad.studio.engine.PreviewScaleMode
import com.arnix.codepad.studio.engine.ScreenRecorderEngine
import com.arnix.codepad.studio.engine.TypingSoundEngine
import com.arnix.codepad.studio.engine.TypingSoundProfile
import com.arnix.codepad.studio.ui.components.StartDelaySelectorChips
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.History
import com.arnix.codepad.studio.ui.components.ComponentStoreSheet
import com.arnix.codepad.studio.ui.components.CreatorEndCtaAnimationOverlay
import com.arnix.codepad.studio.ui.components.CreatorWatermarkBadge
import com.arnix.codepad.studio.ui.components.ExportProjectSheet
import com.arnix.codepad.studio.ui.components.ExportResultDialog
import com.arnix.codepad.studio.ui.components.RenameProjectDialog
import com.arnix.codepad.studio.ui.components.RevisionHistorySheet
import com.arnix.codepad.studio.ui.components.ViralHookHeaderBanner
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.uiSp
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import com.arnix.codepad.studio.viewmodel.CTA_PLATFORM_QUICK_PRESETS
import com.arnix.codepad.studio.viewmodel.CreatorAccentPalette
import com.arnix.codepad.studio.viewmodel.CtaDisplayTiming
import com.arnix.codepad.studio.viewmodel.CtaPlatformQuickPreset
import com.arnix.codepad.studio.viewmodel.EndCtaPosition
import com.arnix.codepad.studio.viewmodel.EndCtaStyle
import com.arnix.codepad.studio.viewmodel.OverlaySizeScale
import com.arnix.codepad.studio.viewmodel.ShortsBackdropPreset
import com.arnix.codepad.studio.viewmodel.StudioModule
import com.arnix.codepad.studio.viewmodel.StudioUiState
import com.arnix.codepad.studio.viewmodel.StudioViewModel
import com.arnix.codepad.studio.viewmodel.TypingSequenceMetrics
import com.arnix.codepad.studio.viewmodel.VIRAL_HOOK_QUICK_PRESETS
import com.arnix.codepad.studio.viewmodel.ViralHookAlignment
import com.arnix.codepad.studio.viewmodel.ViralHookPosition
import com.arnix.codepad.studio.viewmodel.ViralHookQuickPreset
import com.arnix.codepad.studio.viewmodel.ViralHookStyle
import com.arnix.codepad.studio.viewmodel.WatermarkPosition
import com.arnix.codepad.studio.viewmodel.WatermarkStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var pendingSafFormat by remember { mutableStateOf(ExportFormat.BUNDLED_HTML) }

    BackHandler(enabled = selectedCategoryIndex != 0) {
        selectedCategoryIndex = 0
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUserUri(context, uri, pendingSafFormat)
        }
    }

    val onOpenSafExport: (ExportFormat) -> Unit = { format ->
        pendingSafFormat = format
        val safeName = uiState.projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        createDocumentLauncher.launch("$safeName.${format.extension}")
    }

    val categories = listOf(
        "All Sections",
        "Theme & UI",
        "Editor Font",
        "Project & Export",
        "Starter Templates",
        "Typing Sounds",
        "Speed & Duration",
        "Watermark",
        "Viral Hook",
        "End CTA",
        "Video Backdrop",
        "Rec Code Window",
        "Recording Flow",
        "Live Preview",
        "About App",
        "About Developer"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("more_screen_container")
    ) {
        // Top App Bar for More Hub
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Studio Hub & Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All studio settings, recording preferences & branding controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Theme Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("more_quick_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedCategoryIndex == index,
                            onClick = { selectedCategoryIndex = index },
                            label = { Text(title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("more_chip_$index")
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Navigation Hub Banner
            QuickNavBannerCard(
                projectName = uiState.projectName,
                activeTabName = uiState.activeTab.fileName,
                onOpenEditor = { viewModel.selectModule(StudioModule.CODEPAD_IDE) },
                onOpenRec = { viewModel.selectModule(StudioModule.AUTO_WRITE_STUDIO) }
            )

            // Section 1: Display & Theme Settings
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 1) {
                SectionCard(
                    title = "Theme & Appearance",
                    subtitle = "Theme styling, dark mode & typography scale",
                    icon = Icons.Filled.FormatPaint
                ) {
                    DisplayAndThemeSectionContent(
                        isDarkTheme = uiState.isDarkTheme,
                        uiFontScale = uiState.uiFontScale,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onUiFontScaleChange = { viewModel.updateUiFontScale(it) }
                    )
                }
            }

            // Section 2: Editor Font & Typography
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 2) {
                SectionCard(
                    title = "Editor Typography & Font",
                    subtitle = "Code font size presets and fine-grained scale slider",
                    icon = Icons.Filled.TextFields
                ) {
                    EditorTypographySectionContent(
                        fontSize = uiState.editorFontSize,
                        onFontSizePreset = { ed, aw, sc -> viewModel.applyFontSizePreset(ed, aw, sc) },
                        onFontSizeChange = { viewModel.updateEditorFontSize(it) }
                    )
                }
            }

            // Section 3: Project Actions & Export
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 3) {
                SectionCard(
                    title = "Project Management & Export",
                    subtitle = "Project renaming, code formatting & multi-format export",
                    icon = Icons.Filled.FileDownload
                ) {
                    ProjectActionsSectionContent(
                        onRename = { viewModel.setRenameDialogOpen(true) },
                        onFormat = { viewModel.formatActiveTabCode() },
                        onSplitHtml = { viewModel.splitEmbeddedHtmlIntoTabs() },
                        onExportHtml = { onOpenSafExport(ExportFormat.BUNDLED_HTML) },
                        onExportZip = { onOpenSafExport(ExportFormat.ZIP_ARCHIVE) }
                    )
                }
            }

            // Section 4: Starter Templates
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 4) {
                SectionCard(
                    title = "Starter Project Templates",
                    subtitle = "Interactive HTML5, modern CSS3 & ES6 JavaScript templates",
                    icon = Icons.Filled.FolderSpecial
                ) {
                    StarterTemplatesSectionContent(
                        onLoadPreset = { viewModel.loadPreset(it) }
                    )
                }
            }

            // Section 4b: Component Store & Code Revision Snapshots
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 4) {
                SectionCard(
                    title = "Component Store & Code Snapshots",
                    subtitle = "Pre-built UI snippets, particles, and local code rollback checkpoints",
                    icon = Icons.Filled.Widgets
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedCard(
                            onClick = { viewModel.setSnippetStoreOpen(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_open_snippet_store_btn")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Snippet Store",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.uiSp
                                )
                                Text(
                                    text = "7+ UI buttons & scripts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedCard(
                            onClick = { viewModel.setRevisionSheetOpen(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("more_open_revisions_btn")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Revision History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.uiSp
                                )
                                Text(
                                    text = "${uiState.snapshots.size} saved checkpoints",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Typing Sound Effects (Mechanical ASMR) - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 5) {
                SectionCard(
                    title = "ASMR Typing Sound Effects",
                    subtitle = "Authentic mechanical switches, volume, acoustic pitch tuning & stabilizers",
                    icon = Icons.Filled.VolumeUp
                ) {
                    TypingSoundSectionContent(
                        currentProfile = uiState.typingSoundProfile,
                        volume = uiState.typingSoundVolume,
                        minGapMs = uiState.typingSoundMinGapMs,
                        pitchVariation = uiState.typingSoundPitchVar,
                        pitchMultiplier = uiState.typingSoundPitchMultiplier,
                        stabilizerResonance = uiState.typingSoundStabilizerResonance,
                        onSelectProfile = { profile ->
                            viewModel.selectTypingSoundProfile(profile)
                        },
                        onVolumeChange = { viewModel.updateTypingSoundVolume(it) },
                        onMinGapChange = { viewModel.updateTypingSoundMinGap(it) },
                        onTogglePitchVariation = { viewModel.toggleTypingSoundPitchVar() },
                        onPitchMultiplierChange = { viewModel.setTypingSoundPitchMultiplier(it) },
                        onToggleStabilizerResonance = { viewModel.setTypingSoundStabilizerResonance(!uiState.typingSoundStabilizerResonance) },
                        onTestSound = {
                            TypingSoundEngine.playKeyTick(
                                profile = uiState.typingSoundProfile,
                                typedChar = 'a',
                                isManualTest = true,
                                volume = uiState.typingSoundVolume,
                                minGapMs = uiState.typingSoundMinGapMs,
                                pitchVariation = uiState.typingSoundPitchVar,
                                pitchMultiplier = uiState.typingSoundPitchMultiplier,
                                stabilizerResonance = uiState.typingSoundStabilizerResonance
                            )
                        }
                    )
                }
            }

            // Section 6: Target Duration & Speed - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 6) {
                SectionCard(
                    title = "Target Duration & Speed Controls",
                    subtitle = "Shorts duration presets, manual speed slider, turbo burst & live metrics",
                    icon = Icons.Filled.Speed
                ) {
                    SpeedAndDurationSectionContent(
                        currentTarget = uiState.autoDurationTarget,
                        speedLevel = uiState.speedLevel,
                        isTurboMode = uiState.isTurboMode,
                        sequenceMetrics = uiState.sequenceMetrics,
                        onSelectTarget = { viewModel.selectAutoDurationTarget(it) },
                        onSpeedLevelChange = { viewModel.setSpeedLevel(it) },
                        onToggleTurbo = { viewModel.toggleTurboMode() }
                    )
                }
            }

            // Section 7: Creator Watermark & Handle Branding - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 7) {
                SectionCard(
                    title = "Creator Watermark & Social Branding",
                    subtitle = "Handle text, screen position, badge style, opacity & size scale",
                    icon = Icons.Filled.BrandingWatermark
                ) {
                    WatermarkSectionContent(
                        creatorHandle = uiState.creatorHandle,
                        currentPosition = uiState.watermarkPosition,
                        currentStyle = uiState.watermarkStyle,
                        opacity = uiState.watermarkOpacity,
                        sizeScale = uiState.watermarkSizeScale,
                        onUpdateHandle = { viewModel.updateCreatorHandle(it) },
                        onSelectPosition = { viewModel.selectWatermarkPosition(it) },
                        onSelectStyle = { viewModel.selectWatermarkStyle(it) },
                        onOpacityChange = { viewModel.updateWatermarkOpacity(it) },
                        onSelectSizeScale = { viewModel.selectWatermarkSizeScale(it) }
                    )
                }
            }

            // Section 8: Zero-Editing Viral Hook Header Banner - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 8) {
                SectionCard(
                    title = "Viral Hook Header Banner",
                    subtitle = "Headline, badge, 6 styles, transparent BG, alignment, colors & 1-tap presets",
                    icon = Icons.Filled.NotificationAdd
                ) {
                    ViralHookSectionContent(
                        showBanner = uiState.showViralHookBanner,
                        hookBadge = uiState.viralHookBadge,
                        hookTitle = uiState.viralHookTitle,
                        hookStyle = uiState.viralHookStyle,
                        hookPosition = uiState.viralHookPosition,
                        hookAlignment = uiState.viralHookAlignment,
                        transparentBg = uiState.viralHookTransparentBg,
                        showBorder = uiState.viralHookShowBorder,
                        showBadge = uiState.viralHookShowBadge,
                        accentIndex = uiState.viralHookAccentIndex,
                        sizeScale = uiState.viralHookSizeScale,
                        onToggleBanner = { viewModel.toggleShowViralHookBanner() },
                        onUpdateBadge = { viewModel.updateViralHookBadge(it) },
                        onUpdateTitle = { viewModel.updateViralHookTitle(it) },
                        onSelectStyle = { viewModel.selectViralHookStyle(it) },
                        onSelectPosition = { viewModel.selectViralHookPosition(it) },
                        onSelectAlignment = { viewModel.selectViralHookAlignment(it) },
                        onToggleTransparentBg = { viewModel.toggleViralHookTransparentBg() },
                        onToggleShowBorder = { viewModel.toggleViralHookShowBorder() },
                        onToggleShowBadge = { viewModel.toggleViralHookShowBadge() },
                        onSelectAccentIndex = { viewModel.selectViralHookAccentIndex(it) },
                        onSelectSizeScale = { viewModel.selectViralHookSizeScale(it) },
                        onApplyQuickPreset = { viewModel.applyViralHookQuickPreset(it) }
                    )
                }
            }

            // Section 9: Recording End CTA Overlay - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 9) {
                SectionCard(
                    title = "Recording End Outro CTA Overlay",
                    subtitle = "Animated Like, Comment & Subscribe cards, custom labels, timing & presets",
                    icon = Icons.Filled.AutoFixHigh
                ) {
                    EndCtaSectionContent(
                        showEndCta = uiState.showEndCtaAnimation,
                        ctaMessage = uiState.endCtaMessage,
                        ctaStyle = uiState.endCtaStyle,
                        ctaPosition = uiState.endCtaPosition,
                        displayTiming = uiState.ctaDisplayTiming,
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
                        creatorHandle = uiState.creatorHandle,
                        onToggleEndCta = { viewModel.toggleShowEndCtaAnimation() },
                        onUpdateMessage = { viewModel.updateEndCtaMessage(it) },
                        onSelectStyle = { viewModel.selectEndCtaStyle(it) },
                        onSelectPosition = { viewModel.selectEndCtaPosition(it) },
                        onSelectTiming = { viewModel.selectCtaDisplayTiming(it) },
                        onToggleTransparentBg = { viewModel.toggleCtaTransparentBg() },
                        onToggleShowBorder = { viewModel.toggleCtaShowBorder() },
                        onToggleShowHandle = { viewModel.toggleCtaShowHandle() },
                        onSelectAccentIndex = { viewModel.selectCtaAccentIndex(it) },
                        onSelectSizeScale = { viewModel.selectCtaSizeScale(it) },
                        onToggleLike = { viewModel.toggleCtaLike() },
                        onToggleComment = { viewModel.toggleCtaComment() },
                        onToggleSubscribe = { viewModel.toggleCtaSubscribe() },
                        onUpdateLikeLabel = { viewModel.updateCtaLikeLabel(it) },
                        onUpdateCommentLabel = { viewModel.updateCtaCommentLabel(it) },
                        onUpdateSubscribeLabel = { viewModel.updateCtaSubscribeLabel(it) },
                        onUpdateSubscribedLabel = { viewModel.updateCtaSubscribedLabel(it) },
                        onApplyPlatformPreset = { viewModel.applyCtaPlatformPreset(it) },
                        onTestOutro = {
                            viewModel.triggerEndCtaPreview()
                            viewModel.selectModule(StudioModule.AUTO_WRITE_STUDIO)
                        }
                    )
                }
            }

            // Section 10: Shorts Backdrop Gradient Themes - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 10) {
                SectionCard(
                    title = "Shorts Backdrop Color Themes",
                    subtitle = "Cinematic vertical gradients for TikTok, Reels & YT Shorts",
                    icon = Icons.Filled.ColorLens
                ) {
                    BackdropSectionContent(
                        selectedIndex = uiState.backdropPresetIndex,
                        onSelectBackdrop = { viewModel.selectBackdropPreset(it) }
                    )
                }
            }

            // Section 11: Auto-Write Code Window & Typography - NEW DEDICATED SECTION
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 11) {
                SectionCard(
                    title = "Rec Code Window & Typography",
                    subtitle = "Typing viewport font scale and line numbers gutter toggle",
                    icon = Icons.Filled.Code
                ) {
                    AutoWriteCodeWindowSectionContent(
                        autoWriteFontSize = uiState.autoWriteFontSize,
                        showGutter = uiState.autoWriteShowGutter,
                        onUpdateFontSize = { viewModel.updateAutoWriteFontSize(it) },
                        onToggleGutter = { viewModel.toggleAutoWriteShowGutter() }
                    )
                }
            }

            // Section 12: Recording Flow & Automation - FULL CUSTOMIZATIONS
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 12) {
                SectionCard(
                    title = "Recording Flow & Automation",
                    subtitle = "3s countdown, tab auto-chaining, preview on finish & recording options",
                    icon = Icons.Filled.Timer
                ) {
                    RecordingFlowSectionContent(
                        useCountdown = uiState.useRecordingCountdown,
                        startDelayMs = uiState.recordingStartDelayMs,
                        isAutoRecordInApp = uiState.isAutoRecordInApp,
                        prefer1080p = uiState.recorderPrefer1080p,
                        prefer60Fps = uiState.recorderPrefer60Fps,
                        recordAudio = uiState.recorderEnableAudio,
                        isAutoChainTabs = uiState.isAutoChainTabs,
                        autoRevealPreview = uiState.autoRevealPreviewOnFinish,
                        keepPreviewInRecord = uiState.keepPreviewOpenInRecord,
                        onToggleCountdown = { viewModel.toggleUseRecordingCountdown() },
                        onSelectStartDelayMs = { viewModel.setRecordingStartDelayMs(it) },
                        onToggleAutoRecordInApp = { viewModel.setAutoRecordInApp(!uiState.isAutoRecordInApp) },
                        onSet1080p = { viewModel.setRecorderPrefer1080p(it) },
                        onSet60Fps = { viewModel.setRecorderPrefer60Fps(it) },
                        onToggleAudio = { viewModel.setRecorderEnableAudio(!uiState.recorderEnableAudio) },
                        onToggleAutoChain = { viewModel.toggleAutoChainTabs() },
                        onToggleAutoReveal = { viewModel.toggleAutoRevealPreview() },
                        onToggleKeepPreviewInRecord = { viewModel.toggleKeepPreviewOpenInRecord() }
                    )
                }
            }

            // Section 13: Live Output Preview in Rec - NEW DEDICATED SECTION
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 13) {
                SectionCard(
                    title = "Live Output & Floating Preview",
                    subtitle = "Preview height, scaling modes & gesture shortcuts in Rec studio",
                    icon = Icons.Filled.OpenInFull
                ) {
                    RecPreviewSectionContent(
                        previewHeightDp = uiState.previewSectionHeightDp,
                        scaleMode = uiState.previewScaleMode,
                        onUpdateHeight = { viewModel.setPreviewSectionHeight(it) },
                        onCycleScaleMode = { viewModel.cyclePreviewScaleMode() }
                    )
                }
            }

            // Section 14: About CodePad Studio Architecture & Features
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 14) {
                AppAboutSection(onOpenGuide = { viewModel.openOnboardingGuide() })
            }

            // Section 15: About Developer & Creator
            if (selectedCategoryIndex == 0 || selectedCategoryIndex == 15) {
                AboutDeveloperSection()
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Dialogs managed from More Hub
    if (uiState.isRenameDialogOpen) {
        RenameProjectDialog(
            currentName = uiState.projectName,
            onConfirm = { newName -> viewModel.updateProjectName(newName) },
            onDismiss = { viewModel.setRenameDialogOpen(false) }
        )
    }

    if (uiState.isExportSheetOpen) {
        ExportProjectSheet(
            projectName = uiState.projectName,
            onExportAppStorage = { format ->
                viewModel.exportToAppStorage(context, format)
                viewModel.setExportSheetOpen(false)
            },
            onExportDeviceFilePicker = { format ->
                onOpenSafExport(format)
            },
            onDismiss = { viewModel.setExportSheetOpen(false) }
        )
    }

    uiState.latestExportResult?.let { exportResult ->
        ExportResultDialog(
            result = exportResult,
            onDismiss = { viewModel.dismissExportResult() }
        )
    }

    if (uiState.isSnippetStoreOpen) {
        ComponentStoreSheet(
            onInsertSnippet = { snippet ->
                viewModel.insertSnippet(snippet)
            },
            onDismiss = { viewModel.setSnippetStoreOpen(false) }
        )
    }

    if (uiState.isRevisionSheetOpen) {
        RevisionHistorySheet(
            snapshots = uiState.snapshots,
            onCreateSnapshot = { tag -> viewModel.createRevisionSnapshot(tag) },
            onRestoreSnapshot = { snap -> viewModel.restoreRevisionSnapshot(snap) },
            onDeleteSnapshot = { id -> viewModel.deleteRevisionSnapshot(id) },
            onClearAllSnapshots = { viewModel.clearAllRevisionSnapshots() },
            onDismiss = { viewModel.setRevisionSheetOpen(false) }
        )
    }
}

/**
 * Reusable Dedicated Section Card Container for Cleaner UI Division
 */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            content()
        }
    }
}

/**
 * Quick Navigation Banner between Editor and Rec modules
 */
@Composable
private fun QuickNavBannerCard(
    projectName: String,
    activeTabName: String,
    onOpenEditor: () -> Unit,
    onOpenRec: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active Project: $projectName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Current Tab: $activeTabName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenEditor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("more_nav_to_editor")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = "Open Editor",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editor")
                }

                Button(
                    onClick = onOpenRec,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("more_nav_to_rec")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Open Rec",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rec")
                }
            }
        }
    }
}

@Composable
private fun DisplayAndThemeSectionContent(
    isDarkTheme: Boolean,
    uiFontScale: Float,
    onToggleTheme: () -> Unit,
    onUiFontScaleChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Dark Theme Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isDarkTheme) "High-contrast dark palette active" else "Clean bright light theme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                modifier = Modifier.testTag("more_dark_theme_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UI Visual Scaling",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(uiFontScale * 100).roundToInt()}%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = uiFontScale,
            onValueChange = onUiFontScaleChange,
            valueRange = 0.75f..1.30f,
            steps = 10,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorTypographySectionContent(
    fontSize: Float,
    onFontSizePreset: (editorSize: Float, autoWriteSize: Float, uiScale: Float) -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Typography Presets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = fontSize <= 10f,
                onClick = { onFontSizePreset(10f, 9.5f, 0.82f) },
                label = { Text("Extra Small (10px)") }
            )
            FilterChip(
                selected = fontSize in 10.5f..12f,
                onClick = { onFontSizePreset(11.5f, 11f, 0.92f) },
                label = { Text("Compact (11.5px)") }
            )
            FilterChip(
                selected = fontSize in 12.1f..13.5f,
                onClick = { onFontSizePreset(13f, 12.5f, 1.0f) },
                label = { Text("Medium (13px)") }
            )
            FilterChip(
                selected = fontSize >= 14f,
                onClick = { onFontSizePreset(15f, 14f, 1.12f) },
                label = { Text("Large (15px)") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editor Code Font Size",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${fontSize.roundToInt()} px",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 8f..22f,
            steps = 13,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProjectActionsSectionContent(
    onRename: () -> Unit,
    onFormat: () -> Unit,
    onSplitHtml: () -> Unit,
    onExportHtml: () -> Unit,
    onExportZip: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRename,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("more_rename_btn")
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rename")
            }

            OutlinedButton(
                onClick = onFormat,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("more_format_btn")
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Format")
            }
        }

        OutlinedButton(
            onClick = onSplitHtml,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("more_split_btn")
        ) {
            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Split Inline <style> & <script> into Tabs")
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            text = "Export Project Files",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onExportHtml,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("more_export_html_btn")
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bundled .html")
            }

            OutlinedButton(
                onClick = onExportZip,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("more_export_zip_btn")
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Full .zip")
            }
        }
    }
}

@Composable
private fun StarterTemplatesSectionContent(
    onLoadPreset: (ProjectPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProjectTemplates.PRESETS.forEach { preset ->
            Surface(
                onClick = { onLoadPreset(preset) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Load",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingSoundSectionContent(
    currentProfile: TypingSoundProfile,
    volume: Float,
    minGapMs: Long,
    pitchVariation: Boolean,
    pitchMultiplier: Float = 1.0f,
    stabilizerResonance: Boolean = true,
    onSelectProfile: (TypingSoundProfile) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMinGapChange: (Long) -> Unit,
    onTogglePitchVariation: () -> Unit,
    onPitchMultiplierChange: (Float) -> Unit = {},
    onToggleStabilizerResonance: () -> Unit = {},
    onTestSound: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Switch Sound Profile",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = onTestSound,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("more_test_sound_btn")
            ) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Click", fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypingSoundProfile.entries.forEach { profile ->
                FilterChip(
                    selected = currentProfile == profile,
                    onClick = { onSelectProfile(profile) },
                    label = { Text(profile.label) },
                    modifier = Modifier.testTag("more_sound_chip_${profile.name}")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Sound Volume Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sound Effect Volume",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(volume * 100).roundToInt()}%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0.1f..1.0f,
            steps = 9,
            modifier = Modifier.fillMaxWidth().testTag("typing_sound_volume_slider")
        )

        // Pitch Multiplier Slider (0.80x Deep Thock to 1.30x Crisp Click)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Acoustic Pitch Tuning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "0.80x (Deep Bass Thock) to 1.30x (Crisp Click)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format("%.2fx", pitchMultiplier),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = pitchMultiplier,
            onValueChange = onPitchMultiplierChange,
            valueRange = 0.80f..1.30f,
            steps = 10,
            modifier = Modifier.fillMaxWidth().testTag("typing_sound_pitch_slider")
        )

        // Spacebar & Return Stabilizer Body Resonance Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stabilizer Cavity Resonance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enhanced acoustic body for Space, Enter & Braces",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = stabilizerResonance,
                onCheckedChange = { onToggleStabilizerResonance() },
                modifier = Modifier.testTag("typing_sound_stabilizer_switch")
            )
        }

        // Randomized Break Interval Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keystroke Cadence Gap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Randomized break so audio breathes naturally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${minGapMs}ms",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = minGapMs.toFloat(),
            onValueChange = { onMinGapChange(it.toLong()) },
            valueRange = 40f..180f,
            steps = 13,
            modifier = Modifier.fillMaxWidth().testTag("typing_sound_gap_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Organic Pitch Modulation (±3.5%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Mimics human finger pressure variations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = pitchVariation,
                onCheckedChange = { onTogglePitchVariation() },
                modifier = Modifier.testTag("typing_sound_pitch_switch")
            )
        }
    }
}

@Composable
private fun SpeedAndDurationSectionContent(
    currentTarget: AutoDurationTarget,
    speedLevel: Int,
    isTurboMode: Boolean,
    sequenceMetrics: TypingSequenceMetrics,
    onSelectTarget: (AutoDurationTarget) -> Unit,
    onSpeedLevelChange: (Int) -> Unit,
    onToggleTurbo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Video Target Duration",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoDurationTarget.entries.forEach { target ->
                FilterChip(
                    selected = currentTarget == target,
                    onClick = { onSelectTarget(target) },
                    label = { Text(target.fullLabel) },
                    modifier = Modifier.testTag("more_target_chip_${target.name}")
                )
            }
        }

        // Real-Time Speed Optimizer Metrics Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "AUTO PACING METRICS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${sequenceMetrics.totalChars} total characters across tabs • ~${sequenceMetrics.effectiveSteps} typing steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Auto-Calculated Level: Lv.${sequenceMetrics.recommendedSpeedLevel}  |  Estimated Video: ~${sequenceMetrics.estimatedTotalSeconds}s",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Manual Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Manual Speed Level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Lv.$speedLevel (1-20)",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = speedLevel.toFloat(),
            onValueChange = { onSpeedLevelChange(it.roundToInt()) },
            valueRange = 1f..20f,
            steps = 18,
            modifier = Modifier.fillMaxWidth().testTag("more_speed_level_slider")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 3, 6, 10, 14, 20).forEach { lvl ->
                FilterChip(
                    selected = speedLevel == lvl,
                    onClick = { onSpeedLevelChange(lvl) },
                    label = { Text("${lvl}×") }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Turbo High-Speed Burst (2×)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Double-char burst advance per frame",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isTurboMode,
                onCheckedChange = { onToggleTurbo() },
                modifier = Modifier.testTag("more_turbo_mode_switch")
            )
        }
    }
}

@Composable
private fun WatermarkSectionContent(
    creatorHandle: String,
    currentPosition: WatermarkPosition,
    currentStyle: WatermarkStyle,
    opacity: Float,
    sizeScale: OverlaySizeScale,
    onUpdateHandle: (String) -> Unit,
    onSelectPosition: (WatermarkPosition) -> Unit,
    onSelectStyle: (WatermarkStyle) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onSelectSizeScale: (OverlaySizeScale) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Watermark Preview Box
        Text(
            text = "LIVE WATERMARK PREVIEW",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1120))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CreatorWatermarkBadge(
                handle = creatorHandle,
                style = currentStyle,
                isOverlay = currentPosition == WatermarkPosition.CENTER_OVERLAY,
                onClick = {},
                opacity = opacity,
                sizeScale = sizeScale
            )
        }

        OutlinedTextField(
            value = creatorHandle,
            onValueChange = onUpdateHandle,
            label = { Text("Creator Handle (@username)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("more_creator_handle_input")
        )

        Text(
            text = "Screen Position (Where it appears in Video)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WatermarkPosition.entries.forEach { pos ->
                FilterChip(
                    selected = currentPosition == pos,
                    onClick = { onSelectPosition(pos) },
                    label = { Text(pos.label) }
                )
            }
        }

        Text(
            text = "Watermark Badge Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WatermarkStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onSelectStyle(style) },
                    label = { Text(style.label) }
                )
            }
        }

        // Opacity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Watermark Opacity",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(opacity * 100).roundToInt()}%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = opacity,
            onValueChange = onOpacityChange,
            valueRange = 0.15f..1.0f,
            steps = 16,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Size Scale",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlaySizeScale.entries.forEach { scale ->
                FilterChip(
                    selected = sizeScale == scale,
                    onClick = { onSelectSizeScale(scale) },
                    label = { Text(scale.label) }
                )
            }
        }
    }
}

@Composable
private fun ViralHookSectionContent(
    showBanner: Boolean,
    hookBadge: String,
    hookTitle: String,
    hookStyle: ViralHookStyle,
    hookPosition: ViralHookPosition,
    hookAlignment: ViralHookAlignment,
    transparentBg: Boolean,
    showBorder: Boolean,
    showBadge: Boolean,
    accentIndex: Int,
    sizeScale: OverlaySizeScale,
    onToggleBanner: () -> Unit,
    onUpdateBadge: (String) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onSelectStyle: (ViralHookStyle) -> Unit,
    onSelectPosition: (ViralHookPosition) -> Unit,
    onSelectAlignment: (ViralHookAlignment) -> Unit,
    onToggleTransparentBg: () -> Unit,
    onToggleShowBorder: () -> Unit,
    onToggleShowBadge: () -> Unit,
    onSelectAccentIndex: (Int) -> Unit,
    onSelectSizeScale: (OverlaySizeScale) -> Unit,
    onApplyQuickPreset: (ViralHookQuickPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Viral Hook Banner",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Grab attention instantly in first 3 seconds of shorts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showBanner,
                onCheckedChange = { onToggleBanner() },
                modifier = Modifier.testTag("more_viral_hook_switch")
            )
        }

        if (showBanner) {
            // Live Preview Box for Viral Hook Banner
            Text(
                text = "LIVE VIRAL HOOK PREVIEW",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF0B1120), Color(0xFF1E293B)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                ViralHookHeaderBanner(
                    badgeText = hookBadge,
                    titleText = hookTitle.ifBlank { "Build a Developer Portfolio Card" },
                    style = hookStyle,
                    alignment = hookAlignment,
                    transparentBg = transparentBg,
                    showBorder = showBorder,
                    showBadge = showBadge,
                    accentIndex = accentIndex,
                    sizeScale = sizeScale,
                    onClick = {}
                )
            }

            // Quick 1-Tap Ready-Made Viral Hook Presets
            Text(
                text = "1-Tap Ready-Made Viral Hook Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VIRAL_HOOK_QUICK_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = hookTitle == preset.title && hookBadge == preset.badge,
                        onClick = { onApplyQuickPreset(preset) },
                        label = { Text(preset.label) }
                    )
                }
            }

            OutlinedTextField(
                value = hookBadge,
                onValueChange = onUpdateBadge,
                label = { Text("Badge Label (e.g. 🔥 HTML • CSS • JS)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = hookTitle,
                onValueChange = onUpdateTitle,
                label = { Text("Main Headline Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Banner Position",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ViralHookPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = hookPosition == pos,
                        onClick = { onSelectPosition(pos) },
                        label = { Text(pos.label) }
                    )
                }
            }

            Text(
                text = "Visual Style (6 Styles)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ViralHookStyle.entries.forEach { style ->
                    FilterChip(
                        selected = hookStyle == style,
                        onClick = { onSelectStyle(style) },
                        label = { Text(style.label) }
                    )
                }
            }

            Text(
                text = "Appearance & Frame Toggles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = transparentBg,
                    onClick = onToggleTransparentBg,
                    label = { Text("Transparent BG ✨") }
                )
                FilterChip(
                    selected = showBorder,
                    onClick = onToggleShowBorder,
                    label = { Text("Border Outline") }
                )
                FilterChip(
                    selected = showBadge,
                    onClick = onToggleShowBadge,
                    label = { Text("Show Badge Pill") }
                )
            }

            Text(
                text = "Text Alignment & Size Scale",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ViralHookAlignment.entries.forEach { align ->
                    FilterChip(
                        selected = hookAlignment == align,
                        onClick = { onSelectAlignment(align) },
                        label = { Text(align.label) }
                    )
                }
                OverlaySizeScale.entries.forEach { scale ->
                    FilterChip(
                        selected = sizeScale == scale,
                        onClick = { onSelectSizeScale(scale) },
                        label = { Text("Size: ${scale.label}") }
                    )
                }
            }

            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorAccentPalette.entries.forEach { palette ->
                    FilterChip(
                        selected = accentIndex == palette.index,
                        onClick = { onSelectAccentIndex(palette.index) },
                        label = { Text(palette.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EndCtaSectionContent(
    showEndCta: Boolean,
    ctaMessage: String,
    ctaStyle: EndCtaStyle,
    ctaPosition: EndCtaPosition,
    displayTiming: CtaDisplayTiming,
    transparentBg: Boolean,
    showBorder: Boolean,
    showHandle: Boolean,
    accentIndex: Int,
    sizeScale: OverlaySizeScale,
    showLike: Boolean,
    showComment: Boolean,
    showSubscribe: Boolean,
    likeLabel: String,
    commentLabel: String,
    subscribeLabel: String,
    subscribedLabel: String,
    creatorHandle: String,
    onToggleEndCta: () -> Unit,
    onUpdateMessage: (String) -> Unit,
    onSelectStyle: (EndCtaStyle) -> Unit,
    onSelectPosition: (EndCtaPosition) -> Unit,
    onSelectTiming: (CtaDisplayTiming) -> Unit,
    onToggleTransparentBg: () -> Unit,
    onToggleShowBorder: () -> Unit,
    onToggleShowHandle: () -> Unit,
    onSelectAccentIndex: (Int) -> Unit,
    onSelectSizeScale: (OverlaySizeScale) -> Unit,
    onToggleLike: () -> Unit,
    onToggleComment: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onUpdateLikeLabel: (String) -> Unit,
    onUpdateCommentLabel: (String) -> Unit,
    onUpdateSubscribeLabel: (String) -> Unit,
    onUpdateSubscribedLabel: (String) -> Unit,
    onApplyPlatformPreset: (CtaPlatformQuickPreset) -> Unit,
    onTestOutro: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "End CTA Animation Overlay",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Like, Comment & Subscribe animated overlay cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showEndCta,
                onCheckedChange = { onToggleEndCta() },
                modifier = Modifier.testTag("more_end_cta_switch")
            )
        }

        if (showEndCta) {
            // Live Preview Box of CTA Overlay
            Text(
                text = "LIVE END CTA PREVIEW",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF0B1120), Color(0xFF1E293B)))
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                CreatorEndCtaAnimationOverlay(
                    creatorHandle = creatorHandle,
                    ctaMessage = ctaMessage,
                    style = ctaStyle,
                    transparentBg = transparentBg,
                    showBorder = showBorder,
                    showHandle = showHandle,
                    accentIndex = accentIndex,
                    sizeScale = sizeScale,
                    showLike = showLike,
                    showComment = showComment,
                    showSubscribe = showSubscribe,
                    likeLabel = likeLabel,
                    commentLabel = commentLabel,
                    subscribeLabel = subscribeLabel,
                    subscribedLabel = subscribedLabel,
                    loopAnimation = true,
                    onDismiss = {}
                )
            }

            Button(
                onClick = onTestOutro,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("more_test_outro_btn")
            ) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Outro on Full Rec Stage")
            }

            // Platform 1-Tap Presets
            Text(
                text = "1-Tap Platform Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CTA_PLATFORM_QUICK_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = subscribeLabel == preset.subscribeLabel && ctaMessage == preset.message,
                        onClick = { onApplyPlatformPreset(preset) },
                        label = { Text(preset.label) }
                    )
                }
            }

            Text(
                text = "CTA Visual Style (6 Styles)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EndCtaStyle.entries.forEach { style ->
                    FilterChip(
                        selected = ctaStyle == style,
                        onClick = { onSelectStyle(style) },
                        label = { Text(style.label) }
                    )
                }
            }

            Text(
                text = "Screen Position",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EndCtaPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = ctaPosition == pos,
                        onClick = { onSelectPosition(pos) },
                        label = { Text(pos.label) }
                    )
                }
            }

            Text(
                text = "Display Timing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CtaDisplayTiming.entries.forEach { timing ->
                    FilterChip(
                        selected = displayTiming == timing,
                        onClick = { onSelectTiming(timing) },
                        label = { Text(timing.label) }
                    )
                }
            }

            Text(
                text = "Appearance & Frame Toggles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = transparentBg,
                    onClick = onToggleTransparentBg,
                    label = { Text("Transparent BG ✨") }
                )
                FilterChip(
                    selected = showBorder,
                    onClick = onToggleShowBorder,
                    label = { Text("Border Frame") }
                )
                FilterChip(
                    selected = showHandle,
                    onClick = onToggleShowHandle,
                    label = { Text("Show @Handle") }
                )
            }

            Text(
                text = "Action Button Toggles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showLike,
                    onClick = onToggleLike,
                    label = { Text("Like Button") }
                )
                FilterChip(
                    selected = showComment,
                    onClick = onToggleComment,
                    label = { Text("Comment Button") }
                )
                FilterChip(
                    selected = showSubscribe,
                    onClick = onToggleSubscribe,
                    label = { Text("Subscribe Button") }
                )
            }

            OutlinedTextField(
                value = ctaMessage,
                onValueChange = onUpdateMessage,
                label = { Text("Call to Action Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = likeLabel,
                    onValueChange = onUpdateLikeLabel,
                    label = { Text("Like Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = commentLabel,
                    onValueChange = onUpdateCommentLabel,
                    label = { Text("Comment Label") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = subscribeLabel,
                    onValueChange = onUpdateSubscribeLabel,
                    label = { Text("Subscribe Text") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = subscribedLabel,
                    onValueChange = onUpdateSubscribedLabel,
                    label = { Text("Subscribed State") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Color Palette & Size",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorAccentPalette.entries.forEach { palette ->
                    FilterChip(
                        selected = accentIndex == palette.index,
                        onClick = { onSelectAccentIndex(palette.index) },
                        label = { Text(palette.label) }
                    )
                }
                OverlaySizeScale.entries.forEach { scale ->
                    FilterChip(
                        selected = sizeScale == scale,
                        onClick = { onSelectSizeScale(scale) },
                        label = { Text("Size: ${scale.label}") }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackdropSectionContent(
    selectedIndex: Int,
    onSelectBackdrop: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortsBackdropPreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedIndex == preset.index,
                    onClick = { onSelectBackdrop(preset.index) },
                    label = { Text(preset.label) }
                )
            }
        }

        val activePreset = ShortsBackdropPreset.fromIndex(selectedIndex)
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (activePreset == ShortsBackdropPreset.CLEAN_M3) {
                            Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(activePreset.startColorHex),
                                    Color(activePreset.endColorHex)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Active Backdrop: ${activePreset.label}",
                    color = Color.White,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AutoWriteCodeWindowSectionContent(
    autoWriteFontSize: Float,
    showGutter: Boolean,
    onUpdateFontSize: (Float) -> Unit,
    onToggleGutter: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto-Write Font Size",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${autoWriteFontSize.roundToInt()} px",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = autoWriteFontSize,
            onValueChange = onUpdateFontSize,
            valueRange = 8f..22f,
            steps = 13,
            modifier = Modifier.fillMaxWidth().testTag("more_autowrite_font_slider")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = autoWriteFontSize <= 9.5f,
                onClick = { onUpdateFontSize(9.5f) },
                label = { Text("Extra Small (9.5px)") }
            )
            FilterChip(
                selected = autoWriteFontSize in 10f..11.5f,
                onClick = { onUpdateFontSize(11f) },
                label = { Text("Compact (11px)") }
            )
            FilterChip(
                selected = autoWriteFontSize in 12f..13.5f,
                onClick = { onUpdateFontSize(13f) },
                label = { Text("Medium (13px)") }
            )
            FilterChip(
                selected = autoWriteFontSize >= 14f,
                onClick = { onUpdateFontSize(15f) },
                label = { Text("Large (15px)") }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Line Numbers (Gutter) Display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Shows line counts on the left of code window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showGutter,
                onCheckedChange = { onToggleGutter() },
                modifier = Modifier.testTag("more_autowrite_gutter_switch")
            )
        }
    }
}

@Composable
private fun RecordingFlowSectionContent(
    useCountdown: Boolean,
    startDelayMs: Long,
    isAutoRecordInApp: Boolean,
    prefer1080p: Boolean,
    prefer60Fps: Boolean,
    recordAudio: Boolean,
    isAutoChainTabs: Boolean,
    autoRevealPreview: Boolean,
    keepPreviewInRecord: Boolean,
    onToggleCountdown: () -> Unit,
    onSelectStartDelayMs: (Long) -> Unit,
    onToggleAutoRecordInApp: () -> Unit,
    onSet1080p: (Boolean) -> Unit,
    onSet60Fps: (Boolean) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleAutoChain: () -> Unit,
    onToggleAutoReveal: () -> Unit,
    onToggleKeepPreviewInRecord: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleCountdown() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "3-Second Recording Countdown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "3-2-1 timer before first character is typed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useCountdown,
                onCheckedChange = { onToggleCountdown() },
                modifier = Modifier.testTag("more_countdown_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Post-Countdown Start Delay (Breathing Pause)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Post-Countdown Start Delay",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Breathing pause after countdown before typing begins so video starts cleanly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StartDelaySelectorChips(
                currentDelayMs = startDelayMs,
                onSelectDelayMs = onSelectStartDelayMs
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // In-App Screen Video Recording
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleAutoRecordInApp() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "In-App Screen Video Recorder",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Auto-record video directly to device Gallery when clean record starts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isAutoRecordInApp,
                onCheckedChange = { onToggleAutoRecordInApp() },
                modifier = Modifier.testTag("more_autorecord_inapp_switch")
            )
        }

        // Video Resolution
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Video Resolution",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = prefer1080p,
                    onClick = { onSet1080p(true) },
                    label = { Text("1080p Full HD", fontSize = 11.sp, fontWeight = if (prefer1080p) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("more_rec_1080p_chip")
                )
                FilterChip(
                    selected = !prefer1080p,
                    onClick = { onSet1080p(false) },
                    label = { Text("720p HD (Safe / Emulator)", fontSize = 11.sp, fontWeight = if (!prefer1080p) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("more_rec_720p_chip")
                )
            }
        }

        // Video Frame Rate
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Video Frame Rate",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = prefer60Fps,
                    onClick = { onSet60Fps(true) },
                    label = { Text("60 FPS (Ultra Smooth)", fontSize = 11.sp, fontWeight = if (prefer60Fps) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("more_rec_60fps_chip")
                )
                FilterChip(
                    selected = !prefer60Fps,
                    onClick = { onSet60Fps(false) },
                    label = { Text("30 FPS (Standard)", fontSize = 11.sp, fontWeight = if (!prefer60Fps) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("more_rec_30fps_chip")
                )
            }
        }

        // Microphone Audio Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleAudio() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Microphone Audio (Voiceover & Keyboard ASMR)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Record ambient mechanical clicks or creator commentary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = recordAudio,
                onCheckedChange = { onToggleAudio() },
                modifier = Modifier.testTag("more_rec_audio_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleAutoChain() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto Chain Tabs (HTML ➔ CSS ➔ JS)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Smooth 680ms slide between files automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isAutoChainTabs,
                onCheckedChange = { onToggleAutoChain() },
                modifier = Modifier.testTag("more_autochain_switch")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleAutoReveal() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Reveal Live Output on Finish",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Expands the live DOM preview card when auto-write finishes typing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoRevealPreview,
                onCheckedChange = { onToggleAutoReveal() },
                modifier = Modifier.testTag("more_autoreveal_switch")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleKeepPreviewInRecord() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep Floating Preview in Record Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keeps live DOM window visible during fullscreen recording",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = keepPreviewInRecord,
                onCheckedChange = { onToggleKeepPreviewInRecord() },
                modifier = Modifier.testTag("more_keep_preview_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reveal Live Output Preview on Finish",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Automatically pops up finished web UI when typing completes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoRevealPreview,
                onCheckedChange = { onToggleAutoReveal() },
                modifier = Modifier.testTag("more_autoreveal_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Keep Floating Preview in Record Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keeps live DOM window visible during fullscreen recording",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = keepPreviewInRecord,
                onCheckedChange = { onToggleKeepPreviewInRecord() },
                modifier = Modifier.testTag("more_keep_preview_switch")
            )
        }
    }
}

@Composable
private fun RecPreviewSectionContent(
    previewHeightDp: Float,
    scaleMode: PreviewScaleMode,
    onUpdateHeight: (Float) -> Unit,
    onCycleScaleMode: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Floating Preview Height",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${previewHeightDp.roundToInt()} dp",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = previewHeightDp,
            onValueChange = onUpdateHeight,
            valueRange = 100f..400f,
            steps = 15,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Preview Scale Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (scaleMode) {
                        PreviewScaleMode.AUTO_FIT -> "Auto-Fit (Fluid Scale)"
                        PreviewScaleMode.SCALE_100 -> "100% Desktop Scale"
                        PreviewScaleMode.SCALE_75 -> "75% Scale"
                        PreviewScaleMode.SCALE_50 -> "50% Scale"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(onClick = onCycleScaleMode) {
                Text("Cycle Mode")
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "REC SCREEN GESTURE SHORTCUTS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Double Tap: Toggle Floating Live Preview\n• Triple Tap: Toggle Clean Recording Mode\n• Single Tap: Play / Pause Auto-Write",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * About CodePad Studio Section (Professional Architecture & Feature Overview)
 */
@Composable
private fun AppAboutSection(
    onOpenGuide: () -> Unit = {}
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "About CodePad Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dual-Engine Architecture & Feature Overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = "CodePad Studio is a high-performance developer sandbox and viral coding shorts video engine designed for web development practice and automated cinematic video creation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "1. Editor Module (Codepad IDE):\n" +
                    "• Full-fidelity HTML5, CSS3, and modern JavaScript IDE with syntax highlighting and line-height gutter.\n" +
                    "• Real-time reactive Web Sandbox, DOM hierarchy inspector, and integrated console logs.\n" +
                    "• Code formatting, automatic SQLite persistence, and complete bundled HTML / ZIP export.\n\n" +
                    "2. Rec Module (Auto-Write Studio):\n" +
                    "• Typewriter-cadence video engine tailored for YouTube Shorts, Instagram Reels & TikTok.\n" +
                    "• Target duration presets (15s, 30s, 60s) with intelligent pacing and cadence calculation.\n" +
                    "• Direct 16-bit PCM synthesized mechanical keyboard switch acoustics with zero latency.\n" +
                    "• Creator watermark, viral hook header banner, and interactive outro CTA overlays.\n\n" +
                    "3. More Hub (Customization Hub):\n" +
                    "• Comprehensive controls for every IDE and recording overlay feature organized into clean, dedicated sections.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onOpenGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_btn_launch_guide"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Interactive App Tour & Guide",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * About Developer Section
 */
@Composable
private fun AboutDeveloperSection() {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "About Developer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Developer Profile & Contact",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = "Arnix / CodeWithArnix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mobile & Web Dev Creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Building high-performance developer tools, reactive UI sandboxes, and satisfying code animations for the creator community.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "codewitharnix@gmail.com",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
