package com.example.cookbookai.ui.history

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAnalysisAdapter(
    private var items: List<AnalysisResult>,
    private val onItemClick: (AnalysisResult) -> Unit
) : RecyclerView.Adapter<HistoryAnalysisAdapter.Holder>() {

    fun updateItems(
        newItems: List<AnalysisResult>
    ) {

        items =
            newItems

        notifyDataSetChanged()
    }

    inner class Holder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalysisResult) {

            binding.textName.text =
                item.name

            binding.textCalories.text =
                "${item.calories} ккал"

            binding.textMacros.text =
                "Б ${formatMacro(item.proteins)}  Ж ${formatMacro(item.fats)}  У ${formatMacro(item.carbs)}"

            binding.textWeight.text =
                "${item.weight} г"

            binding.textConfidence.text =
                "AI confidence: ${(item.confidence * 100).toInt()}%"

            binding.textAiSummary.text =
                item.aiSummary

            val formatter =
                SimpleDateFormat(
                    "dd.MM.yyyy HH:mm",
                    Locale.getDefault()
                )

            binding.textDate.text =
                formatter.format(
                    Date(item.date)
                )

            item.imageUri?.let {

                binding.imageFood.setImageURI(
                    Uri.parse(it)
                )
            } ?: run {

                binding.imageFood.setImageResource(
                    com.example.cookbookai.R.drawable.ic_placeholder
                )
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {

        val binding =
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return Holder(binding)
    }

    override fun getItemCount() =
        items.size

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {

        holder.bind(items[position])
    }

    private fun formatMacro(
        value: Float
    ): String {

        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            "%.1f".format(value)
        }
    }
}
