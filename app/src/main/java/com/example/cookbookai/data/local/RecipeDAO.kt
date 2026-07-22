package com.example.cookbookai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cookbookai.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<Recipe>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: Recipe)

    @Query("DELETE FROM recipes")
    suspend fun clear()

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Recipe?

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getFavoriteRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%'")
    fun searchRecipes(query: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE category = :category")
    fun getRecipesByCategory(category: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' AND (:category IS NULL OR category = :category)")
    fun searchAndFilter(query: String, category: String?): Flow<List<Recipe>>
}
