package com.example.f1_kotlin.data.circuits

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Загружает curated stats трасс из `assets/data/circuit_stats.json` один раз. */
@Singleton
class CircuitStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) {
    private val adapter = moshi.adapter<Map<String, CircuitStats>>(
        Types.newParameterizedType(Map::class.java, String::class.java, CircuitStats::class.java),
    )
    private val mutex = Mutex()
    private var cache: Map<String, CircuitStats>? = null

    /** Stats по Jolpica `circuitId`, либо `null`. */
    suspend fun of(circuitId: String): CircuitStats? {
        val map = ensureLoaded()
        return map[circuitId.trim().lowercase()]
    }

    private suspend fun ensureLoaded(): Map<String, CircuitStats> {
        cache?.let { return it }
        return mutex.withLock {
            cache?.let { return it }
            withContext(Dispatchers.IO) {
                val raw = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
                val decoded = adapter.fromJson(raw).orEmpty()
                decoded.mapKeys { it.key.lowercase() }.also { cache = it }
            }
        }
    }

    companion object {
        const val ASSET_PATH = "data/circuit_stats.json"
    }
}
