package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.local.CacheDao
import com.example.f1_kotlin.data.local.CacheEntry
import com.example.f1_kotlin.data.local.CacheJsonMapper
import com.example.f1_kotlin.data.local.CacheKeys
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.DriverStandingsCache
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.HistoricalStandingsCache
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.StandingsListsModel
import com.example.f1_kotlin.domain.ApiCallHandler
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Repository — единая точка доступа к данным.
 *
 * **Общая стратегия для всех экранов:**
 * 1. [peek*Cache] — мгновенно отдать Room/память (UI не «висит» на loader);
 * 2. Сеть через [ApiCallHandler.safeCall] с автоповтором;
 * 3. При ошибке — fallback на Room.
 *
 * API Jolpica лёгкий (~100–300 ms на запрос); тормозит N+1 в пит-стопах — исправлено
 * через кэш имён пилотов и лимит параллельных getDriver.
 */
@Singleton
class F1Repository @Inject constructor(
    private val api: F1ApiService,
    private val cacheDao: CacheDao,
    private val cacheJsonMapper: CacheJsonMapper,
) {
    @Volatile
    private var circuitsMemoryCache: List<CircuitModel>? = null

    private val driverNamesCache = ConcurrentHashMap<String, String>()

    // region peek — мгновенный UI без ожидания сети

    suspend fun peekCurrentDriversCache(): Pair<List<DriverStandingsModel>, StandingsListsModel>? =
        loadCache(CacheKeys.CURRENT_DRIVERS, DriverStandingsCache::class.java)?.let { cached ->
            Pair(cached.drivers, StandingsListsModel(cached.season, cached.round, cached.drivers, null))
        }

    suspend fun peekCurrentConstructorsCache(): List<ConstructorStandingsModel>? =
        loadCacheList(CacheKeys.CURRENT_CONSTRUCTORS, ConstructorStandingsModel::class.java)

    suspend fun peekLastRaceCache(): RaceModel? =
        loadCache(CacheKeys.LAST_RACE, RaceModel::class.java)

    suspend fun peekScheduleCache(): List<RaceModel>? =
        loadCacheList(CacheKeys.SCHEDULE, RaceModel::class.java)

    suspend fun peekCircuitsCache(): List<CircuitModel>? {
        circuitsMemoryCache?.let { return it }
        return loadCacheList(CacheKeys.CIRCUITS, CircuitModel::class.java)?.also { circuitsMemoryCache = it }
    }

    suspend fun peekHistoricalStandingsCache(
        year: String,
    ): Pair<List<DriverStandingsModel>, List<ConstructorStandingsModel>>? =
        loadCache(CacheKeys.historicalStandings(year), HistoricalStandingsCache::class.java)?.let {
            Pair(it.drivers, it.constructors)
        }

    // endregion

    suspend fun getCurrentDriverStandings(): Result<Pair<List<DriverStandingsModel>, StandingsListsModel>> {
        val network = ApiCallHandler.safeCall {
            val list = api.getCurrentDriverStandings().mrData.standingsTable.standingsLists.first()
            Pair(list.driverStandings.orEmpty(), list)
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { (drivers, meta) ->
                saveCache(
                    CacheKeys.CURRENT_DRIVERS,
                    DriverStandingsCache(drivers, meta.season, meta.round),
                    DriverStandingsCache::class.java,
                )
            }
            return network
        }
        return peekCurrentDriversCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getCurrentConstructorStandings(): Result<List<ConstructorStandingsModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentConstructorStandings()
                .mrData.standingsTable.standingsLists.first()
                .constructorStandings.orEmpty()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.CURRENT_CONSTRUCTORS, it, ConstructorStandingsModel::class.java) }
            return network
        }
        return peekCurrentConstructorsCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getLastRace(): Result<RaceModel> {
        val network = ApiCallHandler.safeCall {
            api.getLastRaceResults().mrData.raceTable.races.first()
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCache(CacheKeys.LAST_RACE, it, RaceModel::class.java) }
            return network
        }
        return peekLastRaceCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getRaceResults(year: String, round: String): Result<RaceModel?> =
        ApiCallHandler.safeCall {
            api.getRaceResults(year, round).mrData.raceTable.races.firstOrNull()
        }

    suspend fun getQualifyingResults(year: String, round: String): Result<List<QualifyingResultModel>> =
        ApiCallHandler.safeCall {
            api.getQualifyingResults(year, round).mrData.raceTable.races
                .firstOrNull()?.qualifyingResults.orEmpty()
        }

    /**
     * Пит-стопы: один запрос pitstops + уникальные driverId (не каждая остановка).
     * Имена кэшируются в [driverNamesCache]; не более [MAX_DRIVER_FETCH_PARALLEL] параллельных getDriver.
     */
    suspend fun getPitStopsWithDriverNames(year: String, round: String): Result<List<PitStopModel>> =
        ApiCallHandler.safeCall {
            val stops = api.getPitStops(year, round).mrData.raceTable.races
                .firstOrNull()?.pitStops.orEmpty()
            if (stops.isEmpty()) return@safeCall emptyList()

            val uniqueDriverIds = stops.map { it.driverId }.distinct()
            val namesById = resolveDriverNames(uniqueDriverIds)
            stops.map { stop ->
                namesById[stop.driverId]?.let { stop.copy(driverId = it) } ?: stop
            }
        }

    suspend fun getCurrentSchedule(): Result<List<RaceModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCurrentSchedule().mrData.raceTable.races
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { saveCacheList(CacheKeys.SCHEDULE, it, RaceModel::class.java) }
            return network
        }
        return peekScheduleCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getHistoricalStandings(
        year: String,
    ): Result<Pair<List<DriverStandingsModel>, List<ConstructorStandingsModel>>> {
        val network = ApiCallHandler.safeCall {
            coroutineScope {
                val driversDeferred = async {
                    api.getDriverStandings(year).mrData.standingsTable.standingsLists
                        .firstOrNull()?.driverStandings.orEmpty()
                }
                val constructorsDeferred = async {
                    api.getConstructorStandings(year).mrData.standingsTable.standingsLists
                        .firstOrNull()?.constructorStandings.orEmpty()
                }
                Pair(driversDeferred.await(), constructorsDeferred.await())
            }
        }
        if (network.isSuccess) {
            network.getOrNull()?.let { (drivers, constructors) ->
                saveCache(
                    CacheKeys.historicalStandings(year),
                    HistoricalStandingsCache(drivers, constructors),
                    HistoricalStandingsCache::class.java,
                )
            }
            return network
        }
        return peekHistoricalStandingsCache(year)?.let { Result.success(it) } ?: network
    }

    suspend fun getCircuits(): Result<List<CircuitModel>> {
        val network = ApiCallHandler.safeCall {
            api.getCircuits().mrData.circuitTable.circuits
        }
        if (network.isSuccess) {
            network.getOrNull()?.let {
                circuitsMemoryCache = it
                saveCacheList(CacheKeys.CIRCUITS, it, CircuitModel::class.java)
            }
            return network
        }
        return peekCircuitsCache()?.let { Result.success(it) } ?: network
    }

    suspend fun getCircuitById(circuitId: String): Result<CircuitModel?> {
        circuitsMemoryCache?.find { it.circuitId == circuitId }?.let { return Result.success(it) }
        peekCircuitsCache()?.find { it.circuitId == circuitId }?.let { return Result.success(it) }
        return getCircuits().map { circuits -> circuits.find { it.circuitId == circuitId } }
    }

    private suspend fun resolveDriverNames(driverIds: List<String>): Map<String, String> = coroutineScope {
        val semaphore = Semaphore(MAX_DRIVER_FETCH_PARALLEL)
        driverIds.map { driverId ->
            async {
                driverNamesCache[driverId]?.let { return@async driverId to it }
                semaphore.withPermit {
                    val name = runCatching {
                        api.getDriver(driverId).mrData.driverTable.drivers.firstOrNull()?.fullName
                    }.getOrNull()
                    if (name != null) driverNamesCache[driverId] = name
                    driverId to name
                }
            }
        }.awaitAll().mapNotNull { (id, name) -> name?.let { id to it } }.toMap()
    }

    private suspend fun <T> saveCache(key: String, value: T, clazz: Class<T>) {
        val json = cacheJsonMapper.toJson(value, clazz)
        cacheDao.insert(CacheEntry(key, json, System.currentTimeMillis()))
    }

    private suspend fun <T> saveCacheList(key: String, items: List<T>, itemClass: Class<T>) {
        val json = cacheJsonMapper.toJsonList(items, itemClass)
        cacheDao.insert(CacheEntry(key, json, System.currentTimeMillis()))
    }

    private suspend fun <T> loadCache(key: String, clazz: Class<T>): T? =
        cacheDao.get(key)?.json?.let { cacheJsonMapper.fromJson(it, clazz) }

    private suspend fun <T> loadCacheList(key: String, itemClass: Class<T>): List<T>? =
        cacheDao.get(key)?.json?.let { cacheJsonMapper.fromJsonList(it, itemClass) }

    private companion object {
        const val MAX_DRIVER_FETCH_PARALLEL = 8
    }
}
