package com.example.f1_kotlin.domain.predictor

import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.QualifyingResult
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceResult
import com.example.f1_kotlin.domain.model.RaceSession
import com.example.f1_kotlin.util.RaceDateTimeHelper
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

object PredictorLock {
    private val lead: Duration = Duration.ofHours(1)

    fun lockAt(race: Race): ZonedDateTime? {
        val quali = race.qualifying ?: return null
        return RaceDateTimeHelper.toLocal(quali).minus(lead)
    }

    fun isLocked(race: Race, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val at = lockAt(race) ?: return false
        return !now.isBefore(at)
    }
}

object PredictorScoreService {
    fun scoreOrders(predicted: List<String>, actualByPosition: List<String>): Int {
        val length = minOf(predicted.size, actualByPosition.size)
        var points = 0
        for (i in 0 until length) {
            if (predicted[i] == actualByPosition[i]) points++
        }
        return points
    }

    fun qualifyingActualOrder(results: List<QualifyingResult>): List<String> =
        results.sortedBy { it.position.toIntOrNull() ?: 999 }
            .map { it.driver.driverId }

    fun raceActualOrder(results: List<RaceResult>): List<String> =
        results.filter { it.positionText.toIntOrNull() != null }
            .sortedBy { it.position.toIntOrNull() ?: 999 }
            .map { it.driver.driverId }

    fun applyResults(
        weekend: PredictorWeekendPrediction,
        qualifyingResults: List<QualifyingResult>? = null,
        raceResults: List<RaceResult>? = null,
        actualQualifyingOrder: List<String>? = null,
        actualRaceOrder: List<String>? = null,
        now: Instant = Instant.now(),
    ): PredictorWeekendPrediction {
        val qualiActual = actualQualifyingOrder
            ?: qualifyingResults?.let { qualifyingActualOrder(it) }
        val raceActual = actualRaceOrder
            ?: raceResults?.let { raceActualOrder(it) }

        val qualiPts = qualiActual?.let { scoreOrders(weekend.qualifyingOrder, it) }
        val racePts = raceActual?.let { scoreOrders(weekend.raceOrder, it) }

        return weekend.copy(
            qualiPoints = qualiPts ?: weekend.qualiPoints,
            racePoints = racePts ?: weekend.racePoints,
            actualQualifyingOrder = qualiActual ?: weekend.actualQualifyingOrder,
            actualRaceOrder = raceActual ?: weekend.actualRaceOrder,
            scoredAt = if (qualiPts != null || racePts != null) now else weekend.scoredAt,
        )
    }
}

object PredictorOrder {
    fun hasUsableDriverCode(driver: Driver): Boolean {
        val code = driver.code?.trim().orEmpty()
        return code.isNotEmpty() && !code.equals("none", ignoreCase = true)
    }

    fun defaultPredictorOrder(rosterIds: List<String>, championshipOrder: List<String>): List<String> {
        val roster = rosterIds.toSet()
        val fromStandings = championshipOrder.filter { it in roster }
        val rest = rosterIds.filter { it !in fromStandings.toSet() }
        return fromStandings + rest
    }

    fun syncOrderToRoster(saved: List<String>, rosterIds: List<String>): List<String> {
        val roster = rosterIds.toSet()
        val kept = saved.filter { it in roster }
        val missing = rosterIds.filter { it !in kept.toSet() }
        return kept + missing
    }
}

object PredictorNickname {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 16
    private val allowed = Regex("^[a-zA-Z0-9_]+$")

    fun normalize(raw: String): String = raw.trim().lowercase()

    /** null = ok, иначе ключ ошибки. */
    fun validate(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length !in MIN_LENGTH..MAX_LENGTH) {
            return "predictorNicknameErrorLength"
        }
        if (!allowed.matches(trimmed)) {
            return "predictorNicknameErrorChars"
        }
        return null
    }
}

fun predictorDriverLabel(driver: Driver?, fallbackId: String?): String {
    if (driver != null) {
        val code = driver.code?.trim().orEmpty()
        if (code.isNotEmpty() && !code.equals("none", ignoreCase = true)) {
            return "$code · ${driver.familyName}"
        }
        return driver.fullName
    }
    return fallbackId?.takeIf { it.isNotBlank() } ?: "—"
}
