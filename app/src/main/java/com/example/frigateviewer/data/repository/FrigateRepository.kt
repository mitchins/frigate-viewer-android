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
                        // Extract stream name from ffmpeg inputs (most reliable)
                        // Prefer "record" role (higher quality main streams) for libVLC which supports H.265
                        val streamName = cameraConfig.ffmpeg?.inputs
                            ?.firstOrNull { it.roles?.contains("record") == true }
                            ?.path
                            ?.substringAfterLast("/")
                            ?: cameraConfig.ffmpeg?.inputs
                                ?.firstOrNull { it.roles?.contains("detect") == true }
                                ?.path
                                ?.substringAfterLast("/")
                            ?: cameraConfig.live?.streamName // Fallback to live.stream_name
                            ?: id // Final fallback to camera ID

                        Camera(
                            id = id,
                            name = cameraConfig.name ?: id.replaceFirstChar { it.uppercase() },
                            streamName = streamName,
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
