package com.arnix.codepad.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.model.RecDividerStyle
import com.arnix.codepad.studio.model.RecRevealTransition
import com.arnix.codepad.studio.model.RecordingLayoutConfig
import com.arnix.codepad.studio.model.RecordingLayoutMode
import com.arnix.codepad.studio.model.SplitRatioPreset
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

/**
 * Material 3 Customization Sheet for Video Recording Layouts:
 * - Editor Only
 * - Editor Top • Preview Bottom
 * - Preview Top • Editor Bottom
 * - Editor First ➔ Fullscreen Preview at End
 * With live proportion mini-mockups, split ratio slider, divider styling, and reveal settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingLayoutCustomizerSheet(
    config: RecordingLayoutConfig,
    onUpdateConfig: (RecordingLayoutConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 38.dp, height = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Recording Screen Layouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Built-in layouts & ultra-customizable stage splits for video recording",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = config.layoutMode.shortLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                thickness = 0.5.dp
            )

            // 1. Four Core Built-in Layout Mode Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select Layout Style",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                RecordingLayoutMode.entries.forEach { mode ->
                    val isSelected = config.layoutMode == mode

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onUpdateConfig(config.copy(layoutMode = mode))
                            }
                            .testTag("layout_option_${mode.id}"),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Layout Visual Mockup Mini-Icon
                            LayoutMiniMockup(
                                mode = mode,
                                isSelected = isSelected,
                                editorWeight = config.editorWeight
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = mode.badge,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdateConfig(config.copy(layoutMode = mode)) }
                            )
                        }
                    }
                }
            }

            // 2. Ultra-Customizations: Split Ratio & Heights (Only for split modes)
            val isSplitMode = config.layoutMode == RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM ||
                config.layoutMode == RecordingLayoutMode.PREVIEW_TOP_EDITOR_BOTTOM

            if (isSplitMode) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Split Proportions (Editor : Preview)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adjust balance between typing code and live web view",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val editorPercent = (config.editorWeight * 100).roundToInt()
                            val previewPercent = 100 - editorPercent
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$editorPercent / $previewPercent",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Split Ratio Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SplitRatioPreset.entries.forEach { preset ->
                                val isChosen = kotlin.math.abs(config.editorWeight - preset.editorWeight) < 0.02f
                                FilterChip(
                                    selected = isChosen,
                                    onClick = {
                                        onUpdateConfig(config.copy(editorWeight = preset.editorWeight))
                                    },
                                    label = { Text(preset.label, fontSize = 11.sp) },
                                    leadingIcon = {
                                        if (isChosen) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                        }
                                    },
                                    modifier = Modifier.height(28.dp).testTag("split_chip_${preset.id}")
                                )
                            }
                        }

                        // Continuous Split Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (config.layoutMode == RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM) "Top Editor Height" else "Bottom Editor Height",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(config.editorWeight * 100).roundToInt()}% Code • ${((1f - config.editorWeight) * 100).roundToInt()}% Preview",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = config.editorWeight,
                                onValueChange = { weight ->
                                    onUpdateConfig(config.copy(editorWeight = (weight * 100).roundToInt() / 100f))
                                },
                                valueRange = 0.25f..0.75f,
                                modifier = Modifier.fillMaxWidth().testTag("editor_split_slider")
                            )
                        }
                    }
                }
            }

            // 3. Reveal Transition Settings (Only for EDITOR_THEN_FULL_PREVIEW_AT_END)
            if (config.layoutMode == RecordingLayoutMode.EDITOR_THEN_FULL_PREVIEW_AT_END) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "End Fullscreen Reveal Customization",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Code types full screen, then live web output auto-expands to 100% full screen at 100% completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Reveal Transition Chips
                        Text(
                            text = "Expansion Animation Style",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecRevealTransition.entries.forEach { transition ->
                                val isChosen = config.revealTransition == transition
                                FilterChip(
                                    selected = isChosen,
                                    onClick = { onUpdateConfig(config.copy(revealTransition = transition)) },
                                    label = { Text(transition.id, fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Delay Slider before expand
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pause Before Reveal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${config.revealDelayMs}ms", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = config.revealDelayMs.toFloat(),
                                onValueChange = { onUpdateConfig(config.copy(revealDelayMs = it.toLong())) },
                                valueRange = 200f..1500f,
                                steps = 12,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Divider & Border Styling
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Divider Style & Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecDividerStyle.entries.forEach { style ->
                        val isChosen = config.dividerStyle == style
                        FilterChip(
                            selected = isChosen,
                            onClick = { onUpdateConfig(config.copy(dividerStyle = style)) },
                            label = { Text(style.label, fontSize = 11.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Corner Radius Chips (0dp for seamless 9:16 reels, 8dp, 12dp, 16dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Window Corner Radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "0dp (Video Seamless)", 8 to "8dp", 14 to "14dp").forEach { (rad, lbl) ->
                            FilterChip(
                                selected = config.cornerRadiusDp == rad,
                                onClick = { onUpdateConfig(config.copy(cornerRadiusDp = rad)) },
                                label = { Text(lbl, fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Post-Typing Preview Showcase Duration (Auto-Balanced vs Fixed Presets)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Post-Typing Preview Showcase",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "How long viewers watch the live output after writing finishes (fits within selected target time)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (config.previewShowcaseSeconds == 0) "Auto-Balanced" else "${config.previewShowcaseSeconds}s Fixed",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Showcase Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "Auto (Smart Fit)",
                            3 to "3s Quick",
                            5 to "5s Recommended",
                            8 to "8s Detailed",
                            12 to "12s Extended"
                        ).forEach { (secs, lbl) ->
                            val isChosen = config.previewShowcaseSeconds == secs
                            FilterChip(
                                selected = isChosen,
                                onClick = { onUpdateConfig(config.copy(previewShowcaseSeconds = secs)) },
                                label = { Text(lbl, fontSize = 10.5.sp) },
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("showcase_preset_$secs")
                            )
                        }
                    }

                    if (config.previewShowcaseSeconds > 0) {
                        Slider(
                            value = config.previewShowcaseSeconds.toFloat(),
                            onValueChange = { onUpdateConfig(config.copy(previewShowcaseSeconds = it.roundToInt())) },
                            valueRange = 2f..15f,
                            steps = 12,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("showcase_duration_slider")
                        )
                    }

                    // Show badge toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateConfig(config.copy(showPreviewShowcaseBadge = !config.showPreviewShowcaseBadge)) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Show Showcase Timer Badge in Rec Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = config.showPreviewShowcaseBadge,
                            onCheckedChange = { onUpdateConfig(config.copy(showPreviewShowcaseBadge = it)) },
                            modifier = Modifier.testTag("show_showcase_badge_switch")
                        )
                    }
                }
            }

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("apply_recording_layout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Recording Layout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Animated Mini-Mockup showing visual proportions of each layout.
 */
