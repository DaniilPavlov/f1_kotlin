package com.example.f1_kotlin.domain

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun language(): String =
        preferences.getString(KEY_APP_LOCALE, "ru").orEmpty().takeIf { it in SUPPORTED } ?: "ru"

    fun save(language: String) {
        preferences.edit().putString(KEY_APP_LOCALE, language).apply()
    }

    private companion object {
        const val PREFERENCES = "f1_preferences"
        const val KEY_APP_LOCALE = "app_locale"
        val SUPPORTED = setOf("ru", "en")
    }
}

/**
 * Смена языка без пересоздания Activity: Compose слушает [language] и подменяет
 * [androidx.compose.ui.platform.LocalContext] / [androidx.compose.ui.platform.LocalConfiguration].
 *
 * GoF Creational Singleton — один общий экземпляр на всё приложение:
 * Compose и домен читают тот же [language], без повторного создания.
 */
object LocaleController {
    private val _language = MutableStateFlow("ru")
    val language: StateFlow<String> = _language.asStateFlow()

    fun init(context: Context) {
        applyLanguage(LocalePreferences(context).language())
    }

    fun toggle(context: Context): String {
        val next = if (_language.value == "ru") "en" else "ru"
        LocalePreferences(context).save(next)
        applyLanguage(next)
        return next
    }

    /** Locale for formatting (dates, calendar) — not Android system default. */
    fun currentLocale(): Locale = localeFor(_language.value)

    fun applyLanguage(language: String) {
        _language.value = language
        // Keep JVM default in sync; Activity config changes can reset it otherwise.
        Locale.setDefault(localeFor(language))
    }

    private fun localeFor(language: String): Locale =
        if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("ru")
}
