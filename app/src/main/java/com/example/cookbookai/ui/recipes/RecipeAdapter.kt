package com.example.cookbookai.ui.recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cookbookai.R
import com.example.cookbookai.data.model.Recipe
import com.example.cookbookai.databinding.ItemRecipeBinding

class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onFavoriteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe)
    }

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: Recipe) {

            binding.tvTitle.text = recipe.title
            binding.tvDescription.text = recipe.description

            // Загрузка изображения
            recipe.imageUri?.let {
                binding.ivImage.load(it) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_placeholder)
                }
            } ?: run {
                binding.ivImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Иконка избранного
            if (recipe.isFavorite) {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart_filled)
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_heart_outline)
            }

            binding.root.setOnClickListener {
                onClick(recipe)
            }

            binding.ivFavorite.setOnClickListener {
                onFavoriteClick(recipe)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe) =
            oldItem == newItem
    }
}
