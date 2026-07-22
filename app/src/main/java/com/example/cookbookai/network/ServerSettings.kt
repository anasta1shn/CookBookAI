package com.example.cookbookai.network

import android.content.Context
import android.net.Uri

object ServerSettings {

    private const val PREFS_NAME =
        "server_settings"

    private const val SERVER_URL_KEY =
        "server_url"

    const val DEFAULT_SERVER_URL =
        "http://10.2.30.45:8000/"

    private var appContext: Context? =
        null

    fun init(
        context: Context
    ) {

        appContext =
            context.applicationContext
    }

    fun getServerUrl(): String {

        val context =
            appContext
                ?: return DEFAULT_SERVER_URL

        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .getString(
                SERVER_URL_KEY,
                DEFAULT_SERVER_URL
            )
            ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(
        url: String
    ) {

        val context =
            appContext
                ?: return

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                SERVER_URL_KEY,
                normalizeUrl(url)
            )
            .apply()
    }

    fun normalizeUrl(
        rawUrl: String
    ): String {

        val trimmed =
            rawUrl.trim()

        val urlWithScheme =
            if (
                trimmed.startsWith("http://") ||
                trimmed.startsWith("https://")
            ) {
                trimmed
            } else {
                "http://$trimmed"
            }

        return if (urlWithScheme.endsWith("/")) {
            urlWithScheme
        } else {
            "$urlWithScheme/"
        }
    }

    fun isValidUrl(
        rawUrl: String
    ): Boolean {

        val normalized =
            normalizeUrl(rawUrl)

        val uri =
            Uri.parse(normalized)

        return uri.scheme in listOf("http", "https") &&
                !uri.host.isNullOrBlank()
    }

    fun isHotspotSafeAddress(
        rawUrl: String
    ): Boolean {

        val host =
            Uri.parse(
                normalizeUrl(rawUrl)
            )
                .host
                ?: return false

        if (
            host == "localhost" ||
            host == "127.0.0.1"
        ) {
            return true
        }

        val parts =
            host.split(".")
                .mapNotNull {
                    it.toIntOrNull()
                }

        if (parts.size != 4) {
            return false
        }

        val first =
            parts[0]

        val second =
            parts[1]

        return first == 10 ||
                (
                        first == 192 &&
                                second == 168
                        ) ||
                (
                        first == 172 &&
                                second in 16..31
                        )
    }
}
