package com.example.f1_kotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.f1_kotlin.data.deeplink.DeepLinkBus
import com.example.f1_kotlin.domain.ForceUpdateGate
import com.example.f1_kotlin.domain.live.LiveWeekendController
import com.example.f1_kotlin.notifications.RaceReminderScheduler
import com.example.f1_kotlin.widgets.AppWidgetSyncService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderScheduler: RaceReminderScheduler
    @Inject lateinit var forceUpdateGate: ForceUpdateGate
    @Inject lateinit var deepLinkBus: DeepLinkBus
    @Inject lateinit var liveWeekendController: LiveWeekendController
    @Inject lateinit var appWidgetSyncService: AppWidgetSyncService

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
                liveWeekendController.onAppForeground()
                lifecycleScope.launch {
                    forceUpdateGate.onResume()
                    if (!forceUpdateGate.required.value) {
                        reminderScheduler.sync()
                        runCatching { appWidgetSyncService.sync() }
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                liveWeekendController.onAppBackground()
            }
        })
        deepLinkBus.offer(intent?.data)
        setContent { App(forceUpdateGate) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkBus.offer(intent.data)
    }
}
