package com.example.f1_kotlin.util

/** Валидация пользовательского ввода (год сезона / номер гонки). */
object F1InputValidation {
    const val MIN_F1_YEAR = 1950
    const val MAX_F1_YEAR = 2030
    const val MAX_RACE_ROUND = 99

    /** Четырёхзначный год F1 в допустимом диапазоне. */
    fun isValidYear(text: String): Boolean {
        if (text.length != 4) return false
        val year = text.toIntOrNull() ?: return false
        return year in MIN_F1_YEAR..MAX_F1_YEAR
    }

    /** Номер гонки в сезоне: 1–99. */
    fun isValidRound(text: String): Boolean {
        if (text.isEmpty()) return false
        val round = text.toIntOrNull() ?: return false
        return round in 1..MAX_RACE_ROUND
    }
}
