package com.example.data.model

/**
 * Manages the locking rules for high-performance Pro recording features:
 * 1. 1080p FHD
 * 2. 2K QHD
 * 3. 120 FPS
 * 4. Video Bitrate 24 Mbps
 *
 * Users unlock these features per recording session by watching a rewarded ad.
 */
object ProFeatureManager {

    /**
     * Checks if a resolution is a locked Pro feature
     */
    fun isProResolution(resolution: VideoResolution): Boolean {
        return resolution == VideoResolution.RES_1080P || resolution == VideoResolution.RES_2K
    }

    /**
     * Checks if a frame rate is a locked Pro feature
     */
    fun isProFps(fps: Int): Boolean {
        return fps >= 120
    }

    /**
     * Checks if a video bitrate is a locked Pro feature
     */
    fun isProBitrate(bitrateMbps: Int): Boolean {
        return bitrateMbps >= 24
    }

    /**
     * Checks if a RecorderConfig utilizes any locked Pro features
     */
    fun isProConfig(config: RecorderConfig): Boolean {
        return isProResolution(config.resolution) || isProFps(config.fps) || isProBitrate(config.bitrateMbps)
    }

    /**
     * Returns a human-readable list of Pro features currently selected in the config
     */
    fun getProFeaturesList(config: RecorderConfig): List<String> {
        val features = mutableListOf<String>()
        if (config.resolution == VideoResolution.RES_1080P) {
            features.add("1080p FHD Resolution")
        } else if (config.resolution == VideoResolution.RES_2K) {
            features.add("2K QHD Ultra Resolution")
        }
        if (isProFps(config.fps)) {
            features.add("${config.fps} FPS Ultra-Smooth Frame Rate")
        }
        if (isProBitrate(config.bitrateMbps)) {
            features.add("${config.bitrateMbps} Mbps High-Fidelity Bitrate")
        }
        return features
    }

    /**
     * Downgrades any Pro features in the configuration to standard free options:
     * - Resolution -> 720p HD
     * - FPS -> 60 FPS
     * - Bitrate -> 16 Mbps
     */
    fun downgradeToStandard(config: RecorderConfig): RecorderConfig {
        return config.copy(
            resolution = if (isProResolution(config.resolution)) VideoResolution.RES_720P else config.resolution,
            fps = if (isProFps(config.fps)) 60 else config.fps,
            bitrateMbps = if (isProBitrate(config.bitrateMbps)) 16 else config.bitrateMbps
        )
    }
}
