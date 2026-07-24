package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единый контракт pull-to-refresh: soft-invalidate кэшей приложения.
 *
 * GoF Structural Facade — один [clearAll] координирует ESPN TTL и in-memory F1-кэши;
 * UI не знает внутренностей репозиториев. Room **не** чистится (offline-fallback).
 */
@Singleton
class AppDataRefresh @Inject constructor(
    private val espnRepository: IEspnRepository,
    private val f1Repository: IF1Repository,
) {
    /** Soft-invalidate перед принудительной перезагрузкой экрана. */
    suspend fun clearAll() {
        espnRepository.clearCaches()
        f1Repository.clearInMemoryCaches()
    }
}
