package com.example.cookbookai.ui.recipes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.cookbookai.data.model.Recipe
import com.example.cookbookai.databinding.ActivityAddRecipeBinding
import com.example.cookbookai.viewmodel.RecipeViewModel

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecipeBinding

    private val viewModel: RecipeViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    private var editingRecipeId: String? = null

    private var currentRecipe: Recipe? = null

    private val categories = listOf(
        "Завтрак",
        "Обед",
        "Ужин",
        "Десерт",
        "Перекус",
        "Напиток"
    )

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            uri?.let {

                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                selectedImageUri = it
                showRecipeImage()

                binding.ivRecipeImage.load(it) {
                    crossfade(true)
                    error(com.example.cookbookai.R.drawable.ic_placeholder)
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityAddRecipeBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        editingRecipeId =
            intent.getStringExtra(
                "recipe_id"
            )

        setupCategoryDropdown()

        if (editingRecipeId != null) {
            enableEditMode()
        }

        binding.btnSaveRecipe.setOnClickListener {
            saveRecipe()
        }

        binding.ivRecipeImage.setOnClickListener {
            pickImageLauncher.launch(
                arrayOf("image/*")
            )
        }

        binding.recipeImageUploadArea.setOnClickListener {
            pickImageLauncher.launch(
                arrayOf("image/*")
            )
        }

        binding.uploadPlaceholder.setOnClickListener {
            pickImageLauncher.launch(
                arrayOf("image/*")
            )
        }
    }

    private fun setupCategoryDropdown() {

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                categories
            )

        binding.etCategory.setAdapter(adapter)

        binding.etCategory.setOnClickListener {
            binding.etCategory.showDropDown()
        }
    }

    private fun enableEditMode() {

        binding.btnSaveRecipe.text =
            "Сохранить изменения"

        val recipeId =
            editingRecipeId ?: return

        viewModel.getRecipeById(recipeId) { recipe ->

            recipe?.let {

                currentRecipe = it

                binding.etTitle.setText(it.title)
                binding.etDescription.setText(it.description)
                binding.etIngredients.setText(it.ingredients)
                binding.etCalories.setText(it.calories.toString())
                binding.etProteins.setText(it.proteins.toString())
                binding.etFats.setText(it.fats.toString())
                binding.etCarbs.setText(it.carbs.toString())
                binding.etCookingTime.setText(it.cookingTime.toString())
                binding.etCategory.setText(it.category)

                it.imageUri?.let { url ->
                    showRecipeImage()

                    binding.ivRecipeImage.load(url) {
                        crossfade(true)
                        error(com.example.cookbookai.R.drawable.ic_placeholder)
                    }
                }
            }
        }
    }

    private fun showRecipeImage() {
        binding.uploadPlaceholder.visibility =
            View.GONE

        binding.ivRecipeImage.alpha =
            1f
    }

    private fun saveRecipe() {

        val title =
            binding.etTitle.text
                .toString()
                .trim()

        val description =
            binding.etDescription.text
                .toString()
                .trim()

        val ingredients =
            binding.etIngredients.text
                .toString()
                .trim()

        val calories =
            binding.etCalories.text
                .toString()
                .toIntOrNull()
                ?: 0

        val proteins =
            binding.etProteins.text
                .toString()
                .toIntOrNull()
                ?: 0

        val fats =
            binding.etFats.text
                .toString()
                .toIntOrNull()
                ?: 0

        val carbs =
            binding.etCarbs.text
                .toString()
                .toIntOrNull()
                ?: 0

        val cookingTime =
            binding.etCookingTime.text
                .toString()
                .toIntOrNull()
                ?: 0

        val category =
            binding.etCategory.text
                .toString()
                .ifBlank {
                    "Без категории"
                }

        if (title.isBlank()) {

            Toast.makeText(
                this,
                "Введите название рецепта",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.btnSaveRecipe.isEnabled =
            false

        binding.btnSaveRecipe.text =
            "Сохранение..."

        if (editingRecipeId == null) {

            viewModel.addRecipe(
                context = this,
                title = title,
                description = description,
                ingredients = ingredients,
                calories = calories,
                proteins = proteins,
                fats = fats,
                carbs = carbs,
                cookingTime = cookingTime,
                imageUri = selectedImageUri,
                category = category
            )

            Toast.makeText(
                this,
                "Рецепт сохраняется",
                Toast.LENGTH_SHORT
            ).show()

            finish()

        } else {

            val recipe =
                currentRecipe

            if (recipe == null) {

                Toast.makeText(
                    this,
                    "Рецепт не найден",
                    Toast.LENGTH_SHORT
                ).show()

                binding.btnSaveRecipe.isEnabled =
                    true

                binding.btnSaveRecipe.text =
                    "Сохранить изменения"

                return
            }

            val updatedRecipe =
                recipe.copy(
                    title = title,
                    description = description,
                    ingredients = ingredients,
                    calories = calories,
                    proteins = proteins,
                    fats = fats,
                    carbs = carbs,
                    cookingTime = cookingTime,
                    category = category,

                    // ВАЖНО:
                    // если новое фото не выбрано, оставляем старое
                    imageUri = selectedImageUri?.toString()
                        ?: recipe.imageUri,

                    updatedAt = System.currentTimeMillis()
                )

            viewModel.updateRecipe(
                updatedRecipe
            )

            Toast.makeText(
                this,
                "Изменения сохранены",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    private fun showDeleteDialog() {

        AlertDialog.Builder(this)
            .setTitle("Удаление")
            .setMessage("Точно хотите удалить рецепт?")
            .setPositiveButton("Да") { _, _ ->

                currentRecipe?.let { recipe ->

                    viewModel.deleteRecipe(
                        recipe
                    )
                }

                finish()
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }
}
