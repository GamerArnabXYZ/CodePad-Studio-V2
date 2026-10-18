package com.arnix.codepad.studio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Clean Material 3 Light Theme Palette (Human-friendly, crisp & modern)
val LightPrimary = Color(0xFF2563EB)          // Royal Blue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDBEAFE)
val LightOnPrimaryContainer = Color(0xFF1E3A8A)

val LightSecondary = Color(0xFF475569)        // Slate
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE2E8F0)
val LightOnSecondaryContainer = Color(0xFF0F172A)

val LightTertiary = Color(0xFF7C3AED)         // Soft Violet
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFEDE9FE)
val LightOnTertiaryContainer = Color(0xFF4C1D95)

val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

// Clean Material 3 Dark Theme Palette (Comfortable Slate Dark, like modern Android Studio / VS Code)
val DarkPrimary = Color(0xFF60A5FA)           // Soft Sky Blue
val DarkOnPrimary = Color(0xFF0F172A)
val DarkPrimaryContainer = Color(0xFF1E3A8A)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)

val DarkSecondary = Color(0xFF94A3B8)
val DarkOnSecondary = Color(0xFF0F172A)
val DarkSecondaryContainer = Color(0xFF1E293B)
val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

val DarkTertiary = Color(0xFFA78BFA)
val DarkOnTertiary = Color(0xFF1E1B4B)
val DarkTertiaryContainer = Color(0xFF3B0764)
val DarkOnTertiaryContainer = Color(0xFFEDE9FE)

val DarkBackground = Color(0xFF0F172A)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOnSurfaceVariant = Color(0xFFCBD5E1)
val DarkOutline = Color(0xFF475569)
val DarkOutlineVariant = Color(0xFF334155)

// Semantic Status Colors (Saved Green, Unsaved Orange, Warnings & Errors)
val StatusSavedGreenLight = Color(0xFF15803D)
val StatusSavedGreenBgLight = Color(0xFFDCFCE7)
val StatusSavedGreenDark = Color(0xFF4ADE80)
val StatusSavedGreenBgDark = Color(0xFF14532D)

val StatusUnsavedOrangeLight = Color(0xFFB45309)
val StatusUnsavedOrangeBgLight = Color(0xFFFEF3C7)
val StatusUnsavedOrangeDark = Color(0xFFFBBF24)
val StatusUnsavedOrangeBgDark = Color(0xFF78350F)

val ConsoleWarnAmber = Color(0xFFD97706)
val ConsoleErrorRed = Color(0xFFDC2626)
val ConsoleErrorRedDark = Color(0xFFF87171)
val ConsoleInfoBlue = Color(0xFF0284C7)

@Immutable
data class EditorColorPalette(
    val editorBackground: Color,
    val gutterBackground: Color,
    val gutterText: Color,
    val gutterActiveText: Color,
    val activeLineBackground: Color,
    val divider: Color,
    val plainText: Color,
    val keyword: Color,
    val tag: Color,
    val attribute: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val function: Color,
    val cursor: Color
)

val LightEditorPalette = EditorColorPalette(
    editorBackground = Color(0xFFFFFFFF),
    gutterBackground = Color(0xFFF8FAFC),
    gutterText = Color(0xFF94A3B8),
    gutterActiveText = Color(0xFF2563EB),
    activeLineBackground = Color(0xFFEFF6FF),
    divider = Color(0xFFE2E8F0),
    plainText = Color(0xFF1E293B),
    keyword = Color(0xFF7C3AED),      // Purple for keywords
    tag = Color(0xFF0369A1),          // Deep Blue for HTML tags
    attribute = Color(0xFFB45309),    // Warm Amber for attributes/properties
    string = Color(0xFF15803D),       // Forest Green for strings
    comment = Color(0xFF94A3B8),      // Muted Slate for comments
    number = Color(0xFFBE185D),       // Rose for numbers & units
    function = Color(0xFF1D4ED8),     // Royal Blue for functions/selectors
    cursor = Color(0xFF2563EB)
)

val DarkEditorPalette = EditorColorPalette(
    editorBackground = Color(0xFF0F172A),
    gutterBackground = Color(0xFF162032),
    gutterText = Color(0xFF64748B),
    gutterActiveText = Color(0xFF60A5FA),
    activeLineBackground = Color(0xFF1E293B),
    divider = Color(0xFF334155),
    plainText = Color(0xFFE2E8F0),
    keyword = Color(0xFFC084FC),      // Soft Lavender for keywords
    tag = Color(0xFF38BDF8),          // Sky Blue for HTML tags
    attribute = Color(0xFFFBBF24),    // Warm Gold for attributes/properties
    string = Color(0xFF4ADE80),       // Soft Mint Green for strings
    comment = Color(0xFF64748B),      // Muted Slate for comments
    number = Color(0xFFF472B6),       // Soft Pink for numbers & units
    function = Color(0xFF93C5FD),     // Light Blue for functions/selectors
    cursor = Color(0xFF60A5FA)
)

val LocalEditorPalette = staticCompositionLocalOf { LightEditorPalette }
