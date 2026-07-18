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
 */
object LocaleController {
    private val _language = MutableStateFlow("ru")
    val language: StateFlow<String> = _language.asStateFlow()

    fun init(context: Context) {
        val saved = LocalePreferences(context).language()
        _language.value = saved
        Locale.setDefault(Locale.forLanguageTag(saved))
    }

    fun toggle(context: Context): String {
        val next = if (_language.value == "ru") "en" else "ru"
        LocalePreferences(context).save(next)
        _language.value = next
        Locale.setDefault(Locale.forLanguageTag(next))
        return next
    }
}
