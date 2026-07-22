package com.example.cookbookai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.data.local.BarcodeProductDatabaseManager
import com.example.cookbookai.data.local.FoodDatabaseManager
import com.example.cookbookai.databinding.ActivityMainBinding
import com.example.cookbookai.network.ServerSettings
import com.example.cookbookai.ui.account.AccountFragment
import com.example.cookbookai.ui.ai.AiAnalysisFragment
import com.example.cookbookai.ui.favorites.FavoritesFragment
import com.example.cookbookai.ui.recipes.AddRecipeActivity
import com.example.cookbookai.ui.recipes.RecipesFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedNavItem: Int =
        R.id.menu_recipes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ загружаем food_database.json
        ServerSettings.init(this)
        FoodDatabaseManager.loadDatabase(this)
        BarcodeProductDatabaseManager.loadDatabase(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBarInsets()

        lifecycleScope.launch {
            BarcodeProductDatabaseManager.loadSavedProducts(this@MainActivity)
            BarcodeProductDatabaseManager.syncFromSupabase(this@MainActivity)
        }

        // ✅ стартовый фрагмент
        if (savedInstanceState == null) {
            replaceFragment(RecipesFragment())
        }

        setupBottomNavigation()
        selectBottomNavItem(R.id.menu_recipes)

        // ✅ кнопка добавления рецепта
        binding.btnAddRecipe.setOnClickListener {
            startActivity(
                Intent(this, AddRecipeActivity::class.java)
            )
        }
    }

    // ✅ замена фрагментов
    private fun replaceFragment(
        fragment: androidx.fragment.app.Fragment
    ) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateToHome() {

        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        replaceFragment(
            RecipesFragment()
        )

        selectBottomNavItem(
            R.id.menu_recipes
        )
    }

    fun navigateToAiAnalysis() {

        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        replaceFragment(
            AiAnalysisFragment()
        )

        selectBottomNavItem(
            R.id.menu_ai
        )
    }

    private fun setupBottomNavigation() {

        binding.navRecipes.setOnClickListener {
            if (selectedNavItem != R.id.menu_recipes) {
                replaceFragment(RecipesFragment())
                selectBottomNavItem(R.id.menu_recipes)
            }
        }

        binding.navFavorites.setOnClickListener {
            if (selectedNavItem != R.id.menu_favorites) {
                replaceFragment(FavoritesFragment())
                selectBottomNavItem(R.id.menu_favorites)
            }
        }

        binding.navAi.setOnClickListener {
            if (selectedNavItem != R.id.menu_ai) {
                replaceFragment(AiAnalysisFragment())
                selectBottomNavItem(R.id.menu_ai)
            }
        }

        binding.navAccount.setOnClickListener {
            if (selectedNavItem != R.id.menu_account) {
                replaceFragment(AccountFragment())
                selectBottomNavItem(R.id.menu_account)
            }
        }
    }

    private fun selectBottomNavItem(
        itemId: Int
    ) {

        selectedNavItem =
            itemId

        updateNavItem(
            icon = binding.navIconRecipes,
            text = binding.navTextRecipes,
            selected = itemId == R.id.menu_recipes
        )

        updateNavItem(
            icon = binding.navIconFavorites,
            text = binding.navTextFavorites,
            selected = itemId == R.id.menu_favorites
        )

        updateNavItem(
            icon = binding.navIconAi,
            text = binding.navTextAi,
            selected = itemId == R.id.menu_ai
        )

        updateNavItem(
            icon = binding.navIconAccount,
            text = binding.navTextAccount,
            selected = itemId == R.id.menu_account
        )
    }

    private fun updateNavItem(
        icon: ImageView,
        text: TextView,
        selected: Boolean
    ) {

        val color =
            ContextCompat.getColor(
                this,
                if (selected) {
                    R.color.bottom_nav_item
                } else {
                    R.color.bottom_nav_item_unselected
                }
            )

        icon.setColorFilter(color)
        text.setTextColor(color)
    }

    private fun setupSystemBarInsets() {

        val baseBottomMargin =
            resources.getDimensionPixelSize(
                R.dimen.bottom_nav_margin_bottom
            )

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.mainRoot
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            binding.bottomNavCard.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomMargin =
                    baseBottomMargin + systemBars.bottom
            }

            insets
        }
    }
}
