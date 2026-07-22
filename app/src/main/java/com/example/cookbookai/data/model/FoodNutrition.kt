package com.example.cookbookai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FoodNutrition(

    val name: String,

    val category: String,

    val calories: Float,

    val proteins: Float,

    val fats: Float,

    val carbs: Float
)