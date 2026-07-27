package com.example.f1_kotlin.data.appmetrica

import android.content.Context
import com.example.f1_kotlin.BuildConfig
import com.example.f1_kotlin.util.AppLogger
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

/**
 * Инициализация AppMetrica.
 *
 * Ключ: [BuildConfig.APPMETRICA_API_KEY] из `local.properties` (`appmetrica.apiKey=...`)
 * или `-Dappmetrica.apiKey=` в CI. Пустой ключ → skip.
 *
 * Краши отдаём Firebase Crashlytics, поэтому reporting в AppMetrica выключен.
 */
object AppMetricaBootstrap {
    private const val TAG = "AppMetricaBootstrap"

    fun bootstrap(context: Context) {
        val apiKey = BuildConfig.APPMETRICA_API_KEY
        if (apiKey.isBlank()) {
            AppLogger.d(TAG, "AppMetrica skipped (no appmetrica.apiKey in local.properties)")
            return
        }

        try {
            val builder = AppMetricaConfig.newConfigBuilder(apiKey)
                .withCrashReporting(false)
                .withNativeCrashReporting(false)
                .withLocationTracking(false)
            if (BuildConfig.DEBUG) {
                builder.withLogs()
            }
            AppMetrica.activate(context, builder.build())
            AppLogger.d(TAG, "AppMetrica activated")
        } catch (e: Exception) {
            AppLogger.e(TAG, "AppMetrica activate failed", e)
        }
    }
}
