package com.example.f1_kotlin.data.career

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.model.CareerRaceResult
import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.data.model.MrDataTotalModel
import com.example.f1_kotlin.data.model.RaceModel
import kotlinx.coroutines.delay

/**
 * Загрузка карьерной статистики через Jolpica (≤4 req/s → пауза 500 ms между запросами).
 */
object CareerLoader {
    private const val MIN_GAP_MS = 500L
    private const val MAX_PAGE_SIZE = 100

    suspend fun loadDriverCareer(
        api: F1ApiService,
        driverId: String,
        current: List<ConstructorModel> = emptyList(),
    ): CareerStats<ConstructorModel> {
        val prefix = "drivers/$driverId"
        val totals = getThrottled(
            api,
            listOf(
                "$prefix/results",
                "$prefix/results/1",
                "$prefix/results/2",
                "$prefix/results/3",
                "$prefix/qualifying/1",
                "$prefix/constructors",
            ),
            limit = 1,
        )
        val winPages = fetchAllPages(api, "$prefix/results/1")
        val secondPages = fetchAllPages(api, "$prefix/results/2")
        val thirdPages = fetchAllPages(api, "$prefix/results/3")
        val polePages = fetchAllPages(api, "$prefix/qualifying/1")

        val wins = totalOf(totals[1])
        val second = totalOf(totals[2])
        val third = totalOf(totals[3])
        val winRaces = parseAllRaceResults(winPages, position = 1).sortedWith(newestFirst)
        val podiumRaces = (
            winRaces +
                parseAllRaceResults(secondPages, position = 2) +
                parseAllRaceResults(thirdPages, position = 3)
            ).sortedWith(newestFirst)
        val poleRaces = parseAllQualifyingPoles(polePages).sortedWith(newestFirst)

        return CareerStats(
            races = totalOf(totals[0]),
            wins = wins,
            podiums = wins + second + third,
            poles = totalOf(totals[4]),
            current = current,
            related = totals[5].constructorTable?.constructors.orEmpty(),
            winRaces = winRaces,
            podiumRaces = podiumRaces,
            poleRaces = poleRaces,
        )
    }

    suspend fun loadConstructorCareer(
        api: F1ApiService,
        constructorId: String,
        current: List<DriverModel> = emptyList(),
    ): CareerStats<DriverModel> {
        val prefix = "constructors/$constructorId"
        val allResultsPages = fetchAllPages(api, "$prefix/results")
        val winPages = fetchAllPages(api, "$prefix/results/1")
        val secondPages = fetchAllPages(api, "$prefix/results/2")
        val thirdPages = fetchAllPages(api, "$prefix/results/3")
        val polePages = fetchAllPages(api, "$prefix/qualifying/1")
        val driversResponse = getThrottled(api, listOf("$prefix/drivers"), limit = 100).first()

        val winRaces = parseAllRaceResults(winPages, position = 1).sortedWith(newestFirst)
        val podiumRaces = dedupeByBestPosition(
            winRaces +
                parseAllRaceResults(secondPages, position = 2) +
                parseAllRaceResults(thirdPages, position = 3),
        ).sortedWith(newestFirst)
        val poleRaces = parseAllQualifyingPoles(polePages).sortedWith(newestFirst)

        val uniqueRaces = uniqueRaceCountAcross(allResultsPages)
        val winsTotal = winPages.firstOrNull()?.let { totalOf(it) } ?: 0
        val polesTotal = polePages.firstOrNull()?.let { totalOf(it) } ?: 0

        return CareerStats(
            races = if (uniqueRaces > 0) {
                uniqueRaces
            } else {
                allResultsPages.firstOrNull()?.let { totalOf(it) } ?: 0
            },
            wins = if (winsTotal > 0) winsTotal else winRaces.size,
            podiums = podiumRaces.size,
            poles = if (polesTotal > 0) polesTotal else poleRaces.size,
            current = current,
            related = driversResponse.driverTable?.drivers.orEmpty(),
            winRaces = winRaces,
            podiumRaces = podiumRaces,
            poleRaces = poleRaces,
        )
    }

    /** Totals через `MRData.total` с limit=1 (карьера или сезон). */
    suspend fun loadH2hStats(
        api: F1ApiService,
        entityPath: String,
        season: String? = null,
    ): H2hStats {
        val prefix = seasonPrefix(season)
        val responses = getThrottled(
            api,
            listOf(
                "$prefix$entityPath/results",
                "$prefix$entityPath/results/1",
                "$prefix$entityPath/results/2",
                "$prefix$entityPath/results/3",
                "$prefix$entityPath/qualifying/1",
            ),
            limit = 1,
        )
        val wins = totalOf(responses[1])
        val second = totalOf(responses[2])
        val third = totalOf(responses[3])
        return H2hStats(
            races = totalOf(responses[0]),
            wins = wins,
            podiums = wins + second + third,
            poles = totalOf(responses[4]),
        )
    }

