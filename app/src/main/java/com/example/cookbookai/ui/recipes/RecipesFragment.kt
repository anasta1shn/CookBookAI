package com.example.cookbookai.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookbookai.databinding.FragmentRecipesBinding
import com.example.cookbookai.viewmodel.RecipeViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by activityViewModels()

    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupRecycler()
        setupSearch()
        setupCategories()
        observeRecipes()
    }

    private fun setupRecycler() {

        adapter = RecipeAdapter(
            onClick = { recipe ->
                val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
                intent.putExtra("recipe_id", recipe.id)
                startActivity(intent)
            },
            onFavoriteClick = { recipe ->
                viewModel.toggleFavorite(recipe)
            }
        )

        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerView.adapter = adapter
    }

    private fun observeRecipes() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.filteredRecipes.collect { list ->

                adapter.submitList(list)

                // empty state
                binding.emptyState.isVisible = list.isEmpty()
                binding.recyclerView.isVisible = list.isNotEmpty()
            }
        }
    }

    private fun setupSearch() {

        binding.etSearch.addTextChangedListener {
            viewModel.updateSearch(it.toString())
        }
    }

    private fun setupCategories() {

        val categories = listOf(
            "Все",
            "Завтрак",
            "Обед",
            "Ужин",
            "Десерт",
            "Перекус",
            "Напиток"
        )

        categories.forEach { category ->

            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isClickable = true
            }

            binding.chipGroup.addView(chip)
        }

        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->

            val chip = group.findViewById<Chip>(
                checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            )

            val category = chip.text.toString()

            if (category == "Все") {
                viewModel.updateCategory(null)
            } else {
                viewModel.updateCategory(category)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}