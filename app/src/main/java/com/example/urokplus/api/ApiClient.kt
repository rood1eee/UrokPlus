package com.example.urokplus.api

import com.example.urokplus.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Универсальный обход страниц-заглушек туннелей (ngrok, tuna, localtunnel и др.)
    private val tunnelInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("Bypass-Tunnel-Reminder", "true")
            .build()
        chain.proceed(request)
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(tunnelInterceptor)
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().removeSuffix("/") + "/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: UrokPlusApi = retrofit.create(UrokPlusApi::class.java)
}
