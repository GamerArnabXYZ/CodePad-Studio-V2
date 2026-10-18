package com.arnix.codepad.studio.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import com.arnix.codepad.studio.data.ProjectPreset
import com.arnix.codepad.studio.data.ProjectTemplates
import com.arnix.codepad.studio.engine.ExportFormat
import com.arnix.codepad.studio.engine.ExportResult
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.uiSp
import kotlin.math.roundToInt

/**
 * Clean Material 3 Rename Project Dialog
 */
@Composable
fun RenameProjectDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draftName by remember(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Project",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a clear name for your project:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_project_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(draftName) },
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Clean Material 3 Settings Sheet (Theme Switcher + Full Font Size Controls + Starter Templates)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsSheet(
    currentFontSize: Float,
    autoWriteFontSize: Float = 11f,
    uiFontScale: Float = 0.92f,
    isDarkTheme: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onAutoWriteFontSizeChange: (Float) -> Unit = {},
    onUiFontScaleChange: (Float) -> Unit = {},
    onApplyFontPreset: (editorSize: Float, autoWriteSize: Float, uiScale: Float) -> Unit = { _, _, _ -> },
    onThemeChange: (Boolean) -> Unit,
    onSelectPreset: (ProjectPreset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Studio & Typography Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Light / Dark Theme Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isDarkTheme,
                        onClick = { onThemeChange(false) },
                        label = { Text("Light Theme") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = "Light Theme",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isDarkTheme,
                        onClick = { onThemeChange(true) },
                        label = { Text("Dark Theme") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Theme",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Comprehensive Font & Text Size Controls (IDE, Auto-Write, and App UI Scale)
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Font Size Presets",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentFontSize <= 10.5f && autoWriteFontSize <= 10f && uiFontScale <= 0.85f,
                            onClick = { onApplyFontPreset(10f, 9.5f, 0.82f) },
                            label = { Text("Extra Small") },
                            modifier = Modifier.testTag("font_preset_xs")
                        )
                        FilterChip(
                            selected = currentFontSize in 11f..12f && autoWriteFontSize in 10.5f..11.5f && uiFontScale in 0.88f..0.95f,
                            onClick = { onApplyFontPreset(11.5f, 11f, 0.92f) },
                            label = { Text("Compact (Default)") },
                            modifier = Modifier.testTag("font_preset_compact")
                        )
                        FilterChip(
                            selected = currentFontSize in 12.5f..14f && uiFontScale in 0.96f..1.05f,
                            onClick = { onApplyFontPreset(13f, 12.5f, 1.0f) },
                            label = { Text("Medium") },
                            modifier = Modifier.testTag("font_preset_medium")
                        )
                        FilterChip(
                            selected = currentFontSize >= 15f || uiFontScale >= 1.1f,
                            onClick = { onApplyFontPreset(15f, 14f, 1.12f) },
                            label = { Text("Large") },
                            modifier = Modifier.testTag("font_preset_large")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    // Slider A: IDE Editor Code Font Size (8px - 22px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IDE Editor Font Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentFontSize.roundToInt()} px",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.uiSp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = currentFontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 8f..22f,
                        steps = 13,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_size_slider")
                    )

                    // Slider B: Auto-Write Code Font Size (8px - 22px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Write Code Font Size",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${autoWriteFontSize.roundToInt()} px",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.uiSp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = autoWriteFontSize,
                        onValueChange = onAutoWriteFontSizeChange,
                        valueRange = 8f..22f,
                        steps = 13,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("autowrite_font_size_slider")
                    )

                    // Slider C: App UI Text Scale (75% - 130%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App UI & Labels Scale",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(uiFontScale * 100).roundToInt()}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.uiSp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = uiFontScale,
                        onValueChange = onUiFontScaleChange,
                        valueRange = 0.75f..1.30f,
                        steps = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ui_font_scale_slider")
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "IDE: ${currentFontSize.roundToInt()}px | AutoWrite: ${autoWriteFontSize.roundToInt()}px | UI: ${(uiFontScale * 100).roundToInt()}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = currentFontSize.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. Starter Project Templates
            Text(
                text = "Starter Templates",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProjectTemplates.PRESETS.forEach { preset ->
                Surface(
                    onClick = {
                        onSelectPreset(preset)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. About App & How to Use (Simple Hinglish)
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_app_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "About CodePad Studio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Ye Kya Hai: Mobile Web IDE (HTML, CSS, JS) + Viral Coding Shorts / Reels Auto-Write Creator Studio.\n" +
                            "• Kyun Banaya Gaya: Bina PC ke phone par live web coding karne aur Instagram Reels / YouTube Shorts ke liye smooth auto-typing coding videos + live output record karne ke liye.\n" +
                            "• Kaise Use Karein:\n" +
                            "  1. Studio Tab: index.html, style.css, script.js mein code likhein ya paste karein. Niche Live Preview aur JS Console mein turant output dekhein.\n" +
                            "  2. Auto-Write Tab: Apna code setup karke 'Start Auto-Write' dabayein. Speed, Video Ratio (9:16, 16:9, 1:1), Typing Sound, aur Live Output Popup control karein.\n" +
                            "  3. Export & Settings: Top bar se project ko Single .html ya .zip mein save karein aur Font Size / Theme apne hisaab se set karein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Clean Material 3 Export Sheet (.html Single Bundle or .zip Archive)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportProjectSheet(
    projectName: String,
    onExportAppStorage: (ExportFormat) -> Unit,
    onExportDeviceFilePicker: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Export \"$projectName\"",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Download your project as a single bundled HTML file or a complete ZIP archive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExportOptionCard(
                title = "Single Bundled .html",
                subtitle = "Combines index.html, style.css, and script.js into one file",
                icon = Icons.Default.Code,
                primaryActionLabel = "Quick Export",
                secondaryActionLabel = "Save to Device",
                onPrimaryClick = { onExportAppStorage(ExportFormat.BUNDLED_HTML) },
                onSecondaryClick = { onExportDeviceFilePicker(ExportFormat.BUNDLED_HTML) },
                testTagPrefix = "export_html"
            )

            ExportOptionCard(
                title = "Project .zip Archive",
                subtitle = "Includes separate index.html, style.css, script.js, and README.md",
                icon = Icons.Default.Archive,
                primaryActionLabel = "Quick Export",
                secondaryActionLabel = "Save to Device",
                onPrimaryClick = { onExportAppStorage(ExportFormat.ZIP_ARCHIVE) },
                onSecondaryClick = { onExportDeviceFilePicker(ExportFormat.ZIP_ARCHIVE) },
                testTagPrefix = "export_zip"
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    testTagPrefix: String
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPrimaryClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_quick_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = primaryActionLabel,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = primaryActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                OutlinedButton(
                    onClick = onSecondaryClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_saf_button")
                ) {
                    Text(
                        text = secondaryActionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Clean Material 3 Export Confirmation Dialog
 */
@Composable
fun ExportResultDialog(
    result: ExportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Exported",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Export Ready",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${result.fileName} (${result.sizeBytes} bytes)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Saved to: ${result.savedPathOrUri}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    Text(
                        text = result.bundledPreviewContent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, result.fileName)
                            putExtra(Intent.EXTRA_TEXT, result.bundledPreviewContent)
                        }
                        val chooser = Intent.createChooser(sendIntent, "Share ${result.fileName}").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Share",
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
