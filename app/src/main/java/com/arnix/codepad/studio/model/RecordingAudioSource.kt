package com.arnix.codepad.studio.model

/**
 * Audio capture modes for CodePad Studio screen recordings.
 */
enum class RecordingAudioSource(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortLabel: String
) {
    SYSTEM_ONLY(
        id = "system_only",
        title = "System Sound Only",
        subtitle = "App typing audio & internal system sounds (no mic noise)",
        shortLabel = "System Only"
    ),
    MIC_ONLY(
        id = "mic_only",
        title = "Microphone Only",
        subtitle = "Voiceover commentary via device mic (typing clicks muted)",
        shortLabel = "Mic Only"
    ),
    SYSTEM_AND_MIC(
        id = "system_and_mic",
        title = "System Sound + Microphone",
        subtitle = "Simultaneous mechanical typing clicks & voice commentary",
        shortLabel = "Sys + Mic"
    ),
    SILENT(
        id = "silent",
        title = "Silent (No Audio)",
        subtitle = "Zero audio, pure visual video for editing reels/shorts",
        shortLabel = "Silent"
    );

    companion object {
        fun fromId(id: String?): RecordingAudioSource {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM_ONLY
        }
    }
}
