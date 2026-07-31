package com.example.f1_kotlin

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.f1_kotlin.domain.ForceUpdateGate
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.navigation.F1App
import com.example.f1_kotlin.ui.screens.ForceUpdateScreen
import com.example.f1_kotlin.ui.theme.F1Theme
import java.util.Locale

/**
 * Корневой Composable приложения.
 *
 * Слои снаружи внутрь:
 * 1. Локаль — [CompositionLocalProvider] без recreate Activity;
 * 2. [F1Theme] — Material + брендовые цвета;
 * 3. Force-update gate или [F1App] — Scaffold / вкладки.
 */
@Composable
fun App(forceUpdateGate: ForceUpdateGate) {
    val language by LocaleController.language.collectAsState()
    val forceUpdate by forceUpdateGate.required.collectAsState()
    val baseContext = LocalContext.current
    val localizedContext = remember(language, baseContext) {
        baseContext.withAppLocale(language)
    }

    // Re-assert JVM default on every language/config frame — Activity attach can reset it.
    SideEffect {
        LocaleController.applyLanguage(language)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
    ) {
        F1Theme {
            if (forceUpdate) {
                ForceUpdateScreen()
            } else {
                F1App()
            }
        }
    }
}

private fun Context.withAppLocale(language: String): Context {
    val locale = Locale.forLanguageTag(language)
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList(locale))
    val localized = createConfigurationContext(config)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localized.resources
        override fun getAssets(): AssetManager = localized.assets
    }
}
