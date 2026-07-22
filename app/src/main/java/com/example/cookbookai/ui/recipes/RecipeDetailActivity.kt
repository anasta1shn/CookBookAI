package com.example.cookbookai.ui.recipes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.cookbookai.R
import com.example.cookbookai.data.model.Recipe
import com.example.cookbookai.databinding.ActivityRecipeDetailBinding
import com.example.cookbookai.viewmodel.RecipeViewModel
import androidx.activity.viewModels

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private val viewModel: RecipeViewModel by viewModels()

    private var recipeId: String? = null
    private var currentRecipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recipeId = intent.getStringExtra("recipe_id")

        recipeId?.let { id ->
            viewModel.getRecipeById(id) { recipe ->
                recipe?.let {
                    currentRecipe = it
                    showRecipe(it)
                }
            }
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, AddRecipeActivity::class.java)
            intent.putExtra("recipe_id", recipeId)
            startActivity(intent)
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Удаление")
                .setMessage("Точно хотите удалить рецепт?")
                .setPositiveButton("Да") { _, _ ->
                    currentRecipe?.let { viewModel.deleteRecipe(it) }
                    finish()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun showRecipe(recipe: Recipe) {
        binding.tvTitle.text = recipe.title
        binding.tvDescription.text = recipe.description
        binding.tvIngredients.text =
            recipe.ingredients.ifBlank { "Ингредиенты не указаны" }
        binding.tvCategory.text = "Категория: ${recipe.category}"
        binding.tvCookingTime.text = "⏱ Время приготовления: ${recipe.cookingTime} мин"

        binding.tvCalories.text = recipe.calories.toString()
        binding.tvProteins.text = recipe.proteins.toString()
        binding.tvFats.text = recipe.fats.toString()
        binding.tvCarbs.text = recipe.carbs.toString()

        recipe.imageUri?.let { url ->
            binding.ivRecipeImage.load(url) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
        } ?: run {
            binding.ivRecipeImage.setImageResource(R.drawable.ic_placeholder)
        }
    }
}
