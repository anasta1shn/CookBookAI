package com.example.cookbookai.domain

import com.example.cookbookai.network.model.Detection

data class DishFeatures(
    val meat: Int = 0,
    val fish: Int = 0,
    val egg: Int = 0,
    val soup: Int = 0,
    val salad: Int = 0,
    val sideDish: Int = 0,
    val bakery: Int = 0,
    val dessert: Int = 0,
    val fruit: Int = 0,
    val vegetable: Int = 0,
    val drink: Int = 0,
    val fastFood: Int = 0
)

data class DecisionResult(
    val dishName: String,
    val features: DishFeatures,
    val confidence: Float
)

class DecisionEngine {

    fun analyze(detections: List<Detection>): DecisionResult {

        val features = mapToFeatures(detections)

        val dishName = classifyDish(features)

        val confidence = detections.map { it.confidence }.average().toFloat()

        return DecisionResult(
            dishName = dishName,
            features = features,
            confidence = confidence
        )
    }

    private fun mapToFeatures(detections: List<Detection>): DishFeatures {

        var meat = 0
        var fish = 0
        var egg = 0
        var soup = 0
        var salad = 0
        var sideDish = 0
        var bakery = 0
        var dessert = 0
        var fruit = 0
        var vegetable = 0
        var drink = 0
        var fastFood = 0

        detections.forEach {

            when (it.`class`) {

                "meat" -> meat++
                "fish" -> fish++
                "egg" -> egg++
                "soup" -> soup++
                "salad" -> salad++
                "side_dish" -> sideDish++
                "bakery" -> bakery++
                "dessert" -> dessert++
                "fruit" -> fruit++
                "vegetable" -> vegetable++
                "drink" -> drink++
                "fast_food" -> fastFood++
            }
        }

        return DishFeatures(
            meat, fish, egg, soup, salad,
            sideDish, bakery, dessert,
            fruit, vegetable, drink, fastFood
        )
    }

    private fun classifyDish(f: DishFeatures): String {

        // 🍲 СУПЫ (приоритет)
        if (f.soup > 0) return "Суп"

        // 🍝 ПАСТА С МЯСОМ (твой кейс)
        if (f.meat > 0 && f.sideDish > 0 && f.vegetable > 0) {
            return "Макароны с мясным гуляшом"
        }

        // 🍽 МЯСО + ГАРНИР
        if (f.meat > 0 && f.sideDish > 0) {
            return "Мясное блюдо с гарниром"
        }

        // 🐟 РЫБА + ГАРНИР
        if (f.fish > 0 && f.sideDish > 0) {
            return "Рыба с гарниром"
        }

        // 🍳 ЯЙЦА
        if (f.egg > 0 && f.vegetable > 0) {
            return "Омлет с овощами"
        }

        // 🥗 САЛАТ
        if (f.salad > 0) return "Салат"

        // 🍰 ДЕСЕРТ
        if (f.dessert > 0) return "Десерт"

        // 🍞 ВЫПЕЧКА
        if (f.bakery > 0) return "Выпечка"

        // 🍔 FAST FOOD
        if (f.fastFood > 0) return "Фастфуд"

        // 🍎 ФРУКТЫ / ОВОЩИ
        if (f.fruit > 0) return "Фрукты"
        if (f.vegetable > 0) return "Овощи"

        // 🥤 НАПИТКИ
        if (f.drink > 0) return "Напиток"

        return "Неизвестное блюдо"
    }
}