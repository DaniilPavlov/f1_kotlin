package com.example.f1_kotlin.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Цветовая схема Material 3 — влияет на стандартные Compose-компоненты (кнопки, поля). */
private val LightColorScheme = lightColorScheme(
    primary = F1Red,
    onPrimary = F1White,
    background = F1White,
    surface = F1White,
    onBackground = F1Black,
    onSurface = F1Black,
)

/**
 * Корневая тема приложения.
 *
 * [SideEffect] выполняется после композиции: красим статус-бар в F1-чёрный
 * и делаем иконки статус-бара светлыми (для тёмного фона).
 */
@Composable
fun F1Theme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = F1Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
