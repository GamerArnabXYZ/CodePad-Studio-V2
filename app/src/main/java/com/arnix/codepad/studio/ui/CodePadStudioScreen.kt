package com.arnix.codepad.studio.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.data.CodeTab
import com.arnix.codepad.studio.engine.ExportFormat
import com.arnix.codepad.studio.engine.SyntaxHighlightVisualTransformation
import com.arnix.codepad.studio.engine.WebSandboxEngine
import com.arnix.codepad.studio.ui.components.ComponentStoreSheet
import com.arnix.codepad.studio.ui.components.CssColorPickerDialog
import com.arnix.codepad.studio.ui.components.ExportProjectSheet
import com.arnix.codepad.studio.ui.components.ExportResultDialog
import com.arnix.codepad.studio.ui.components.IntegratedConsolePanel
import com.arnix.codepad.studio.ui.components.NativeDomPreviewPanel
import com.arnix.codepad.studio.ui.components.RenameProjectDialog
import com.arnix.codepad.studio.ui.components.RevisionHistorySheet
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.LocalEditorPalette
import com.arnix.codepad.studio.ui.theme.uiSp
import com.arnix.codepad.studio.ui.theme.StatusSavedGreenBgDark
import com.arnix.codepad.studio.ui.theme.StatusSavedGreenBgLight
import com.arnix.codepad.studio.ui.theme.StatusSavedGreenDark
import com.arnix.codepad.studio.ui.theme.StatusSavedGreenLight
import com.arnix.codepad.studio.ui.theme.StatusUnsavedOrangeBgDark
import com.arnix.codepad.studio.ui.theme.StatusUnsavedOrangeBgLight
import com.arnix.codepad.studio.ui.theme.StatusUnsavedOrangeDark
import com.arnix.codepad.studio.ui.theme.StatusUnsavedOrangeLight
import com.arnix.codepad.studio.viewmodel.StudioUiState
import com.arnix.codepad.studio.viewmodel.StudioViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CodePadStudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editorPalette = LocalEditorPalette.current
    var pendingSafFormat by remember { mutableStateOf(ExportFormat.BUNDLED_HTML) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUserUri(context, uri, pendingSafFormat)
        }
    }

    var textFieldValue by remember(uiState.activeTab) {
        mutableStateOf(
            TextFieldValue(
                text = uiState.activeCode,
                selection = TextRange(0)
            )
        )
    }

    LaunchedEffect(uiState.activeCode, uiState.activeTab) {
        if (textFieldValue.text != uiState.activeCode) {
            val safeSelection = textFieldValue.selection.start.coerceIn(0, uiState.activeCode.length)
            textFieldValue = TextFieldValue(
                text = uiState.activeCode,
                selection = TextRange(safeSelection)
            )
        }
    }

    LaunchedEffect(uiState.activeMatchRange) {
        uiState.activeMatchRange?.let { (start, end) ->
            if (start in 0..textFieldValue.text.length && end in 0..textFieldValue.text.length) {
                textFieldValue = textFieldValue.copy(selection = TextRange(start, end))
            }
        }
    }

    val visualTransformation = remember(uiState.activeTab, editorPalette) {
        SyntaxHighlightVisualTransformation(uiState.activeTab, editorPalette)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Balanced Top App Bar (Project Title + Saved/Unsaved Status + Actions)
        IdeTopAppBar(
            projectName = uiState.projectName,
            isSaved = uiState.isSaved,
            isDarkTheme = uiState.isDarkTheme,
            onEditProjectName = { viewModel.setRenameDialogOpen(true) },
            onToggleSearch = { viewModel.toggleFindReplace() },
            onOpenSnippets = { viewModel.setSnippetStoreOpen(true) },
            onOpenRevisions = { viewModel.setRevisionSheetOpen(true) },
            onOpenColorPicker = { viewModel.setColorPickerOpen(true) },
            onFormatCode = { viewModel.formatActiveTabCode() },
            onToggleTheme = { viewModel.toggleTheme() },
            onOpenExport = { viewModel.setExportSheetOpen(true) },
            onOpenGuide = { viewModel.openOnboardingGuide() }
        )

        // 2. Native Material 3 PrimaryTabRow (index.html, style.css, script.js)
        PrimaryTabRow(
            selectedTabIndex = uiState.activeTab.index,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            CodeTab.entries.forEach { tab ->
                val selected = tab == uiState.activeTab
                Tab(
                    selected = selected,
                    onClick = { viewModel.selectTab(tab) },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("editor_tab_${tab.extension}"),
                    text = {
                        Text(
                            text = tab.fileName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Find & Replace Suite
        AnimatedVisibility(
            visible = uiState.isFindReplaceOpen,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            FindAndReplaceBar(
                query = uiState.findQuery,
                replaceQuery = uiState.replaceQuery,
                matchCount = uiState.findTotalMatches,
                currentMatchIndex = uiState.findCurrentMatchIndex,
                isMatchCase = uiState.isFindMatchCase,
                isUseRegex = uiState.isFindUseRegex,
                onQueryChange = { viewModel.setFindQuery(it) },
                onReplaceQueryChange = { viewModel.setReplaceQuery(it) },
                onToggleMatchCase = { viewModel.toggleFindMatchCase() },
                onToggleUseRegex = { viewModel.toggleFindUseRegex() },
                onFindPrevious = { viewModel.findPreviousMatch() },
                onFindNext = { viewModel.findNextMatch() },
                onReplaceCurrent = { viewModel.replaceCurrentMatch() },
                onReplaceAll = { viewModel.replaceAllMatches() },
                onClose = { viewModel.toggleFindReplace(false) }
            )
        }

        // Optional 1-Tap Split Banner when user pastes a full HTML document with <style> or <script> into index.html
        val hasEmbeddedBlocks = remember(uiState.activeTab, uiState.htmlCode) {
            uiState.activeTab == CodeTab.INDEX_HTML && WebSandboxEngine.hasEmbeddedStyleOrScript(uiState.htmlCode)
        }
        AnimatedVisibility(
            visible = hasEmbeddedBlocks,
            enter = expandVertically(tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Embedded <style> / <script> detected in HTML",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { viewModel.splitEmbeddedHtmlIntoTabs() },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("split_embedded_html_button")
                    ) {
                        Text(
                            text = "Split to CSS/JS Tabs",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.uiSp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 3. Synchronized Code Editor with Exact Line-Height Gutter
        SynchronizedCodeEditorWithGutter(
            textFieldValue = textFieldValue,
            onValueChange = { updatedValue ->
                val textChanged = updatedValue.text != textFieldValue.text
                textFieldValue = updatedValue
                if (textChanged) {
                    viewModel.onCodeChanged(updatedValue.text, updatedValue.selection.start)
                } else {
                    viewModel.onCursorPositionChanged(updatedValue.text, updatedValue.selection.start)
                }
            },
            visualTransformation = visualTransformation,
            fontSizeSp = uiState.editorFontSize,
            currentLine = uiState.currentLine,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // 4. Collapsible Live Preview (mounts only when preview is visible)
        AnimatedVisibility(
            visible = uiState.isPreviewVisible,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                NativeDomPreviewPanel(
                    domNodes = uiState.domNodes,
                    bodyStyle = uiState.bodyStyle,
                    onDomButtonClick = { fn -> viewModel.onNativeDomButtonClicked(fn) },
                    onRefreshRunJs = { viewModel.forceReloadPreview() },
                    htmlCode = uiState.htmlCode,
                    cssCode = uiState.cssCode,
                    jsCode = uiState.jsCode,
                    runJs = true,
                    isProgressiveAutoWrite = false,
                    previewScaleMode = uiState.previewScaleMode,
                    previewReloadNonce = uiState.previewReloadNonce,
                    onCycleScaleMode = { viewModel.cyclePreviewScaleMode() },
                    onToggleExpandHeight = { viewModel.togglePreviewExpandedHeight() },
                    onWebConsoleLog = { level, msg, src, stack ->
                        viewModel.onWebConsoleMessage(level, msg, src, stack)
                    },
                    onWebConsoleClear = { viewModel.onWebConsoleClear() },
                    onWebViewAttachedChanged = { attached ->
                        viewModel.setWebViewEngineAttached(attached)
                    },
                    onClose = { viewModel.togglePreview() },
                    maxBodyHeight = uiState.previewSectionHeightDp.dp,
                    onResizeDeltaDp = { deltaDp -> viewModel.adjustPreviewSectionHeight(deltaDp) }
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.isConsoleVisible,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                IntegratedConsolePanel(
                    logs = uiState.consoleLogs,
                    activeFilter = uiState.consoleFilter,
                    onSelectFilter = { viewModel.setConsoleFilter(it) },
                    onSimulateError = { viewModel.forceReloadPreview() },
                    onClearConsole = { viewModel.clearConsole() },
                    onClose = { viewModel.toggleConsole() }
                )
            }
        }

        // 5. Full-Width Quick Symbol Bar (dedicated 38.dp row so symbols never collide with Preview/Console buttons)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        QuickSymbolBar(
            onInsertSymbol = { sym ->
                val currentText = textFieldValue.text
                val start = textFieldValue.selection.start.coerceIn(0, currentText.length)
                val end = textFieldValue.selection.end.coerceIn(0, currentText.length)
                val pairs = mapOf(
                    "{" to "}",
                    "(" to ")",
                    "[" to "]",
                    "\"" to "\"",
                    "'" to "'"
                )
                val isPair = pairs.containsKey(sym)
                viewModel.pushUndoCheckpoint(uiState.activeTab, currentText)
                if (start != end && isPair) {
                    val selectedText = currentText.substring(start, end)
                    val closer = pairs[sym] ?: ""
                    val newText = currentText.replaceRange(start, end, "$sym$selectedText$closer")
                    val newCursor = end + sym.length + closer.length
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                    viewModel.onCodeChanged(newText, newCursor)
                } else if (isPair && uiState.isAutoClosePairs) {
                    val closer = pairs[sym] ?: ""
                    val newText = currentText.replaceRange(start, end, "$sym$closer")
                    val newCursor = start + sym.length
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                    viewModel.onCodeChanged(newText, newCursor)
                } else {
                    val newText = currentText.replaceRange(start, end, sym)
                    val newCursor = start + sym.length
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                    viewModel.onCodeChanged(newText, newCursor)
                }
            },
            onUndo = {
                val res = viewModel.undoActiveTabCode()
                if (res != null) {
                    val safeIdx = res.second.coerceIn(0, res.first.length)
                    textFieldValue = TextFieldValue(text = res.first, selection = TextRange(safeIdx))
                }
            },
            onRedo = {
                val res = viewModel.redoActiveTabCode()
                if (res != null) {
                    val safeIdx = res.second.coerceIn(0, res.first.length)
                    textFieldValue = TextFieldValue(text = res.first, selection = TextRange(safeIdx))
                }
            },
            onOpenColorPicker = { viewModel.setColorPickerOpen(true) },
            onClearActiveTab = {
                viewModel.pushUndoCheckpoint(uiState.activeTab, textFieldValue.text)
                textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                viewModel.clearActiveTabCode()
            },
            onClearAllTabs = {
                viewModel.pushUndoCheckpoint(uiState.activeTab, textFieldValue.text)
                textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                viewModel.clearAllTabsForNewSnippet()
            }
        )

        // 6. Unified Bottom Status & Panel Bar (Ln X, Col Y + Language Tag on left, Preview & Console toggles on right)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        IdeBottomStatusAndToolsBar(
            currentLine = uiState.currentLine,
            currentColumn = uiState.currentColumn,
            activeTab = uiState.activeTab,
            fontSize = uiState.editorFontSize,
            isPreviewVisible = uiState.isPreviewVisible,
            isConsoleVisible = uiState.isConsoleVisible,
            errorCount = uiState.errorCount,
            onTogglePreview = { viewModel.togglePreview() },
            onToggleConsole = { viewModel.toggleConsole() }
        )
    }

    // Dialogs & Modal Bottom Sheets
    if (uiState.isColorPickerOpen) {
        CssColorPickerDialog(
            initialColorHex = uiState.initialColorHex,
            onInsertColor = { colorVal ->
                val currentText = textFieldValue.text
                val start = textFieldValue.selection.start.coerceIn(0, currentText.length)
                val end = textFieldValue.selection.end.coerceIn(0, currentText.length)
                viewModel.pushUndoCheckpoint(uiState.activeTab, currentText)
                val newText = currentText.replaceRange(start, end, colorVal)
                val newCursor = start + colorVal.length
                textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                viewModel.onCodeChanged(newText, newCursor)
            },
            onDismiss = { viewModel.setColorPickerOpen(false) }
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

    // Dialogs & Modal Bottom Sheets
    if (uiState.isRenameDialogOpen) {
        RenameProjectDialog(
            currentName = uiState.projectName,
            onConfirm = { viewModel.updateProjectName(it) },
            onDismiss = { viewModel.setRenameDialogOpen(false) }
        )
    }

    if (uiState.isExportSheetOpen) {
        ExportProjectSheet(
            projectName = uiState.projectName,
            onExportAppStorage = { format ->
                viewModel.exportToAppStorage(context, format)
            },
            onExportDeviceFilePicker = { format ->
                pendingSafFormat = format
                val safeName = uiState.projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                createDocumentLauncher.launch("$safeName.${format.extension}")
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
}

/**
 * Balanced Top Bar:
 * Left Column: Project Name + Edit icon on top line, Saved/Unsaved status pill on second line.
 * This prevents the Project Name and Status Badge from fighting for horizontal space on 360dp screens!
 */
@Composable
private fun IdeTopAppBar(
    projectName: String,
    isSaved: Boolean,
    isDarkTheme: Boolean,
    onEditProjectName: () -> Unit,
    onToggleSearch: () -> Unit,
    onOpenSnippets: () -> Unit,
    onOpenRevisions: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onFormatCode: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val statusTextColor by animateColorAsState(
        targetValue = when {
            isSaved && isDarkTheme -> StatusSavedGreenDark
            isSaved && !isDarkTheme -> StatusSavedGreenLight
            !isSaved && isDarkTheme -> StatusUnsavedOrangeDark
            else -> StatusUnsavedOrangeLight
        },
        animationSpec = tween(220),
        label = "statusTextColor"
    )
    val statusBgColor by animateColorAsState(
        targetValue = when {
            isSaved && isDarkTheme -> StatusSavedGreenBgDark
            isSaved && !isDarkTheme -> StatusSavedGreenBgLight
            !isSaved && isDarkTheme -> StatusUnsavedOrangeBgDark
            else -> StatusUnsavedOrangeBgLight
        },
        animationSpec = tween(220),
        label = "statusBgColor"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Two-line Project Title + Saved/Unsaved Status Badge
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEditProjectName)
                    .padding(vertical = 2.dp, horizontal = 2.dp)
                    .testTag("edit_project_name_button"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Rename Project",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Saved (Green) / Unsaved (Orange) Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("save_status_badge")
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusTextColor)
                    )
                    Text(
                        text = if (isSaved) "Saved" else "Unsaved",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.uiSp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor
                    )
                }
            }

            // Right: Productivity Action Buttons with clean 38.dp size in horizontal scroll
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .weight(1.35f, fill = false)
                    .horizontalScroll(rememberScrollState())
            ) {
                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Find and Replace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSnippets,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_snippets_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = "Component and Snippet Store",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenRevisions,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_revisions_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Code Revision Snapshots",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenColorPicker,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("open_color_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ColorLens,
                        contentDescription = "CSS Color Picker",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onFormatCode,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("format_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = "Format Code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_theme_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle Light or Dark Theme",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenExport,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("export_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Export Project",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenGuide,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("onboarding_tour_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Guided App Tour & Tips",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom Code Editor with Synchronized Line Number Gutter.
 * Uses identical PlatformTextStyle(includeFontPadding = false) and fixed row height
 * so every line number aligns pixel-perfectly with its code line!
 */
@Composable
private fun SynchronizedCodeEditorWithGutter(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    visualTransformation: SyntaxHighlightVisualTransformation,
    fontSizeSp: Float,
    currentLine: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalEditorPalette.current
    val density = LocalDensity.current
    val sharedVerticalScroll = rememberScrollState()
    val horizontalCodeScroll = rememberScrollState()

    LaunchedEffect(visualTransformation) {
        sharedVerticalScroll.scrollTo(0)
        horizontalCodeScroll.scrollTo(0)
    }

    val lineCount = remember(textFieldValue.text) {
        max(1, textFieldValue.text.count { it == '\n' } + 1)
    }

    val lineHeightSp = (fontSizeSp * 1.5f)
    val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }
    val verticalPaddingPx = with(density) { 12.dp.toPx() }

    val sharedCodeTextStyle = remember(fontSizeSp, palette.plainText) {
        TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = lineHeightSp.sp,
            color = palette.plainText,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    }

    val gutterAnnotatedNumbers = remember(lineCount, currentLine, palette.gutterText, palette.gutterActiveText) {
        buildAnnotatedString {
            for (lineIdx in 1..lineCount) {
                val isCurrent = lineIdx == currentLine
                withStyle(
                    SpanStyle(
                        color = if (isCurrent) palette.gutterActiveText else palette.gutterText,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                ) {
                    append(lineIdx.toString())
                }
                if (lineIdx < lineCount) {
                    append('\n')
                }
            }
        }
    }

    Row(
        modifier = modifier
            .background(palette.editorBackground)
    ) {
        // Left: Single-node Line Numbers Gutter for O(1) layout even on 1,000+ line files
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(palette.gutterBackground)
                .verticalScroll(sharedVerticalScroll)
                .drawBehind {
                    val activeIdx = (currentLine - 1).coerceAtLeast(0)
                    val topY = verticalPaddingPx + (activeIdx * lineHeightPx)
                    drawRect(
                        color = palette.activeLineBackground,
                        topLeft = Offset(0f, topY),
                        size = Size(size.width, lineHeightPx)
                    )
                }
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .testTag("editor_line_gutter"),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = gutterAnnotatedNumbers,
                style = sharedCodeTextStyle.copy(
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 1.dp Divider between Gutter and Code
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(palette.divider)
        )

        // Right: Syntax-Highlighted Code Editor Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(palette.editorBackground)
                .verticalScroll(sharedVerticalScroll)
                .horizontalScroll(horizontalCodeScroll)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                textStyle = sharedCodeTextStyle,
                cursorBrush = SolidColor(palette.cursor),
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .defaultMinSize(minWidth = 320.dp, minHeight = 260.dp)
                    .fillMaxSize()
                    .testTag("code_editor_input")
            )
        }
    }
}

/**
 * Dedicated Full-Width Quick Symbol Bar (38.dp tall) so symbols are easy to tap and never cramped.
 */
@Composable
private fun QuickSymbolBar(
    onInsertSymbol: (String) -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onOpenColorPicker: () -> Unit = {},
    onClearActiveTab: () -> Unit = {},
    onClearAllTabs: () -> Unit = {}
) {
    val symbols = listOf("  ", "<", ">", "/", "=", "\"", "'", "{", "}", "(", ")", "[", "]", ";", ":", "!", "$", "#", ".", "/* */", "->")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo & Redo quick action pills
            Surface(
                onClick = onUndo,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.testTag("symbol_undo_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Undo, contentDescription = "Undo", modifier = Modifier.size(13.dp))
                    Text(text = "Undo", fontSize = 10.uiSp, fontWeight = FontWeight.Medium)
                }
            }

            Surface(
                onClick = onRedo,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.testTag("symbol_redo_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Redo, contentDescription = "Redo", modifier = Modifier.size(13.dp))
                    Text(text = "Redo", fontSize = 10.uiSp, fontWeight = FontWeight.Medium)
                }
            }

            Surface(
                onClick = onOpenColorPicker,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.testTag("symbol_color_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ColorLens, contentDescription = "Color", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Color", fontSize = 10.uiSp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            symbols.forEach { sym ->
                val label = if (sym == "  ") "Tab" else sym
                Surface(
                    onClick = { onInsertSymbol(sym) },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.uiSp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Surface(
                onClick = onClearActiveTab,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.testTag("clear_active_tab_button")
            ) {
                Text(
                    text = "Clear Tab",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.uiSp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Surface(
                onClick = onClearAllTabs,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("clear_all_tabs_button")
            ) {
                Text(
                    text = "Clear All",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.uiSp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun FindAndReplaceBar(
    query: String,
    replaceQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    isMatchCase: Boolean,
    isUseRegex: Boolean,
    onQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleMatchCase: () -> Unit,
    onToggleUseRegex: () -> Unit,
    onFindPrevious: () -> Unit,
    onFindNext: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("find_and_replace_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Line 1: Search row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.uiSp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("find_input_field"),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Find in code...",
                                style = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.uiSp, color = MaterialTheme.colorScheme.outline)
                            )
                        }
                        innerTextField()
                    }
                )

                // Match count badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (matchCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (query.isEmpty()) "0" else "${if (matchCount > 0) currentMatchIndex + 1 else 0}/$matchCount",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.uiSp,
                        fontWeight = FontWeight.Bold,
                        color = if (matchCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Match case toggle
                Surface(
                    onClick = onToggleMatchCase,
                    shape = RoundedCornerShape(6.dp),
                    color = if (isMatchCase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("find_match_case_button")
                ) {
                    Text(
                        text = "Aa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.uiSp,
                        color = if (isMatchCase) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Regex toggle
                Surface(
                    onClick = onToggleUseRegex,
                    shape = RoundedCornerShape(6.dp),
                    color = if (isUseRegex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("find_regex_button")
                ) {
                    Text(
                        text = ".*",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.uiSp,
                        color = if (isUseRegex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Prev match
                IconButton(
                    onClick = onFindPrevious,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("find_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Previous Match",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Next match
                IconButton(
                    onClick = onFindNext,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("find_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Next Match",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("find_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Find",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Line 2: Replace row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.uiSp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("replace_input_field"),
                    decorationBox = { innerTextField ->
                        if (replaceQuery.isEmpty()) {
                            Text(
                                text = "Replace with...",
                                style = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.uiSp, color = MaterialTheme.colorScheme.outline)
                            )
                        }
                        innerTextField()
                    }
                )

                Surface(
                    onClick = onReplaceCurrent,
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("replace_current_button")
                ) {
                    Text(
                        text = "Replace",
                        fontSize = 10.uiSp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    onClick = onReplaceAll,
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("replace_all_button")
                ) {
                    Text(
                        text = "Replace All",
                        fontSize = 10.uiSp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Unified Bottom Status Bar & Preview/Console Toggle Bar (42.dp tall).
 * Left: Ln X, Col Y • Active Language Tag • Font Size
 * Right: Compact Preview & Console toggle pills
 */
@Composable
private fun IdeBottomStatusAndToolsBar(
    currentLine: Int,
    currentColumn: Int,
    activeTab: CodeTab,
    fontSize: Float,
    isPreviewVisible: Boolean,
    isConsoleVisible: Boolean,
    errorCount: Int,
    onTogglePreview: () -> Unit,
    onToggleConsole: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("ide_bottom_status_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Line/Column indicator + Language badge + Font size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ln $currentLine, Col $currentColumn",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.uiSp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("status_line_indicator")
                )

                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("active_language_tag")
                ) {
                    Text(
                        text = activeTab.languageTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.uiSp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${fontSize.roundToInt()}px",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.uiSp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Compact Preview & Console Toggle Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactPanelTogglePill(
                    label = "Preview",
                    icon = Icons.Outlined.Visibility,
                    selected = isPreviewVisible,
                    badgeCount = 0,
                    onClick = onTogglePreview,
                    testTag = "toggle_preview_button"
                )

                CompactPanelTogglePill(
                    label = "Console",
                    icon = Icons.Outlined.Terminal,
                    selected = isConsoleVisible,
                    badgeCount = errorCount,
                    onClick = onToggleConsole,
                    testTag = "toggle_console_button"
                )
            }
        }
    }
}

@Composable
private fun CompactPanelTogglePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier
            .height(30.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.uiSp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
