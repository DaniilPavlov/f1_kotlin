package com.example.f1_kotlin.data.analytics

import android.os.Bundle
import com.example.f1_kotlin.BuildConfig
import com.example.f1_kotlin.util.AppLogger
import com.google.firebase.analytics.FirebaseAnalytics
import io.appmetrica.analytics.AppMetrica
import javax.inject.Inject
import javax.inject.Singleton

/** Единая точка аналитики: один [log] → Firebase + AppMetrica (UI не знает SDK). */
interface AnalyticsGateway {
    fun log(event: AnalyticsEvent)
}

/** Двойная отправка событий; в DEBUG дублирует в [AppLogger]. */
@Singleton
class AppAnalyticsGateway @Inject constructor() : AnalyticsGateway {
    override fun log(event: AnalyticsEvent) {
        runCatching { logToFirebase(event) }
            .onFailure { e -> AppLogger.e(TAG, "Firebase analytics failed", e) }
        runCatching { logToAppMetrica(event) }
            .onFailure { e -> AppLogger.e(TAG, "AppMetrica analytics failed", e) }
        if (BuildConfig.DEBUG) {
            AppLogger.d(TAG, "${event.name} ${event.params}")
        }
    }

    private fun logToFirebase(event: AnalyticsEvent) {
        val analytics = FirebaseAnalytics.getInstance(
            // Application context via FirebaseApp; getInstance needs Context —
            // FirebaseApp.getInstance().applicationContext
            com.google.firebase.FirebaseApp.getInstance().applicationContext,
        )
        if (event is AnalyticsEvent.ScreenView) {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, event.screenName)
                event.screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            return
        }
        val bundle = Bundle()
        event.params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(event.name, bundle)
    }

    private fun logToAppMetrica(event: AnalyticsEvent) {
        val map = event.params.takeIf { it.isNotEmpty() }
        if (map == null) {
            AppMetrica.reportEvent(event.name)
        } else {
            AppMetrica.reportEvent(event.name, map)
        }
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
