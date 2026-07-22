package com.example.cookbookai.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.data.model.FoodNutrition
import com.example.cookbookai.data.local.FoodDatabaseManager
import com.example.cookbookai.data.repository.AnalysisRepository
import com.example.cookbookai.domain.AiSummaryGenerator
import com.example.cookbookai.domain.CustomNutritionRequest
import com.example.cookbookai.domain.DishIngredientInput
import com.example.cookbookai.domain.DynamicNutritionCalculator
import com.example.cookbookai.domain.FoodMapper
import com.example.cookbookai.domain.MappingResult
import com.example.cookbookai.domain.NutritionCalculator
import com.example.cookbookai.domain.PreliminaryAnalysis
import com.example.cookbookai.domain.SelectionState
import com.example.cookbookai.domain.SelectionStep
import com.example.cookbookai.domain.WeightEstimator
import com.example.cookbookai.network.RetrofitClient
import com.example.cookbookai.network.model.PredictionResponse
import com.example.cookbookai.network.model.PredictionResponseDeserializer
import com.example.cookbookai.network.model.TopPrediction
import com.example.cookbookai.network.model.TopPredictionDeserializer
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.MessageDigest

class AnalysisViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        AnalysisRepository(application)

    private val foodMapper =
        FoodMapper()

    private val nutritionCalculator =
        NutritionCalculator()

    private val dynamicCalculator =
        DynamicNutritionCalculator()

    private val weightEstimator =
        WeightEstimator()

    private val selectionState =
        SelectionState()

    private val summaryGenerator =
        AiSummaryGenerator()

    private val predictionResponseGson =
        GsonBuilder()
            .registerTypeAdapter(
                TopPrediction::class.java,
                TopPredictionDeserializer()
            )
            .registerTypeAdapter(
                PredictionResponse::class.java,
                PredictionResponseDeserializer()
            )
            .create()

    private var currentImageUri: String? = null

    private var currentStep = ""

    // данные для простого блюда, когда нужно сначала уточнить вес
    private var pendingSimpleFoodName: String? = null
    private var pendingConfidence: Float = 0f
    private var pendingTopPredictions: List<String> = emptyList()
    private var currentImageHash: String? = null
    private var pendingModelFoodName: String? = null

    private val correctionCache =
        application.getSharedPreferences(
            "analysis_correction_cache_v2",
            Context.MODE_PRIVATE
        )

    // ==================================================
    // RESULT
    // ==================================================

    private val _result =
        MutableLiveData<AnalysisResult>()

    val result: LiveData<AnalysisResult> =
        _result

    // ==================================================
    // HISTORY
    // ==================================================

    private val _history =
        MutableStateFlow<List<AnalysisResult>>(
            emptyList()
        )

    val history: StateFlow<List<AnalysisResult>> =
        _history

    private var historyJob: Job? =
        null

    // ==================================================
    // USER CHOICE
    // ==================================================

    private val _needUserChoice =
        MutableLiveData<MappingResult>()

    val needUserChoice: LiveData<MappingResult> =
        _needUserChoice

    private val _needManualFoodInput =
        MutableLiveData<String>()

    val needManualFoodInput: LiveData<String> =
        _needManualFoodInput

    // ==================================================
    // WEIGHT INPUT
    // ==================================================

    private val _needWeightInput =
        MutableLiveData<Int>()

    val needWeightInput: LiveData<Int> =
        _needWeightInput

    // ==================================================
    // PRELIMINARY RESULT
    // ==================================================

    private val _preliminaryAnalysis =
        MutableLiveData<PreliminaryAnalysis>()

    val preliminaryAnalysis: LiveData<PreliminaryAnalysis> =
        _preliminaryAnalysis

    private val _isAnalyzing =
        MutableLiveData(false)

    val isAnalyzing: LiveData<Boolean> =
        _isAnalyzing

    private val _errorMessage =
        MutableLiveData<String?>()

    val errorMessage: LiveData<String?> =
        _errorMessage

    private val _needCustomNutritionInput =
        MutableLiveData<CustomNutritionRequest>()

    val needCustomNutritionInput: LiveData<CustomNutritionRequest> =
        _needCustomNutritionInput

    // ==================================================
    // ANALYZE IMAGE
    // ==================================================

    fun analyzeImage(
        bitmap: Bitmap,
        imageUri: String?
    ) {

        currentImageUri = imageUri

        resetCurrentSelection()

        _errorMessage.postValue(null)
        _isAnalyzing.postValue(true)

        viewModelScope.launch {

            try {

                val stream =
                    ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    stream
                )

                val imageBytes =
                    stream.toByteArray()

                currentImageHash =
                    calculateImageHash(
                        imageBytes
                    )

                val cachedCorrection =
                    currentImageHash
                        ?.let { imageHash ->
                            correctionCache.getString(
                                imageHash,
                                null
                            )
                        }
                        ?.takeIf { it.isNotBlank() }

                val requestBody =
                    imageBytes
                        .toRequestBody(
                            "image/jpeg".toMediaTypeOrNull()
                        )

                val part =
                    MultipartBody.Part.createFormData(
                        "file",
                        "image.jpg",
                        requestBody
                    )

                val response =
                    RetrofitClient.api.predict(part)

                if (!response.isSuccessful) {

                    Log.e(
                        "AI",
                        "Server error: ${response.code()}, body: ${response.errorBody()?.string()}"
                    )

                    showAnalysisError(
                        "Сервер анализа временно недоступен. Проверьте адрес ML-сервера и попробуйте еще раз."
                    )

                    return@launch
                }

                val bodyJson =
                    response.body()

                if (bodyJson == null) {

                    Log.e(
                        "AI",
                        "Empty body"
                    )

                    showAnalysisError(
                        "Сервер вернул пустой ответ. Попробуйте другое фото или повторите анализ."
                    )

                    return@launch
                }

                Log.d(
                    "AI_RAW_RESPONSE",
                    bodyJson.toString()
                )

                val body =
                    predictionResponseGson.fromJson(
                        bodyJson,
                        PredictionResponse::class.java
                    )

                Log.d(
                    "AI_PARSED_RESPONSE",
                    "dish=${body.dish}, className=${body.className}, top=${body.topPredictions}, components=${body.components}, detections=${body.detections}"
                )

                if (cachedCorrection != null) {

                    Log.d(
                        "AI_CORRECTION_CACHE",
                        "Applying cached correction after server response: $cachedCorrection"
                    )

                    pendingModelFoodName =
                        resolveRecognizedFoodName(
                            body,
                            MappingResult()
                        )

                    val cachedResultWasPrepared =
                        preparePreliminaryResult(
                            foodName = cachedCorrection,
                            confidence = 1f,
                            topPredictions = buildManualCorrectionPredictions(
                                cachedCorrection
                            )
                        )

                    if (cachedResultWasPrepared) {
                        return@launch
                    }
                }

                val earlyMapped =
                    foodMapper.mapDetections(
                        body.detections
                    )

                val detectedMixedDishName =
                    componentsDishName(
                        body
                    ) ?: detectionDishName(
                        body
                    )

                if (detectedMixedDishName != null) {

                    val resultWasPrepared =
                        preparePreliminaryResult(
                            foodName = detectedMixedDishName,
                            confidence = body.confidence ?: calculateAverageConfidence(
                                body.detections
                            ),
                            topPredictions = buildTopPredictions(
                                predictions = formatTopPredictions(
                                    body
                                ),
                                fallbackFoodName = detectedMixedDishName,
                                confidence = body.confidence ?: calculateAverageConfidence(
                                    body.detections
                                )
                            )
                        )

                    if (resultWasPrepared) {
                        return@launch
                    }
                }

                if (
                    earlyMapped.needUserChoice &&
                    earlyMapped.options.isNotEmpty()
                ) {

                    requestMappedChoiceWithCandidate(
                        mapped = earlyMapped,
                        candidate = resolveRecognizedFoodName(
                            body,
                            earlyMapped
                        )
                    )

                    return@launch
                }

                val classifiedFoodName =
                    resolveRecognizedFoodName(
                        body,
                        MappingResult()
                    )

                if (classifiedFoodName != null) {

                    val resultWasPrepared =
                        preparePreliminaryResult(
                            foodName = classifiedFoodName,
                            confidence = body.confidence ?: calculateAverageConfidence(
                                body.detections
                            ),
                            topPredictions = formatTopPredictions(
                                body
                            )
                        )

                    if (resultWasPrepared) {
                        return@launch
                    }

                    showUnverifiedPreliminaryResult(
                        foodName = classifiedFoodName,
                        response = body,
                        mapped = MappingResult()
                    )

                    return@launch
                }

                // ==================================================
                // SINGLE FOOD MODE FROM EFFICIENTNET
                // настоящий TOP-3 приходит с сервера
                // ==================================================

                if (
                    body.mode == "single_food" &&
                    body.dish != null
                ) {

                    val dishName =
                        body.dish

                    val nutrition =
                        nutritionCalculator.calculate(
                            dishName
                        )

                    if (nutrition == null) {

                        Log.e(
                            "AI",
                            "Food not found: $dishName"
                        )

                        showUnverifiedPreliminaryResult(
                            foodName = dishName,
                            response = body,
                            mapped = MappingResult()
                        )

                        return@launch
                    }

                    pendingSimpleFoodName =
                        nutrition.name

                    pendingConfidence =
                        body.confidence ?: 0f

                    pendingTopPredictions =
                        formatTopPredictions(
                            body
                        )

                    currentStep =
                        "simple_food"

                    showPreliminaryResult(
                        foodName = nutrition.name,
                        estimatedWeight = weightEstimator.estimateWeight(1),
                        confidence = pendingConfidence,
                        topPredictions = pendingTopPredictions
                    )

                    return@launch
                }

                // ==================================================
                // MULTI FOOD MODE FROM YOLO
                // ==================================================

                val mapped =
                    earlyMapped

                // ==================================================
                // COMPLEX FOOD: meat + side_dish
                // ==================================================

                if (
                    mapped.step == SelectionStep.SIDE_DISH ||
                    (
                            mapped.detectedClasses.contains("meat") &&
                                    mapped.detectedClasses.contains("side_dish")
                            )
                ) {

                    currentStep =
                        "mixed_food"

                    val recognizedFoodName =
                        resolveRecognizedFoodName(
                            body,
                            mapped
                    )

                    if (recognizedFoodName == null) {

                        requestMappedChoice(
                            mapped
                        )

                        if (mapped.options.isNotEmpty()) {
                            return@launch
                        }

                        showUnverifiedPreliminaryResult(
                            foodName = null,
                            response = body,
                            mapped = mapped,
                            estimatedItemsCount = 2
                        )
                        return@launch
                    }

                    val mixedParts =
                        parseMixedDishParts(
                            recognizedFoodName
                        )

                    if (mixedParts.size >= 2) {
                        selectionState.selectedSideDish =
                            mixedParts.first()

                        selectionState.selectedMeat =
                            mixedParts[1]
                    } else {
                        val nutrition =
                            nutritionCalculator.calculate(
                                recognizedFoodName
                            )

                        if (nutrition == null) {
                            showUnverifiedPreliminaryResult(
                                foodName = recognizedFoodName,
                                response = body,
                                mapped = mapped
                            )
                            return@launch
                        }

                        currentStep =
                            "simple_food"

                        pendingSimpleFoodName =
                            nutrition.name
                    }

                    pendingConfidence =
                        body.confidence ?: calculateAverageConfidence(
                            body.detections
                        )

                    pendingTopPredictions =
                        buildTopPredictions(
                            formatTopPredictions(
                                body
                            ),
                            recognizedFoodName,
                            pendingConfidence
                        )

                    showPreliminaryResult(
                        foodName = recognizedFoodName,
                        estimatedWeight = weightEstimator.estimateWeight(
                            if (mixedParts.size >= 2) 2 else 1
                        ),
                        confidence = pendingConfidence,
                        topPredictions = pendingTopPredictions
                    )

                    return@launch
                }

                // ==================================================
                // NEED USER CHOICE FOR SIMPLE CATEGORY
                // soup / salad / egg / dessert / etc.
                // ==================================================

                if (mapped.needUserChoice) {

                    val recognizedFoodName =
                        resolveRecognizedFoodName(
                            body,
                            mapped
                    )

                    if (recognizedFoodName == null) {

                        requestMappedChoice(
                            mapped
                        )

                        if (mapped.options.isNotEmpty()) {
                            return@launch
                        }

                        showUnverifiedPreliminaryResult(
                            foodName = null,
                            response = body,
                            mapped = mapped
                        )
                        return@launch
                    }

                    val nutrition =
                        nutritionCalculator.calculate(
                            recognizedFoodName
                        )

                    if (nutrition == null) {
                        showUnverifiedPreliminaryResult(
                            foodName = recognizedFoodName,
                            response = body,
                            mapped = mapped
                        )
                        return@launch
                    }

                    pendingSimpleFoodName =
                        nutrition.name

                    pendingConfidence =
                        body.confidence ?: calculateAverageConfidence(
                            body.detections
                        )

                    pendingTopPredictions =
                        buildTopPredictions(
                            formatTopPredictions(
                                body
                            ),
                            nutrition.name,
                            pendingConfidence
                        )

                    currentStep =
                        "simple_food"

                    showPreliminaryResult(
                        foodName = nutrition.name,
                        estimatedWeight = weightEstimator.estimateWeight(1),
                        confidence = pendingConfidence,
                        topPredictions = pendingTopPredictions
                    )

                    return@launch
                }

                // ==================================================
                // AUTO SIMPLE FOOD WITHOUT USER CHOICE
                // ==================================================

                if (mapped.foodName.isNotBlank()) {

                    val nutrition =
                        nutritionCalculator.calculate(
                            mapped.foodName
                        )

                    if (nutrition == null) {

                        Log.e(
                            "AI",
                            "Nutrition null for ${mapped.foodName}"
                        )

                        showUnverifiedPreliminaryResult(
                            foodName = mapped.foodName,
                            response = body,
                            mapped = mapped
                        )

                        return@launch
                    }

                    pendingSimpleFoodName =
                        nutrition.name

                    pendingConfidence =
                        body.confidence ?: calculateAverageConfidence(
                            body.detections
                        )

                    pendingTopPredictions =
                        formatTopPredictions(
                            body
                        )

                    currentStep =
                        "simple_food"

                    showPreliminaryResult(
                        foodName = nutrition.name,
                        estimatedWeight = weightEstimator.estimateWeight(1),
                        confidence = pendingConfidence,
                        topPredictions = pendingTopPredictions
                    )
                } else {

                    showUnverifiedPreliminaryResult(
                        foodName = null,
                        response = body,
                        mapped = mapped
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "AI",
                    "Exception: ${e.message}"
                )

                showAnalysisError(
                    networkErrorMessage(
                        e
                    )
                )

            } finally {

                _isAnalyzing.postValue(false)
            }
        }
    }

    fun clearError() {

        _errorMessage.value =
            null
    }

    fun addCustomNutritionAndConfirm(
        foodName: String,
        weight: Int,
        calories: Float,
        proteins: Float,
        fats: Float,
        carbs: Float
    ) {

        val normalizedFoodName =
            foodName.trim()

        if (normalizedFoodName.isBlank()) {
            showAnalysisError(
                "Введите название продукта."
            )
            return
        }

        val customFood =
            FoodNutrition(
                name = normalizedFoodName,
                category = "custom",
                calories = calories,
                proteins = proteins,
                fats = fats,
                carbs = carbs
            )

        FoodDatabaseManager.addCustomFood(
            getApplication(),
            customFood
        )

        pendingSimpleFoodName =
            customFood.name

        currentStep =
            "simple_food"

        if (pendingConfidence == 0f) {
            pendingConfidence =
                0.92f
        }

        pendingTopPredictions =
            buildTopPredictions(
                predictions = pendingTopPredictions,
                fallbackFoodName = customFood.name,
                confidence = pendingConfidence
            )

        saveCorrectionIfNeeded(
            customFood.name
        )

        confirmWeight(
            weight
        )
    }

    fun calculateDishFromIngredients(
        foodName: String,
        ingredients: List<DishIngredientInput>
    ) {

        val normalizedFoodName =
            foodName.trim()

        if (normalizedFoodName.isBlank()) {
            showAnalysisError(
                "Введите название блюда."
            )
            return
        }

        if (ingredients.isEmpty()) {
            showAnalysisError(
                "Добавьте хотя бы один ингредиент состава."
            )
            return
        }

        var totalWeight =
            0

        var totalCalories =
            0f

        var totalProteins =
            0f

        var totalFats =
            0f

        var totalCarbs =
            0f

        val missingIngredients =
            mutableListOf<String>()

        ingredients.forEach { ingredient ->

            val nutrition =
                nutritionCalculator.calculate(
                    ingredient.name
                )

            if (nutrition == null) {
                missingIngredients.add(
                    ingredient.name
                )
                return@forEach
            }

            val multiplier =
                ingredient.weight / 100f

            totalWeight +=
                ingredient.weight

            totalCalories +=
                nutrition.calories * multiplier

            totalProteins +=
                nutrition.proteins * multiplier

            totalFats +=
                nutrition.fats * multiplier

            totalCarbs +=
                nutrition.carbs * multiplier
        }

        if (missingIngredients.isNotEmpty()) {
            showAnalysisError(
                "Не найдены ингредиенты: ${missingIngredients.joinToString(", ")}. Добавьте их в КБЖУ или исправьте название."
            )
            return
        }

        if (totalWeight <= 0) {
            showAnalysisError(
                "Укажите вес ингредиентов в граммах."
            )
            return
        }

        val caloriesPer100 =
            totalCalories * 100f / totalWeight

        val proteinsPer100 =
            totalProteins * 100f / totalWeight

        val fatsPer100 =
            totalFats * 100f / totalWeight

        val carbsPer100 =
            totalCarbs * 100f / totalWeight

        FoodDatabaseManager.addCustomFood(
            getApplication(),
            FoodNutrition(
                name = normalizedFoodName,
                category = "custom_mixed",
                calories = caloriesPer100,
                proteins = proteinsPer100,
                fats = fatsPer100,
                carbs = carbsPer100
            )
        )

        currentStep =
            "simple_food"

        pendingSimpleFoodName =
            normalizedFoodName

        if (pendingConfidence == 0f) {
            pendingConfidence =
                0.92f
        }

        pendingTopPredictions =
            ingredients.map {
                "${it.name} — ${it.weight} г"
            }

        saveCorrectionIfNeeded(
            normalizedFoodName
        )

        val tempResult =
            AnalysisResult(
                name = normalizedFoodName,
                calories = totalCalories.toInt(),
                proteins = totalProteins,
                fats = totalFats,
                carbs = totalCarbs,
                imageUri = currentImageUri,
                weight = totalWeight,
                confidence = pendingConfidence,
                topPredictions = pendingTopPredictions
            )

        val finalResult =
            tempResult.copy(
                aiSummary =
                    summaryGenerator.generate(
                        tempResult
                    )
            )

        _result.postValue(
            finalResult
        )

        resetCurrentSelection(
            keepImageUri = true
        )
    }

    // ==================================================
    // USER CHOICE
    // ==================================================

    fun resolveUserChoice(
        selected: String
    ) {

        when (currentStep) {

            // ==================================================
            // STEP 1: SIDE DISH
            // ==================================================

            "side_dish" -> {

                selectionState.selectedSideDish =
                    selected

                currentStep =
                    "meat"

                _needUserChoice.postValue(

                    MappingResult(
                        foodName = "",
                        needUserChoice = true,
                        options = listOf(
                            "Курица",
                            "Говядина",
                            "Свинина",
                            "Баранина",
                            "Индейка",
                            "Котлета",
                            "Гуляш"
                        ),
                        detectedClasses = listOf("meat")
                    )
                )
            }

            // ==================================================
            // STEP 2: MEAT
            // ==================================================

            "meat" -> {

                selectionState.selectedMeat =
                    selected

                val estimatedWeight =
                    weightEstimator.estimateWeight(2)

                _needWeightInput.postValue(
                    estimatedWeight
                )
            }

            // ==================================================
            // SIMPLE FOOD SELECTED BY USER
            // soup / salad / egg / dessert / other
            // ==================================================

            else -> {

                if (shouldAskForIngredients(selected)) {

                    pendingConfidence =
                        0.92f

                    pendingTopPredictions =
                        listOf(
                            "$selected — требуется уточнить состав"
                        )

                    requestCustomNutritionInput(
                        foodName = selected,
                        weight = weightEstimator.estimateWeight(1)
                    )

                    return
                }

                val nutrition =
                    nutritionCalculator.calculate(
                        selected
                    )

                if (nutrition == null) {

                    Log.e(
                        "AI",
                        "Food not found: $selected"
                    )

                    return
                }

                pendingSimpleFoodName =
                    nutrition.name

                // Для ручного выбора top-3 от сервера нет,
                // потому что пользователь сам выбрал вариант.
                // Confidence оставляем средним рабочим значением.
                pendingConfidence =
                    0.92f

                pendingTopPredictions =
                    listOf(
                        "${nutrition.name} — 92%",
                        "Альтернативный вариант — 5%",
                        "Другое блюдо — 3%"
                    )

                currentStep =
                    "simple_food"

                showPreliminaryResult(
                    foodName = nutrition.name,
                    estimatedWeight = weightEstimator.estimateWeight(1),
                    confidence = pendingConfidence,
                    topPredictions = pendingTopPredictions
                )
            }
        }
    }

    fun confirmPreliminaryResult(
        foodName: String,
        weight: Int
    ) {

        applyEditedPreliminaryResult(
            foodName = foodName,
            weight = weight
        )
    }

    fun editPreliminaryResult(
        foodName: String,
        weight: Int
    ) {

        applyEditedPreliminaryResult(
            foodName = foodName,
            weight = weight
        )
    }

    // ==================================================
    // CONFIRM WEIGHT
    // ==================================================

    fun confirmWeight(
        weight: Int
    ) {

        selectionState.selectedWeight =
            weight

        // ==================================================
        // SIMPLE FOOD
        // ==================================================

        if (
            currentStep == "simple_food" &&
            pendingSimpleFoodName != null
        ) {

            val foodName =
                pendingSimpleFoodName ?: return

            val nutrition =
                nutritionCalculator.calculate(
                    foodName
                )

            if (nutrition == null) {

                Log.e(
                    "AI",
                    "Nutrition null for simple food: $foodName"
                )

                return
            }

            val multiplier =
                weight / 100f

            val tempResult =
                AnalysisResult(

                    name =
                        nutrition.name,

                    calories =
                        (nutrition.calories * multiplier).toInt(),

                    proteins =
                        nutrition.proteins * multiplier,

                    fats =
                        nutrition.fats * multiplier,

                    carbs =
                        nutrition.carbs * multiplier,

                    imageUri =
                        currentImageUri,

                    weight =
                        weight,

                    confidence =
                        pendingConfidence,

                    topPredictions =
                        pendingTopPredictions
                )

            val finalResult =
                tempResult.copy(
                    aiSummary =
                        summaryGenerator.generate(
                            tempResult
                        )
                )

            _result.postValue(
                finalResult
            )

            resetCurrentSelection(
                keepImageUri = true
            )

            return
        }

        // ==================================================
        // MIXED FOOD: side dish + meat
        // ==================================================

        val sideDish =
            selectionState.selectedSideDish

        val meat =
            selectionState.selectedMeat

        if (
            sideDish == null ||
            meat == null
        ) {

            Log.e(
                "AI",
                "Side dish or meat is null"
            )

            return
        }

        val nutrition =
            dynamicCalculator.calculateMixedDish(
                sideDish = sideDish,
                meat = meat,
                totalWeight = weight
            )

        if (nutrition == null) {

            Log.e(
                "AI",
                "Nutrition is null for mixed dish: $sideDish + $meat"
            )

            publishResultWithoutNutritionMatch(
                foodName = "$sideDish + $meat",
                weight = weight
            )

            return
        }

        val dishName =
            "$sideDish + $meat"

        val tempResult =
            AnalysisResult(

                name =
                    dishName,

                calories =
                    nutrition.calories.toInt(),

                proteins =
                    nutrition.proteins,

                fats =
                    nutrition.fats,

                carbs =
                    nutrition.carbs,

                imageUri =
                    currentImageUri,

                weight =
                    weight,

                // Для multi_food настоящий top-3 от EfficientNet
                // сейчас не применяется, потому что работает YOLO + выбор пользователя.
                confidence =
                    0.90f,

                topPredictions =
                    listOf(
                        "$dishName — 90%",
                        "$sideDish с другим мясом — 6%",
                        "Другое составное блюдо — 4%"
                    )
            )

        val finalResult =
            tempResult.copy(
                aiSummary =
                    summaryGenerator.generate(
                        tempResult
                    )
            )

        _result.postValue(
            finalResult
        )

        resetCurrentSelection(
            keepImageUri = true
        )
    }

    private fun showPreliminaryResult(
        foodName: String,
        estimatedWeight: Int,
        confidence: Float,
        topPredictions: List<String>
    ) {

        if (pendingModelFoodName == null) {
            pendingModelFoodName =
                foodName
        }

        _preliminaryAnalysis.postValue(
            PreliminaryAnalysis(
                foodName = foodName,
                estimatedWeight = estimatedWeight,
                confidence = confidence,
                topPredictions = buildTopPredictions(
                    predictions = topPredictions,
                    fallbackFoodName = foodName,
                    confidence = confidence
                )
            )
        )
    }

    private fun preparePreliminaryResult(
        foodName: String,
        confidence: Float,
        topPredictions: List<String>
    ): Boolean {

        val directNutrition =
            nutritionCalculator.calculate(
                foodName
            )

        if (directNutrition != null) {

            pendingSimpleFoodName =
                directNutrition.name

            currentStep =
                "simple_food"

            pendingConfidence =
                confidence

            pendingTopPredictions =
                buildTopPredictions(
                    predictions = topPredictions,
                    fallbackFoodName = directNutrition.name,
                    confidence = confidence
                )

            showPreliminaryResult(
                foodName = directNutrition.name,
                estimatedWeight = weightEstimator.estimateWeight(1),
                confidence = confidence,
                topPredictions = pendingTopPredictions
            )

            return true
        }

        val mixedParts =
            parseMixedDishParts(
                foodName
            )

        if (mixedParts.size >= 2) {

            selectionState.selectedSideDish =
                mixedParts.first()

            selectionState.selectedMeat =
                mixedParts[1]

            currentStep =
                "mixed_food"

            pendingConfidence =
                confidence

        pendingTopPredictions =
            buildTopPredictions(
                predictions = topPredictions,
                fallbackFoodName = foodName,
                confidence = confidence
            ).withMixedDishHintIfNeeded(
                foodName
            )

            showPreliminaryResult(
                foodName = foodName,
                estimatedWeight = weightEstimator.estimateWeight(2),
                confidence = confidence,
                topPredictions = pendingTopPredictions
            )

            return true
        }

        return false
    }

    private fun showUnverifiedPreliminaryResult(
        foodName: String?,
        response: PredictionResponse,
        mapped: MappingResult,
        estimatedItemsCount: Int = 1
    ) {

        val displayName =
            foodName
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.contains("Неизвест", true) }
                ?: resolveDisplayFoodName(
                    response,
                    mapped
                )

                ?: "Неизвестное блюдо"

        val mixedParts =
            if (nutritionCalculator.calculate(displayName) == null) {
                parseMixedDishParts(
                    displayName
                )
            } else {
                emptyList()
            }

        if (mixedParts.size >= 2) {

            selectionState.selectedSideDish =
                mixedParts.first()

            selectionState.selectedMeat =
                mixedParts[1]

            currentStep =
                "mixed_food"
        } else {

            pendingSimpleFoodName =
                nutritionCalculator.calculate(
                    displayName
                )?.name

            currentStep =
                "simple_food"
        }

        pendingConfidence =
            response.confidence ?: calculateAverageConfidence(
                response.detections
            )

        pendingTopPredictions =
            buildTopPredictions(
                predictions = response.topPredictions.map {
                    "${it.displayName()} — ${(it.confidence * 100).toInt()}%"
                },
                fallbackFoodName = displayName,
                confidence = pendingConfidence
            )

        showPreliminaryResult(
            foodName = displayName,
            estimatedWeight = weightEstimator.estimateWeight(
                if (mixedParts.size >= 2) 2 else estimatedItemsCount
            ),
            confidence = pendingConfidence,
            topPredictions = pendingTopPredictions
        )
    }

    private fun applyEditedPreliminaryResult(
        foodName: String,
        weight: Int
    ) {

        val normalizedFoodName =
            foodName.trim()

        if (normalizedFoodName.isBlank()) {
            showPreliminaryResult(
                foodName = "Неизвестное блюдо",
                estimatedWeight = weight,
                confidence = pendingConfidence,
                topPredictions = pendingTopPredictions
            )
            return
        }

        if (shouldAskForIngredients(normalizedFoodName)) {

            requestCustomNutritionInput(
                foodName = normalizedFoodName,
                weight = weight
            )

            return
        }

        val mixedParts =
            if (looksLikeMixedDishName(normalizedFoodName)) {
                parseMixedDishParts(
                    normalizedFoodName
                )
            } else {
                if (nutritionCalculator.calculate(normalizedFoodName) == null) {
                    parseMixedDishParts(
                        normalizedFoodName
                    )
                } else {
                    emptyList()
                }
            }

        if (mixedParts.size >= 2) {

            selectionState.selectedSideDish =
                mixedParts.first()

            selectionState.selectedMeat =
                mixedParts[1]

            if (pendingConfidence == 0f) {
                pendingConfidence =
                    0.98f
            }

            pendingTopPredictions =
                buildManualCorrectionPredictions(
                    normalizedFoodName
                )

            saveCorrectionIfNeeded(
                normalizedFoodName
            )

            currentStep =
                "mixed_food"

            confirmWeight(
                weight
            )

            return
        }

        if (
            currentStep == "mixed_food" &&
            selectionState.selectedSideDish != null
        ) {

            selectionState.selectedMeat =
                normalizedFoodName

            if (pendingConfidence == 0f) {
                pendingConfidence =
                    0.98f
            }

            pendingTopPredictions =
                buildManualCorrectionPredictions(
                    "${selectionState.selectedSideDish} + $normalizedFoodName"
                )

            saveCorrectionIfNeeded(
                "${selectionState.selectedSideDish} + $normalizedFoodName"
            )

            confirmWeight(
                weight
            )

            return
        }

        val nutrition =
            nutritionCalculator.calculate(
                normalizedFoodName
            )

        if (nutrition == null) {

            requestCustomNutritionInput(
                foodName = normalizedFoodName,
                weight = weight
            )

            return
        }

        pendingSimpleFoodName =
            nutrition.name

        saveCorrectionIfNeeded(
            nutrition.name
        )

        currentStep =
            "simple_food"

        if (pendingConfidence == 0f) {
            pendingConfidence = 0.92f
        }

        pendingTopPredictions =
            buildManualCorrectionPredictions(
                nutrition.name
            )

        confirmWeight(
            weight
        )
    }

    private fun publishResultWithoutNutritionMatch(
        foodName: String,
        weight: Int
    ) {

        currentStep =
            "simple_food"

        val tempResult =
            AnalysisResult(
                name = foodName,
                calories = 0,
                proteins = 0f,
                fats = 0f,
                carbs = 0f,
                imageUri = currentImageUri,
                weight = weight,
                confidence = pendingConfidence,
                topPredictions = buildTopPredictions(
                    predictions = pendingTopPredictions,
                    fallbackFoodName = foodName,
                    confidence = pendingConfidence
                )
            )

        val finalResult =
            tempResult.copy(
                aiSummary =
                    "Блюдо распознано как \"$foodName\". Для расчета КБЖУ добавьте это название в локальную базу продуктов."
            )

        _result.postValue(
            finalResult
        )

        resetCurrentSelection(
            keepImageUri = true
        )
    }

    private fun requestCustomNutritionInput(
        foodName: String,
        weight: Int
    ) {

        _needCustomNutritionInput.postValue(
            CustomNutritionRequest(
                foodName = foodName,
                weight = weight
            )
        )
    }

    private fun saveCorrectionIfNeeded(
        correctedFoodName: String
    ) {

        val imageHash =
            currentImageHash ?: return

        correctionCache
            .edit()
            .putString(
                imageHash,
                correctedFoodName
            )
            .apply()

        Log.d(
            "AI_CORRECTION_CACHE",
            "Saved correction: ${pendingModelFoodName.orEmpty()} -> $correctedFoodName"
        )
    }

    // ==================================================
    // SAVE
    // ==================================================

    fun save(
        result: AnalysisResult,
        onComplete: ((Boolean) -> Unit)? = null
    ) {

        viewModelScope.launch {

            val saved =
                repository.saveAnalysis(
                result
            )

            if (saved) {
                loadHistory()
            }

            onComplete?.invoke(
                saved
            )
        }
    }

    // ==================================================
    // HISTORY
    // ==================================================

    fun loadHistory() {

        historyJob?.cancel()

        historyJob =
            viewModelScope.launch {

            repository.getHistory()
                .collect { historyList ->

                    _history.value =
                        historyList
                }
        }
    }

    fun deleteAnalysis(
        result: AnalysisResult
    ) {

        val id =
            result.id

        if (id == null) {

            Log.e(
                "AI",
                "Cannot delete: id is null"
            )

            return
        }

        viewModelScope.launch {

            repository.deleteAnalysis(id)

            loadHistory()
        }
    }

    // ==================================================
    // HELPERS
    // ==================================================

    private fun calculateAverageConfidence(
        detections: List<com.example.cookbookai.network.model.Detection>
    ): Float {

        if (detections.isEmpty()) {
            return 0f
        }

        return detections
            .map { it.confidence }
            .average()
            .toFloat()
    }

    private fun calculateImageHash(
        bytes: ByteArray
    ): String {

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private fun formatTopPredictions(
        response: PredictionResponse
    ): List<String> {

        return response.topPredictions
            .mapNotNull { prediction ->
                val name =
                    prediction.displayName()

                if (name.isBlank()) {
                    null
                } else {
                    "$name — ${(prediction.confidence * 100).toInt()}%"
                }
            }
    }

    private fun TopPrediction.displayName(): String {

        return name
            .ifBlank {
                className.orEmpty()
            }
            .trim()
    }

    private fun requestClarification(
        customTitle: String = "Уточните блюдо",
        customOptions: List<String> = listOf(
            "Курица",
            "Говядина",
            "Рис",
            "Гречка",
            "Картофель",
            "Овощной салат",
            "Борщ",
            "Омлет"
        )
    ) {

        currentStep =
            "simple_food"

        _needUserChoice.postValue(
            MappingResult(
                foodName = "",
                needUserChoice = true,
                options = customOptions,
                detectedClasses = listOf(customTitle)
            )
        )
    }

    private fun requestMappedChoice(
        mapped: MappingResult
    ) {

        if (mapped.options.isEmpty()) {
            return
        }

        currentStep =
            if (mapped.step == SelectionStep.SIDE_DISH) {
                "side_dish"
            } else {
                "simple_food"
            }

        _needUserChoice.postValue(
            mapped
        )
    }

    private fun requestMappedChoiceWithCandidate(
        mapped: MappingResult,
        candidate: String?
    ) {

        val options =
            (
                    listOfNotNull(
                        candidate
                            ?.trim()
                            ?.takeIf { it.isNotBlank() && !it.contains("Неизвест", true) }
                    ) + mapped.options
                    )
                .distinct()

        requestMappedChoice(
            mapped.copy(
                options = options
            )
        )
    }

    private fun requestManualFoodInput(
        title: String = "Введите название блюда"
    ) {

        currentStep =
            "simple_food"

        _needManualFoodInput.postValue(
            title
        )
    }

    private fun shouldAskForIngredients(
        foodName: String
    ): Boolean {

        val normalized =
            foodName
                .lowercase()
                .replace("ё", "е")

        val exactDishes =
            setOf(
                "оливье",
                "винегрет",
                "цезарь",
                "греческий салат",
                "крабовый салат",
                "мимоза",
                "салат с курицей",
                "овощной салат",
                "фруктовый салат",
                "фруктовая тарелка",
                "овощная тарелка",
                "поке",
                "боул",
                "шаурма"
            )

        if (exactDishes.any { normalized == it }) {
            return true
        }

        return listOf(
            "салат",
            "оливье",
            "винегрет",
            "цезарь",
            "мимоза",
            "поке",
            "боул",
            "шаурм"
        ).any {
            normalized.contains(
                it
            )
        }
    }

    private fun showAnalysisError(
        message: String
    ) {

        _errorMessage.postValue(
            message
        )
    }

    private fun networkErrorMessage(
        error: Exception
    ): String {

        return when (error) {
            is SocketTimeoutException ->
                "Сервер долго отвечает. Если модель только запустилась, подождите 1-2 минуты и повторите анализ."

            is ConnectException ->
                "Не удалось подключиться к ML-серверу. Проверьте, что сервер запущен и адрес указан правильно."

            is UnknownHostException ->
                "Адрес ML-сервера не найден. Проверьте IP-адрес в профиле приложения."

            else ->
                "Не удалось выполнить анализ. Проверьте подключение к ML-серверу и попробуйте снова."
        }
    }

    private fun resolveRecognizedFoodName(
        response: PredictionResponse,
        mapped: MappingResult
    ): String? {

        val candidates =
            buildList {
                if (response.mode == "multi_food") {
                    bestMultiFoodTopPrediction(response)?.let {
                        add(it)
                    }

                    componentsDishName(response)?.let {
                        add(it)
                    }

                    response.dish?.let {
                        add(it)
                    }
                } else {
                    response.dish?.let {
                        add(it)
                    }
                }

                response.topPredictions.forEach {
                    it.displayName()
                        .takeIf { name -> name.isNotBlank() }
                        ?.let { name ->
                            add(name)
                        }

                    it.className?.let { className ->
                        add(className)
                    }
                }

                response.className?.let {
                    add(it)
                }

                mapped.foodName
                    .takeIf { it.isNotBlank() && !it.contains("Неизвест", true) }
                    ?.let {
                        add(it)
                    }
            }

        return chooseBestFoodCandidate(
            candidates = candidates,
            preferMixedDish = hasMixedDishSignal(
                response,
                mapped
            )
        )
    }

    private fun resolveDisplayFoodName(
        response: PredictionResponse,
        mapped: MappingResult
    ): String? {

        val candidates = buildList {
            if (response.mode == "multi_food") {
                bestMultiFoodTopPrediction(response)?.let {
                    add(it)
                }

                componentsDishName(response)?.let {
                    add(it)
                }

                response.dish?.let {
                    add(it)
                }
            } else {
                response.dish?.let {
                    add(it)
                }
            }

            response.topPredictions.forEach {
                it.displayName()
                    .takeIf { name -> name.isNotBlank() }
                    ?.let { name ->
                        add(name)
                    }

                it.className?.let { className ->
                    add(className)
                }
            }

            response.className?.let {
                add(it)
            }

            mapped.foodName
                .takeIf { it.isNotBlank() && !it.contains("Неизвест", true) }
                ?.let {
                    add(it)
                }

        }

        return chooseBestFoodCandidate(
            candidates = candidates,
            preferMixedDish = hasMixedDishSignal(
                response,
                mapped
            )
        )
    }

    private fun chooseBestFoodCandidate(
        candidates: List<String>,
        preferMixedDish: Boolean = false
    ): String? {

        val cleanedCandidates =
            candidates
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.contains("Неизвест", true) }
                .distinct()

        val mixedCandidate =
            cleanedCandidates.firstOrNull {
                !shouldAskForIngredients(it) &&
                        parseMixedDishParts(it).size >= 2
            }

        if (
            preferMixedDish &&
            mixedCandidate != null
        ) {
            return mixedCandidate
        }

        return cleanedCandidates.firstOrNull {
            nutritionCalculator.calculate(it) != null
        } ?: mixedCandidate
        ?: cleanedCandidates.firstOrNull()
    }

    private fun hasMixedDishSignal(
        response: PredictionResponse,
        mapped: MappingResult
    ): Boolean {

        if (response.mode == "multi_food") {
            return true
        }

        if (response.components.size >= 2) {
            return true
        }

        if (
            response.topPredictions.any {
                parseMixedDishParts(
                    it.displayName()
                ).size >= 2
            }
        ) {
            return true
        }

        return mapped.detectedClasses.contains("side_dish") &&
                (
                        mapped.detectedClasses.contains("meat") ||
                                mapped.detectedClasses.contains("fish")
                        )
    }

    private fun bestMultiFoodTopPrediction(
        response: PredictionResponse
    ): String? {

        return response.topPredictions
            .filter { prediction ->
                prediction.displayName().contains("+")
            }
            .maxByOrNull { prediction ->
                prediction.confidence
            }
            ?.displayName()
            ?.takeIf { it.isNotBlank() }
    }

    private fun componentsDishName(
        response: PredictionResponse
    ): String? {

        val names =
            response.components
                .map { it.displayName() }
                .filter { it.isNotBlank() }
                .distinct()

        if (names.size < 2) {
            return null
        }

        return names.joinToString(" + ")
    }

    private fun detectionDishName(
        response: PredictionResponse
    ): String? {

        val componentNames =
            response.detections
                .filter { it.confidence >= 0.35f }
                .mapNotNull {
                    detectionClassToFoodName(
                        it.`class`
                    )
                }
                .distinct()

        if (componentNames.size < 2) {
            return null
        }

        val sideDish =
            componentNames.firstOrNull {
                nutritionCalculator.calculate(
                    it
                )?.category == "side_dish"
            }

        val protein =
            componentNames.firstOrNull {
                nutritionCalculator.calculate(
                    it
                )?.category in setOf(
                    "meat",
                    "fish"
                )
            }

        if (
            sideDish == null ||
            protein == null
        ) {
            return null
        }

        return "$sideDish + $protein"
    }

    private fun detectionClassToFoodName(
        value: String
    ): String? {

        val normalized =
            value
                .lowercase()
                .replace("ё", "е")
                .replace("-", "_")
                .replace(" ", "_")
                .trim()

        return when (normalized) {
            "buckwheat", "grechka", "гречка" -> "Гречка"
            "rice", "рис" -> "Рис"
            "pasta", "macaroni", "макароны", "паста" -> "Макароны"
            "potato", "potatoes", "картофель", "картошка" -> "Картофель"
            "mashed_potato", "puree", "пюре", "картофельное_пюре" -> "Картофельное пюре"
            "bulgur", "булгур" -> "Булгур"
            "chicken", "курица", "куриная_грудка", "chicken_breast" -> "Курица"
            "pork", "свинина" -> "Свинина"
            "beef", "говядина" -> "Говядина"
            "turkey", "индейка" -> "Индейка"
            "cutlet", "котлета" -> "Котлета"
            "fish", "fried_fish", "рыба", "жареная_рыба" -> "Жареная рыба"
            "salmon", "лосось" -> "Лосось"
            "pollock", "минтай" -> "Минтай"
            else -> null
        }
    }

    private fun buildClarificationOptions(
        recognizedFoodName: String,
        mapped: MappingResult
    ): List<String> {

        return (
                listOf(recognizedFoodName) +
                        mapped.options +
                        listOf(
                            "Куриный суп",
                            "Гречка + Курица",
                            "Рис + Курица",
                            "Картофель + Свинина"
                        )
                )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun parseMixedDishParts(
        foodName: String
    ): List<String> {

        return foodName
            .replace(" with ", " + ", ignoreCase = true)
            .replace(" и ", " + ", ignoreCase = true)
            .replace(" с ", " + ", ignoreCase = true)
            .replace(" со ", " + ", ignoreCase = true)
            .split("+")
            .map { it.trim() }
            .map { normalizeMixedDishPart(it) }
            .filter { it.isNotBlank() }
    }

    private fun looksLikeMixedDishName(
        foodName: String
    ): Boolean {

        val normalized =
            " ${foodName.lowercase().replace("ё", "е")} "

        return normalized.contains("+") ||
                normalized.contains(" с ") ||
                normalized.contains(" со ") ||
                normalized.contains(" и ") ||
                normalized.contains(" with ")
    }

    private fun normalizeMixedDishPart(
        value: String
    ): String {

        return when (value.lowercase().replace("ё", "е")) {
            "курицей", "куриная грудка", "куриное филе", "chicken" -> "Курица"
            "говядиной", "beef" -> "Говядина"
            "свининой", "pork" -> "Свинина"
            "котлетой", "котлета" -> "Котлета"
            "сосиской", "сосисками" -> "Сосиски"
            "рыбой", "fish" -> "Жареная рыба"
            "рисом", "rice" -> "Рис"
            "гречкой", "buckwheat" -> "Гречка"
            "макаронами", "пастой", "pasta" -> "Макароны"
            "картофелем", "картошкой" -> "Картофель"
            "пюре" -> "Картофельное пюре"
            "овощами", "vegetables" -> "Тушеные овощи"
            else -> value.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase()
                } else {
                    char.toString()
                }
            }
        }
    }

    private fun buildTopPredictions(
        predictions: List<String>,
        fallbackFoodName: String,
        confidence: Float
    ): List<String> {

        if (predictions.isNotEmpty()) {
            return predictions
        }

        val percent =
            ((confidence.takeIf { it > 0f } ?: 0.92f) * 100)
                .toInt()

        return listOf(
            "$fallbackFoodName — $percent%",
            "Похожее блюдо — 6%",
            "Другое блюдо — 2%"
        )
    }

    private fun buildManualCorrectionPredictions(
        correctedFoodName: String
    ): List<String> {

        return listOf(
            "$correctedFoodName — исправлено пользователем",
            "Предыдущее предсказание модели заменено",
            "КБЖУ рассчитаны по исправленному названию"
        )
    }

    private fun List<String>.withMixedDishHintIfNeeded(
        foodName: String
    ): List<String> {

        val category =
            nutritionCalculator.calculate(
                foodName
            )?.category

        val canBePartOfMixedDish =
            category in setOf(
                "meat",
                "fish",
                "side_dish"
            )

        if (!canBePartOfMixedDish) {
            return this
        }

        if (
            any {
                it.contains(
                    "состав",
                    ignoreCase = true
                )
            }
        ) {
            return this
        }

        return (
                this + "Возможно составное блюдо — добавьте второй компонент"
                )
            .distinct()
    }

    private fun resetCurrentSelection(
        keepImageUri: Boolean = true
    ) {

        selectionState.selectedSideDish =
            null

        selectionState.selectedMeat =
            null

        selectionState.selectedWeight =
            300

        pendingSimpleFoodName =
            null

        pendingConfidence =
            0f

        pendingTopPredictions =
            emptyList()

        pendingModelFoodName =
            null

        currentStep =
            ""

        if (!keepImageUri) {
            currentImageUri = null
        }
    }
}
