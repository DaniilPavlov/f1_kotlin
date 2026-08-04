package com.example.f1_kotlin.data.career

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.mapper.toDomain
import com.example.f1_kotlin.data.model.CareerRaceResult
import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.model.H2hEntityCompareData
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.data.model.MrDataTotalModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import kotlinx.coroutines.delay
import retrofit2.HttpException

/**
 * Загрузка карьерной статистики через Jolpica (≤4 req/s → пауза 500 ms между запросами).
 */
object CareerLoader {
    private const val MIN_GAP_MS = 500L
    private const val MAX_PAGE_SIZE = 100
    private const val PAGE_RETRIES = 3
    private const val RATE_LIMIT_BACKOFF_MS = 1_000L

    suspend fun loadDriverCareer(
        api: F1ApiService,
        driverId: String,
        current: List<Constructor> = emptyList(),
    ): CareerStats<Constructor> {
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
            related = totals[5].constructorTable?.constructors.orEmpty().map { it.toDomain() },
            winRaces = winRaces,
            podiumRaces = podiumRaces,
            poleRaces = poleRaces,
        )
    }

    suspend fun loadConstructorCareer(
        api: F1ApiService,
        constructorId: String,
        current: List<Driver> = emptyList(),
    ): CareerStats<Driver> {
        val prefix = "constructors/$constructorId"
        // `/races` даёт unique GPs (total) за 1 запрос — не пагинируем все results (у McLaren ~20 стр. → 429).
        val totals = getThrottled(
            api,
            listOf(
                "$prefix/races",
                "$prefix/results/1",
                "$prefix/results/2",
                "$prefix/results/3",
                "$prefix/qualifying/1",
                "$prefix/drivers",
            ),
            limit = 1,
        )
        val winPages = fetchAllPages(api, "$prefix/results/1")
        val secondPages = fetchAllPages(api, "$prefix/results/2")
        val thirdPages = fetchAllPages(api, "$prefix/results/3")
        val polePages = fetchAllPages(api, "$prefix/qualifying/1")

        val wins = totalOf(totals[1])
        val poles = totalOf(totals[4])
        val winRaces = parseAllRaceResults(winPages, position = 1).sortedWith(newestFirst)
        val podiumRaces = dedupeByBestPosition(
            winRaces +
                parseAllRaceResults(secondPages, position = 2) +
                parseAllRaceResults(thirdPages, position = 3),
        ).sortedWith(newestFirst)
        val poleRaces = parseAllQualifyingPoles(polePages).sortedWith(newestFirst)

        return CareerStats(
            races = totalOf(totals[0]),
            wins = if (wins > 0) wins else winRaces.size,
            podiums = podiumRaces.size,
            poles = if (poles > 0) poles else poleRaces.size,
            current = current,
            related = totals[5].driverTable?.drivers.orEmpty().map { it.toDomain() },
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

    /**
     * Stats + round scores за один проход (results → sprint → poles), как Flutter H2hRepository.
     * Не дублирует пагинацию после лёгких totals — иначе Jolpica 429 и график пустеет.
     */
    suspend fun loadH2hCompareData(
        api: F1ApiService,
        entityPath: String,
        season: String? = null,
    ): H2hEntityCompareData {
        val prefix = seasonPrefix(season)
        val racePages = fetchAllPages(api, "$prefix$entityPath/results")
        val sprintPages = fetchAllPagesOrEmpty(api, "$prefix$entityPath/sprint")
        val polesResponse = getThrottled(
            api,
            listOf("$prefix$entityPath/qualifying/1"),
            limit = 1,
        )
        val acc = H2hScoreAccumulator()
        acc.mergePages(racePages, sprint = false)
        acc.mergePages(sprintPages, sprint = true)
        return acc.toCompareData(poles = totalOf(polesResponse.first()))
    }

    /** Round-by-round points (race + sprint) for H2H cumulative chart. */
    suspend fun loadH2hRoundScores(
        api: F1ApiService,
        entityPath: String,
        season: String? = null,
    ): List<com.example.f1_kotlin.viewmodel.H2hRoundScore> =
        loadH2hCompareData(api, entityPath, season).scores

    private class H2hScoreAccumulator {
        private val byKey = linkedMapOf<String, com.example.f1_kotlin.viewmodel.H2hRoundScore>()
        private val raceKeys = linkedSetOf<String>()
        private var wins = 0
        private var seconds = 0
        private var thirds = 0

        fun mergePages(pages: List<MrDataTotalModel>, sprint: Boolean) {
            pages.forEach { page ->
                page.raceTable?.races.orEmpty().forEach { race ->
                    mergeRace(race, sprint)
                }
            }
        }

        private fun mergeRace(race: RaceModel, sprint: Boolean) {
            val entries = if (sprint) race.sprintResults else race.results
            if (entries.isNullOrEmpty()) return
            val key = raceKey(race.season, race.round)
            if (!sprint) raceKeys.add(key)
            var points = 0.0
            entries.forEach { entry ->
                points += entry.points.toDoubleOrNull() ?: 0.0
                if (!sprint) {
                    when (entry.position.toIntOrNull()) {
                        1 -> wins++
                        2 -> seconds++
                        3 -> thirds++
                    }
                }
            }
            val prev = byKey[key]
            byKey[key] = com.example.f1_kotlin.viewmodel.H2hRoundScore(
                season = race.season,
                round = race.round,
                raceName = race.raceName,
                points = (prev?.points ?: 0.0) + points,
            )
        }

        fun toCompareData(poles: Int): H2hEntityCompareData {
            val scores = byKey.values.sortedWith(
                compareBy({ it.season }, { it.roundNumber }),
            )
            return H2hEntityCompareData(
                stats = H2hStats(
                    races = raceKeys.size,
                    wins = wins,
                    podiums = wins + seconds + thirds,
                    poles = poles,
                ),
                scores = scores,
            )
        }
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
            val (page, nextStart) = getPage(api, path, limit = limit, offset = 0, lastStart = lastStart)
            responses.add(page)
            lastStart = nextStart
        }
        return responses
    }

    private suspend fun fetchAllPages(api: F1ApiService, path: String): List<MrDataTotalModel> {
        val pages = mutableListOf<MrDataTotalModel>()
        var offset = 0
        var lastStart = 0L
        var fetchMore = true
        while (fetchMore) {
            val (page, nextStart) = getPage(
                api,
                path,
                limit = MAX_PAGE_SIZE,
                offset = offset,
                lastStart = lastStart,
            )
            lastStart = nextStart
            pages.add(page)
            fetchMore = shouldFetchNextPage(page, offset)
            if (fetchMore) {
                offset += MAX_PAGE_SIZE
            }
        }
        return pages
    }

    /** Sprint endpoint may 404 for older careers — treat as empty, don't fail the chart. */
    private suspend fun fetchAllPagesOrEmpty(api: F1ApiService, path: String): List<MrDataTotalModel> =
        try {
            fetchAllPages(api, path)
        } catch (e: HttpException) {
            if (e.code() == 404) emptyList() else throw e
        }

    /**
     * Один HTTP-запрос с throttle и retry на 429 (без перезапуска всей карьеры с offset=0).
     * @return страница и timestamp старта успешного запроса для следующего throttle.
     */
    private suspend fun getPage(
        api: F1ApiService,
        path: String,
        limit: Int,
        offset: Int,
        lastStart: Long,
    ): Pair<MrDataTotalModel, Long> {
        var throttleFrom = lastStart
        repeat(PAGE_RETRIES + 1) { attempt ->
            throttle(throttleFrom)
            val startedAt = System.currentTimeMillis()
            try {
                return api.getMrDataTotal(path, limit = limit, offset = offset).mrData to startedAt
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < PAGE_RETRIES) {
                    delay(RATE_LIMIT_BACKOFF_MS * (attempt + 1))
                    throttleFrom = 0L
                } else {
                    throw e
                }
            }
        }
        error("unreachable")
    }

    private fun shouldFetchNextPage(page: MrDataTotalModel, offset: Int): Boolean {
        val pageRaces = page.raceTable?.races?.size ?: 0
        if (pageRaces == 0) return false
        val nextOffset = offset + MAX_PAGE_SIZE
        val total = totalOf(page)
        if (total > 0 && nextOffset >= total) return false
        if (pageRaces < MAX_PAGE_SIZE && total == 0) return false
        return true
    }

    private suspend fun throttle(lastStart: Long) {
        if (lastStart <= 0) return
        val elapsed = System.currentTimeMillis() - lastStart
        if (elapsed < MIN_GAP_MS) delay(MIN_GAP_MS - elapsed)
    }

    private fun totalOf(data: MrDataTotalModel): Int =
        data.total?.toIntOrNull() ?: 0

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
                constructor = entry.constructor.toDomain(),
                circuit = race.circuit.toDomain(),
                driver = entry.driver.toDomain(),
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
                constructor = entry.constructor.toDomain(),
                circuit = race.circuit.toDomain(),
                driver = entry.driver.toDomain(),
            )
        }

    private fun raceKey(season: String, round: String) = "$season-$round"

    private val newestFirst = Comparator<CareerRaceResult> { a, b ->
        val seasonCmp = b.season.compareTo(a.season)
        if (seasonCmp != 0) seasonCmp else b.round.compareTo(a.round)
    }
}
