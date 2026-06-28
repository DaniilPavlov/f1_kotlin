package com.example.f1kotlin.di

import android.content.Context
import androidx.room.Room
import com.example.f1kotlin.data.local.CacheDao
import com.example.f1kotlin.data.local.F1Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль локальной базы Room.
 *
 * [@ApplicationContext] — безопасный Context приложения (не Activity, которая может уничтожиться).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Создаёт SQLite-файл `f1_kotlin.db` в internal storage устройства. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): F1Database =
        Room.databaseBuilder(context, F1Database::class.java, "f1_kotlin.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCacheDao(database: F1Database): CacheDao = database.cacheDao()
}
