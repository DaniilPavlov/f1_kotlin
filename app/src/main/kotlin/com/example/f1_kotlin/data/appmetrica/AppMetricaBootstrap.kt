package com.example.f1_kotlin.data.appmetrica

import android.content.Context
import android.util.Log
import com.example.f1_kotlin.BuildConfig
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AppMetrica skipped (no appmetrica.apiKey in local.properties)")
            }
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AppMetrica activated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AppMetrica activate failed", e)
        }
    }
}
