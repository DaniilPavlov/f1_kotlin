package com.example.f1_kotlin.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Быстрая проверка отсутствия сети (баннер «сохранённые данные»).
 *
 * Без TCP-probe: смотрим только наличие интерфейса с INTERNET capability.
 */
@Singleton
class NetworkReachability @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var memoized: Boolean? = null

    @Volatile
    private var memoizedAtMs: Long = 0L

    /** Override for unit tests (avoid real ConnectivityManager). */
    @VisibleForTesting
    var debugIsOfflineOverride: (() -> Boolean)? = null

    fun clearMemo() {
        memoized = null
        memoizedAtMs = 0L
    }

    fun isOffline(): Boolean {
        debugIsOfflineOverride?.let { return it() }

        val now = SystemClock.elapsedRealtime()
        val cached = memoized
        if (cached != null && now - memoizedAtMs < MEMO_TTL_MS) {
            return cached
        }

        val value = probe()
        memoized = value
        memoizedAtMs = now
        return value
    }

    private fun probe(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val MEMO_TTL_MS = 2_000L
    }
}

/** Баннер «сохранённые данные»: есть контент и сейчас нет сети. */
fun shouldShowOfflineCachedBanner(
    hasCachedContent: Boolean,
    reachability: NetworkReachability,
): Boolean {
    if (!hasCachedContent) return false
    return reachability.isOffline()
}

/**
 * После resume: если баннер показан и сеть появилась — спрятать без перезагрузки.
 * @return новое значение [showingCachedData] (true = всё ещё офлайн).
 */
fun clearOfflineBannerIfOnline(
    currentlyShowing: Boolean,
    reachability: NetworkReachability,
): Boolean {
    if (!currentlyShowing) return false
    reachability.clearMemo()
    return reachability.isOffline()
}
