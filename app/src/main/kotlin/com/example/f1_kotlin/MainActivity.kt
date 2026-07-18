package com.example.f1_kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.f1_kotlin.notifications.RaceReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Единственная Activity в приложении.
 *
 * [@AndroidEntryPoint] подключает Activity к Hilt: теперь в Composable можно вызывать
 * [androidx.hilt.navigation.compose.hiltViewModel], а зависимости (Repository, API)
 * создаются автоматически через DI-граф из [F1Application].
 *
 * UI рисуется через Compose внутри [App]; XML-layout'ы не используются.
 * Смена языка идёт через Compose LocalContext — Activity не пересоздаётся.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderScheduler: RaceReminderScheduler

    /**
     * [installSplashScreen] — splash с логотипом на #333333.
     * [setContent] передаёт дерево Compose вместо XML-layout.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                reminderScheduler.sync()
            }
        })
        setContent { App() }
    }
}
