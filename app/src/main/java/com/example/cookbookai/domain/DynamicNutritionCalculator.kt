package com.example.cookbookai.domain

import com.example.cookbookai.data.local.FoodDatabaseManager
import com.example.cookbookai.data.model.FoodNutrition

class DynamicNutritionCalculator {

    fun calculateMixedDish(
        sideDish: String,
        meat: String,
        totalWeight: Int
    ): FoodNutrition? {

        val sideDishFood =
            FoodDatabaseManager.findFood(sideDish)

        val meatFood =
            FoodDatabaseManager.findFood(meat)

        if (
            sideDishFood == null ||
            meatFood == null
        ) {
            return null
        }

        // 60% гарнир
        val sideDishWeight =
            totalWeight * 0.6f

        // 40% мясо
        val meatWeight =
            totalWeight * 0.4f

        val calories =
            (
                    sideDishFood.calories * sideDishWeight / 100f
                    ) +
                    (
                            meatFood.calories * meatWeight / 100f
                            )

        val proteins =
            (
                    sideDishFood.proteins * sideDishWeight / 100f
                    ) +
                    (
                            meatFood.proteins * meatWeight / 100f
                            )

        val fats =
            (
                    sideDishFood.fats * sideDishWeight / 100f
                    ) +
                    (
                            meatFood.fats * meatWeight / 100f
                            )

        val carbs =
            (
                    sideDishFood.carbs * sideDishWeight / 100f
                    ) +
                    (
                            meatFood.carbs * meatWeight / 100f
                            )

        return FoodNutrition(

            name =
                "$sideDish с $meat",

            category =
                "mixed_dish",

            calories =
                calories,

            proteins =
                proteins,

            fats =
                fats,

            carbs =
                carbs
        )
    }
}