    private fun seasonPrefix(season: String?): String {
        val trimmed = season?.trim().orEmpty()
        return if (trimmed.isEmpty()) "" else "$trimmed/"
    }

    private suspend fun getThrottled(
        api: F1ApiService,
        paths: List<String>,
        limit: Int = MAX_PAGE_SIZE,
    ): List<MrDataTotalModel> {
        val responses = mutableListOf<MrDataTotalModel>()
        var lastStart = 0L
        for (path in paths) {
            throttle(lastStart)
            lastStart = System.currentTimeMillis()
            responses.add(api.getMrDataTotal(path, limit = limit).mrData)
        }
        return responses
    }

    private suspend fun fetchAllPages(api: F1ApiService, path: String): List<MrDataTotalModel> {
        val pages = mutableListOf<MrDataTotalModel>()
        var offset = 0
        var lastStart = 0L
        while (true) {
            throttle(lastStart)
            lastStart = System.currentTimeMillis()
            val page = api.getMrDataTotal(path, limit = MAX_PAGE_SIZE, offset = offset).mrData
            pages.add(page)
            val pageRaces = page.raceTable?.races?.size ?: 0
            if (pageRaces == 0) break
            offset += MAX_PAGE_SIZE
            val total = totalOf(page)
            if (total > 0 && offset >= total) break
            if (pageRaces < MAX_PAGE_SIZE && total == 0) break
        }
        return pages
    }

    private suspend fun throttle(lastStart: Long) {
        if (lastStart <= 0) return
        val elapsed = System.currentTimeMillis() - lastStart
        if (elapsed < MIN_GAP_MS) delay(MIN_GAP_MS - elapsed)
    }

    private fun totalOf(data: MrDataTotalModel): Int =
        data.total?.toIntOrNull() ?: 0

    private fun uniqueRaceCountAcross(pages: List<MrDataTotalModel>): Int {
        val keys = linkedSetOf<String>()
        pages.forEach { page ->
            page.raceTable?.races.orEmpty().forEach { race ->
                keys.add(raceKey(race.season, race.round))
            }
        }
        return keys.size
    }

    private fun dedupeByBestPosition(races: List<CareerRaceResult>): List<CareerRaceResult> {
        val best = linkedMapOf<String, CareerRaceResult>()
        races.forEach { race ->
            val key = raceKey(race.season, race.round)
            val prev = best[key]
            if (prev == null || race.position < prev.position) {
                best[key] = race
            }
        }
        return best.values.toList()
    }

    private fun parseAllRaceResults(pages: List<MrDataTotalModel>, position: Int): List<CareerRaceResult> =
        pages.flatMap { parseRaceResults(it.raceTable?.races.orEmpty(), position) }

    private fun parseAllQualifyingPoles(pages: List<MrDataTotalModel>): List<CareerRaceResult> =
        pages.flatMap { parseQualifyingPoles(it.raceTable?.races.orEmpty()) }

    private fun parseRaceResults(races: List<RaceModel>, position: Int): List<CareerRaceResult> =
        races.mapNotNull { race ->
            val entry = race.results?.firstOrNull() ?: return@mapNotNull null
            CareerRaceResult(
                season = race.season,
                round = race.round,
                raceName = race.raceName,
                position = position,
                constructor = entry.constructor,
                circuit = race.circuit,
                driver = entry.driver,
            )
        }

    private fun parseQualifyingPoles(races: List<RaceModel>): List<CareerRaceResult> =
        races.mapNotNull { race ->
            val entry = race.qualifyingResults?.firstOrNull() ?: return@mapNotNull null
            CareerRaceResult(
                season = race.season,
                round = race.round,
                raceName = race.raceName,
                position = 1,
                constructor = entry.constructor,
                circuit = race.circuit,
                driver = entry.driver,
            )
        }

    private fun raceKey(season: String, round: String) = "$season-$round"

    private val newestFirst = Comparator<CareerRaceResult> { a, b ->
        val seasonCmp = b.season.compareTo(a.season)
        if (seasonCmp != 0) seasonCmp else b.round.compareTo(a.round)
    }
}
