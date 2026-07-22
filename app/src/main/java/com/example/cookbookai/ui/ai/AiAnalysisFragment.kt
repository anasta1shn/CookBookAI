package com.example.cookbookai.ui.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.R
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.databinding.FragmentAiAnalysisBinding
import com.example.cookbookai.domain.DishIngredientInput
import com.example.cookbookai.ui.history.HistoryFragment
import com.example.cookbookai.viewmodel.AnalysisViewModel
import android.content.Intent
import com.example.cookbookai.ui.barcode.BarcodeScannerFragment
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

class AiAnalysisFragment : Fragment() {

    private var _binding: FragmentAiAnalysisBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel:
            AnalysisViewModel by viewModels()

    private var bitmap: Bitmap? = null

    private var lastResult:
            AnalysisResult? = null

    private var imageUri: String? = null

    private var openedResultDate: Long? = null

    private var activeDialog: android.app.Dialog? = null

    private var currentStatisticsText =
        "Сегодня анализов: 0 • 0 ккал"

    private var currentErrorMessage: String? =
        null

    private var isAnalyzing =
        false

    // ==================================================
    // IMAGE PICKER
    // ==================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {

                try {

                    requireContext()
                        .contentResolver
                        .takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                }

                val source =
                    ImageDecoder.createSource(
                        requireContext().contentResolver,
                        it
                    )

                bitmap =
                    ImageDecoder.decodeBitmap(source)

                imageUri =
                    it.toString()

                viewModel.clearError()

                binding.uploadPlaceholder.visibility =
                    View.GONE

