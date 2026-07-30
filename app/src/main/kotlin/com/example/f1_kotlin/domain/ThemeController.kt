package com.example.f1_kotlin.domain

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePreference {
    System,
    Light,
    Dark,
}

class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun preference(): AppThemePreference =
        when (preferences.getString(KEY_APP_THEME, "system")) {
            "light" -> AppThemePreference.Light
            "dark" -> AppThemePreference.Dark
            else -> AppThemePreference.System
        }

    fun save(preference: AppThemePreference) {
        val raw = when (preference) {
            AppThemePreference.System -> "system"
            AppThemePreference.Light -> "light"
            AppThemePreference.Dark -> "dark"
        }
        preferences.edit().putString(KEY_APP_THEME, raw).apply()
    }

    private companion object {
        const val PREFERENCES = "f1_preferences"
        const val KEY_APP_THEME = "app_theme_preference"
    }
}

/**
 * Cycle system → light → dark; Compose listens to [preference].
 */
object ThemeController {
    private val _preference = MutableStateFlow(AppThemePreference.System)
    val preference: StateFlow<AppThemePreference> = _preference.asStateFlow()

    fun init(context: Context) {
        _preference.value = ThemePreferences(context).preference()
    }

    fun cycle(context: Context): AppThemePreference {
        val next = when (_preference.value) {
            AppThemePreference.System -> AppThemePreference.Light
            AppThemePreference.Light -> AppThemePreference.Dark
            AppThemePreference.Dark -> AppThemePreference.System
        }
        ThemePreferences(context).save(next)
        _preference.value = next
        return next
    }

    fun preferenceAnalyticsValue(): String = when (_preference.value) {
        AppThemePreference.System -> "system"
        AppThemePreference.Light -> "light"
        AppThemePreference.Dark -> "dark"
    }
}
