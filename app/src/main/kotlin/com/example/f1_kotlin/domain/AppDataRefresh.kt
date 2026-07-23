package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.local.CacheDao
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единый контракт pull-to-refresh: сбрасывает слои кэша приложения.
 *
 * Facade — один метод [clearAll] закрывает подсистему из ESPN TTL-кэша,
 * Room Jolpica-кэша и in-memory кэшей F1Repository.
 */
@Singleton
class AppDataRefresh @Inject constructor(
    private val espnRepository: IEspnRepository,
    private val cacheDao: CacheDao,
    private val f1Repository: IF1Repository,
) {
    /** Очищает все кэши перед принудительной перезагрузкой экрана. */
    suspend fun clearAll() {
        espnRepository.clearCaches()
        cacheDao.clearAll()
        f1Repository.clearInMemoryCaches()
    }
}
