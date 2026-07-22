package com.example.cookbookai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.data.remote.SupabaseManager
import com.example.cookbookai.databinding.ActivityRegisterBinding
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityRegisterBinding

    private var pendingEmail: String? =
        null

    private var pendingPassword: String? =
        null

    private var isCodeStep =
        false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityRegisterBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        binding.btnRegister.setOnClickListener {

            if (isCodeStep) {
                verifyCode()
            } else {
                registerUser()
            }
        }

        binding.tvLogin.setOnClickListener {

            finish()
        }

        binding.tvResendCode.setOnClickListener {

            resendCode()
        }
    }

    private fun registerUser() {

        val email =
            binding.etEmail.text
                ?.toString()
                ?.trim()
                ?: ""

        val password =
            binding.etPassword.text
                ?.toString()
                ?.trim()
                ?: ""

        val confirmPassword =
            binding.etConfirmPassword.text
                ?.toString()
                ?.trim()
                ?: ""

        clearErrors()

        if (email.isBlank()) {

            binding.tilEmail.error =
                "Введите email"

            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            binding.tilEmail.error =
                "Некорректный email"

            return
        }

        if (password.isBlank()) {

            binding.tilPassword.error =
                "Введите пароль"

            return
        }

        if (password.length < 6) {

            binding.tilPassword.error =
                "Пароль должен быть не меньше 6 символов"

            return
        }

        if (confirmPassword.isBlank()) {

            binding.tilConfirmPassword.error =
                "Повторите пароль"

            return
        }

        if (password != confirmPassword) {

            binding.tilConfirmPassword.error =
                "Пароли не совпадают"

            return
        }

        binding.btnRegister.isEnabled =
            false

        binding.btnRegister.text =
            "Регистрация..."

        lifecycleScope.launch {

            try {

                SupabaseManager.client.auth.signInWith(OTP) {

                    this.email =
                        email

                    this.createUser =
                        true
                }

                SupabaseManager.client.auth.signOut()

                pendingEmail =
                    email

                pendingPassword =
                    password

                showCodeStep()

                Toast.makeText(
                    this@RegisterActivity,
                    "Код подтверждения отправлен на $email",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@RegisterActivity,
                    "Ошибка регистрации: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                binding.btnRegister.isEnabled =
                    true

                binding.btnRegister.text =
                    "Зарегистрироваться"
            }
        }
    }

    private fun verifyCode() {

        val email =
            pendingEmail ?: return

        val code =
            binding.etCode.text
                ?.toString()
                ?.trim()
                ?: ""

        binding.tilCode.error =
            null

        if (code.length < 6) {

            binding.tilCode.error =
                "Введите 6-значный код"

            return
        }

        binding.btnRegister.isEnabled =
            false

        binding.btnRegister.text =
            "Проверяем..."

        lifecycleScope.launch {

            try {

                SupabaseManager.client.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = email,
                    token = code
                )

                val password =
                    pendingPassword

                if (password != null) {

                    SupabaseManager.client.auth.updateUser {
                        this.password =
                            password
                    }
                }

                SupabaseManager.client.auth.signOut()

                Toast.makeText(
                    this@RegisterActivity,
                    "Почта подтверждена. Теперь можно войти.",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(
                        this@RegisterActivity,
                        LoginActivity::class.java
                    )
                )

                finish()

            } catch (e: Exception) {

                binding.tilCode.error =
                    "Неверный или устаревший код"

                binding.btnRegister.isEnabled =
                    true

                binding.btnRegister.text =
                    "Подтвердить код"
            }
        }
    }

    private fun resendCode() {

        val email =
            pendingEmail ?: return

        lifecycleScope.launch {

            try {

                SupabaseManager.client.auth.signInWith(OTP) {
                    this.email =
                        email

                    this.createUser =
                        true
                }

                Toast.makeText(
                    this@RegisterActivity,
                    "Код отправлен повторно",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@RegisterActivity,
                    "Не удалось отправить код: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showCodeStep() {

        isCodeStep =
            true

        binding.tilEmail.isEnabled =
            false

        binding.tilPassword.isEnabled =
            false

        binding.tilConfirmPassword.isEnabled =
            false

        binding.tvCodeHint.visibility =
            View.VISIBLE

        binding.tilCode.visibility =
            View.VISIBLE

        binding.tvResendCode.visibility =
            View.VISIBLE

        binding.btnRegister.isEnabled =
            true

        binding.btnRegister.text =
            "Подтвердить код"
    }

    private fun clearErrors() {

        binding.tilEmail.error =
            null

        binding.tilPassword.error =
            null

        binding.tilConfirmPassword.error =
            null

        binding.tilCode.error =
            null
    }
}
