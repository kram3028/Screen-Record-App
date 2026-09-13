package com.example.data.model

enum class VideoResolution(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String
) {
    RES_720P("720p HD", 1280, 720, "1280 x 720 • Fast encoding"),
    RES_1080P("1080p FHD", 1920, 1080, "1920 x 1080 • Standard Full HD"),
    RES_2K("2K QHD", 2560, 1440, "2560 x 1440 • Ultra Sharp")
}

enum class VideoFormat(
    val extension: String,
    val label: String,
    val mimeType: String
) {
    MP4("mp4", "MP4 (MPEG-4)", "video/mp4"),
    MKV("mkv", "MKV (Matroska)", "video/x-matroska")
}

enum class AudioSourceMode(
    val label: String,
    val description: String
) {
    MIC_AND_SYSTEM("System Audio + Mic", "Simultaneously captures game/app sound and voice"),
    MIC_ONLY("Microphone", "Voice-overs, commentaries, external audio"),
    SYSTEM_ONLY("System Audio", "Internal device audio only (Android 10+)"),
    MUTE("No Audio (Mute)", "Silent screen video")
}

enum class RecordingState {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    SAVING
}

data class RecorderConfig(
    val resolution: VideoResolution = VideoResolution.RES_720P,
    val format: VideoFormat = VideoFormat.MP4,
    val fps: Int = 60,                     // 30, 60, 120
    val bitrateMbps: Int = 16,             // 4, 8, 12, 16, 24
    val audioSource: AudioSourceMode = AudioSourceMode.MIC_AND_SYSTEM,
    val audioBitrateKbps: Int = 192,       // 128, 192, 256, 320
    val audioSampleRate: Int = 44100,      // 44100, 48000
    val countdownSeconds: Int = 3,         // 0, 3, 5
    val showFloatingControls: Boolean = true,
    val recordOrientation: String = "Auto", // "Auto", "Portrait", "Landscape"
    val showTouches: Boolean = true,
    val stopOnScreenOff: Boolean = true
)

data class LiveRecordingMetrics(
    val elapsedMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val currentBitrateMbps: Double = 0.0,
    val fps: Int = 60,
    val droppedFrames: Int = 0,
    val audioLevelDb: Float = -12f
)
