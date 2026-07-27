package com.example.f1_kotlin.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

/**
 * Проверка и нормализация внешних ссылок перед открытием в браузере.
 *
 * Парсинг через [java.net.URI], чтобы логика работала и в JVM unit-тестах
 * (android.net.Uri там — stub).
 */
object TrustedUrl {
    private const val TAG = "TrustedUrl"

    private val allowedHostSuffixes = listOf(
        "wikipedia.org",
        "wikimedia.org",
        "espn.com",
        "espn.co.uk",
        "github.com",
        "formula1.com",
        "jolpi.ca",
        "ergast.com",
    )

    /**
     * Возвращает нормализованный https-URL или `null`, если ссылка не доверенная.
     */
    fun parse(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null

        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val host = uri.host?.takeIf { it.isNotEmpty() } ?: return null
        val scheme = uri.scheme?.lowercase()

        val httpsUri = when (scheme) {
            "https" -> uri
            "http" -> URI(
                "https",
                uri.userInfo,
                uri.host,
                uri.port,
                uri.path,
                uri.query,
                uri.fragment,
            )
            else -> return null
        }
        if (!isAllowedHost(host)) return null
        return httpsUri.toString()
    }

    /** Для загрузки изображений: http → https без проверки allowlist. */
    fun preferHttps(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return rawUrl
        if (uri.scheme?.equals("http", ignoreCase = true) != true) return rawUrl
        return URI(
            "https",
            uri.userInfo,
            uri.host,
            uri.port,
            uri.path,
            uri.query,
            uri.fragment,
        ).toString()
    }

    fun isWikipediaHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "wikipedia.org" || lower.endsWith(".wikipedia.org")
    }

    private fun isAllowedHost(host: String): Boolean {
        val lower = host.lowercase()
        return allowedHostSuffixes.any { suffix ->
            lower == suffix || lower.endsWith(".$suffix")
        }
    }

    internal fun logOpenFailure(rawUrl: String) {
        AppLogger.w(TAG, "Rejected or failed to open URL (len=${rawUrl.length})")
    }
}

/** Открывает доверенную https-ссылку во внешнем браузере. */
fun openUrl(context: Context, url: String) {
    val normalized = TrustedUrl.parse(url)
    if (normalized == null) {
        TrustedUrl.logOpenFailure(url)
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
    } catch (_: ActivityNotFoundException) {
        TrustedUrl.logOpenFailure(url)
    }
}
