package com.example.cookbookai.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookbookai.data.local.AppDatabase
import com.example.cookbookai.data.model.Recipe
import com.example.cookbookai.data.remote.RecipeRemoteDataSource
import com.example.cookbookai.data.repository.RecipeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecipeViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).recipeDao()
    private val remote = RecipeRemoteDataSource()
    private val repository = RecipeRepository(dao, remote)

    // --- SEARCH & CATEGORY STATE ---
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    // --- ОСНОВНОЙ FLOW С ФИЛЬТРАЦИЕЙ ---
    val filteredRecipes: StateFlow<List<Recipe>> =
        combine(searchQuery, selectedCategory) { query, category ->
            Pair(query, category)
        }.flatMapLatest { (query, category) ->
            dao.searchAndFilter(query, category)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val favoriteRecipes = dao.getFavoriteRecipes()

    init {
        viewModelScope.launch {
            repository.sync()
        }
    }

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    fun updateCategory(category: String?) {
        selectedCategory.value = category
    }

    fun addRecipe(
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
        viewModelScope.launch {
            repository.addRecipe(
                context,
                title,
                description,
                ingredients,
                calories,
                proteins,
                fats,
                carbs,
                cookingTime,
                imageUri,
                category
            )
        }
    }

    fun getRecipeById(id: String, onResult: (Recipe?) -> Unit) {
        viewModelScope.launch {
            onResult(dao.getById(id))
        }
    }

    fun updateRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.updateRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            repository.updateRecipe(
                recipe.copy(isFavorite = !recipe.isFavorite)
            )
        }
    }
}
