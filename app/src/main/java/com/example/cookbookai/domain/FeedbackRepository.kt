package com.example.cookbookai.domain

object FeedbackRepository {

    private val feedbacks = mutableListOf<FoodFeedback>()

    fun save(feedback: FoodFeedback) {
        feedbacks.add(feedback)
    }

    fun getAll(): List<FoodFeedback> = feedbacks

    fun getCorrectionsFor(className: String): List<String> {
        return feedbacks
            .filter { it.detectedClass == className }
            .map { it.correctedClass }
    }
}