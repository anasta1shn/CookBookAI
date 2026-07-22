package com.example.cookbookai.data.model

data class AnalysisResult(

    val id: String? = null,

    val name: String,

    val calories: Int,

    val proteins: Float,

    val fats: Float,

    val carbs: Float,

    val imageUri: String? = null,

    val weight: Int = 0,

    val confidence: Float = 0f,

    val aiSummary: String = "",

    val topPredictions: List<String> = emptyList(),

    val date: Long =
        System.currentTimeMillis()
)