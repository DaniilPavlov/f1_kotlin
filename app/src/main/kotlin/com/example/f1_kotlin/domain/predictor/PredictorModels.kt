package com.example.f1_kotlin.domain.predictor

import java.time.Instant

/** Предсказание одного уикенда (значение в `users/{uid}/seasons/{year}.weekends`). */
data class PredictorWeekendPrediction(
    val round: String,
    val raceName: String = "",
    val qualifyingOrder: List<String> = emptyList(),
    val raceOrder: List<String> = emptyList(),
    val lockedAt: Instant? = null,
    val qualiPoints: Int? = null,
    val racePoints: Int? = null,
    val scoredAt: Instant? = null,
    val actualQualifyingOrder: List<String>? = null,
    val actualRaceOrder: List<String>? = null,
) {
    val totalPoints: Int get() = (qualiPoints ?: 0) + (racePoints ?: 0)
    val hasAnyPoints: Boolean get() = qualiPoints != null || racePoints != null

    fun toFirestoreMap(): Map<String, Any?> = buildMap {
        put("round", round)
        put("raceName", raceName)
        put("qualifyingOrder", qualifyingOrder)
        put("raceOrder", raceOrder)
        lockedAt?.let { put("lockedAt", it.toString()) }
        qualiPoints?.let { put("qualiPoints", it) }
        racePoints?.let { put("racePoints", it) }
        scoredAt?.let { put("scoredAt", it.toString()) }
        actualQualifyingOrder?.let { put("actualQualifyingOrder", it) }
        actualRaceOrder?.let { put("actualRaceOrder", it) }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(data: Map<String, Any?>): PredictorWeekendPrediction {
            fun strList(key: String): List<String> =
                (data[key] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            fun strListOrNull(key: String): List<String>? =
                (data[key] as? List<*>)?.mapNotNull { it as? String }
            fun intOrNull(key: String): Int? = when (val v = data[key]) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
            fun instantOrNull(key: String): Instant? =
                (data[key] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() }

            return PredictorWeekendPrediction(
                round = data["round"] as? String ?: "",
                raceName = data["raceName"] as? String ?: "",
                qualifyingOrder = strList("qualifyingOrder"),
                raceOrder = strList("raceOrder"),
                lockedAt = instantOrNull("lockedAt"),
                qualiPoints = intOrNull("qualiPoints"),
                racePoints = intOrNull("racePoints"),
                scoredAt = instantOrNull("scoredAt"),
                actualQualifyingOrder = strListOrNull("actualQualifyingOrder"),
                actualRaceOrder = strListOrNull("actualRaceOrder"),
            )
        }
    }
}

/** Сезон предиктора (`users/.../seasons/{year}`): weekends + суммарные очки. */
data class PredictorSeason(
    val year: String,
    val weekends: Map<String, PredictorWeekendPrediction> = emptyMap(),
) {
    val totalPoints: Int get() = weekends.values.sumOf { it.totalPoints }

    val weekendsSorted: List<PredictorWeekendPrediction>
        get() = weekends.values.sortedBy { it.round.toIntOrNull() ?: Int.MAX_VALUE }

    fun upsertWeekend(weekend: PredictorWeekendPrediction): PredictorSeason =
        copy(weekends = weekends + (weekend.round to weekend))

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "weekends" to weekends.mapValues { (_, w) -> w.toFirestoreMap() },
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(year: String, data: Map<String, Any?>): PredictorSeason {
            val raw = data["weekends"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val weekends = raw.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val map = v as? Map<*, *> ?: return@mapNotNull null
                key to PredictorWeekendPrediction.fromFirestoreMap(
                    map.entries.associate { (mk, mv) -> mk.toString() to mv },
                )
            }.toMap()
            return PredictorSeason(year = year, weekends = weekends)
        }
    }
}

