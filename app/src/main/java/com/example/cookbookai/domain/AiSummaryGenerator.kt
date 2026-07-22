package com.example.cookbookai.domain

import com.example.cookbookai.data.model.AnalysisResult

class AiSummaryGenerator {

    fun generate(
        result: AnalysisResult
    ): String {

        val summaries =
            mutableListOf<String>()

        // =====================================
        // PROTEIN
        // =====================================

        if (result.proteins >= 20f) {

            summaries.add(
                "Высокое содержание белка"
            )
        }

        // =====================================
        // FATS
        // =====================================

        if (result.fats >= 20f) {

            summaries.add(
                "Высокое содержание жиров"
            )
        }

        // =====================================
        // CARBS
        // =====================================

        if (result.carbs <= 15f) {

            summaries.add(
                "Низкоуглеводное блюдо"
            )
        }

        // =====================================
        // CALORIES
        // =====================================

        if (result.calories <= 350) {

            summaries.add(
                "Подходит для похудения"
            )
        }

        if (result.calories >= 700) {

            summaries.add(
                "Высокая калорийность"
            )
        }

        // =====================================
        // BALANCE
        // =====================================

        if (
            result.proteins > 10 &&
            result.fats > 10 &&
            result.carbs > 10
        ) {

            summaries.add(
                "Сбалансированный состав"
            )
        }

        // =====================================
        // DEFAULT
        // =====================================

        if (summaries.isEmpty()) {

            summaries.add(
                "Обычное блюдо"
            )
        }

        return summaries.joinToString(
            separator = " • "
        )
    }
}