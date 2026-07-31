package com.example.f1_kotlin.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class H2hPointsTimelineTest {
    @Test
    fun fromScores_empty_isEmptyAndMaxZero() {
        val timeline = H2hPointsTimeline.fromScores(emptyList(), emptyList())
        assertTrue(timeline.isEmpty)
        assertEquals(0.0, timeline.maxCumulative, 0.0)
    }

    @Test
    fun fromScores_accumulatesAndSortsBySeasonRound() {
        val a = listOf(
            H2hRoundScore("2025", "2", "Bahrain", 18.0),
            H2hRoundScore("2025", "1", "Australia", 25.0),
            H2hRoundScore("2024", "22", "Abu Dhabi", 8.0),
        )
        val b = listOf(
            H2hRoundScore("2025", "1", "Australia", 18.0),
            H2hRoundScore("2025", "2", "Bahrain", 25.0),
        )

        val timeline = H2hPointsTimeline.fromScores(a, b)
        assertEquals(3, timeline.points.size)
        assertEquals("2024", timeline.points[0].season)
        assertEquals("22", timeline.points[0].round)
        assertEquals(8.0, timeline.points[0].cumulativeA, 0.0)
        assertEquals(0.0, timeline.points[0].cumulativeB, 0.0)

        assertEquals(33.0, timeline.points[1].cumulativeA, 0.0) // 8 + 25
        assertEquals(18.0, timeline.points[1].cumulativeB, 0.0)
        assertEquals(51.0, timeline.points[2].cumulativeA, 0.0) // 33 + 18
        assertEquals(43.0, timeline.points[2].cumulativeB, 0.0)
        assertEquals(51.0, timeline.maxCumulative, 0.0)
        assertFalse(timeline.isEmpty)
    }

    @Test
    fun fromScores_seasonScope_usesRoundOnlyLabels() {
        val a = listOf(H2hRoundScore("2026", "1", "Australia", 10.0))
        val b = listOf(H2hRoundScore("2026", "1", "Australia", 8.0))
        val timeline = H2hPointsTimeline.fromScores(a, b, seasonScope = "2026")
        assertEquals("1", timeline.points.single().label)
    }

    @Test
    fun fromScores_careerScope_usesSeasonRoundLabels() {
        val a = listOf(H2hRoundScore("2026", "3", "Japan", 12.0))
        val timeline = H2hPointsTimeline.fromScores(a, emptyList(), seasonScope = null)
        assertEquals("2026 · R3", timeline.points.single().label)
        assertEquals(12.0, timeline.points.single().roundPointsA, 0.0)
        assertEquals(0.0, timeline.points.single().roundPointsB, 0.0)
    }

    @Test
    fun roundScore_keyAndRoundNumber() {
        val score = H2hRoundScore("2026", "10", "Monaco", 1.0)
        assertEquals("2026-10", score.key)
        assertEquals(10, score.roundNumber)
        assertEquals(0, H2hRoundScore("2026", "x", "", 0.0).roundNumber)
    }
}
