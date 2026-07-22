package com.example.cookbookai.domain

data class FoodFeedback(
    val imageHash: String,
    val detectedClass: String,
    val correctedClass: String,
    val confidence: Float
)