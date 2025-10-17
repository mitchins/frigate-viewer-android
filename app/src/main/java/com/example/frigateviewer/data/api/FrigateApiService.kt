package com.example.frigateviewer.data.api

import com.example.frigateviewer.data.model.FrigateConfig
import retrofit2.Response
import retrofit2.http.GET

interface FrigateApiService {

    @GET("api/config")
    suspend fun getConfig(): Response<FrigateConfig>

    // Optional: Add more endpoints as needed
    // @GET("api/stats")
    // suspend fun getStats(): Response<FrigateStats>

    // @GET("api/go2rtc/streams/{camera}")
    // suspend fun getStreamInfo(@Path("camera") camera: String): Response<StreamInfo>
}
