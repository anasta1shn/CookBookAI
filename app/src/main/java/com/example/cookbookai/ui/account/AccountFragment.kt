package com.example.cookbookai.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cookbookai.R
import com.example.cookbookai.data.remote.SupabaseManager
import com.example.cookbookai.databinding.FragmentAccountBinding
import com.example.cookbookai.network.RetrofitClient
import com.example.cookbookai.network.ServerSettings
import com.example.cookbookai.ui.auth.LoginActivity
import com.example.cookbookai.ui.favorites.FavoritesFragment
import com.example.cookbookai.ui.history.HistoryFragment
import com.example.cookbookai.viewmodel.RecipeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.*

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by activityViewModels()

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadAvatar(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        updateServerAddressText()
        observeRecipes()
        setupThemeButton()
        setupLogout()
        setupButtons()

        binding.profileImage.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun loadProfile() {
        val user = SupabaseManager.client.auth.currentUserOrNull()
        binding.profileEmail.text = user?.email ?: ""
        val avatarUrl = user?.userMetadata?.get("avatar_url")
        avatarUrl?.let { binding.profileImage.load(it.toString()) }
    }

    private fun uploadAvatar(uri: Uri) {
        lifecycleScope.launch {
            val bytes = requireContext().contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
            val fileName = "${UUID.randomUUID()}.jpg"
            SupabaseManager.client.storage.from("avatars").upload(fileName, bytes)
            val publicUrl = SupabaseManager.client.storage.from("avatars").publicUrl(fileName)
            binding.profileImage.load(publicUrl)
            SupabaseManager.client.auth.updateUser {
                data = buildJsonObject { put("avatar_url", publicUrl) }
            }
        }
    }

    private fun observeRecipes() {
        lifecycleScope.launch {
            viewModel.filteredRecipes.collect { recipes ->
                val total = recipes.size
                val favorites = recipes.count { it.isFavorite }
                val favoriteCategory = recipes.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "—"
                binding.profileStats.text = "Рецептов: $total   Избранных: $favorites"
                binding.profileCategory.text = "Любимая категория: $favoriteCategory"
            }
        }
    }

    private fun setupThemeButton() {
        binding.btnTheme.setOnClickListener {
            val items = arrayOf("Светлая", "Тёмная", "Системная")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Тема приложения")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                }.show()
        }
    }

    private fun updateServerAddressText() {

        binding.textServerAddress.text =
            ServerSettings.getServerUrl()
    }

    private fun setupServerAddressButton() {

        binding.btnServerAddress.setOnClickListener {

            val input =
                EditText(requireContext()).apply {
                    inputType =
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_URI

                    setSingleLine(true)
                    setText(
                        ServerSettings.getServerUrl()
                    )
                    hint =
                        "http://192.168.43.10:8000/"
                    selectAll()
                }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Адрес сервера")
                .setMessage("Подключите телефон и компьютер с сервером к одной точке доступа. Введите локальный адрес, например 192.168.43.10:8000")
                .setView(input)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create()
                .also { dialog ->

                    dialog.setOnShowListener {

                        dialog.getButton(
                            android.app.AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener {

                            val rawUrl =
                                input.text
                                    .toString()
                                    .trim()

                            if (!ServerSettings.isValidUrl(rawUrl)) {

                                input.error =
                                    "Введите локальный адрес сервера"

                                return@setOnClickListener
                            }

                            if (!ServerSettings.isHotspotSafeAddress(rawUrl)) {

                                input.error =
                                    "Используйте адрес из сети точки доступа"

                                return@setOnClickListener
                            }

                            ServerSettings.setServerUrl(rawUrl)
                            RetrofitClient.reset()
                            updateServerAddressText()

                            Toast.makeText(
                                requireContext(),
                                "Адрес сервера сохранён",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()
                        }
                    }
                }
                .show()
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    lifecycleScope.launch {
                        try {

                            SupabaseManager.client.auth.signOut()

                        } finally {

                            val intent =
                                Intent(
                                    requireContext(),
                                    LoginActivity::class.java
                                ).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }

                            startActivity(intent)
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun setupButtons() {
        binding.btnFavorites.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, FavoritesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnStats.setOnClickListener { showStatsDialog() }

        setupServerAddressButton()

        binding.btnHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun showStatsDialog() {
        lifecycleScope.launch {
            val recipes = viewModel.filteredRecipes.first()
            val total = recipes.size
            val favorites = recipes.count { it.isFavorite }
            val categoryCounts = recipes.groupingBy { it.category }.eachCount()

            val avgCalories = recipes.map { it.calories.toDouble() }.average()
            val avgProteins = recipes.map { it.proteins.toDouble() }.average()
            val avgFats = recipes.map { it.fats.toDouble() }.average()
            val avgCarbs = recipes.map { it.carbs.toDouble() }.average()

            val statsText = buildString {
                appendLine("📊 Всего рецептов: $total")
                appendLine("⭐ Избранных: $favorites")
                appendLine("🍳 Рецептов по категориям:")
                categoryCounts.forEach { (cat, count) -> appendLine("$cat: $count") }
                appendLine("⚡ Средние КБЖУ:")
                appendLine("Калории: ${"%.1f".format(avgCalories)}")
                appendLine("Белки: ${"%.1f".format(avgProteins)}")
                appendLine("Жиры: ${"%.1f".format(avgFats)}")
                appendLine("Углеводы: ${"%.1f".format(avgCarbs)}")
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ваша статистика")
                .setMessage(statsText)
                .setPositiveButton("Ок", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