/** In-memory агрегат всех сезонов пользователя. */
data class PredictorStore(
    val seasons: Map<String, PredictorSeason> = emptyMap(),
) {
    fun season(year: String): PredictorSeason? = seasons[year]

    fun weekend(year: String, round: String): PredictorWeekendPrediction? =
        seasons[year]?.weekends?.get(round)

    fun upsertWeekend(year: String, weekend: PredictorWeekendPrediction): PredictorStore {
        val existing = seasons[year] ?: PredictorSeason(year = year)
        return copy(seasons = seasons + (year to existing.upsertWeekend(weekend)))
    }

    companion object {
        fun empty() = PredictorStore()
    }
}

/** Краткая карточка архивного сезона для списка на главном Predictor. */
data class PredictorSeasonSummary(
    val year: String,
    val totalPoints: Int,
    val weekendCount: Int,
) {
    companion object {
        fun fromSeason(season: PredictorSeason) = PredictorSeasonSummary(
            year = season.year,
            totalPoints = season.totalPoints,
            weekendCount = season.weekends.size,
        )
    }
}

/** Какая сетка активна в UI: квалификация или гонка. */
enum class PredictorGridKind { Qualifying, Race }

/** Одна строка сравнения predicted vs actual на позиции. */
data class PredictorComparisonRow(
    val position: Int,
    val predictedDriverId: String?,
    val actualDriverId: String?,
) {
    val isCorrect: Boolean
        get() = predictedDriverId != null && predictedDriverId == actualDriverId
}

/** Side-by-side predicted vs actual + очки сессии для detail-экрана. */
data class PredictorSessionCompare(
    val rows: List<PredictorComparisonRow>,
    val points: Int,
) {
    companion object {
        fun fromOrders(predicted: List<String>, actual: List<String>): PredictorSessionCompare {
            val len = maxOf(predicted.size, actual.size)
            val rows = (0 until len).map { i ->
                PredictorComparisonRow(
                    position = i + 1,
                    predictedDriverId = predicted.getOrNull(i),
                    actualDriverId = actual.getOrNull(i),
                )
            }
            return PredictorSessionCompare(rows = rows, points = PredictorScoreService.scoreOrders(predicted, actual))
        }
    }
}

/** Профиль участия: ник + opt-in; [canShowOnLeaderboard] = оба заданы. */
data class PredictorLeaderboardProfile(
    val nickname: String? = null,
    val leaderboardOptIn: Boolean = false,
) {
    val canShowOnLeaderboard: Boolean
        get() = leaderboardOptIn && !nickname.isNullOrBlank()

    companion object {
        fun fromFirestoreMap(data: Map<String, Any?>?): PredictorLeaderboardProfile {
            if (data == null) return PredictorLeaderboardProfile()
            return PredictorLeaderboardProfile(
                nickname = data["nickname"] as? String,
                leaderboardOptIn = data["leaderboardOptIn"] as? Boolean ?: false,
            )
        }
    }
}

/** Строка таблицы лидерборда с рангом после сортировки. */
data class PredictorLeaderboardEntry(
    val uid: String,
    val nickname: String,
    val totalPoints: Int,
    val rank: Int = 0,
) {
    fun withRank(rank: Int) = copy(rank = rank)

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "nickname" to nickname,
        "totalPoints" to totalPoints,
    )

    companion object {
        fun fromFirestoreMap(uid: String, data: Map<String, Any?>): PredictorLeaderboardEntry =
            PredictorLeaderboardEntry(
                uid = uid,
                nickname = data["nickname"] as? String ?: "",
                totalPoints = (data["totalPoints"] as? Number)?.toInt() ?: 0,
            )
    }
}

/** Результат join/leave/rename: Ok или l10n-ключ ошибки. */
sealed class PredictorLeaderboardResult {
    data object Ok : PredictorLeaderboardResult()
    data class Fail(val errorKey: String) : PredictorLeaderboardResult()
    val isSuccess: Boolean get() = this is Ok
}
