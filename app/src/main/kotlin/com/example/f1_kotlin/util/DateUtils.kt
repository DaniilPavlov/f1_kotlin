package com.example.f1_kotlin.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Утилиты для работы с датами и временем F1-сессий.
 *
 * API Ergast отдаёт дату и время в UTC (например `"14:00:00"`). Перед показом пользователю
 * время нужно перевести в локальный часовой пояс устройства — для этого [toLocalDateTime].
 */
object DateUtils {
    /** Сравнивает две даты без учёта времени. */
    fun isSameDay(first: LocalDate, second: LocalDate): Boolean = first == second

    /**
     * Название месяца в именительном падеже на языке текущей локали приложения.
     * Например: `3` → «Март» / «March».
     */
    fun monthName(month: Int, locale: Locale = Locale.getDefault()): String =
        java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    /** Форматирует время как `HH:mm`. */
    fun formatHourMinute(dateTime: LocalDateTime): String =
        "%02d:%02d".format(dateTime.hour, dateTime.minute)

    /**
     * Собирает [LocalDateTime] из даты и времени гонки/сессии и переводит UTC → локальное время.
     *
     * Если время в ответе API пустое, возвращает null (сессия без точного времени).
     */
    fun toLocalDateTime(date: String, time: String?): LocalDateTime? {
        if (time.isNullOrBlank()) return null
        return parseUtcSession(date, time).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    /**
     * Парсит UTC-дату/время сессии. Пустое [time] → полночь UTC.
     */
    fun parseUtcSession(date: String, time: String?): ZonedDateTime {
        val raw = time?.trim().orEmpty().removeSuffix("Z")
        val localTime = if (raw.isEmpty()) {
            LocalTime.MIDNIGHT
        } else {
            val parts = raw.split(":")
            require(parts.size >= 2) { "Invalid session time: $time" }
            LocalTime.of(
                parts[0].toInt(),
                parts[1].toInt(),
                parts.getOrNull(2)?.toInt() ?: 0,
            )
        }
        return LocalDate.parse(date).atTime(localTime).atZone(ZoneOffset.UTC)
    }
}
