package com.example.cookbookai.ui.history

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import android.view.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.databinding.FragmentHistoryBinding
import com.example.cookbookai.domain.StatisticsCalculator
import com.example.cookbookai.viewmodel.AnalysisViewModel
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding:
            FragmentHistoryBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel:
            AnalysisViewModel by viewModels()

    private val statisticsCalculator =
        StatisticsCalculator()

    private var currentHistory =
        emptyList<AnalysisResult>()

    private lateinit var historyAdapter:
            HistoryAnalysisAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHistoryBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        setupHistoryList()

        viewModel.loadHistory()

        // SHARE

        binding.buttonExportPdf
            .setOnClickListener {

                if (currentHistory.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "История пока пустая",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                shareHistory()
            }

        // OBSERVE

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.history.collect { list ->

                    currentHistory =
                        list

                    historyAdapter.updateItems(
                        currentHistory
                    )

                    binding.emptyState.isVisible =
                        currentHistory.isEmpty()

                    binding.recyclerHistory.isVisible =
                        currentHistory.isNotEmpty()

                    binding.buttonExportPdf.isEnabled =
                        currentHistory.isNotEmpty()

                    binding.buttonExportPdf.alpha =
                        if (currentHistory.isNotEmpty()) 1f else 0.5f

                    val statistics =
                        statisticsCalculator
                            .calculate(currentHistory)

                    binding.textTotalCalories.text =
                        "${statistics.totalCalories}\nккал всего"

                    binding.textAverageCalories.text =
                        "${statistics.averageCalories}\nккал среднее"

                    binding.textAnalysesCount.text =
                        "${statistics.analysesCount}\nанализов"

                    binding.textMacros.text =
                        "Б ${statistics.totalProteins.toInt()} | " +
                                "Ж ${statistics.totalFats.toInt()} | " +
                                "У ${statistics.totalCarbs.toInt()}"

                    setupLineChart(
                        currentHistory.reversed()
                    )

                    setupPieChart(statistics)
                }
            }
        }
    }

    private fun shareHistory() {

        val statistics =
            statisticsCalculator.calculate(
                currentHistory
            )

        val recentItems =
            currentHistory
                .take(5)
                .joinToString("\n") { item ->
                    "${item.name}: ${item.calories} ккал, Б ${item.proteins.toInt()} | Ж ${item.fats.toInt()} | У ${item.carbs.toInt()}"
                }

        val text =
            """
            История анализа CookBook AI

            Всего анализов: ${statistics.analysesCount}
            Калории всего: ${statistics.totalCalories} ккал
            Среднее: ${statistics.averageCalories} ккал
            БЖУ всего: Б ${statistics.totalProteins.toInt()} | Ж ${statistics.totalFats.toInt()} | У ${statistics.totalCarbs.toInt()}

            Последние анализы:
            $recentItems
            """.trimIndent()

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type =
                    "text/plain"

                putExtra(
                    Intent.EXTRA_TEXT,
                    text
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Поделиться историей"
            )
        )
    }

    private fun setupHistoryList() {

        historyAdapter =
            HistoryAnalysisAdapter(
                emptyList()
            ) { result ->

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        com.example.cookbookai.R.id.fragmentContainer,
                        com.example.cookbookai.ui.ai.AnalysisDetailFragment
                            .newInstance(result)
                    )
                    .addToBackStack(null)
                    .commit()
            }

        binding.recyclerHistory.layoutManager =
            LinearLayoutManager(
                requireContext()
            )

        binding.recyclerHistory.adapter =
            historyAdapter
    }

    // ==================================================
    // LINE CHART
    // ==================================================

    private fun setupLineChart(
        history: List<AnalysisResult>
    ) {

        val entries =
            history.mapIndexed { index, item ->

                Entry(
                    index.toFloat(),
                    item.calories.toFloat()
                )
            }

        if (history.isEmpty()) {

            binding.chart.clear()
            binding.chart.setNoDataText("Нет данных для графика")
            binding.chart.invalidate()
            return
        }

        val dataSet =
            LineDataSet(
                entries,
                "Калории"
            )

        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.valueTextSize = 12f

        val accent =
            ContextCompat.getColor(
                requireContext(),
                com.example.cookbookai.R.color.accent_dark
            )

        val muted =
            ContextCompat.getColor(
                requireContext(),
                com.example.cookbookai.R.color.text_secondary
            )

        val text =
            ContextCompat.getColor(
                requireContext(),
                com.example.cookbookai.R.color.text_primary
            )

        dataSet.color =
            accent

        dataSet.setCircleColor(
            accent
        )

        dataSet.valueTextColor =
            text

        binding.chart.data =
            LineData(dataSet)

        binding.chart.animateX(1000)

        val description =
            Description()

        description.text =
            ""

        binding.chart.description =
            description

        binding.chart.axisLeft.textColor =
            muted

        binding.chart.xAxis.textColor =
            muted

        binding.chart.axisRight.isEnabled =
            false

        binding.chart.legend.textColor =
            text

        binding.chart.invalidate()
    }

    // ==================================================
    // PIE CHART
    // ==================================================

    private fun setupPieChart(
        statistics: com.example.cookbookai.domain.StatisticsResult
    ) {

        if (
            statistics.totalProteins == 0f &&
            statistics.totalFats == 0f &&
            statistics.totalCarbs == 0f
        ) {

            binding.pieChart.clear()
            binding.pieChart.setNoDataText("Нет данных по БЖУ")
            binding.pieChart.invalidate()
            return
        }

        val entries =
            listOf(

                PieEntry(
                    statistics.totalProteins,
                    "Белки"
                ),

                PieEntry(
                    statistics.totalFats,
                    "Жиры"
                ),

                PieEntry(
                    statistics.totalCarbs,
                    "Углеводы"
                )
            )

        val dataSet =
            PieDataSet(
                entries,
                "БЖУ"
            )

        dataSet.colors =
            listOf(
                ContextCompat.getColor(
                    requireContext(),
                    com.example.cookbookai.R.color.protein_accent
                ),
                ContextCompat.getColor(
                    requireContext(),
                    com.example.cookbookai.R.color.fat_accent
                ),
                ContextCompat.getColor(
                    requireContext(),
                    com.example.cookbookai.R.color.carb_accent
                )
            )

        val data =
            PieData(dataSet)

        data.setValueTextSize(14f)
        data.setValueTextColor(
            Color.WHITE
        )

        binding.pieChart.data =
            data

        binding.pieChart.centerText =
            "БЖУ"

        binding.pieChart.setCenterTextColor(
            ContextCompat.getColor(
                requireContext(),
                com.example.cookbookai.R.color.text_primary
            )
        )

        binding.pieChart.legend.textColor =
            ContextCompat.getColor(
                requireContext(),
                com.example.cookbookai.R.color.text_primary
            )

        binding.pieChart.description.text =
            ""

        binding.pieChart.animateY(1000)

        binding.pieChart.invalidate()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
