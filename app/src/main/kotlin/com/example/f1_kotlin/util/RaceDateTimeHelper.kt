package com.example.f1_kotlin.util

import com.example.f1_kotlin.data.model.RaceDateModel
import com.example.f1_kotlin.data.model.RaceModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Конвертация UTC-дат сессий Ergast/Jolpica в локальное время устройства.
 */
object RaceDateTimeHelper {
    /**
     * Парсит [RaceDateModel] (дата + время в UTC) в локальный [ZonedDateTime].
     * Пустое время трактуется как полночь UTC указанного дня.
     */
    fun toLocal(date: RaceDateModel): ZonedDateTime =
        DateUtils.parseUtcSession(date.date, date.time).withZoneSameInstant(ZoneId.systemDefault())

    /** Локальное время старта основной гонки. */
    fun raceLocal(race: RaceModel): ZonedDateTime =
        toLocal(RaceDateModel(date = race.date, time = race.time))

    /** Цель countdown: FP1, иначе первая доступная сессия, иначе гонка. */
    fun countdownTarget(race: RaceModel): ZonedDateTime {
        for (session in orderedSessions(race)) {
            if (session != null) return toLocal(session)
        }
        return raceLocal(race)
    }

    /** Гонка ещё не стартовала (по времени race). */
    fun isUpcoming(race: RaceModel, now: ZonedDateTime = ZonedDateTime.now()): Boolean =
        raceLocal(race).isAfter(now)

    /** Начало уикенда (первая сессия или гонка). */
    fun weekendStart(race: RaceModel): ZonedDateTime = countdownTarget(race)

    private fun orderedSessions(race: RaceModel): List<RaceDateModel?> = listOf(
        race.firstPractice,
        race.secondPractice,
        race.thirdPractice,
        race.sprintQualifying,
        race.sprint,
        race.qualifying,
    )
}

/** Разбивка оставшегося времени до события. */
data class CountdownParts(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val isZero: Boolean get() = days == 0 && hours == 0 && minutes == 0 && seconds == 0

    companion object {
        val ZERO = CountdownParts(0, 0, 0, 0)

        fun until(target: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): CountdownParts {
            var diff = Duration.between(now, target)
            if (diff.isNegative) return ZERO
            val days = diff.toDays().toInt()
            diff = diff.minusDays(days.toLong())
            val hours = diff.toHours().toInt()
            diff = diff.minusHours(hours.toLong())
            val minutes = diff.toMinutes().toInt()
            diff = diff.minusMinutes(minutes.toLong())
            return CountdownParts(days, hours, minutes, diff.seconds.toInt())
        }

        fun until(target: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): CountdownParts =
            until(
                target.atZone(ZoneId.systemDefault()),
                now.atZone(ZoneId.systemDefault()),
            )
    }
}
