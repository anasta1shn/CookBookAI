package com.example.cookbookai.ui.history

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.databinding.ActivityAnalysisHistoryBinding

class AnalysisHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisHistoryBinding

    private lateinit var adapter: HistoryAnalysisAdapter

    // временная история
    private val historyList =
        mutableListOf<AnalysisResult>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityAnalysisHistoryBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // ==================================================
        // TEST DATA
        // ==================================================

        loadFakeData()

        // ==================================================
        // ADAPTER
        // ==================================================

        adapter =
            HistoryAnalysisAdapter(
                historyList
            ) { result ->

                Toast.makeText(
                    this,
                    "Выбрано: ${result.name}",
                    Toast.LENGTH_SHORT
                ).show()

                /*
                 Сейчас это Activity, а AnalysisDetailFragment
                 мы открываем через MainActivity/fragmentContainer.
                 Поэтому здесь пока оставляем Toast.

                 Если ты уже не используешь AnalysisHistoryActivity,
                 этот файл можно вообще удалить из проекта.
                */
            }

        binding.recyclerHistory.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerHistory.adapter =
            adapter

        // ==================================================
        // CLEAR HISTORY
        // ==================================================

        binding.btnClear.setOnClickListener {

            historyList.clear()

            adapter.notifyDataSetChanged()
        }
    }

    // ==================================================
    // FAKE DATA
    // ==================================================

    private fun loadFakeData() {

        historyList.addAll(

            listOf(

                AnalysisResult(
                    name = "Паста Карбонара",
                    calories = 620,
                    proteins = 22f,
                    fats = 28f,
                    carbs = 65f,
                    imageUri = null,
                    weight = 300,
                    confidence = 0.92f,
                    aiSummary = "Высокая калорийность • Сбалансированный состав",
                    topPredictions = listOf(
                        "Паста Карбонара — 92%",
                        "Макароны — 5%",
                        "Лазанья — 3%"
                    )
                ),

                AnalysisResult(
                    name = "Салат Цезарь",
                    calories = 420,
                    proteins = 18f,
                    fats = 24f,
                    carbs = 30f,
                    imageUri = null,
                    weight = 250,
                    confidence = 0.88f,
                    aiSummary = "Средняя калорийность • Повышенное содержание жиров",
                    topPredictions = listOf(
                        "Цезарь — 88%",
                        "Овощной салат — 7%",
                        "Греческий салат — 5%"
                    )
                )
            )
        )
    }
}