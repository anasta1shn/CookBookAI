package com.example.cookbookai.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cookbookai.R
import com.example.cookbookai.ui.recipes.RecipeAdapter
import com.example.cookbookai.ui.recipes.RecipeDetailActivity
import com.example.cookbookai.viewmodel.RecipeViewModel
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val viewModel: RecipeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerFavorites)

        val adapter = RecipeAdapter(
            onClick = { recipe ->
                val intent =
                    Intent(
                        requireContext(),
                        RecipeDetailActivity::class.java
                    )

                intent.putExtra(
                    "recipe_id",
                    recipe.id
                )

                startActivity(intent)
            },
            onFavoriteClick = { recipe ->
                viewModel.toggleFavorite(recipe)
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoriteRecipes.collect { list ->
                adapter.submitList(list)
            }
        }
    }
}
