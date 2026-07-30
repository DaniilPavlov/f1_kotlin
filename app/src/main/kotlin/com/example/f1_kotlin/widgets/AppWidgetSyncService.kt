package com.example.f1_kotlin.widgets

import android.content.Context
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class AppWidgetSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IF1Repository,
) {
    private val mutex = Mutex()

    suspend fun sync() = mutex.withLock {
        val payload = buildMap<String, Any?> {
            putAll(nextGpPayload())
            putAll(standingsPayload())
        }
        WidgetDataStore.save(context, payload)
        WidgetUpdater.updateProviders(
            context,
            NextGpWidgetProvider::class.java,
            StandingsWidgetProvider::class.java,
        )
    }

    private suspend fun nextGpPayload(): Map<String, Any?> {
        val races = repository.getCurrentSchedule().getOrNull().orEmpty()
        val now = System.currentTimeMillis()
        val upcoming = races.mapNotNull { race ->
            val local = DateUtils.toLocalDateTime(race.date, race.time) ?: return@mapNotNull null
            val ms = local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (ms > now) race to ms else null
        }.sortedBy { it.second }
        val next = upcoming.firstOrNull()
        if (next == null) {
            return mapOf(WidgetDataStore.NEXT_GP_HAS_DATA to false)
        }
        val (race, targetMs) = next
        return mapOf(
            WidgetDataStore.NEXT_GP_HAS_DATA to true,
            WidgetDataStore.NEXT_GP_RACE_NAME to shortRaceName(race),
            WidgetDataStore.NEXT_GP_CIRCUIT to race.circuit.circuitName,
            WidgetDataStore.NEXT_GP_TARGET_MS to targetMs.toString(),
        )
    }

    private suspend fun standingsPayload(): Map<String, Any?> {
        val pair = repository.getCurrentDriverStandings().getOrNull()
        val list = pair?.first.orEmpty().take(3)
        if (list.isEmpty()) {
            return mapOf(WidgetDataStore.STANDINGS_HAS_DATA to false)
        }
        val meta = pair?.second
        val data = mutableMapOf<String, Any?>(
            WidgetDataStore.STANDINGS_HAS_DATA to true,
            WidgetDataStore.STANDINGS_SEASON to (meta?.season.orEmpty()),
            WidgetDataStore.STANDINGS_ROUND to (meta?.round.orEmpty()),
        )
        for (i in 1..3) {
            val entry = list.getOrNull(i - 1)
            data[WidgetDataStore.driverCode(i)] = entry?.let { driverLabel(it.driver.code, it.driver.familyName) }.orEmpty()
            data[WidgetDataStore.driverPoints(i)] = entry?.points.orEmpty()
        }
        return data
    }

    private fun shortRaceName(race: Race): String {
        val name = race.raceName.trim()
        val suffix = " Grand Prix"
        return if (name.endsWith(suffix) && name.length > suffix.length) {
            name.substring(0, name.length - suffix.length)
        } else {
            name
        }
    }

    private fun driverLabel(code: String?, familyName: String): String {
        val c = code?.trim().orEmpty()
        return if (c.isNotEmpty() && c != "none") c else familyName.uppercase()
    }
}
