package com.example.frigateviewer.data.model

/**
 * Simplified camera model for the app
 */
data class Camera(
    val id: String,
    val name: String,
    val streamName: String, // main/record quality stream (may differ from camera ID)
    val subStreamName: String?, // detect/sub quality stream (may be null)
    val aspectRatio: Float? = null, // width/height if known
    val enabled: Boolean = true
) {
    /**
     * Get the RTSP stream URL for this camera via go2rtc
     * Format: rtsp://<frigate-ip>:8554/<stream-name>
     * @param frigateHost The Frigate host URL
     * @param useSubStream If true, use the substream (detect role) for lower quality/bandwidth
     */
    fun getRtspUrl(frigateHost: String, useSubStream: Boolean = false): String {
        // Extract host without protocol
        val host = frigateHost.replace("http://", "").replace("https://", "")
            .substringBefore(":")
        // Use substream if requested and available, otherwise use main stream
        val stream = if (useSubStream && subStreamName != null) subStreamName else streamName
        return "rtsp://$host:8554/$stream"
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
