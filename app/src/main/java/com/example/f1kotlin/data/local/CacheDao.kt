package com.example.f1kotlin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO (Data Access Object) — интерфейс Room для работы с таблицей [CacheEntry].
 *
 * Room генерирует реализацию на этапе компиляции (KSP). Все методы suspend —
 * вызываются из корутин Repository, не блокируют главный поток.
 */
@Dao
interface CacheDao {

    /** Читает одну запись кэша по ключу или null, если ещё не сохраняли. */
    @Query("SELECT * FROM cache_entries WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheEntry?

    /** Сохраняет или перезаписывает запись с тем же [CacheEntry.key]. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CacheEntry)
}
