package com.example.f1_kotlin.domain.predictor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictorModelsTest {
    @Test
    fun weekend_totalPoints_andFirestoreRoundTrip() {
        val weekend = PredictorWeekendPrediction(
            round = "3",
            raceName = "Australia",
            qualifyingOrder = listOf("a", "b"),
            raceOrder = listOf("b", "a"),
            qualiPoints = 2,
            racePoints = 1,
        )
        assertEquals(3, weekend.totalPoints)
        assertTrue(weekend.hasAnyPoints)

        val restored = PredictorWeekendPrediction.fromFirestoreMap(weekend.toFirestoreMap())
        assertEquals(weekend.round, restored.round)
        assertEquals(weekend.qualifyingOrder, restored.qualifyingOrder)
        assertEquals(weekend.raceOrder, restored.raceOrder)
        assertEquals(2, restored.qualiPoints)
        assertEquals(1, restored.racePoints)
    }

    @Test
    fun season_upsertAndSummary() {
        val season = PredictorSeason(year = "2026")
            .upsertWeekend(PredictorWeekendPrediction(round = "1", qualiPoints = 1, racePoints = 2))
            .upsertWeekend(PredictorWeekendPrediction(round = "2", qualiPoints = 0, racePoints = 3))
        assertEquals(6, season.totalPoints)
        val summary = PredictorSeasonSummary.fromSeason(season)
        assertEquals("2026", summary.year)
        assertEquals(2, summary.weekendCount)
        assertEquals(6, summary.totalPoints)
    }

    @Test
    fun store_weekendLookup() {
        val store = PredictorStore.empty()
            .upsertWeekend("2026", PredictorWeekendPrediction(round = "5", raceName = "Japan"))
        assertEquals("Japan", store.weekend("2026", "5")?.raceName)
        assertEquals(null, store.weekend("2025", "5"))
    }

    @Test
    fun sessionCompare_marksCorrectRows() {
        val compare = PredictorSessionCompare.fromOrders(listOf("a", "b", "c"), listOf("a", "x", "c"))
        assertEquals(2, compare.points)
        assertTrue(compare.rows[0].isCorrect)
        assertFalse(compare.rows[1].isCorrect)
        assertTrue(compare.rows[2].isCorrect)
    }

    @Test
    fun leaderboardProfile_optInRequiresNickname() {
        assertFalse(PredictorLeaderboardProfile(nickname = "Ace", leaderboardOptIn = false).canShowOnLeaderboard)
        assertFalse(PredictorLeaderboardProfile(nickname = null, leaderboardOptIn = true).canShowOnLeaderboard)
        assertTrue(PredictorLeaderboardProfile(nickname = "Ace", leaderboardOptIn = true).canShowOnLeaderboard)
    }
}
