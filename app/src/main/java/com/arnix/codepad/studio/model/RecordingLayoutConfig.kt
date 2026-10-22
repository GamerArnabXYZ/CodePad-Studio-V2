package com.arnix.codepad.studio.model

/**
 * Built-in Recording Layouts for Clean Screen Recording Mode and Shorts/Reels creation:
 * 1. EDITOR_ONLY: Pure full-screen code editor, no preview split.
 * 2. EDITOR_TOP_PREVIEW_BOTTOM: Code editor on top, live web preview on bottom.
 * 3. PREVIEW_TOP_EDITOR_BOTTOM: Live web preview on top, code editor on bottom.
 * 4. EDITOR_THEN_FULL_PREVIEW_AT_END: Full-screen code during typing, then auto-expands
 *    live preview to full-screen at 100% completion for the grand reveal!
 */
enum class RecordingLayoutMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val badge: String
) {
    EDITOR_ONLY(
        id = "editor_only",
        title = "Editor Only (Full Code)",
        shortLabel = "Editor Only",
        subtitle = "Full-screen code typing without preview split. Pure ASMR & clean tutorials.",
        badge = "Full Code"
    ),
    EDITOR_TOP_PREVIEW_BOTTOM(
        id = "editor_top_preview_bottom",
        title = "Editor Top • Preview Bottom",
        shortLabel = "Code Top",
        subtitle = "Classic split: Code typed on top, live DOM result rendered below.",
        badge = "Split Top/Bottom"
    ),
    PREVIEW_TOP_EDITOR_BOTTOM(
        id = "preview_top_editor_bottom",
        title = "Preview Top • Editor Bottom",
        shortLabel = "Preview Top",
        subtitle = "Result on top: Viewers see live interactive UI while code types underneath.",
        badge = "Preview First"
    ),
    EDITOR_THEN_FULL_PREVIEW_AT_END(
        id = "editor_then_full_preview_at_end",
        title = "Editor ➔ Fullscreen Result at End",
        shortLabel = "End Reveal",
        subtitle = "Types code in full editor, then auto-expands live preview to 100% full screen!",
        badge = "Viral Reveal"
    );

    companion object {
        fun fromId(id: String?): RecordingLayoutMode =
            entries.firstOrNull { it.id == id } ?: EDITOR_TOP_PREVIEW_BOTTOM
    }
}

/**
 * Split Ratio Presets for dual-pane recording modes.
 */
enum class SplitRatioPreset(
    val id: String,
    val label: String,
    val editorWeight: Float,
    val previewWeight: Float
) {
    RATIO_50_50("ratio_50_50", "50 / 50 Balanced", 0.50f, 0.50f),
    RATIO_60_40("ratio_60_40", "60 / 40 Code Focus", 0.60f, 0.40f),
    RATIO_70_30("ratio_70_30", "70 / 30 Code Dominant", 0.70f, 0.30f),
    RATIO_40_60("ratio_40_60", "40 / 60 Output Focus", 0.40f, 0.60f),
    RATIO_30_70("ratio_30_70", "30 / 70 Output Dominant", 0.30f, 0.70f);

    companion object {
        fun fromEditorWeight(weight: Float): SplitRatioPreset? =
            entries.firstOrNull { kotlin.math.abs(it.editorWeight - weight) < 0.03f }
    }
}

/**
 * Aesthetic visual styles for the divider between editor and live preview.
 */
enum class RecDividerStyle(val id: String, val label: String, val heightDp: Int) {
    NEON_GLOW("neon_glow", "Neon Cyber Glow", 3),
    MINIMAL_BAR("minimal_bar", "Minimalist Hairline", 1),
    PILL_HANDLE("pill_handle", "Draggable Pill Handle", 8),
    SEAMLESS_FLAT("seamless_flat", "Seamless Borderless", 0)
}

/**
 * Animation style when preview expands to full screen in EDITOR_THEN_FULL_PREVIEW_AT_END.
 */
enum class RecRevealTransition(val id: String, val label: String) {
    EXPAND_AND_SCALE("Cinematic Expand & Scale", "Spring scale with fluid slide up"),
    SMOOTH_CROSSFADE("Smooth Dissolve / Crossfade", "Gentle 350ms opacity dissolve"),
    SLIDE_UP("Dynamic Slide Up", "Slide from bottom over code window")
}

/**
 * Comprehensive Ultra-Customizable Recording Layout Configuration.
 */
data class RecordingLayoutConfig(
    val layoutMode: RecordingLayoutMode = RecordingLayoutMode.EDITOR_TOP_PREVIEW_BOTTOM,
    val editorWeight: Float = 0.52f, // 0.20f .. 0.80f
    val dividerStyle: RecDividerStyle = RecDividerStyle.MINIMAL_BAR,
    val cornerRadiusDp: Int = 0, // 0dp for seamless 9:16 video, or 8, 12, 16dp
    val stagePaddingDp: Int = 0, // 0dp for edge-to-edge full bleed, 4dp, 8dp
    val showDividerAccentGlow: Boolean = true,
    val revealTransition: RecRevealTransition = RecRevealTransition.EXPAND_AND_SCALE,
    val revealDelayMs: Long = 600L, // Delay after code ends before preview expands
    val revealHoldSeconds: Int = 3, // How long preview holds in full screen
    val showSplitHandleBar: Boolean = true,
    val autoScrollToCaret: Boolean = true,
    val previewShowcaseSeconds: Int = 0, // 0 = Auto-Balanced with Target Duration (e.g. 4.2s for 15s, 8s for 30s), or 2..15s fixed
    val showPreviewShowcaseBadge: Boolean = true // Visual badge showing remaining preview showcase time
) {
    val previewWeight: Float get() = (1.0f - editorWeight).coerceIn(0.15f, 0.85f)

    /**
     * Calculates the exact post-typing preview showcase duration in milliseconds.
     * When previewShowcaseSeconds == 0 (Auto-Balanced), it allocates an ideal window
     * of the selected target duration (or 4.0s for manual) so code typing completes early
     * and viewers enjoy the interactive output.
     */
    fun getEffectiveShowcaseDurationMs(target: com.arnix.codepad.studio.viewmodel.AutoDurationTarget): Long {
        if (previewShowcaseSeconds > 0) {
            return previewShowcaseSeconds * 1000L
        }
        return when (target) {
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.SEC_15 -> 4200L
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.SEC_20 -> 5500L
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.SEC_30 -> 8000L
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.SEC_45 -> 12000L
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.SEC_60 -> 15000L
            com.arnix.codepad.studio.viewmodel.AutoDurationTarget.MANUAL -> 4000L
        }
    }

    fun getShowcaseLabel(target: com.arnix.codepad.studio.viewmodel.AutoDurationTarget): String {
        return if (previewShowcaseSeconds == 0) {
            "Auto (${(getEffectiveShowcaseDurationMs(target) / 1000f).toInt()}s for ${target.shortLabel})"
        } else {
            "${previewShowcaseSeconds}s Fixed"
        }
    }
}

