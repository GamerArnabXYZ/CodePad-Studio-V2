package com.arnix.codepad.studio.model

import com.arnix.codepad.studio.viewmodel.AutoDurationTarget

/**
 * Built-in royalty-free procedural ambient & beat tracks for developer reels and ASMR shorts.
 */
data class BgmPresetTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val genreTag: String,
    val estimatedDurationSeconds: Int = 180
)

val BGM_PRESET_TRACKS = listOf(
    BgmPresetTrack(
        id = "preset_lofi",
        title = "Lo-Fi Sunset Coding",
        subtitle = "Warm chill-hop chords, subtle sub-bass & soft vinyl texture",
        genreTag = "Lo-Fi Chill",
        estimatedDurationSeconds = 180
    ),
    BgmPresetTrack(
        id = "preset_synthwave",
        title = "Cyberpunk Synthwave",
        subtitle = "80s retro wave arpeggio, neon bassline & driving rhythm",
        genreTag = "Synthwave",
        estimatedDurationSeconds = 180
    ),
    BgmPresetTrack(
        id = "preset_ambient",
        title = "Deep Focus Ambient",
        subtitle = "Calming binaural drone & acoustic pad for flow state",
        genreTag = "Ambient ASMR",
        estimatedDurationSeconds = 240
    ),
    BgmPresetTrack(
        id = "preset_tech",
        title = "Upbeat Tech Reel",
        subtitle = "Punchy hi-hat groove & modern pulse for viral reels",
        genreTag = "Tech Beat",
        estimatedDurationSeconds = 180
    )
)

/**
 * Configuration for Custom Background Music & Audio Timing Customization.
 * Allows creators to add any long audio track and customize the timing window
 * (e.g., 15s, 20s, 30s, 45s, 60s) with precise start offset, volume, and fade-out.
 */
data class BgmConfig(
    val isEnabled: Boolean = false,
    val selectedTrackId: String = "preset_lofi",
    val trackTitle: String = "Lo-Fi Sunset Coding",
    val customAudioPath: String? = null,
    val customAudioFileName: String? = null,
    val customAudioDurationSeconds: Int = 180,
    val startOffsetSeconds: Int = 0,
    val clipDurationSeconds: Int = 30,
    val autoSyncWithTargetDuration: Boolean = true,
    val volume: Float = 0.40f,
    val isLoopEnabled: Boolean = true,
    val isFadeOutEnabled: Boolean = true
) {
    val isCustomFile: Boolean get() = selectedTrackId == "custom_file" && customAudioPath != null

    val totalDurationSeconds: Int
        get() = if (isCustomFile) customAudioDurationSeconds.coerceAtLeast(10) else 180

    fun getEffectiveDuration(target: AutoDurationTarget): Int {
        return if (autoSyncWithTargetDuration && target != AutoDurationTarget.MANUAL && target.seconds > 0) {
            target.seconds
        } else if (clipDurationSeconds > 0) {
            clipDurationSeconds
        } else {
            30
        }
    }
}
