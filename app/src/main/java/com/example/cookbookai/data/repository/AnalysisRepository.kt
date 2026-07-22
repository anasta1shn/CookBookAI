package com.example.cookbookai.data.repository

import android.content.Context
import android.util.Log
import com.example.cookbookai.data.local.AppDatabase
import com.example.cookbookai.data.model.AnalysisHistoryEntity
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.data.model.AnalysisResultDto
import com.example.cookbookai.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AnalysisRepository(
    context: Context
) {

    private val localDao =
        AppDatabase.get(context).analysisHistoryDao()

    suspend fun saveAnalysis(
        result: AnalysisResult
    ): Boolean {

        val localId =
            result.id ?: UUID.randomUUID().toString()

        localDao.insert(
            AnalysisHistoryEntity.from(
                result,
                localId
            )
        )

        try {

            SupabaseManager.client
                .auth
                .awaitInitialization()

            val user =
                SupabaseManager.client
                    .auth
                    .currentUserOrNull()
                    ?: return true

            SupabaseManager.client
                .postgrest["analysis_results"]
                .insert(
                    mapOf(
                        "user_id" to user.id,
                        "name" to result.name,
                        "calories" to result.calories,
                        "proteins" to result.proteins,
                        "fats" to result.fats,
                        "carbs" to result.carbs,
                        "image_url" to result.imageUri,
                        "weight" to result.weight,
                        "confidence" to result.confidence,
                        "ai_summary" to result.aiSummary,
                        "top_predictions" to result.topPredictions
                            .joinToString("|")
                    )
                )

        } catch (e: Exception) {

            Log.e(
                "AnalysisRepository",
                "Save error: ${e.message}"
            )
        }

        return true
    }

    fun getHistory(): Flow<List<AnalysisResult>> =
        localDao.observeAll()
            .map { items ->
                items.map {
                    it.toAnalysisResult()
                }
            }

    fun getRemoteHistory(): Flow<List<AnalysisResult>> =
        flow {

            try {

                SupabaseManager.client
                    .auth
                    .awaitInitialization()

                val user =
                    SupabaseManager.client
                        .auth
                        .currentUserOrNull()
                        ?: run {
                            emit(emptyList())
                            return@flow
                        }

                val response =
                    SupabaseManager.client
                        .postgrest["analysis_results"]
                        .select {
                            filter {
                                eq(
                                    "user_id",
                                    user.id
                                )
                            }

                            order(
                                "created_at",
                                order = Order.DESCENDING
                            )
                        }
                        .decodeList<AnalysisResultDto>()

                val history =
                    response.map {
                        it.toAnalysisResult()
                    }

                emit(history)

            } catch (e: Exception) {

                Log.e(
                    "AnalysisRepository",
                    "Load history error: ${e.message}"
                )

                emit(emptyList())
            }
        }

    suspend fun deleteAnalysis(
        id: String
    ) {

        localDao.deleteById(id)

        try {

            SupabaseManager.client
                .auth
                .awaitInitialization()

            val user =
                SupabaseManager.client
                    .auth
                    .currentUserOrNull()
                    ?: return

            SupabaseManager.client
                .postgrest["analysis_results"]
                .delete {
                    filter {
                        eq("id", id)
                        eq("user_id", user.id)
                    }
                }

        } catch (e: Exception) {

            Log.e(
                "AnalysisRepository",
                "Delete error: ${e.message}"
            )
        }
    }
}
