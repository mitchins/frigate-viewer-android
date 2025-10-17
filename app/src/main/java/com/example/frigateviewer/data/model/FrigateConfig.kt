package com.example.frigateviewer.data.model

import com.google.gson.annotations.SerializedName

data class FrigateConfig(
    @SerializedName("cameras")
    val cameras: Map<String, CameraConfig>
)

data class CameraConfig(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("ffmpeg")
    val ffmpeg: FfmpegConfig? = null,
    @SerializedName("live")
    val live: LiveConfig? = null,
    @SerializedName("detect")
    val detect: DetectConfig? = null
)

data class LiveConfig(
    @SerializedName("stream_name")
    val streamName: String
)

data class FfmpegConfig(
    @SerializedName("inputs")
    val inputs: List<InputConfig>? = null
)

data class InputConfig(
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("roles")
    val roles: List<String>? = null
)

data class DetectConfig(
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)
