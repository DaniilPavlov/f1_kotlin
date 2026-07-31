package com.example.f1_kotlin.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.f1_kotlin.domain.AppThemePreference
import com.example.f1_kotlin.domain.ThemeController

private fun lightScheme(colors: AppColors) = lightColorScheme(
    primary = colors.red,
    onPrimary = F1OnChrome,
    background = colors.white,
    surface = colors.white,
    onBackground = colors.black,
    onSurface = colors.black,
    outline = colors.strokeGray,
    onSurfaceVariant = colors.textGray,
)

private fun darkScheme(colors: AppColors) = darkColorScheme(
    primary = colors.red,
    onPrimary = F1OnChrome,
    background = colors.white,
    surface = colors.white,
    onBackground = colors.black,
    onSurface = colors.black,
    outline = colors.strokeGray,
    onSurfaceVariant = colors.textGray,
)

/**
 * Root theme: respects [ThemeController] preference (system / light / dark).
 */
@Composable
fun F1Theme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val preference by ThemeController.preference.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val useDark = darkTheme ?: when (preference) {
        AppThemePreference.System -> systemDark
        AppThemePreference.Light -> false
        AppThemePreference.Dark -> true
    }
    val colors = if (useDark) AppColors.Dark else AppColors.Light
    val colorScheme = if (useDark) darkScheme(colors) else lightScheme(colors)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = F1Chrome.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

fun Configuration.isNightMode(): Boolean =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
