package com.example.f1_kotlin.util

import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
        val parts = time.split(":")
        if (parts.size < 2) return null
        val utc = LocalDate.parse(date).atTime(parts[0].toInt(), parts[1].toInt())
        val offsetHours = ZoneId.systemDefault().rules.getOffset(utc.atZone(ZoneId.of("UTC")).toInstant()).totalSeconds / 3600
        return utc.plusHours(offsetHours.toLong())
    }
}

/** Открывает ссылку (например, Wikipedia трассы) во внешнем браузере. */
fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
