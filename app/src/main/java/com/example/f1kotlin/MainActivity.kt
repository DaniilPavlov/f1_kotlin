package com.example.f1kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.f1kotlin.ui.navigation.F1App
import com.example.f1kotlin.ui.theme.F1KotlinTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Единственная Activity в приложении.
 *
 * [@AndroidEntryPoint] подключает Activity к Hilt: теперь в Composable можно вызывать
 * [androidx.hilt.navigation.compose.hiltViewModel], а зависимости (Repository, API)
 * создаются автоматически через DI-граф из [F1Application].
 *
 * UI рисуется через Compose внутри [F1App]; XML-layout'ы не используются.
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
        setContent {
            F1KotlinTheme {
                F1App()
            }
        }
    }
}
