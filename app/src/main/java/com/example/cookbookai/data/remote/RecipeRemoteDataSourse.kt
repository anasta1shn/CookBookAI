package com.example.cookbookai.data.remote

import android.content.Context
import android.net.Uri
import com.example.cookbookai.data.model.Recipe
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class RecipeRemoteDataSource {

    private val client = SupabaseManager.client

    suspend fun insertRecipe(recipe: Recipe) {
        client.postgrest
            .from("recipes")
            .insert(recipe)
    }

    suspend fun getRecipes(): List<Recipe> {
        return client.postgrest
            .from("recipes")
            .select()
            .decodeList<Recipe>()
    }

    suspend fun updateRecipe(recipe: Recipe) {
        client.postgrest
            .from("recipes")
            .update(recipe) {
                filter {
                    eq("id", recipe.id)
                }
            }
    }

    suspend fun deleteRecipe(id: String) {
        client.postgrest
            .from("recipes")
            .delete {
                filter {
                    eq("id", id)
                }
            }
    }

    suspend fun uploadImage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return null

            val fileName = "recipe_${System.currentTimeMillis()}.jpg"

            SupabaseManager.client.storage
                .from("recipe-images")
                .upload(fileName, bytes)

            SupabaseManager.client.storage
                .from("recipe-images")
                .publicUrl(fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}