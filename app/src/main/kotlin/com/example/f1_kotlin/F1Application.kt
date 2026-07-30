package com.example.f1_kotlin

import android.app.Application
import com.example.f1_kotlin.data.appmetrica.AppMetricaBootstrap
import com.example.f1_kotlin.data.firebase.FirebaseBootstrap
import com.example.f1_kotlin.data.firebase.RemoteConfigService
import com.example.f1_kotlin.domain.ForceUpdateGate
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.domain.ThemeController
import com.example.f1_kotlin.notifications.RaceReminderScheduler
import com.example.f1_kotlin.ui.map.OsmdroidInitializer
import com.example.f1_kotlin.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Глобальный класс приложения, указанный в AndroidManifest.
 *
 * [@HiltAndroidApp] запускает кодогенерацию Dagger/Hilt: создаётся граф зависимостей
 * (SingletonComponent), куда попадают [com.example.f1_kotlin.di.NetworkModule],
 * [com.example.f1_kotlin.di.DatabaseModule] и все классы с [@Inject].
 *
 * Без этого класса Hilt не сможет внедрять [com.example.f1_kotlin.data.repository.IF1Repository]
 * во ViewModel'и.
 */
@HiltAndroidApp
class F1Application : Application() {
    @Inject lateinit var reminderScheduler: RaceReminderScheduler
    @Inject lateinit var remoteConfig: RemoteConfigService
    @Inject lateinit var forceUpdateGate: ForceUpdateGate

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LocaleController.init(this)
        ThemeController.init(this)
        OsmdroidInitializer.ensureInitialized(this)

        // Sync only — Remote Config fetch must not block main (was ANR / failed startup).
        runCatching { FirebaseBootstrap.initializeSync(this) }
            .onFailure { e -> AppLogger.e(TAG, "Firebase core init failed", e) }
        AppMetricaBootstrap.bootstrap(this)

        applicationScope.launch {
            runCatching { FirebaseBootstrap.fetchRemoteConfig(remoteConfig) }
                .onFailure { e -> AppLogger.e(TAG, "Remote Config bootstrap failed", e) }
            forceUpdateGate.check()
            if (!forceUpdateGate.required.value) {
                reminderScheduler.sync()
            }
        }
    }

    fun toggleLocale(): String {
        val next = LocaleController.toggle(this)
        if (!forceUpdateGate.required.value) {
            reminderScheduler.sync()
        }
        return next
    }

    private companion object {
        const val TAG = "F1Application"
    }
}
