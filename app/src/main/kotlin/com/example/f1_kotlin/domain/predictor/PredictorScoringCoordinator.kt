package com.example.f1_kotlin.domain.predictor

import com.example.f1_kotlin.data.repository.IF1Repository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictorScoringCoordinator @Inject constructor(
    private val f1Repository: IF1Repository,
) {
    suspend fun scoreAllPending(store: PredictorStore, year: String, now: Instant = Instant.now()): PredictorStore? {
        val season = store.season(year) ?: return null
        var next = store
        var changed = false
        for (weekend in season.weekendsSorted) {
            val scored = scoreWeekend(year, weekend, now) ?: continue
            if (weekendScoreChanged(weekend, scored)) {
                next = next.upsertWeekend(year, scored)
                changed = true
            }
        }
        return next.takeIf { changed }
    }

    suspend fun scoreWeekend(
        year: String,
        weekend: PredictorWeekendPrediction,
        now: Instant = Instant.now(),
    ): PredictorWeekendPrediction? {
        if (weekend.qualifyingOrder.isEmpty() && weekend.raceOrder.isEmpty()) return null

        var qualiActual = weekend.actualQualifyingOrder
        var raceActual = weekend.actualRaceOrder
        var qualiResults = emptyList<com.example.f1_kotlin.domain.model.QualifyingResult>()
        var raceResults = emptyList<com.example.f1_kotlin.domain.model.RaceResult>()

        if (qualiActual == null) {
            f1Repository.getQualifyingResults(year, weekend.round).getOrNull()?.let {
                qualiResults = it
                qualiActual = PredictorScoreService.qualifyingActualOrder(it)
            }
        }
        if (raceActual == null) {
            f1Repository.getRaceResults(year, weekend.round).getOrNull()?.results?.let {
                raceResults = it
                raceActual = PredictorScoreService.raceActualOrder(it)
            }
        }

        if (qualiActual == null && raceActual == null) return null

        return PredictorScoreService.applyResults(
            weekend = weekend,
            qualifyingResults = qualiResults.takeIf { it.isNotEmpty() },
            raceResults = raceResults.takeIf { it.isNotEmpty() },
            actualQualifyingOrder = qualiActual,
            actualRaceOrder = raceActual,
            now = now,
        )
    }

    fun weekendScoreChanged(before: PredictorWeekendPrediction, after: PredictorWeekendPrediction): Boolean =
        before.qualiPoints != after.qualiPoints ||
            before.racePoints != after.racePoints ||
            before.actualQualifyingOrder != after.actualQualifyingOrder ||
            before.actualRaceOrder != after.actualRaceOrder
}
