package com.example.f1_kotlin.data.local

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
