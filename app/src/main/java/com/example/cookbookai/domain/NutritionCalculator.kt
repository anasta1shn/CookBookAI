package com.example.cookbookai.domain

import com.example.cookbookai.data.local.FoodDatabaseManager
import com.example.cookbookai.data.model.FoodNutrition

class NutritionCalculator {

    fun calculate(
        foodName: String
    ): FoodNutrition? {

        return FoodDatabaseManager.findFood(foodName)
    }
}