package com.example.cookbookai.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cookbookai.MainActivity
import com.example.cookbookai.data.remote.SupabaseManager
import com.example.cookbookai.databinding.ActivitySplashBinding
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(kotlin.time.ExperimentalTime::class)
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivitySplashBinding.inflate(layoutInflater)

        setContentView(binding.root)

        lifecycleScope.launch {

            delay(1200)

            withTimeoutOrNull(2500) {
                SupabaseManager.client
                    .auth
                    .awaitInitialization()
            }

            val user =
                runCatching {
                    SupabaseManager.client
                        .auth
                        .currentUserOrNull()
                }.getOrNull()

            val destination =
                runCatching {
                    if (
                        SupabaseManager.client.auth.currentSessionOrNull() != null &&
                        user?.emailConfirmedAt != null
                    ) {
                        MainActivity::class.java
                    } else {
                        LoginActivity::class.java
                    }
                }.getOrElse {
                    LoginActivity::class.java
                }

            startActivity(
                Intent(
                    this@SplashActivity,
                    destination
                )
            )

            finish()
        }
    }
}
