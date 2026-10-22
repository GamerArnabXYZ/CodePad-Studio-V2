package com.arnix.codepad.studio.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnix.codepad.studio.engine.BackgroundMusicEngine
import com.arnix.codepad.studio.model.BGM_PRESET_TRACKS
import com.arnix.codepad.studio.model.BgmConfig
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.viewmodel.AutoDurationTarget
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bottom Sheet for adding long background music / sound files and customizing
 * timing cuts (15s, 20s, 30s, 45s, 60s) with live audio preview and volume balance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAudioBgmSheet(
    bgmConfig: BgmConfig,
    activeDurationTarget: AutoDurationTarget,
    onUpdateConfig: (BgmConfig) -> Unit,
    onImportAudio: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isPreviewPlaying by BackgroundMusicEngine.isPlaying.collectAsStateWithLifecycle()
    val previewProgress by BackgroundMusicEngine.previewProgress.collectAsStateWithLifecycle()
    val previewElapsed by BackgroundMusicEngine.previewElapsedSec.collectAsStateWithLifecycle()
    val previewTotal by BackgroundMusicEngine.previewTotalSec.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            BackgroundMusicEngine.stopPlayback()
        }
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "custom_audio.mp3"
            onImportAudio(uri, fileName)
        }
    }

    val effectiveClipDuration = bgmConfig.getEffectiveDuration(activeDurationTarget)
    val maxStartOffset = (bgmConfig.totalDurationSeconds - effectiveClipDuration).coerceAtLeast(0)

    ModalBottomSheet(
        onDismissRequest = {
            BackgroundMusicEngine.stopPlayback()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title + Master Enable Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Background Music & Timing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Add custom audio & customize timing for 15s, 20s, 30s reels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = bgmConfig.isEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdateConfig(bgmConfig.copy(isEnabled = isChecked))
                        if (!isChecked) BackgroundMusicEngine.stopPlayback()
                    },
                    modifier = Modifier.testTag("bgm_master_switch")
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // 1. Audio Source Selection (Custom File or Procedural Presets)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Soundtrack Source",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Custom User File Picker Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (bgmConfig.customAudioPath != null) {
                                onUpdateConfig(
                                    bgmConfig.copy(
                                        selectedTrackId = "custom_file",
                                        trackTitle = bgmConfig.customAudioFileName ?: "Custom Audio File",
                                        isEnabled = true
                                    )
                                )
                            } else {
                                audioFilePicker.launch("audio/*")
                            }
                        }
                        .testTag("bgm_custom_file_card"),
                    color = if (bgmConfig.selectedTrackId == "custom_file") {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        width = if (bgmConfig.selectedTrackId == "custom_file") 1.5.dp else 0.5.dp,
                        color = if (bgmConfig.selectedTrackId == "custom_file") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (bgmConfig.selectedTrackId == "custom_file") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (bgmConfig.selectedTrackId == "custom_file") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (bgmConfig.customAudioFileName != null) bgmConfig.customAudioFileName else "Import Custom Audio File (MP3, WAV, M4A)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (bgmConfig.customAudioPath != null) {
                                    val mins = bgmConfig.customAudioDurationSeconds / 60
                                    val secs = bgmConfig.customAudioDurationSeconds % 60
                                    String.format(Locale.US, "Imported file • Duration: %02d:%02d", mins, secs)
                                } else {
                                    "Tap to select any song from your device storage"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { audioFilePicker.launch("audio/*") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(if (bgmConfig.customAudioPath != null) "Change" else "Browse", fontSize = 11.sp)
                        }
                    }
                }

                // Preset Soundtrack Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BGM_PRESET_TRACKS.forEach { preset ->
                        val isSelected = bgmConfig.selectedTrackId == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdateConfig(
                                    bgmConfig.copy(
                                        selectedTrackId = preset.id,
                                        trackTitle = preset.title,
                                        isEnabled = true
                                    )
                                )
                                BackgroundMusicEngine.stopPlayback()
                            },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = preset.genreTag,
                                        fontSize = 10.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("bgm_preset_${preset.id}")
                        )
                    }
                }
            }

            // 2. Timing Customization Section (15s, 20s, 30s, etc.)
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
                                text = "Recording Timing Window",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trim music to fit 15s, 20s, 30s or full auto-write length",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${effectiveClipDuration}s Clip",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Duration Target Chips (15s, 20s, 30s, 45s, 60s, Auto-Sync)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val durationOptions = listOf(15, 20, 30, 45, 60)
                        durationOptions.forEach { sec ->
                            val isChosen = !bgmConfig.autoSyncWithTargetDuration && bgmConfig.clipDurationSeconds == sec
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    onUpdateConfig(
                                        bgmConfig.copy(
                                            clipDurationSeconds = sec,
                                            autoSyncWithTargetDuration = false
                                        )
                                    )
                                    BackgroundMusicEngine.stopPlayback()
                                },
                                label = { Text("${sec}s", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp).testTag("bgm_duration_${sec}s")
                            )
                        }

                        FilterChip(
                            selected = bgmConfig.autoSyncWithTargetDuration,
                            onClick = {
                                onUpdateConfig(bgmConfig.copy(autoSyncWithTargetDuration = true))
                                BackgroundMusicEngine.stopPlayback()
                            },
                            label = { Text("Auto-Sync (${activeDurationTarget.shortLabel})", fontSize = 11.sp) },
                            leadingIcon = {
                                if (bgmConfig.autoSyncWithTargetDuration) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                }
                            },
                            modifier = Modifier.height(28.dp).testTag("bgm_duration_autosync")
                        )
                    }

                    // Precise custom duration slider if not auto-synced
                    if (!bgmConfig.autoSyncWithTargetDuration) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Duration Slider",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${bgmConfig.clipDurationSeconds}s",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = bgmConfig.clipDurationSeconds.toFloat(),
                                onValueChange = { newSec ->
                                    onUpdateConfig(bgmConfig.copy(clipDurationSeconds = newSec.roundToInt()))
                                    BackgroundMusicEngine.stopPlayback()
                                },
                                valueRange = 5f..120f,
                                modifier = Modifier.fillMaxWidth().testTag("bgm_custom_duration_slider")
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )

                    // Start Position Offset Slider
                    val safeStartOffset = bgmConfig.startOffsetSeconds.coerceIn(0, maxStartOffset)
                    val startMin = safeStartOffset / 60
                    val startSec = safeStartOffset % 60
                    val endTotalSec = safeStartOffset + effectiveClipDuration
                    val endMin = endTotalSec / 60
                    val endSec = endTotalSec % 60

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Start Position in Song",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d ➔ %02d:%02d (%ds)", startMin, startSec, endMin, endSec, effectiveClipDuration),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = safeStartOffset.toFloat(),
                            onValueChange = { newSec ->
                                onUpdateConfig(bgmConfig.copy(startOffsetSeconds = newSec.roundToInt()))
                            },
                            valueRange = 0f..maxStartOffset.toFloat().coerceAtLeast(1f),
                            modifier = Modifier.fillMaxWidth().testTag("bgm_start_offset_slider")
                        )

                        // Quick Jump Start Offset Buttons (Intro, 15s, Drop 30s, 45s, 1:00, 1:30, 2:00)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val jumpOffsets = listOf(
                                0 to "Intro (0:00)",
                                15 to "0:15",
                                30 to "0:30 (Drop)",
                                45 to "0:45",
                                60 to "1:00",
                                90 to "1:30",
                                120 to "2:00"
                            )
                            jumpOffsets.forEach { (sec, label) ->
                                if (sec <= maxStartOffset) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (safeStartOffset == sec) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                onUpdateConfig(bgmConfig.copy(startOffsetSeconds = sec))
                                                BackgroundMusicEngine.stopPlayback()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = if (safeStartOffset == sec) FontWeight.Bold else FontWeight.Normal,
                                            color = if (safeStartOffset == sec) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Audio Preview Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (isPreviewPlaying) {
                                        BackgroundMusicEngine.stopPlayback()
                                    } else {
                                        BackgroundMusicEngine.startPreview(context, bgmConfig, activeDurationTarget)
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("bgm_preview_play_button")
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPreviewPlaying) "Pause Preview" else "Play Preview",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isPreviewPlaying) "Previewing ${effectiveClipDuration}s Segment..." else "Preview Trimmed Audio",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPreviewPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${previewElapsed}s / ${effectiveClipDuration}s",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { previewProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Audio Balance & Controls (Volume, Fade-Out, Looping)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sound Balance & Transitions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Volume Slider
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Music Volume",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${(bgmConfig.volume * 100).roundToInt()}% (${if (bgmConfig.volume <= 0.45f) "Balanced for ASMR" else "Prominent"})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = bgmConfig.volume,
                            onValueChange = { vol ->
                                onUpdateConfig(bgmConfig.copy(volume = vol))
                            },
                            valueRange = 0.10f..1.0f,
                            modifier = Modifier.fillMaxWidth().testTag("bgm_volume_slider")
                        )
                    }
                }

                // Smooth 1.5s Fade Out & Loop Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdateConfig(bgmConfig.copy(isFadeOutEnabled = !bgmConfig.isFadeOutEnabled))
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("1.5s Fade Out", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Smooth musical exit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = bgmConfig.isFadeOutEnabled,
                                onCheckedChange = { onUpdateConfig(bgmConfig.copy(isFadeOutEnabled = it)) },
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdateConfig(bgmConfig.copy(isLoopEnabled = !bgmConfig.isLoopEnabled))
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Loop Music", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Repeat if video exceeds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = bgmConfig.isLoopEnabled,
                                onCheckedChange = { onUpdateConfig(bgmConfig.copy(isLoopEnabled = it)) },
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            // Apply Button
            Button(
                onClick = {
                    BackgroundMusicEngine.stopPlayback()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("bgm_save_and_apply_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply Audio Customization")
            }
        }
    }
}

@Composable
private fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
