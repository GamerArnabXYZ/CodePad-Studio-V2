package com.arnix.codepad.studio.viewmodel

enum class CreatorAccentPalette(
    val index: Int,
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val successHex: Long
) {
    CYBER_CYAN_PINK(0, "Cyber Cyan & Pink", 0xFF38BDF8, 0xFFEC4899, 0xFF22C55E),
    CRIMSON_EMERALD(1, "YouTube Crimson", 0xFFEF4444, 0xFFF43F5E, 0xFF22C55E),
    ELECTRIC_INDIGO(2, "Electric Indigo", 0xFF6366F1, 0xFFA855F7, 0xFF10B981),
    EMERALD_MATRIX(3, "Emerald Matrix", 0xFF10B981, 0xFF06B6D4, 0xFF34D399),
    SUNSET_GOLD(4, "Sunset Gold", 0xFFF59E0B, 0xFFEC4899, 0xFF22C55E),
    MONOCHROME_PRO(5, "Clean White Mono", 0xFFF8FAFC, 0xFF94A3B8, 0xFF38BDF8);

    companion object {
        fun fromIndex(index: Int): CreatorAccentPalette =
            entries.firstOrNull { it.index == index } ?: CYBER_CYAN_PINK
    }
}

enum class OverlaySizeScale(
    val index: Int,
    val label: String,
    val scaleFactor: Float
) {
    COMPACT(0, "Compact", 0.88f),
    STANDARD(1, "Standard", 1.0f),
    LARGE(2, "Bold Large", 1.15f);

    companion object {
        fun fromIndex(index: Int): OverlaySizeScale =
            entries.firstOrNull { it.index == index } ?: STANDARD
    }
}

enum class ViralHookPosition(val label: String) {
    TOP_STAGE("Above Code Window"),
    CODE_TOP_OVERLAY("Code Top Overlay"),
    CODE_BOTTOM_OVERLAY("Code Bottom Overlay"),
    BOTTOM_STAGE("Below Code Window")
}

enum class ViralHookAlignment(val label: String) {
    START("Left Align"),
    CENTER("Centered"),
    SPACE_BETWEEN("Badge Left • Title Right")
}

data class ViralHookQuickPreset(
    val label: String,
    val badge: String,
    val title: String
)

val VIRAL_HOOK_QUICK_PRESETS = listOf(
    ViralHookQuickPreset(
        label = "Wait for Result 🤯",
        badge = "🔥 HTML • CSS • JS",
        title = "Wait for the final UI result! 🤯"
    ),
    ViralHookQuickPreset(
        label = "60s UI Challenge ⚡",
        badge = "⚡ 60s UI CHALLENGE",
        title = "Can we build this in 60 seconds?"
    ),
    ViralHookQuickPreset(
        label = "Frontend Magic 💻",
        badge = "💻 FRONTEND MAGIC",
        title = "Satisfying Clean Code & Live Output"
    ),
    ViralHookQuickPreset(
        label = "100 Days Code 🚀",
        badge = "🚀 DAY 1 / 100",
        title = "100 Days of Web Dev Coding Shorts"
    ),
    ViralHookQuickPreset(
        label = "No Frameworks ✨",
        badge = "✨ PURE VANILLA",
        title = "Zero Frameworks! Pure HTML, CSS & JS"
    )
)

enum class EndCtaPosition(val label: String) {
    CODE_BOTTOM_OVERLAY("Code Bottom Overlay"),
    CODE_TOP_OVERLAY("Code Top Overlay"),
    CODE_CENTER_OVERLAY("Center Screen Popup"),
    SIDE_RIGHT_OVERLAY("Right Side Overlay"),
    STAGE_BOTTOM_BAR("Below Code Window")
}

enum class CtaDisplayTiming(val label: String) {
    AT_END_100("At 100% End (Outro)"),
    ALWAYS_VISIBLE("Always Visible Bar"),
    LAST_20_PERCENT("Last 20% (From 80%)"),
    FROM_50_PERCENT("Halfway (From 50%)")
}

data class CtaPlatformQuickPreset(
    val label: String,
    val likeLabel: String,
    val commentLabel: String,
    val subscribeLabel: String,
    val subscribedLabel: String,
    val message: String
)

val CTA_PLATFORM_QUICK_PRESETS = listOf(
    CtaPlatformQuickPreset(
        label = "YouTube Shorts",
        likeLabel = "Like",
        commentLabel = "Comment",
        subscribeLabel = "SUBSCRIBE",
        subscribedLabel = "SUBSCRIBED ✓",
        message = "Like, Comment & Subscribe for more!"
    ),
    CtaPlatformQuickPreset(
        label = "Reels / TikTok",
        likeLabel = "24.8K",
        commentLabel = "642",
        subscribeLabel = "FOLLOW",
        subscribedLabel = "FOLLOWING ✓",
        message = "Follow for daily Web Dev Reels & source code!"
    ),
    CtaPlatformQuickPreset(
        label = "Rate 1-10 Hook",
        likeLabel = "Rate 1-10",
        commentLabel = "Drop 🔥",
        subscribeLabel = "SUBSCRIBE",
        subscribedLabel = "SUBSCRIBED ✓",
        message = "Rate this UI 1-10 in the comments!"
    ),
    CtaPlatformQuickPreset(
        label = "Minimal Icons",
        likeLabel = "❤️",
        commentLabel = "💬",
        subscribeLabel = "Sub 🔔",
        subscribedLabel = "Subbed ✓",
        message = ""
    )
)
