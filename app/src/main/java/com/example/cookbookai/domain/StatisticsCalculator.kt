package com.example.cookbookai.domain
import com.example.cookbookai.data.model.AnalysisResult
class StatisticsCalculator {
    fun calculate(
        history: List<AnalysisResult>
    ): StatisticsResult {

        if (history.isEmpty()) {

            return StatisticsResult(
                totalCalories = 0,
                averageCalories = 0,
                totalProteins = 0f,
                totalFats = 0f,
                totalCarbs = 0f,
                analysesCount = 0
            )
        }

        val totalCalories = history.sumOf { it.calories }

        val averageCalories = totalCalories / history.size

        val totalProteins = history.sumOf {
                it.proteins.toDouble()
            }.toFloat()

        val totalFats = history.sumOf {
                it.fats.toDouble()
            }.toFloat()

        val totalCarbs = history.sumOf {
                it.carbs.toDouble()
            }.toFloat()

        return StatisticsResult(

            totalCalories = totalCalories,
            averageCalories = averageCalories,
            totalProteins = totalProteins,
            totalFats = totalFats,
            totalCarbs = totalCarbs,
            analysesCount = history.size
        )
    }
}