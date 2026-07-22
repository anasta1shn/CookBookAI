package com.example.cookbookai.ui.ai

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.example.cookbookai.MainActivity
import com.example.cookbookai.R
import com.example.cookbookai.data.local.FoodDatabaseManager
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.databinding.FragmentAnalysisDetailBinding
import com.example.cookbookai.viewmodel.AnalysisViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class AnalysisDetailFragment : Fragment() {

    private var _binding: FragmentAnalysisDetailBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel: AnalysisViewModel by viewModels()

    private var result: AnalysisResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAnalysisDetailBinding.inflate(
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

        super.onViewCreated(
            view,
            savedInstanceState
        )

        result =
            readResultFromArguments()

        result?.let {
            showResult(it)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    navigateToHome()
                }
            }
        )

        binding.buttonBack.setOnClickListener {

            navigateToHome()
        }

        binding.buttonSave.setOnClickListener {

            val currentResult =
                result

            if (currentResult == null) {

                Toast.makeText(
                    requireContext(),
                    "Нет данных для сохранения",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            binding.buttonSave.isEnabled =
                false

            viewModel.save(
                currentResult
            ) { saved ->

                if (!isAdded) {
                    return@save
                }

                binding.buttonSave.isEnabled =
                    true

                if (saved) {

                    Toast.makeText(
                        requireContext(),
                        "Анализ сохранён в историю",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToAiAnalysis()
                } else {

                    Toast.makeText(
                        requireContext(),
                        "Не удалось сохранить анализ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.buttonDelete.setOnClickListener {

            val currentResult =
                result

            if (currentResult == null) {

                Toast.makeText(
                    requireContext(),
                    "Нет данных для удаления",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (currentResult.id == null) {

                Toast.makeText(
                    requireContext(),
                    "Этот анализ ещё не сохранён в истории",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            android.app.AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Удалить анализ?")
                .setMessage("Запись будет удалена из истории.")
                .setPositiveButton("Удалить") { _, _ ->

                    viewModel.deleteAnalysis(
                        currentResult
                    )

                    Toast.makeText(
                        requireContext(),
                        "Анализ удалён",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        binding.buttonExportPdf.setOnClickListener {

            showShareDialog()
        }
    }

    private fun showResult(
        result: AnalysisResult
    ) {

        if (result.imageUri.isNullOrBlank()) {

            binding.imageFoodDetail.visibility =
                View.GONE

            binding.imagePlaceholder.visibility =
                View.VISIBLE

        } else {

            binding.imagePlaceholder.visibility =
                View.GONE

            binding.imageFoodDetail.visibility =
                View.VISIBLE

            binding.imageFoodDetail.setImageURI(
                Uri.parse(result.imageUri)
            )
        }

        binding.textDishName.text =
            result.name

        binding.buttonSave.isEnabled =
            result.id == null

        binding.buttonSave.text =
            if (result.id == null) {
                "Сохранить в историю"
            } else {
                "Уже сохранено в истории"
            }

        binding.textCalories.text =
            "${result.calories} ккал"

        binding.textMacros.text =
            "Итого: Б ${formatMacro(result.proteins)} г   Ж ${formatMacro(result.fats)} г   У ${formatMacro(result.carbs)} г"

        binding.textWeight.text =
            "${result.weight} г"

        showIndividualMacros(
            result
        )

        binding.textConfidence.text =
            "AI confidence: ${(result.confidence * 100).toInt()}%"

        val predictionsText =
            if (result.topPredictions.isNotEmpty()) {
                result.topPredictions.joinToString("\n")
            } else {
                "Нет данных"
            }

        binding.textTopPredictions.text =
            "Top-3 predictions:\n$predictionsText"

        binding.textAiSummary.text =
            if (result.aiSummary.isNotBlank()) {
                result.aiSummary
            } else {
                "AI-рекомендация пока недоступна"
            }
    }

    private fun navigateToHome() {

        parentFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val mainActivity =
            activity as? MainActivity

        if (mainActivity != null) {
            mainActivity.navigateToHome()
        } else {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    com.example.cookbookai.ui.recipes.RecipesFragment()
                )
                .commit()
        }
    }

    private fun navigateToAiAnalysis() {

        parentFragmentManager.popBackStack(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val mainActivity =
            activity as? MainActivity

        if (mainActivity != null) {
            mainActivity.navigateToAiAnalysis()
        } else {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    AiAnalysisFragment()
                )
                .commit()
        }
    }

    private fun showIndividualMacros(
        result: AnalysisResult
    ) {

        val parts =
            result.name
                .split("+")
                .map { it.trim() }
                .filter { it.isNotBlank() }

        if (parts.size < 2) {

            binding.textIndividualMacros.visibility =
                View.GONE

            return
        }

        val firstFood =
            FoodDatabaseManager.findFood(parts[0])

        val secondFood =
            FoodDatabaseManager.findFood(parts[1])

        if (
            firstFood == null ||
            secondFood == null
        ) {

            binding.textIndividualMacros.visibility =
                View.GONE

            return
        }

        val firstWeight =
            (result.weight * 0.6f).roundToInt()

        val secondWeight =
            result.weight - firstWeight

        val firstMultiplier =
            firstWeight / 100f

        val secondMultiplier =
            secondWeight / 100f

        binding.textIndividualMacros.visibility =
            View.VISIBLE

        binding.textIndividualMacros.text =
            """
            КБЖУ по продуктам:
            ${firstFood.name} (${firstWeight} г): ${formatCalories(firstFood.calories * firstMultiplier)} ккал, Б ${formatMacro(firstFood.proteins * firstMultiplier)} г, Ж ${formatMacro(firstFood.fats * firstMultiplier)} г, У ${formatMacro(firstFood.carbs * firstMultiplier)} г
            ${secondFood.name} (${secondWeight} г): ${formatCalories(secondFood.calories * secondMultiplier)} ккал, Б ${formatMacro(secondFood.proteins * secondMultiplier)} г, Ж ${formatMacro(secondFood.fats * secondMultiplier)} г, У ${formatMacro(secondFood.carbs * secondMultiplier)} г

            Суммарно: ${result.calories} ккал, Б ${formatMacro(result.proteins)} г, Ж ${formatMacro(result.fats)} г, У ${formatMacro(result.carbs)} г
            """.trimIndent()
    }

    private fun formatCalories(
        value: Float
    ): Int =
        value.roundToInt()

    private fun formatMacro(
        value: Float
    ): String =
        if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(
                java.util.Locale.US,
                "%.1f",
                value
            )
        }

    private fun showShareDialog() {

        val currentResult =
            result

        if (currentResult == null) {

            Toast.makeText(
                requireContext(),
                "Нет данных для отправки",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        android.app.AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Поделиться результатом")
            .setItems(
                arrayOf(
                    "Текстом",
                    "Картинкой"
                )
            ) { _, which ->

                when (which) {
                    0 -> shareText(currentResult)
                    1 -> shareImage()
                }
            }
            .show()
    }

    private fun shareText(
        result: AnalysisResult
    ) {

        val text =
            buildShareText(result)

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
                "Поделиться анализом"
            )
        )
    }

    private fun shareImage() {

        val uri =
            createResultImageUri()

        if (uri == null) {

            Toast.makeText(
                requireContext(),
                "Не удалось подготовить картинку",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type =
                    "image/png"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Поделиться картинкой"
            )
        )
    }

    private fun createResultImageUri(): Uri? {

        val view =
            binding.root

        if (
            view.width == 0 ||
            view.height == 0
        ) {
            return null
        }

        val bitmap =
            Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        view.draw(canvas)

        val folder =
            File(
                requireContext().cacheDir,
                "shared_images"
            )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file =
            File(
                folder,
                "analysis_result.png"
            )

        FileOutputStream(file).use { output ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output
            )
        }

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun buildShareText(
        result: AnalysisResult
    ): String {

        val predictions =
            if (result.topPredictions.isNotEmpty()) {
                result.topPredictions.joinToString("\n")
            } else {
                "Нет данных"
            }

        val summary =
            if (result.aiSummary.isNotBlank()) {
                result.aiSummary
            } else {
                "AI-рекомендация пока недоступна"
            }

        return """
            Результат анализа CookBook AI

            ${result.name}
            Калории: ${result.calories} ккал
            Белки: ${result.proteins} г
            Жиры: ${result.fats} г
            Углеводы: ${result.carbs} г
            Вес: ${result.weight} г
            Точность: ${(result.confidence * 100).toInt()}%

            Топ предсказаний:
            $predictions

            Рекомендация:
            $summary
        """.trimIndent()
    }

    private fun readResultFromArguments(): AnalysisResult? {

        val args =
            arguments ?: return null

        return AnalysisResult(
            id = args.getString(ARG_ID),
            name = args.getString(ARG_NAME) ?: "",
            calories = args.getInt(ARG_CALORIES),
            proteins = args.getFloat(ARG_PROTEINS),
            fats = args.getFloat(ARG_FATS),
            carbs = args.getFloat(ARG_CARBS),
            imageUri = args.getString(ARG_IMAGE_URI),
            weight = args.getInt(ARG_WEIGHT),
            confidence = args.getFloat(ARG_CONFIDENCE),
            aiSummary = args.getString(ARG_AI_SUMMARY) ?: "",
            topPredictions =
                args.getStringArrayList(ARG_TOP_PREDICTIONS)
                    ?: emptyList(),
            date = args.getLong(ARG_DATE)
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_ID =
            "id"

        private const val ARG_NAME =
            "name"

        private const val ARG_CALORIES =
            "calories"

        private const val ARG_PROTEINS =
            "proteins"

        private const val ARG_FATS =
            "fats"

        private const val ARG_CARBS =
            "carbs"

        private const val ARG_IMAGE_URI =
            "image_uri"

        private const val ARG_WEIGHT =
            "weight"

        private const val ARG_CONFIDENCE =
            "confidence"

        private const val ARG_AI_SUMMARY =
            "ai_summary"

        private const val ARG_TOP_PREDICTIONS =
            "top_predictions"

        private const val ARG_DATE =
            "date"

        fun newInstance(
            result: AnalysisResult
        ): AnalysisDetailFragment {

            val fragment =
                AnalysisDetailFragment()

            val bundle =
                Bundle().apply {

                    putString(
                        ARG_ID,
                        result.id
                    )

                    putString(
                        ARG_NAME,
                        result.name
                    )

                    putInt(
                        ARG_CALORIES,
                        result.calories
                    )

                    putFloat(
                        ARG_PROTEINS,
                        result.proteins
                    )

                    putFloat(
                        ARG_FATS,
                        result.fats
                    )

                    putFloat(
                        ARG_CARBS,
                        result.carbs
                    )

                    putString(
                        ARG_IMAGE_URI,
                        result.imageUri
                    )

                    putInt(
                        ARG_WEIGHT,
                        result.weight
                    )

                    putFloat(
                        ARG_CONFIDENCE,
                        result.confidence
                    )

                    putString(
                        ARG_AI_SUMMARY,
                        result.aiSummary
                    )

                    putStringArrayList(
                        ARG_TOP_PREDICTIONS,
                        ArrayList(result.topPredictions)
                    )

                    putLong(
                        ARG_DATE,
                        result.date
                    )
                }

            fragment.arguments =
                bundle

            return fragment
        }
    }
}