                binding.imageFood
                    .apply {
                        alpha =
                            1f
                    }
                    .setImageBitmap(bitmap)
            }
        }

    // ==================================================
    // ON CREATE VIEW
    // ==================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAiAnalysisBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    // ==================================================
    // ON VIEW CREATED
    // ==================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        // ==================================================
        // UPLOAD IMAGE
        // ==================================================

        observeStatistics()

        viewModel.loadHistory()

        binding.imageFood.setOnClickListener {

            imagePicker.launch("image/*")
        }

        binding.uploadPlaceholder.setOnClickListener {

            imagePicker.launch("image/*")
        }

        // ==================================================
        // ANALYZE IMAGE
        // ==================================================

        binding.buttonAnalyze.setOnClickListener {

            if (bitmap == null) {

                Toast.makeText(
                    requireContext(),
                    "Выберите фото",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.analyzeImage(
                bitmap = bitmap!!,
                imageUri = imageUri
            )
        }

        // ==================================================
        // RESULT
        // ==================================================

        viewModel.result.observe(
            viewLifecycleOwner
        ) { result ->

            if (openedResultDate == result.date) {
                return@observe
            }

            openedResultDate =
                result.date

            lastResult = result

            openAnalysisDetail(
                result
            )
        }

        // ==================================================
        // USER CHOICE
        // ==================================================

        viewModel.needUserChoice.observe(
            viewLifecycleOwner
        ) { mapping ->

            showChoiceDialog(
                options = mapping.options,
                detectedClass = mapping.detectedClasses
                    .firstOrNull()
                    ?: ""
            )
        }

        viewModel.needManualFoodInput.observe(
            viewLifecycleOwner
        ) { title ->

            showCustomInputDialog(
                title
            )
        }

        viewModel.needCustomNutritionInput.observe(
            viewLifecycleOwner
        ) { request ->

            showCustomNutritionDialog(
                foodName = request.foodName,
                weight = request.weight
            )
        }

        // ==================================================
        // WEIGHT INPUT
        // ==================================================

        viewModel.needWeightInput.observe(
            viewLifecycleOwner
        ) { estimatedWeight ->

            showWeightDialog(
                estimatedWeight
            )
        }

        // ==================================================
        // PRELIMINARY RESULT
        // ==================================================

        viewModel.preliminaryAnalysis.observe(
            viewLifecycleOwner
        ) { preliminary ->

            showPreliminaryResultDialog(
                foodName = preliminary.foodName,
                estimatedWeight = preliminary.estimatedWeight,
                confidence = preliminary.confidence
            )
        }

        viewModel.isAnalyzing.observe(
            viewLifecycleOwner
        ) { analyzing ->

            isAnalyzing =
                analyzing

            binding.buttonAnalyze.isEnabled =
                !analyzing

            binding.buttonAnalyze.text =
                if (analyzing) {
                    "Анализ..."
                } else {
                    "Анализ"
                }

            updateAnalysisStatusText()
        }

        viewModel.errorMessage.observe(
            viewLifecycleOwner
        ) { message ->

            currentErrorMessage =
                message

            if (message != null) {
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }

            updateAnalysisStatusText()
        }

        // ==================================================
        // HISTORY
        // ==================================================

        binding.btnHistory.setOnClickListener {

            parentFragmentManager
                .beginTransaction()

                .replace(
                    R.id.fragmentContainer,
                    HistoryFragment()
                )

                .addToBackStack(null)

                .commit()
        }

        // ==================================================
        // BARCODE
        // ==================================================

        binding.buttonBarcode.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    BarcodeScannerFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    // ==================================================
    // SHOW RESULT
    // ==================================================

    private fun showResult(
        result: AnalysisResult
    ) {

        val predictionsText =
            if (result.topPredictions.isNotEmpty()) {
                result.topPredictions.joinToString("\n")
            } else {
                "Нет данных"
            }

        val summaryText =
            if (result.aiSummary.isNotBlank()) {
                result.aiSummary
            } else {
                "AI-рекомендация пока недоступна"
            }

        binding.textResult.text = """
${result.name}

${result.calories} ккал
P ${result.proteins}г   F ${result.fats}г   C ${result.carbs}г

AI confidence: ${(result.confidence * 100).toInt()}%

Top-3 predictions:
$predictionsText

AI Summary:
$summaryText
        """.trimIndent()
    }

    private fun observeStatistics() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.history.collect { history ->
                updateTodayStatistics(
                    history
                )
            }
        }
    }

    private fun updateTodayStatistics(
        history: List<AnalysisResult>
    ) {

        val todayHistory =
            history.filter {
                isToday(
                    it.date
                )
            }

        val totalCalories =
            todayHistory.sumOf {
                it.calories
            }

        val totalProteins =
            todayHistory.sumOf {
                it.proteins.toDouble()
            }.toFloat()

        val totalFats =
            todayHistory.sumOf {
                it.fats.toDouble()
            }.toFloat()

        val totalCarbs =
            todayHistory.sumOf {
                it.carbs.toDouble()
            }.toFloat()

        val averageCalories =
            if (todayHistory.isNotEmpty()) {
                totalCalories / todayHistory.size
            } else {
                0
            }

        binding.textResult.text =
            "Сегодня анализов: ${todayHistory.size} • $totalCalories ккал • среднее $averageCalories ккал"

        currentStatisticsText =
            binding.textResult.text.toString()

        updateAnalysisStatusText()

        binding.textProteinsStat.text =
            "Белки\n${formatMacro(totalProteins)} г"

        binding.textFatsStat.text =
            "Жиры\n${formatMacro(totalFats)} г"

        binding.textCarbsStat.text =
            "Углеводы\n${formatMacro(totalCarbs)} г"

        updateMacroBars(
            proteins = totalProteins,
            fats = totalFats,
            carbs = totalCarbs
        )
    }

    private fun updateAnalysisStatusText() {

        binding.textResult.text =
            when {
                isAnalyzing ->
                    "Анализирую фото... При первом запуске ML-сервера это может занять до 1-2 минут"

                currentErrorMessage != null ->
                    currentErrorMessage

                else ->
                    currentStatisticsText
            }
    }

    private fun updateMacroBars(
        proteins: Float,
        fats: Float,
        carbs: Float
    ) {

        val maxMacro =
            listOf(
                proteins,
                fats,
                carbs,
                1f
            ).max()

        updateBar(
            binding.barProteins,
            proteins / maxMacro
        )

        updateBar(
            binding.barFats,
            fats / maxMacro
        )

        updateBar(
            binding.barCarbs,
            carbs / maxMacro
        )
    }

    private fun updateBar(
        bar: View,
        fraction: Float
    ) {

        val parent =
            bar.parent as? View
                ?: return

        parent.post {
            val width =
                (parent.width * fraction.coerceIn(0f, 1f))
                    .roundToInt()

            bar.layoutParams =
                (bar.layoutParams as ViewGroup.LayoutParams).apply {
                    this.width =
                        width
                }
        }
    }

    private fun isToday(
        timestamp: Long
    ): Boolean {

        val now =
            Calendar.getInstance()

        val date =
            Calendar.getInstance().apply {
                timeInMillis =
                    timestamp
            }

        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatMacro(
        value: Float
    ): String {

        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }


    private fun openAnalysisDetail(
        result: AnalysisResult
    ) {

        dismissActiveDialog()

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                AnalysisDetailFragment.newInstance(
                    result
                )
            )
            .addToBackStack(null)
            .commit()
    }
    // ==================================================
    // CHOICE DIALOG
    // ==================================================

    private fun showChoiceDialog(
        options: List<String>,
        detectedClass: String
    ) {

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                options
            )

        val title =
            when (detectedClass) {

                "side_dish" ->
                    "Выберите гарнир"

                "meat" ->
                    "Выберите мясо"

                "salad" ->
                    "Выберите салат"

                "soup" ->
                    "Выберите суп"

            else ->
                    detectedClass.ifBlank {
                        "Выберите вариант"
                    }
            }

        val dialog =
            android.app.AlertDialog.Builder(
                requireContext()
            )

                .setTitle(title)

                .setAdapter(adapter) { _, which ->

                    val selected =
                        options[which]

                    viewModel.resolveUserChoice(
                        selected
                    )
                }

                .setNegativeButton("Другое") { _, _ ->

                    showCustomInputDialog(
                        "Свой вариант"
                    )
                }

                .setCancelable(false)

                .create()

        dialog.show()

        activeDialog =
            dialog
    }

    // ==================================================
    // CUSTOM INPUT
    // ==================================================

    private fun showCustomInputDialog(
        title: String = "Свой вариант"
    ) {

        val editText =
            EditText(requireContext())

        editText.hint =
            "Введите название"

        val dialog =
            android.app.AlertDialog.Builder(
            requireContext()
        )

            .setTitle(
                title
            )

            .setView(editText)

            .setPositiveButton("OK") { _, _ ->

                val customValue =
                    editText.text
                        .toString()
                        .trim()

                if (customValue.isNotBlank()) {

                    viewModel.resolveUserChoice(
                        customValue.trim()
                    )
                }
            }

            .setNegativeButton(
                "Отмена",
                null
            )

            .show()

        activeDialog =
            dialog
    }

    private fun showCustomNutritionDialog(
        foodName: String,
        weight: Int
    ) {

        val container =
            LinearLayout(requireContext()).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    48,
                    12,
                    48,
                    0
                )
            }

        val hint =
            TextView(requireContext()).apply {
                text =
                    "Для салатов точнее расписать ингредиенты: каждый с новой строки, например \"огурец 80\". Если состав неизвестен, можно ввести готовое КБЖУ вручную."
                setTextColor(
                    resources.getColor(
                        R.color.text_secondary,
                        null
                    )
                )
                textSize =
                    14f
            }

        val nameInput =
            createTextInput(
                hint = "Название продукта",
                text = foodName,
                inputType = InputType.TYPE_CLASS_TEXT
            )

        val ingredientsInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Состав по строкам:\nогурец 80\nпомидоры 100\nлук 20\nподсолнечное масло 10"
                inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines =
                    4
                setSingleLine(false)
            }

        val caloriesInput =
            createTextInput(
                hint = "Ккал на 100 г",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val proteinsInput =
            createTextInput(
                hint = "Белки на 100 г",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val fatsInput =
            createTextInput(
                hint = "Жиры на 100 г",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val carbsInput =
            createTextInput(
                hint = "Углеводы на 100 г",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        listOf(
            hint,
            nameInput,
            ingredientsInput,
            caloriesInput,
            proteinsInput,
            fatsInput,
            carbsInput
        ).forEach { view ->
            container.addView(
                view
            )
        }

        val dialog =
            android.app.AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Добавить КБЖУ")
                .setView(container)
                .setPositiveButton("Рассчитать по составу", null)
                .setNeutralButton("Ввести КБЖУ", null)
                .setNegativeButton("Отмена", null)
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val name =
                        nameInput.text
                            .toString()
                            .trim()

                    val ingredients =
                        parseIngredientsInput(
                            ingredientsInput.text.toString()
                        )

                    if (
                        name.isBlank() ||
                        ingredients.isEmpty()
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Введите название и состав блюда",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    viewModel.calculateDishFromIngredients(
                        foodName = name,
                        ingredients = ingredients
                    )

                    dialog.dismiss()
                }

            dialog
                .getButton(android.app.AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {

                    val name =
                        nameInput.text
                            .toString()
                            .trim()

                    val calories =
                        parseFloatInput(
                            caloriesInput
                        )

                    val proteins =
                        parseFloatInput(
                            proteinsInput
                        )

                    val fats =
                        parseFloatInput(
                            fatsInput
                        )

                    val carbs =
                        parseFloatInput(
                            carbsInput
                        )

                    if (
                        name.isBlank() ||
                        calories == null ||
                        proteins == null ||
                        fats == null ||
                        carbs == null
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Заполните название и все КБЖУ",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    viewModel.addCustomNutritionAndConfirm(
                        foodName = name,
                        weight = weight,
                        calories = calories,
                        proteins = proteins,
                        fats = fats,
                        carbs = carbs
                    )

                    dialog.dismiss()
                }
        }

        dialog.show()

        activeDialog =
            dialog
    }

    private fun createTextInput(
        hint: String,
        text: String = "",
        inputType: Int
    ): EditText {

        return EditText(requireContext()).apply {
            this.hint =
                hint
            this.inputType =
                inputType
            setSingleLine(true)
            setText(
                text
            )
        }
    }

    private fun parseFloatInput(
        editText: EditText
    ): Float? {

        return editText.text
            .toString()
            .trim()
            .replace(',', '.')
            .toFloatOrNull()
    }

    private fun parseIngredientsInput(
        value: String
    ): List<DishIngredientInput> {

        return value
            .lines()
            .mapNotNull { line ->
                val trimmed =
                    line.trim()

                if (trimmed.isBlank()) {
                    return@mapNotNull null
                }

                val match =
                    Regex("""^(.+?)\s+(\d+(?:[.,]\d+)?)\s*(г|гр|g)?$""")
                        .find(trimmed)
                        ?: return@mapNotNull null

                val name =
                    match.groupValues[1]
                        .trim()

                val weight =
                    match.groupValues[2]
                        .replace(',', '.')
                        .toFloatOrNull()
                        ?.roundToInt()
                        ?: return@mapNotNull null

                if (
                    name.isBlank() ||
                    weight <= 0
                ) {
                    null
                } else {
                    DishIngredientInput(
                        name = name,
                        weight = weight
                    )
                }
            }
    }

    // ==================================================
    // PRELIMINARY RESULT DIALOG
    // ==================================================

    private fun showPreliminaryResultDialog(
        foodName: String,
        estimatedWeight: Int,
        confidence: Float
    ) {

        val container =
            LinearLayout(requireContext()).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    16,
                    48,
                    0
                )
            }

        val hint =
            TextView(requireContext()).apply {
                this.text =
                    "Проверьте найденное блюдо и примерный вес. Если все верно — подтвердите, если нет — исправьте поля перед расчетом."

                setTextColor(
                    resources.getColor(
                        R.color.text_secondary,
                        null
                    )
                )

                textSize =
                    14f
            }

        val foodInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Название блюда"

                setText(
                    foodName
                )

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val weightInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Вес, г"

                setText(
                    estimatedWeight.toString()
                )

                inputType =
                    InputType.TYPE_CLASS_NUMBER
            }

        val confidenceText =
            TextView(requireContext()).apply {
                this.text =
                    "Уверенность распознавания: ${(confidence * 100).toInt()}%"

                setTextColor(
                    resources.getColor(
                        R.color.text_secondary,
                        null
                    )
                )

                textSize =
                    13f
            }

        container.addView(hint)
        container.addView(foodInput)
        container.addView(weightInput)
        container.addView(confidenceText)

        val dialog =
            android.app.AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Предварительный результат")
                .setView(container)
                .setPositiveButton("Подтвердить", null)
                .setNeutralButton("Составное", null)
                .setNegativeButton("Отмена", null)
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(android.app.AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {

                    dialog.dismiss()

                    showMixedDishInputDialog(
                        detectedFoodName = foodInput.text
                            .toString()
                            .trim()
                            .ifBlank {
                                foodName
                            },
                        estimatedWeight = weightInput.text
                            .toString()
                            .toIntOrNull()
                            ?: estimatedWeight
                    )
                }

            dialog
                .getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val editedFoodName =
                        foodInput.text
                            .toString()
                            .trim()

                    val editedWeight =
                        weightInput.text
                            .toString()
                            .toIntOrNull()

                    if (
                        editedFoodName.isBlank() ||
                        editedWeight == null ||
                        editedWeight <= 0
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Введите блюдо и вес",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    viewModel.confirmPreliminaryResult(
                        foodName = editedFoodName,
                        weight = editedWeight
                    )

                    dialog.dismiss()
                }
        }

        dialog.show()

        activeDialog =
            dialog
    }

    private fun showMixedDishInputDialog(
        detectedFoodName: String,
        estimatedWeight: Int
    ) {

        val container =
            LinearLayout(requireContext()).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    16,
                    48,
                    0
                )
            }

        val hint =
            TextView(requireContext()).apply {
                text =
                    "Если на тарелке несколько продуктов, укажите два основных компонента. Например: гречка + свинина, рис + курица, картофель + рыба."

                setTextColor(
                    resources.getColor(
                        R.color.text_secondary,
                        null
                    )
                )

                textSize =
                    14f
            }

        val firstInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Первый компонент, например гречка"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val secondInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Второй компонент, например свинина"

                setText(
                    detectedFoodName
                )

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val weightInput =
            EditText(requireContext()).apply {
                this.hint =
                    "Общий вес блюда, г"

                setText(
                    estimatedWeight.toString()
                )

                inputType =
                    InputType.TYPE_CLASS_NUMBER
            }

        container.addView(hint)
        container.addView(firstInput)
        container.addView(secondInput)
        container.addView(weightInput)

        val dialog =
            android.app.AlertDialog.Builder(
                requireContext()
            )
                .setTitle("Составное блюдо")
                .setView(container)
                .setPositiveButton("Рассчитать", null)
                .setNegativeButton("Назад", null)
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val firstComponent =
                        firstInput.text
                            .toString()
                            .trim()

                    val secondComponent =
                        secondInput.text
                            .toString()
                            .trim()

                    val weight =
                        weightInput.text
                            .toString()
                            .toIntOrNull()

                    if (
                        firstComponent.isBlank() ||
                        secondComponent.isBlank() ||
                        weight == null ||
                        weight <= 0
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Введите два компонента и общий вес",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    viewModel.confirmPreliminaryResult(
                        foodName = "$firstComponent + $secondComponent",
                        weight = weight
                    )

                    dialog.dismiss()
                }
        }

        dialog.show()

        activeDialog =
            dialog
    }

    // ==================================================
    // WEIGHT DIALOG
    // ==================================================

    private fun showWeightDialog(
        estimatedWeight: Int
    ) {

        val editText =
            EditText(requireContext())

        editText.inputType =
            InputType.TYPE_CLASS_NUMBER

        editText.setText(
            estimatedWeight.toString()
        )

        val dialog =
            android.app.AlertDialog.Builder(
            requireContext()
        )

            .setTitle(
                "Введите вес блюда (г)"
            )

            .setView(editText)

            .setPositiveButton("OK") { _, _ ->

                val weight =

                    editText.text
                        .toString()
                        .toIntOrNull()

                        ?: estimatedWeight

                viewModel.confirmWeight(
                    weight
                )
            }

            .setNegativeButton(
                "Отмена",
                null
            )

            .setCancelable(false)

            .show()

        activeDialog =
            dialog
    }

    private fun dismissActiveDialog() {

        activeDialog?.dismiss()

        activeDialog =
            null
    }

    // ==================================================
    // DESTROY VIEW
    // ==================================================

    override fun onDestroyView() {

        dismissActiveDialog()

        super.onDestroyView()

        _binding = null
    }
}
