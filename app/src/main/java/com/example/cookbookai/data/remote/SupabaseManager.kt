package com.example.cookbookai.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    private const val SUPABASE_URL = "https://pfhwnkrrrsktmnaadiab.supabase.co"
    private const val SUPABASE_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBmaHdua3JycnNrdG1uYWFkaWFiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMjgyMDMsImV4cCI6MjA4NzYwNDIwM30.yNPzJrNLXh-LLCRako9IwLJW41PceUDYfMfkO5sOVNo"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
            sessionManager = SettingsSessionManager()
        }
        install(Postgrest)
        install(Storage)

    }
}
