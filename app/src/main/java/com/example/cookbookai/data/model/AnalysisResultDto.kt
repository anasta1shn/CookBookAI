package com.example.cookbookai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class AnalysisResultDto(

    @SerialName("id")
    val id: String? = null,

    @SerialName("name")
    val name: String = "",

    @SerialName("calories")
    val calories: Int = 0,

    @SerialName("proteins")
    val proteins: Float = 0f,

    @SerialName("fats")
    val fats: Float = 0f,

    @SerialName("carbs")
    val carbs: Float = 0f,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("weight")
    val weight: Int = 0,

    @SerialName("confidence")
    val confidence: Float = 0f,

    @SerialName("ai_summary")
    val aiSummary: String = "",

    @SerialName("top_predictions")
    val topPredictions: String = "",

    @SerialName("created_at")
    val createdAt: String? = null
) {

    fun toAnalysisResult(): AnalysisResult {

        val dateMillis =
            try {
                createdAt
                    ?.let {
                        OffsetDateTime
                            .parse(it)
                            .toInstant()
                            .toEpochMilli()
                    }
                    ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

        return AnalysisResult(
            id = id,
            name = name,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
            imageUri = imageUrl,
            weight = weight,
            confidence = confidence,
            aiSummary = aiSummary,
            topPredictions = topPredictions
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            date = dateMillis
        )
    }
}