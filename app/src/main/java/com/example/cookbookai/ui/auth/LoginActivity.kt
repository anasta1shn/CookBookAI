package com.example.cookbookai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cookbookai.MainActivity
import com.example.cookbookai.databinding.ActivityLoginBinding
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@OptIn(kotlin.time.ExperimentalTime::class)
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentUser =
            SupabaseManager.client.auth.currentUserOrNull()

        if (
            SupabaseManager.client.auth.currentSessionOrNull() != null &&
            currentUser?.emailConfirmedAt != null
        ) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    SupabaseManager.client.auth.signInWith(Email) {
                        this.email =
                            email

                        this.password =
                            password
                    }

                    val user =
                        SupabaseManager.client.auth.currentUserOrNull()

                    if (user?.emailConfirmedAt == null) {

                        SupabaseManager.client.auth.signOut()

                        Toast.makeText(
                            this@LoginActivity,
                            "Подтвердите email по ссылке из письма, затем войдите снова",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Ошибка входа: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }



        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
