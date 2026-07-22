package com.example.cookbookai.domain

class WeightEstimator {

    fun estimateWeight(
        detectedObjects: Int
    ): Int {

        return when {

            detectedObjects <= 1 -> 220

            detectedObjects == 2 -> 350

            detectedObjects == 3 -> 450

            else -> 550
        }
    }
}