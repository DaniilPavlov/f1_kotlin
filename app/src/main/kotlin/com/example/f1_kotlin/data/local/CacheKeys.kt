package com.example.f1_kotlin.data.local

/** Константы ключей кэша — не опечатайся в строках при чтении/записи. */
object CacheKeys {
    const val CURRENT_DRIVERS = "current_drivers"
    const val CURRENT_CONSTRUCTORS = "current_constructors"
    const val SCHEDULE = "schedule"
    const val CIRCUITS = "circuits"
    const val LAST_RACE = "last_race"
    const val SEASONS = "seasons"

    fun historicalStandings(year: String) = "historical_standings_$year"
}
