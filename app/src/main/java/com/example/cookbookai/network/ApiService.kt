package com.example.cookbookai.network

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("predict")
    suspend fun predict(
        @Part file: MultipartBody.Part
    ): Response<JsonObject>
}