@Composable
private fun LayoutMiniMockup(
    mode: RecordingLayoutMode,
    isSelected: Boolean,
    editorWeight: Float
) {
    val outlineColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val editorColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val previewColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier
            .size(width = 46.dp, height = 64.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, outlineColor)
    ) {
        when (mode) {
            RecordingLayoutMode.EDITOR_ONLY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(editorColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(editorWeight.coerceIn(0.3f, 0.7f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(editorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("<>", fontSize = 8.sp, color = Color.White, fontFamily = JetBrainsMonoFontFamily)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight((1f - editorWeight).coerceIn(0.3f, 0.7f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("UI", fontSize = 8.sp, color = Color.White, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
            }

            RecordingLayoutMode.PREVIEW_TOP_EDITOR_BOTTOM -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight((1f - editorWeight).coerceIn(0.3f, 0.7f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("UI", fontSize = 8.sp, color = Color.White, fontFamily = JetBrainsMonoFontFamily)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(editorWeight.coerceIn(0.3f, 0.7f))
                            .clip(RoundedCornerShape(2.dp))
                            .background(editorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("<>", fontSize = 8.sp, color = Color.White, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
            }

            RecordingLayoutMode.EDITOR_THEN_FULL_PREVIEW_AT_END -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(editorColor, previewColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Aesthetic divider rendered between split panes (Editor and Preview)
 * Supports Neon Cyber Glow, Minimalist Hairline, Draggable Pill Handle, or Seamless Borderless.
 */
@Composable
fun RecordingLayoutDivider(
    style: RecDividerStyle,
    modifier: Modifier = Modifier
) {
    when (style) {
        RecDividerStyle.SEAMLESS_FLAT -> {
            // zero height, completely flat seamless video
        }
        RecDividerStyle.MINIMAL_BAR -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )
        }
        RecDividerStyle.NEON_GLOW -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    )
            )
        }
        RecDividerStyle.PILL_HANDLE -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}
