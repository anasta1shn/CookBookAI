package com.example.cookbookai.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookbookai.domain.DecisionEngine
import com.example.cookbookai.domain.DecisionResult
import com.example.cookbookai.network.RetrofitClient
import com.example.cookbookai.network.model.PredictionResponse
import com.example.cookbookai.network.model.PredictionResponseDeserializer
import com.example.cookbookai.network.model.TopPrediction
import com.example.cookbookai.network.model.TopPredictionDeserializer
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class AiViewModel : ViewModel() {

    private val decisionEngine = DecisionEngine()

    private val predictionResponseGson =
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

    fun uploadImage(bitmap: Bitmap) {

        viewModelScope.launch {

            try {

                // 📸 bitmap → bytes
                val stream = ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    stream
                )

                val requestBody = stream.toByteArray()
                    .toRequestBody(
                        "image/jpeg".toMediaTypeOrNull()
                    )

                val part = MultipartBody.Part.createFormData(
                    "file",
                    "image.jpg",
                    requestBody
                )

                // 🚀 запрос к серверу
                val response =
                    RetrofitClient.api.predict(part)

                if (response.isSuccessful) {

                    val detections =
                        response.body()
                            ?.let { bodyJson ->
                                predictionResponseGson.fromJson(
                                    bodyJson,
                                    PredictionResponse::class.java
                                )
                            }
                            ?.detections
                            ?: emptyList()

                    // 🧠 Decision Engine
                    val result =
                        decisionEngine.analyze(detections)

                    handleResult(result)

                } else {

                    Log.e(
                        "AI",
                        "Server error: ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "AI",
                    "Error: ${e.message}"
                )
            }
        }
    }

    private fun handleResult(
        result: DecisionResult
    ) {

        Log.d(
            "AI",
            "Dish: ${result.dishName}"
        )

        Log.d(
            "AI",
            "Confidence: ${result.confidence}"
        )

        Log.d(
            "AI",
            "Features: ${result.features}"
        )
    }
}
