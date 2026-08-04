package com.example.f1_kotlin.data.firebase

import android.content.Context
import com.example.f1_kotlin.BuildConfig
import com.example.f1_kotlin.util.AppLogger
import com.example.f1_kotlin.util.AppVersion
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Обёртка над Firebase Remote Config.
 *
 * Ключи (defaults при фейле fetch):
 * - [MIN_APP_VERSION_KEY] — semver string, default "0.0.0"
 */
@Singleton
class RemoteConfigService @Inject constructor() {
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    val minAppVersion: String
        get() = remoteConfig.getString(MIN_APP_VERSION_KEY)

    suspend fun init() {
        awaitTask(
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    fetchTimeoutInSeconds = 10
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3_600
                },
            ),
        )
        awaitTask(
            remoteConfig.setDefaultsAsync(
                mapOf(MIN_APP_VERSION_KEY to "0.0.0"),
            ),
        )
        try {
            val activated = awaitTask(remoteConfig.fetchAndActivate())
            AppLogger.d(
                TAG,
                "Remote Config activated=$activated, $MIN_APP_VERSION_KEY=$minAppVersion",
            )
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote Config fetch failed, using defaults", e)
        }
    }

    /** Повторный fetch (например при resume приложения). */
    suspend fun refresh() {
        try {
            awaitTask(remoteConfig.fetchAndActivate())
        } catch (e: Exception) {
            AppLogger.w(TAG, "Remote Config refresh failed", e)
        }
    }

    /** `true`, если установленная версия ниже [minAppVersion]. */
    fun isUpdateRequired(): Boolean {
        val installed = BuildConfig.VERSION_NAME
        val required = AppVersion.isLowerThan(installed, minAppVersion)
        AppLogger.d(
            TAG,
            "Version check: installed=$installed, min=$minAppVersion, updateRequired=$required",
        )
        return required
    }

    companion object {
        const val MIN_APP_VERSION_KEY = "min_app_version"
        private const val TAG = "RemoteConfig"

        private suspend fun <T> awaitTask(task: Task<T>): T =
            suspendCancellableCoroutine { cont ->
                task.addOnCompleteListener { completed ->
                    if (completed.isSuccessful) {
                        cont.resume(completed.result)
                    } else {
                        cont.resumeWithException(
                            completed.exception ?: Exception("Firebase Task failed"),
                        )
                    }
                }
            }
    }
}

/**
 * Инициализация Firebase + Analytics + Crashlytics + Remote Config.
 *
 * [initializeSync] — быстрый sync на main (без сети).
 * [fetchRemoteConfig] — сеть; вызывать с background, иначе ANR в [Application.onCreate].
 */
object FirebaseBootstrap {
    private const val TAG = "FirebaseBootstrap"

    fun initializeSync(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

        AppLogger.d(TAG, "Firebase core initialized (${FirebaseApp.getInstance().options.projectId})")
    }

    /** Non-fatal: сетевые сбои не отправляем. */
    fun recordNonFatal(throwable: Throwable) {
        if (!CrashlyticsReporting.shouldReportUncaughtError(throwable)) return
        runCatching {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    suspend fun fetchRemoteConfig(remoteConfig: RemoteConfigService) {
        remoteConfig.init()
    }
}
