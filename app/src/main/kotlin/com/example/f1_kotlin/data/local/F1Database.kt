package com.example.f1_kotlin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Локальная SQLite-база через Room (Android Architecture Component).
 *
 * Сейчас одна таблица [CacheEntry] — offline-кэш ответов API.
 * [exportSchema = false] — не генерируем JSON-схемы миграций (для pet-проекта достаточно).
 */
@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class F1Database : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
