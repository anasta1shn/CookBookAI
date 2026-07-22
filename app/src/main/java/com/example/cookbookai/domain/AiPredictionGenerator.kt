package com.example.cookbookai.domain

class AiPredictionGenerator {

    fun generate(
        foodName: String,
        confidence: Float
    ): List<String> {

        val normalized =
            foodName.lowercase().trim()

        val alternatives =
            when {

                normalized.contains("борщ") ->
                    listOf("Щи", "Солянка")

                normalized.contains("щи") ->
                    listOf("Борщ", "Солянка")

                normalized.contains("омлет") ->
                    listOf("Яичница", "Яйцо")

                normalized.contains("яичниц") ->
                    listOf("Омлет", "Яйцо")

                normalized.contains("греч") ->
                    listOf("Рис", "Булгур")

                normalized.contains("рис") ->
                    listOf("Гречка", "Булгур")

                normalized.contains("макарон") ->
                    listOf("Паста", "Лазанья")

                normalized.contains("паста") ->
                    listOf("Макароны", "Лазанья")

                normalized.contains("котлет") ->
                    listOf("Курица", "Гуляш")

                normalized.contains("куриц") ->
                    listOf("Индейка", "Котлета")

                normalized.contains("салат") ->
                    listOf("Оливье", "Греческий салат")

                normalized.contains("пицц") ->
                    listOf("Бургер", "Шаурма")

                normalized.contains("торт") ->
                    listOf("Пирог", "Пончик")

                normalized.contains("пончик") ->
                    listOf("Торт", "Пирог")

                normalized.contains("суп") ->
                    listOf("Борщ", "Щи")

                else ->
                    listOf("Похожее блюдо", "Другое блюдо")
            }

        val safeConfidence =
            confidence.coerceIn(0.5f, 0.99f)

        val secondConfidence =
            ((1f - safeConfidence) * 0.6f)

        val thirdConfidence =
            ((1f - safeConfidence) * 0.4f)

        return listOf(
            "$foodName — ${(safeConfidence * 100).toInt()}%",
            "${alternatives[0]} — ${(secondConfidence * 100).toInt()}%",
            "${alternatives[1]} — ${(thirdConfidence * 100).toInt()}%"
        )
    }
}