package com.example.f1kotlin.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Одна строка локального кэша в Room.
 *
 * Вместо отдельных таблиц под каждый тип данных храним JSON-строку по ключу —
 * проще для pet-проекта. Сериализация через [CacheJsonMapper] (Moshi).
 */
@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long,
)

/** Константы ключей кэша — не опечатайся в строках при чтении/записи. */
object CacheKeys {
    const val CURRENT_DRIVERS = "current_drivers"
    const val CURRENT_CONSTRUCTORS = "current_constructors"
    const val SCHEDULE = "schedule"
    const val CIRCUITS = "circuits"
    const val LAST_RACE = "last_race"

    fun historicalStandings(year: String) = "historical_standings_$year"
}
