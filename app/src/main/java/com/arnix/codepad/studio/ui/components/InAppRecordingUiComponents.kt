package com.arnix.codepad.studio.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arnix.codepad.studio.model.RecordingAudioSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.codepad.studio.engine.SavedRecordingInfo
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.uiSp
import java.util.Locale

/**
 * Floating Live Recording Indicator Pill.
 * Shows elapsed time (00:15) with pulsing red dot and instant Stop button.
 */
@Composable
fun InAppRecordingFloatingPill(
    durationSeconds: Int,
    isRecording: Boolean,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isRecording) return

    val infiniteTransition = rememberInfiniteTransition(label = "recDotPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF090D16).copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
        shadowElevation = 8.dp,
        modifier = modifier.testTag("in_app_recording_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )

            Text(
                text = "REC",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFFEF4444)
            )

            Text(
                text = timeFormatted,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                    .clickable { onStopClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop Recording",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Dialog shown right after an In-App Screen Recording finishes.
 * Allows instant video playback and sharing to social media / Shorts.
 */
@Composable
fun SavedRecordingSuccessDialog(
    recordingInfo: SavedRecordingInfo?,
    onPlay: (Uri) -> Unit,
    onShare: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (recordingInfo == null) return

    val mins = recordingInfo.durationSeconds / 60
    val secs = recordingInfo.durationSeconds % 60
    val durationStr = String.format(Locale.US, "%02d:%02d", mins, secs)
    val sizeMb = String.format(Locale.US, "%.1f MB", recordingInfo.fileSizeBytes / (1024.0 * 1024.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Recording Saved",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "In-App Recording Saved!",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "High-definition MP4 video saved cleanly outside Android/data to your CodePad storage folder & Gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = recordingInfo.fileName,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duration: $durationStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Size: $sizeMb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = recordingInfo.filePath,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPlay(recordingInfo.fileUri) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Video")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onShare(recordingInfo.fileUri, recordingInfo.fileName) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}

/**
 * Pre-Roll Start Delay Selector (0s, 0.5s, 1.0s, 1.5s, 2.0s).
 */
@Composable
fun StartDelaySelectorChips(
    currentDelayMs: Long,
    onSelectDelayMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val delayOptions = listOf(
        0L to "None (0s)",
        500L to "0.5s",
        1000L to "1.0s (Best)",
        1500L to "1.5s",
        2000L to "2.0s"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        delayOptions.forEach { (ms, label) ->
            FilterChip(
                selected = currentDelayMs == ms,
                onClick = { onSelectDelayMs(ms) },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (currentDelayMs == ms) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.testTag("start_delay_chip_${ms}")
            )
        }
    }
}

/**
 * Material 3 Modal Bottom Sheet for selecting recording audio source:
 * 1. System sound only (App audio & mechanical typing clicks, zero mic noise)
 * 2. Microphone only (Device mic voiceover, typing sounds muted)
 * 3. System sound with microphone (Both typing sounds & mic commentary)
 * 4. Silent (No audio, pure visual video for reels/shorts)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingAudioSourceBottomSheet(
    selectedSource: RecordingAudioSource,
    onSelectSource: (RecordingAudioSource) -> Unit,
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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Sound Recording Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose audio input for your screen video recording",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecordingAudioSource.entries.forEach { source ->
                    val isSelected = source == selectedSource
                    val badgeText = when (source) {
                        RecordingAudioSource.SYSTEM_ONLY -> "ASMR Typing"
                        RecordingAudioSource.MIC_ONLY -> "Voiceover"
                        RecordingAudioSource.SYSTEM_AND_MIC -> "Dual Audio"
                        RecordingAudioSource.SILENT -> "Visual Only"
                    }
                    val icon = when (source) {
                        RecordingAudioSource.SYSTEM_ONLY -> Icons.Filled.Headphones
                        RecordingAudioSource.MIC_ONLY -> Icons.Filled.Mic
                        RecordingAudioSource.SYSTEM_AND_MIC -> Icons.Filled.GraphicEq
                        RecordingAudioSource.SILENT -> Icons.Filled.VolumeOff
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectSource(source)
                                onDismiss()
                            }
                            .testTag("audio_option_${source.id}"),
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = source.title,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = source.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelectSource(source)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

