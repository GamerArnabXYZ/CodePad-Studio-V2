package com.arnix.codepad.studio.ui.components

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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arnix.codepad.studio.ui.theme.JetBrainsMonoFontFamily
import com.arnix.codepad.studio.ui.theme.uiSp
import kotlin.math.roundToInt

@Composable
fun CssColorPickerDialog(
    initialColorHex: String = "#38BDF8",
    onInsertColor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember(initialColorHex) { mutableStateOf(initialColorHex.uppercase()) }

    var red by remember {
        val parsed = parseHexColor(initialColorHex)
        mutableFloatStateOf(parsed.red)
    }
    var green by remember {
        val parsed = parseHexColor(initialColorHex)
        mutableFloatStateOf(parsed.green)
    }
    var blue by remember {
        val parsed = parseHexColor(initialColorHex)
        mutableFloatStateOf(parsed.blue)
    }
    var alpha by remember {
        val parsed = parseHexColor(initialColorHex)
        mutableFloatStateOf(parsed.alpha)
    }

    val currentColor = Color(red = red, green = green, blue = blue, alpha = alpha)
    val currentHex = String.format("#%02X%02X%02X", (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
    val currentRgba = "rgba(${(red * 255).roundToInt()}, ${(green * 255).roundToInt()}, ${(blue * 255).roundToInt()}, ${String.format("%.2f", alpha)})"

    val curatedPalettes = listOf(
        "Neon Cyber" to listOf("#06B6D4", "#EC4899", "#F43F5E", "#22D3EE", "#F59E0B", "#8B5CF6", "#10B981"),
        "Dark Slate" to listOf("#0F172A", "#1E293B", "#334155", "#64748B", "#94A3B8", "#F8FAFC", "#000000"),
        "Vibrant Pastels" to listOf("#FDE68A", "#A7F3D0", "#BAE6FD", "#C7D2FE", "#FBCFE8", "#FED7AA", "#FFFFFF"),
        "Material Studio" to listOf("#2563EB", "#16A34A", "#DC2626", "#D97706", "#7C3AED", "#0D9488", "#4F46E5")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CSS Color Picker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentHex,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.uiSp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentRgba,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.uiSp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // RGB & Alpha Sliders
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ColorSliderRow(
                        label = "Red",
                        value = red,
                        colorTint = Color(0xFFEF4444),
                        onValueChange = {
                            red = it
                            hexInput = String.format("#%02X%02X%02X", (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
                        }
                    )
                    ColorSliderRow(
                        label = "Green",
                        value = green,
                        colorTint = Color(0xFF22C55E),
                        onValueChange = {
                            green = it
                            hexInput = String.format("#%02X%02X%02X", (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
                        }
                    )
                    ColorSliderRow(
                        label = "Blue",
                        value = blue,
                        colorTint = Color(0xFF3B82F6),
                        onValueChange = {
                            blue = it
                            hexInput = String.format("#%02X%02X%02X", (red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
                        }
                    )
                    ColorSliderRow(
                        label = "Alpha",
                        value = alpha,
                        colorTint = MaterialTheme.colorScheme.primary,
                        onValueChange = { alpha = it }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Quick Swatch Palettes
                Text(
                    text = "Curated Swatches",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                curatedPalettes.forEach { (category, hexList) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hexList.forEach { hex ->
                                val color = parseHexColor(hex)
                                val isSelected = hex.equals(currentHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            red = color.red
                                            green = color.green
                                            blue = color.blue
                                            hexInput = hex
                                        }
                                        .testTag("swatch_$hex")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onInsertColor(currentRgba)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("insert_rgba_button")
                ) {
                    Text("Insert RGBA", fontSize = 11.uiSp)
                }
                Button(
                    onClick = {
                        onInsertColor(currentHex)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("insert_hex_button")
                ) {
                    Text("Insert Hex", fontSize = 11.uiSp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Float,
    colorTint: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFontFamily,
            modifier = Modifier.width(42.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f
        )
        Text(
            text = if (label == "Alpha") "${(value * 100).roundToInt()}%" else "${(value * 255).roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((parsed or 0xFF000000).toInt())
            8 -> Color(parsed.toInt())
            3 -> {
                val r = (clean[0].toString() + clean[0]).toInt(16)
                val g = (clean[1].toString() + clean[1]).toInt(16)
                val b = (clean[2].toString() + clean[2]).toInt(16)
                Color(r, g, b)
            }
            else -> Color(0xFF38BDF8)
        }
    } catch (_: Throwable) {
        Color(0xFF38BDF8)
    }
}
