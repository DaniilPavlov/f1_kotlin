package com.example.f1_kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity в приложении.
 *
 * [@AndroidEntryPoint] подключает Activity к Hilt: теперь в Composable можно вызывать
 * [androidx.hilt.navigation.compose.hiltViewModel], а зависимости (Repository, API)
 * создаются автоматически через DI-граф из [F1Application].
 *
 * UI рисуется через Compose внутри [App]; XML-layout'ы не используются.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * [installSplashScreen] — splash с логотипом на #333333 (как flutter_native_splash во Flutter).
     * [setContent] передаёт дерево Compose вместо XML-layout.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}
