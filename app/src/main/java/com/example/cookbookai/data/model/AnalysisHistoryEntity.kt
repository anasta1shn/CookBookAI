package com.example.cookbookai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(

    @PrimaryKey
    val id: String,

    val name: String,

    val calories: Int,

    val proteins: Float,

    val fats: Float,

    val carbs: Float,

    val imageUri: String? = null,

    val weight: Int = 0,

    val confidence: Float = 0f,

    val aiSummary: String = "",

    val topPredictions: String = "",

    val date: Long = System.currentTimeMillis()
) {

    fun toAnalysisResult(): AnalysisResult =
        AnalysisResult(
            id = id,
            name = name,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
            imageUri = imageUri,
            weight = weight,
            confidence = confidence,
            aiSummary = aiSummary,
            topPredictions = topPredictions
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            date = date
        )

    companion object {

        fun from(
            result: AnalysisResult,
            id: String
        ): AnalysisHistoryEntity =
            AnalysisHistoryEntity(
                id = id,
                name = result.name,
                calories = result.calories,
                proteins = result.proteins,
                fats = result.fats,
                carbs = result.carbs,
                imageUri = result.imageUri,
                weight = result.weight,
                confidence = result.confidence,
                aiSummary = result.aiSummary,
                topPredictions = result.topPredictions.joinToString("|"),
                date = result.date
            )
    }
}
