package com.example.cookbookai.domain

data class StatisticsResult(

    val totalCalories: Int,

    val averageCalories: Int,

    val totalProteins: Float,

    val totalFats: Float,

    val totalCarbs: Float,

    val analysesCount: Int
)