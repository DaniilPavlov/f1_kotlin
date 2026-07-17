package com.example.f1_kotlin

import androidx.compose.runtime.Composable
import com.example.f1_kotlin.ui.navigation.F1App
import com.example.f1_kotlin.ui.theme.F1Theme

/**
 * Корневой Composable приложения.
 *
 * Слои снаружи внутрь:
 * 1. DI (Hilt) — стартует в [F1Application];
 * 2. [F1Theme] — Material + брендовые цвета;
 * 3. [F1App] — Scaffold, нижние вкладки, NavHost.
 */
@Composable
fun App() {
    F1Theme {
        F1App()
    }
}
