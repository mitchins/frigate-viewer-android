package com.example.frigateviewer.data.model

/**
 * Simplified camera model for the app
 */
data class Camera(
    val id: String,
    val name: String,
    val streamName: String, // go2rtc stream name (may differ from camera ID)
    val enabled: Boolean = true
) {
    /**
     * Get the RTSP stream URL for this camera via go2rtc
     * Format: rtsp://<frigate-ip>:8554/<stream-name>
     */
    fun getRtspUrl(frigateHost: String): String {
        // Extract host without protocol
        val host = frigateHost.replace("http://", "").replace("https://", "")
            .substringBefore(":")
        return "rtsp://$host:8554/$streamName"
    }

    /**
     * Get the HLS stream URL for this camera
     */
    fun getHlsUrl(frigateHost: String): String {
        return "$frigateHost/api/go2rtc/streams/$id"
    }

    /**
     * Get the MJPEG stream URL for this camera (fallback)
     */
    fun getMjpegUrl(frigateHost: String): String {
        return "$frigateHost/api/mjpeg/$id"
    }
}
