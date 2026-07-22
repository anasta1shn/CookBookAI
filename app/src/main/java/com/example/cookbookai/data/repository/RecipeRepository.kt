package com.example.cookbookai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.cookbookai.data.local.RecipeDao
import com.example.cookbookai.data.model.Recipe
import com.example.cookbookai.data.remote.RecipeRemoteDataSource
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RecipeRepository(
    private val dao: RecipeDao,
    private val remote: RecipeRemoteDataSource
) {

    fun observeRecipes(): Flow<List<Recipe>> =
        dao.observeAll()

    suspend fun addRecipe(
        context: Context,
        title: String,
        description: String,
        ingredients: String,
        calories: Int,
        proteins: Int,
        fats: Int,
        carbs: Int,
        cookingTime: Int,
        imageUri: Uri?,
        category: String
    ) {

        val recipeId =
            UUID.randomUUID().toString()

        // 1. Сначала создаём рецепт без удалённой ссылки на фото
        val localRecipe =
            Recipe(
                id = recipeId,
                title = title,
                description = description,
                ingredients = ingredients,
                calories = calories,
                proteins = proteins,
                fats = fats,
                carbs = carbs,
                cookingTime = cookingTime,
                imageUri = imageUri?.toString(),
                category = category,
                isFavorite = false,
                createdAt = null,
                updatedAt = System.currentTimeMillis()
            )

        // 2. Сразу сохраняем в Room
        dao.insert(localRecipe)

        var finalRecipe =
            localRecipe

        // 3. Пытаемся загрузить фото в Supabase Storage
        val uploadedImageUrl =
            try {

                if (imageUri != null) {
                    remote.uploadImage(
                        context,
                        imageUri
                    )
                } else {
                    null
                }

            } catch (e: Exception) {

                Log.e(
                    "RecipeRepository",
                    "Image upload failed: ${e.message}"
                )

                null
            }

        // 4. Если фото загрузилось, обновляем рецепт
        if (uploadedImageUrl != null) {

            finalRecipe =
                localRecipe.copy(
                    imageUri = uploadedImageUrl,
                    updatedAt = System.currentTimeMillis()
                )

            dao.insert(finalRecipe)
        }

        // 5. Пробуем сохранить в Supabase таблицу recipes
        try {

            remote.insertRecipe(finalRecipe)

        } catch (e: Exception) {

            Log.e(
                "RecipeRepository",
                "Remote insert failed: ${e.message}"
            )
        }
    }

    suspend fun sync() {

        try {

            val remoteRecipes =
                remote.getRecipes()

            val mergedRecipes =
                remoteRecipes.map { remoteRecipe ->

                    val localRecipe =
                        dao.getById(
                            remoteRecipe.id
                        )

                    if (
                        remoteRecipe.imageUri.isNullOrBlank() &&
                        !localRecipe?.imageUri.isNullOrBlank()
                    ) {
                        remoteRecipe.copy(
                            imageUri = localRecipe?.imageUri
                        )
                    } else {
                        remoteRecipe
                    }
                }

            dao.insertAll(
                mergedRecipes
            )

        } catch (e: Exception) {

            Log.e(
                "RecipeRepository",
                "Sync failed: ${e.message}"
            )
        }
    }

    suspend fun deleteRecipe(
        recipe: Recipe
    ) {

        dao.delete(
            recipe
        )

        try {

            remote.deleteRecipe(
                recipe.id
            )

        } catch (e: Exception) {

            Log.e(
                "RecipeRepository",
                "Delete failed: ${e.message}"
            )
        }
    }

    suspend fun updateRecipe(
        recipe: Recipe
    ) {

        val updated =
            recipe.copy(
                updatedAt = System.currentTimeMillis()
            )

        dao.insert(
            updated
        )

        try {

            remote.updateRecipe(
                updated
            )

        } catch (e: Exception) {

            Log.e(
                "RecipeRepository",
                "Update failed: ${e.message}"
            )
        }
    }

    fun getFavoriteRecipes(): Flow<List<Recipe>> =
        dao.getFavoriteRecipes()
}
