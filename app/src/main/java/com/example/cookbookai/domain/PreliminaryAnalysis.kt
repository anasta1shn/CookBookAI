package com.example.cookbookai.domain

data class PreliminaryAnalysis(
    val foodName: String,
    val estimatedWeight: Int,
    val confidence: Float,
    val topPredictions: List<String> = emptyList()
)
