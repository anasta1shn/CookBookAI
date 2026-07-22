package com.example.cookbookai.data.repository

import com.example.cookbookai.data.model.AnalysisResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class FoodRepository(private val supabase: SupabaseClient) {

    suspend fun searchFood(name: String): List<AnalysisResult> {

        val result = supabase
            .from("foods")
            .select {
                filter {
                    ilike("name", "%$name%")
                }
            }

        return result.decodeList<AnalysisResult>()
    }
}