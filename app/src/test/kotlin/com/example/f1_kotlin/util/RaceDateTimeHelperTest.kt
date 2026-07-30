package com.example.f1_kotlin.util

import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceSession
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceDateTimeHelperTest {

    @Test
    fun countdownTarget_prefersFirstPractice() {
        val race = sampleRace(
            firstPractice = RaceSession("2030-01-01", "10:00:00Z"),
            date = "2030-01-03",
            time = "14:00:00Z",
        )
        val local = RaceDateTimeHelper.countdownTarget(race)
        assertEquals(2030, local.year)
        assertEquals(1, local.monthValue)
        assertEquals(1, local.dayOfMonth)
    }

    @Test
    fun isUpcoming_trueForFutureRace() {
        val race = sampleRace(date = "2099-06-01", time = "12:00:00Z")
        assertTrue(RaceDateTimeHelper.isUpcoming(race))
    }

    @Test
    fun isUpcoming_falseForPastRace() {
        val race = sampleRace(date = "2000-06-01", time = "12:00:00Z")
        assertFalse(RaceDateTimeHelper.isUpcoming(race))
    }

    @Test
    fun countdownParts_untilZoned() {
        val now = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val target = ZonedDateTime.of(2026, 1, 2, 1, 2, 3, 0, ZoneOffset.UTC)
        val parts = CountdownParts.until(target, now)
        assertEquals(1, parts.days)
        assertEquals(1, parts.hours)
        assertEquals(2, parts.minutes)
        assertEquals(3, parts.seconds)
        assertFalse(parts.isZero)
        assertTrue(CountdownParts.ZERO.isZero)
    }

    @Test
    fun countdownParts_pastTarget_isZero() {
        val now = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC)
        val target = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(CountdownParts.ZERO, CountdownParts.until(target, now))
    }

    private fun sampleRace(
        date: String,
        time: String? = null,
        firstPractice: RaceSession? = null,
    ) = Race(
        season = "2026",
        round = "1",
        url = "",
        raceName = "Test",
        circuit = Circuit(
            "test", "", "Test",
            CircuitLocation("0", "0", "City", "Country"),
        ),
        date = date,
        time = time,
        firstPractice = firstPractice,
    )
}
