package com.example.f1_kotlin.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit-тесты [DateUtils] — чистая логика дат без сети и UI. */
class DateUtilsTest {

    @Test
    fun isSameDay_equalDates_returnsTrue() {
        val date = LocalDate.of(2026, 5, 10)
        assertTrue(DateUtils.isSameDay(date, date))
    }

    @Test
    fun isSameDay_differentDates_returnsFalse() {
        assertFalse(
            DateUtils.isSameDay(
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
            ),
        )
    }

    @Test
    fun monthName_returnsRussianName() {
        assertEquals("Март", DateUtils.monthName(3, Locale.forLanguageTag("ru")))
    }

    @Test
    fun formatHourMinute_padsSingleDigits() {
        val dateTime = LocalDateTime.of(2026, 5, 10, 9, 5)
        assertEquals("09:05", DateUtils.formatHourMinute(dateTime))
    }

    @Test
    fun toLocalDateTime_blankTime_returnsNull() {
        assertNull(DateUtils.toLocalDateTime("2026-05-10", null))
        assertNull(DateUtils.toLocalDateTime("2026-05-10", "   "))
    }

    @Test
    fun toLocalDateTime_validTime_returnsLocalDateTime() {
        val result = DateUtils.toLocalDateTime("2026-05-10", "14:00:00Z")
        assertEquals(2026, result!!.year)
        assertEquals(5, result.monthValue)
        assertEquals(10, result.dayOfMonth)
    }

    @Test
    fun parseUtcSession_blankTime_isMidnightUtc() {
        val zoned = DateUtils.parseUtcSession("2026-05-10", null)
        assertEquals(2026, zoned.withZoneSameInstant(ZoneOffset.UTC).year)
        assertEquals(0, zoned.withZoneSameInstant(ZoneOffset.UTC).hour)
        assertEquals(0, zoned.withZoneSameInstant(ZoneOffset.UTC).minute)
    }

    @Test
    fun parseUtcSession_withTime_keepsUtcComponents() {
        val zoned = DateUtils.parseUtcSession("2026-05-10", "14:30:00Z")
        val utc = zoned.withZoneSameInstant(ZoneOffset.UTC)
        assertEquals(14, utc.hour)
        assertEquals(30, utc.minute)
    }
}
