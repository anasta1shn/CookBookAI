package com.example.cookbookai.ui.barcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.R
import com.example.cookbookai.data.local.BarcodeProductDatabaseManager
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.data.model.BarcodeProduct
import com.example.cookbookai.databinding.FragmentBarcodeScannerBinding
import com.example.cookbookai.ui.ai.AnalysisDetailFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private const val NUTRITION_BASE_WEIGHT = 100

class BarcodeScannerFragment : Fragment() {

    private var _binding: FragmentBarcodeScannerBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    private var isBarcodeProcessed =
        false

    private var lastUnknownBarcode: String? =
        null

    private var lastCandidateBarcode: String? =
        null

    private var candidateScanCount =
        0

    private val requiredStableScans =
        2

    private var camera: Camera? =
        null

    private var torchEnabled =
        false

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                startCamera()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Разрешение камеры не выдано",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBarcodeScannerBinding.inflate(
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

        cameraExecutor =
            Executors.newSingleThreadExecutor()

        binding.buttonBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.buttonTorch.setOnClickListener {

            toggleTorch()
        }

        binding.buttonAddProduct.setOnClickListener {

            val barcode =
                lastUnknownBarcode

            if (barcode == null) {

                Toast.makeText(
                    requireContext(),
                    "Сначала отсканируйте штрихкод",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            showAddProductDialog(
                barcode
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            BarcodeProductDatabaseManager.loadSavedProducts(requireContext())
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            startCamera()

        } else {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(
                requireContext()
            )

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(
                            binding.previewView.surfaceProvider
                        )
                    }

            val imageAnalyzer =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()
                    .also {

                        it.setAnalyzer(
                            cameraExecutor
                        ) { imageProxy ->

                            processImageProxy(
                                imageProxy
                            )
                        }
                    }

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            try {

                cameraProvider.unbindAll()

                camera =
                    cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                binding.textStatus.text =
                    "Поместите штрихкод внутрь рамки"

            } catch (e: Exception) {

                Log.e(
                    "BarcodeScanner",
                    "Camera bind error: ${e.message}"
                )
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy
    ) {

        if (isBarcodeProcessed) {

            imageProxy.close()
            return
        }

        val mediaImage =
            imageProxy.image

        if (mediaImage == null) {

            imageProxy.close()
            return
        }

        val image =
            InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

        val options =
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                .build()

        val scanner =
            BarcodeScanning.getClient(
                options
            )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->

                val barcode =
                    barcodes
                        .firstOrNull()
                        ?.rawValue

                if (
                    barcode != null &&
                    !isBarcodeProcessed &&
                    shouldAcceptBarcode(barcode)
                ) {

                    isBarcodeProcessed =
                        true

                    requireActivity()
                        .runOnUiThread {

                            handleBarcode(
                                barcode
                            )
                        }
                }
            }
            .addOnFailureListener { e ->

                Log.e(
                    "BarcodeScanner",
                    "Scan error: ${e.message}"
                )
            }
            .addOnCompleteListener {

                imageProxy.close()
            }
    }

    private fun handleBarcode(
        barcode: String
    ) {

        val cleanBarcode =
            normalizeBarcode(
                barcode
            )

        binding.textStatus.text =
            "Найден штрихкод: $cleanBarcode"

        binding.buttonAddProduct.visibility =
            View.GONE

        val product =
            BarcodeProductDatabaseManager
                .findByBarcode(
                    cleanBarcode
                )

        if (product == null) {

            if (lastUnknownBarcode != cleanBarcode) {

                Toast.makeText(
                    requireContext(),
                    "Продукт не найден в базе",
                    Toast.LENGTH_SHORT
                ).show()
            }

            lastUnknownBarcode =
                cleanBarcode

            binding.textStatus.text =
                "Штрихкод $cleanBarcode не найден.\nМожно добавить продукт или навести камеру на другой товар."

            binding.buttonAddProduct.visibility =
                View.VISIBLE

            isBarcodeProcessed = false

            return
        }

        showProductWeightDialog(
            product,
            cleanBarcode,
            "Данные получены по штрихкоду продукта"
        )
    }

    private fun showProductWeightDialog(
        product: BarcodeProduct,
        barcode: String,
        summary: String
    ) {

        val weightInput =
            createInput(
                hint = "Вес порции, г/мл",
                inputType = InputType.TYPE_CLASS_NUMBER
            ).apply {
                setText(
                    product.weight.toString()
                )
            }

        val message =
            "${product.name}\nКБЖУ указаны на $NUTRITION_BASE_WEIGHT г/мл. Введите фактический вес порции."

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Уточните вес")
            .setMessage(message)
            .setView(weightInput)
            .setPositiveButton("Рассчитать", null)
            .setNegativeButton("Отмена") { _, _ ->
                isBarcodeProcessed =
                    false

                binding.textStatus.text =
                    "Поместите штрихкод внутрь рамки"
            }
            .create()
            .also { dialog ->

                dialog.setOnShowListener {

                    dialog.getButton(
                        android.app.AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val selectedWeight =
                            weightInput.text
                                .toString()
                                .trim()
                                .toIntOrNull()

                        if (
                            selectedWeight == null ||
                            selectedWeight <= 0
                        ) {

                            Toast.makeText(
                                requireContext(),
                                "Введите вес больше 0",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setOnClickListener
                        }

                        dialog.dismiss()

                        openProduct(
                            product = product,
                            barcode = barcode,
                            selectedWeight = selectedWeight,
                            summary = summary
                        )
                    }
                }

                dialog.setOnCancelListener {
                    isBarcodeProcessed =
                        false

                    binding.textStatus.text =
                        "Поместите штрихкод внутрь рамки"
                }
            }
            .show()
    }

    private fun openProduct(
        product: BarcodeProduct,
        barcode: String,
        selectedWeight: Int,
        summary: String
    ) {

        val multiplier =
            selectedWeight / NUTRITION_BASE_WEIGHT.toFloat()

        val result =
            AnalysisResult(
                name =
                    product.name,

                calories =
                    (product.calories * multiplier).toInt(),

                proteins =
                    product.proteins * multiplier,

                fats =
                    product.fats * multiplier,

                carbs =
                    product.carbs * multiplier,

                weight =
                    selectedWeight,

                confidence =
                    1.0f,

                aiSummary =
                    summary,

                topPredictions =
                    listOf(
                        "Штрихкод: $barcode",
                        "${product.name} — 100%",
                        "Порция: $selectedWeight г/мл"
                    )
            )

        openAnalysisDetail(
            result
        )
    }

    private fun openAnalysisDetail(
        result: AnalysisResult
    ) {

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

    private fun showAddProductDialog(
        barcode: String
    ) {

        isBarcodeProcessed =
            true

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

        val nameInput =
            createInput(
                hint = "Название продукта",
                inputType = InputType.TYPE_CLASS_TEXT
            )

        val caloriesInput =
            createInput(
                hint = "Ккал на 100 г/мл",
                inputType = InputType.TYPE_CLASS_NUMBER
            )

        val proteinsInput =
            createInput(
                hint = "Белки на 100 г/мл",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val fatsInput =
            createInput(
                hint = "Жиры на 100 г/мл",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val carbsInput =
            createInput(
                hint = "Углеводы на 100 г/мл",
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            )

        val weightInput =
            createInput(
                hint = "Вес порции, г/мл (обычно 100)",
                inputType = InputType.TYPE_CLASS_NUMBER
            ).apply {
                setText("100")
            }

        listOf(
            nameInput,
            caloriesInput,
            proteinsInput,
            fatsInput,
            carbsInput,
            weightInput
        ).forEach { input ->
            container.addView(
                input
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить продукт")
            .setMessage("Штрихкод: $barcode")
            .setView(container)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена") { _, _ ->
                isBarcodeProcessed =
                    false

                binding.buttonAddProduct.visibility =
                    View.GONE

                binding.textStatus.text =
                    "Поместите штрихкод внутрь рамки"
            }
            .create()
            .also { dialog ->

                dialog.setOnShowListener {

                    dialog.getButton(
                        android.app.AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener {

                        val product =
                            buildProductFromInputs(
                                barcode = barcode,
                                nameInput = nameInput,
                                caloriesInput = caloriesInput,
                                proteinsInput = proteinsInput,
                                fatsInput = fatsInput,
                                carbsInput = carbsInput,
                                weightInput = weightInput
                            )

                        if (product == null) {

                            Toast.makeText(
                                requireContext(),
                                "Заполните название и все КБЖУ",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setOnClickListener
                        }

                        viewLifecycleOwner.lifecycleScope.launch {

                            BarcodeProductDatabaseManager.addProduct(
                                requireContext(),
                                product
                            )

                            Toast.makeText(
                                requireContext(),
                                "Продукт добавлен",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()

                            openProduct(
                                product = product,
                                barcode = barcode,
                                selectedWeight = product.weight,
                                summary = "Продукт добавлен пользователем по штрихкоду"
                            )
                        }
                    }
                }

                dialog.setOnCancelListener {
                    isBarcodeProcessed =
                        false

                    binding.buttonAddProduct.visibility =
                        View.GONE

                    binding.textStatus.text =
                        "Поместите штрихкод внутрь рамки"
                }
            }
            .show()
    }

    private fun shouldAcceptBarcode(
        barcode: String
    ): Boolean {

        val cleanBarcode =
            normalizeBarcode(
                barcode
            )

        if (cleanBarcode.isBlank()) {
            return false
        }

        if (lastCandidateBarcode == cleanBarcode) {

            candidateScanCount++

        } else {

            lastCandidateBarcode =
                cleanBarcode

            candidateScanCount =
                1
        }

        return candidateScanCount >= requiredStableScans
    }

    private fun normalizeBarcode(
        value: String
    ): String {

        return value
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("\n", "")
            .replace("\r", "")
    }

    private fun createInput(
        hint: String,
        inputType: Int
    ): EditText {

        return EditText(requireContext()).apply {
            this.hint =
                hint
            this.inputType =
                inputType
            setSingleLine(true)
        }
    }

    private fun buildProductFromInputs(
        barcode: String,
        nameInput: EditText,
        caloriesInput: EditText,
        proteinsInput: EditText,
        fatsInput: EditText,
        carbsInput: EditText,
        weightInput: EditText
    ): BarcodeProduct? {

        val name =
            nameInput.text
                .toString()
                .trim()

        val calories =
            caloriesInput.text
                .toString()
                .trim()
                .toFloatOrNull()

        val proteins =
            proteinsInput.text
                .toString()
                .trim()
                .replace(',', '.')
                .toFloatOrNull()

        val fats =
            fatsInput.text
                .toString()
                .trim()
                .replace(',', '.')
                .toFloatOrNull()

        val carbs =
            carbsInput.text
                .toString()
                .trim()
                .replace(',', '.')
                .toFloatOrNull()

        val weight =
            weightInput.text
                .toString()
                .trim()
                .toIntOrNull()
                ?: 100

        if (
            name.isBlank() ||
            calories == null ||
            proteins == null ||
            fats == null ||
            carbs == null
        ) {
            return null
        }

        return BarcodeProduct(
            barcode = barcode,
            name = name,
            calories = calories,
            proteins = proteins,
            fats = fats,
            carbs = carbs,
            weight = weight
        )
    }

    private fun toggleTorch() {

        val cameraControl =
            camera?.cameraControl

        val cameraInfo =
            camera?.cameraInfo

        if (
            cameraControl == null ||
            cameraInfo == null ||
            !cameraInfo.hasFlashUnit()
        ) {

            Toast.makeText(
                requireContext(),
                "Фонарик недоступен",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        torchEnabled =
            !torchEnabled

        cameraControl.enableTorch(
            torchEnabled
        )

        binding.buttonTorch.alpha =
            if (torchEnabled) 1f else 0.78f
    }

    override fun onDestroyView() {

        super.onDestroyView()

        cameraExecutor.shutdown()

        _binding = null
    }
}
