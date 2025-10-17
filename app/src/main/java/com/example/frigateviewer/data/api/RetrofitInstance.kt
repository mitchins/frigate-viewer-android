package com.example.frigateviewer.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private var baseUrl: String = "http://192.168.1.15:5000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: FrigateApiService by lazy {
        retrofit.create(FrigateApiService::class.java)
    }

    /**
     * Update the base URL for the Frigate server
     * Call this before making any API requests if the URL changes
     */
    fun updateBaseUrl(newBaseUrl: String) {
        var url = newBaseUrl.trim()
        if (!url.endsWith("/")) {
            url += "/"
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        baseUrl = url
    }

    fun getBaseUrl(): String = baseUrl.removeSuffix("/")
}
