package com.example.cookbookai.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.cookbookai.network.model.TopPrediction
import com.example.cookbookai.network.model.TopPredictionDeserializer
import com.example.cookbookai.network.model.PredictionResponse
import com.example.cookbookai.network.model.PredictionResponseDeserializer
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var cachedBaseUrl: String? =
        null

    private var cachedApi: ApiService? =
        null

    val api: ApiService
        get() {

            val currentBaseUrl =
                ServerSettings.getServerUrl()

            val existingApi =
                cachedApi

            if (
                existingApi != null &&
                cachedBaseUrl == currentBaseUrl
            ) {
                return existingApi
            }

            val newApi =
                createApi(
                    currentBaseUrl
                )

            cachedBaseUrl =
                currentBaseUrl

            cachedApi =
                newApi

            return newApi
        }

    fun reset() {

        cachedBaseUrl =
            null

        cachedApi =
            null
    }

    private fun createApi(
        baseUrl: String
    ): ApiService {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(210, TimeUnit.SECONDS)
            .build()

        val gson =
            GsonBuilder()
                .registerTypeAdapter(
                    TopPrediction::class.java,
                    TopPredictionDeserializer()
                )
                .registerTypeAdapter(
                    PredictionResponse::class.java,
                    PredictionResponseDeserializer()
                )
                .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .build()
            .create(ApiService::class.java)
    }
}
