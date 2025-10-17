package com.example.frigateviewer.data.repository

import com.example.frigateviewer.data.api.RetrofitInstance
import com.example.frigateviewer.data.model.Camera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrigateRepository {

    private val api = RetrofitInstance.api

    /**
     * Fetch the list of cameras from Frigate
     * Returns a list of Camera objects or throws an exception
     */
    suspend fun getCameras(): Result<List<Camera>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getConfig()

            if (response.isSuccessful) {
                val config = response.body()
                if (config != null) {
                    val cameras = config.cameras.map { (id, cameraConfig) ->
                        // Extract main stream name from ffmpeg inputs with "record" role (high quality)
                        val mainStreamName = cameraConfig.ffmpeg?.inputs
                            ?.firstOrNull { it.roles?.contains("record") == true }
                            ?.path
                            ?.substringAfterLast("/")
                            ?: cameraConfig.live?.streamName // Fallback to live.stream_name
                            ?: id // Final fallback to camera ID

                        // Extract substream name from ffmpeg inputs with "detect" role (lower quality)
                        val subStreamName = cameraConfig.ffmpeg?.inputs
                            ?.firstOrNull { it.roles?.contains("detect") == true }
                            ?.path
                            ?.substringAfterLast("/")

                        val aspect: Float? = if (cameraConfig.detect?.width != null && cameraConfig.detect.height != null &&
                            cameraConfig.detect.width!! > 0 && cameraConfig.detect.height!! > 0) {
                            cameraConfig.detect.width.toFloat() / cameraConfig.detect.height.toFloat()
                        } else null

                        Camera(
                            id = id,
                            name = cameraConfig.name ?: id.replaceFirstChar { it.uppercase() },
                            streamName = mainStreamName,
                            subStreamName = subStreamName,
                            aspectRatio = aspect,
                            enabled = cameraConfig.enabled
                        )
                    }.filter { it.enabled } // Only return enabled cameras

                    Result.success(cameras)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the base URL of the Frigate server
     */
    fun getFrigateHost(): String {
        return RetrofitInstance.getBaseUrl()
    }

    /**
     * Update the Frigate server URL
     */
    fun updateFrigateHost(newHost: String) {
        RetrofitInstance.updateBaseUrl(newHost)
    }
}
