package com.example.cookbookai.ui.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cookbookai.databinding.ItemAnalysisBinding
import com.example.cookbookai.data.model.AnalysisResult

class AnalysisAdapter : RecyclerView.Adapter<AnalysisAdapter.ViewHolder>() {

    private val items = mutableListOf<AnalysisResult>()

    fun submitList(list: List<AnalysisResult>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalysisResult) {
            binding.textName.text = item.name
            binding.textCalories.text = "${item.calories} ккал"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}