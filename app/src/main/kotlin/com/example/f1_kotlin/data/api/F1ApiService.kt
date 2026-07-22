package com.example.f1_kotlin.data.api

import com.example.f1_kotlin.data.model.CircuitsModel
import com.example.f1_kotlin.data.model.ConstructorFetchingModel
import com.example.f1_kotlin.data.model.DriverFetchingModel
import com.example.f1_kotlin.data.model.MrDataResponse
import com.example.f1_kotlin.data.model.MrDataTotalModel
import com.example.f1_kotlin.data.model.ScheduleModel
import com.example.f1_kotlin.data.model.StandingsModel
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit-интерфейс: описание REST API Ergast (зеркало Jolpica).
 *
 * Каждый метод — один HTTP GET. Retrofit сам:
 * - подставляет [Path] и [Query] в URL
 * - вызывает suspend-функцию в корутине (не блокирует UI-поток)
 * - парсит JSON в указанный тип ответа
 *
 * Все ответы обёрнуты в `{ "MRData": { ... } }` → [MrDataResponse].
 */
interface F1ApiService {

    /** Текущий чемпионат пилотов (вкладка «Главная»). */
    @GET("current/driverStandings.json")
    suspend fun getCurrentDriverStandings(@Query("limit") limit: Int = 100): MrDataResponse<StandingsModel>

    /** Текущий чемпионат конструкторов. */
    @GET("current/constructorStandings.json")
    suspend fun getCurrentConstructorStandings(@Query("limit") limit: Int = 100): MrDataResponse<StandingsModel>

    /** Результаты последней завершённой гонки. */
    @GET("current/last/results.json")
    suspend fun getLastRaceResults(@Query("limit") limit: Int = 100): MrDataResponse<ScheduleModel>

    /** Результаты конкретной гонки по году и номеру раунда. */
    @GET("{year}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("year") year: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Результаты спринта конкретной гонки. */
    @GET("{year}/{round}/sprint.json")
    suspend fun getSprintResults(
        @Path("year") year: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Квалификация гонки (Q1/Q2/Q3). */
    @GET("{year}/{round}/qualifying.json")
    suspend fun getQualifyingResults(
        @Path("year") year: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Пит-стопы гонки. В ответе только driverId — имя подтягивается отдельным запросом. */
    @GET("{year}/{round}/pitstops.json")
    suspend fun getPitStops(
        @Path("year") year: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Карточка пилота по ID (для расшифровки пит-стопов). */
    @GET("drivers/{driverId}.json")
    suspend fun getDriver(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<DriverFetchingModel>

    /** Календарь текущего сезона со всеми сессиями. */
    @GET("current.json")
    suspend fun getCurrentSchedule(@Query("limit") limit: Int = 100): MrDataResponse<ScheduleModel>

    /** Итоговая таблица пилотов за указанный год («Зал славы»). */
    @GET("{year}/driverStandings.json")
    suspend fun getDriverStandings(
        @Path("year") year: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<StandingsModel>

    /** Итоговая таблица конструкторов за указанный год. */
    @GET("{year}/constructorStandings.json")
    suspend fun getConstructorStandings(
        @Path("year") year: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<StandingsModel>

    /** Список всех F1-трасс с координатами. */
    @GET("circuits.json")
    suspend fun getCircuits(@Query("limit") limit: Int = 100): MrDataResponse<CircuitsModel>

    /** Список сезонов F1 (для picker). */
    @GET("seasons.json")
    suspend fun getSeasons(@Query("limit") limit: Int = 100): MrDataResponse<MrDataTotalModel>

    /** Календарь конкретного сезона (для picker раундов). */
    @GET("{year}.json")
    suspend fun getSeasonSchedule(
        @Path("year") year: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Карточка конструктора по ID. */
    @GET("constructors/{constructorId}.json")
    suspend fun getConstructor(
        @Path("constructorId") constructorId: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ConstructorFetchingModel>

    /** История побед на трассе (все ГП с position=1). */
    @GET("circuits/{circuitId}/results/1.json")
    suspend fun getCircuitWinners(
        @Path("circuitId") circuitId: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<ScheduleModel>

    /** Totals и таблицы для карьерной статистики (поле total в MRData). */
    @GET("{path}.json")
    suspend fun getMrDataTotal(
        @Path(value = "path", encoded = true) path: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): MrDataResponse<MrDataTotalModel>

    /** Статусы финиша сезона (`Finished`, `Retired`, …). */
    @GET("{year}/status.json")
    suspend fun getSeasonStatus(
        @Path("year") year: String,
        @Query("limit") limit: Int = 100,
    ): MrDataResponse<MrDataTotalModel>

    /** Пилоты текущего сезона. */
    @GET("current/drivers.json")
    suspend fun getCurrentDrivers(@Query("limit") limit: Int = 100): MrDataResponse<DriverFetchingModel>

    /** Все пилоты (пагинация). */
    @GET("drivers.json")
    suspend fun getAllDrivers(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): MrDataResponse<DriverFetchingModel>

    /** Конструкторы текущего сезона. */
    @GET("current/constructors.json")
    suspend fun getCurrentConstructors(@Query("limit") limit: Int = 100): MrDataResponse<ConstructorFetchingModel>

    /** Все конструкторы (пагинация). */
    @GET("constructors.json")
    suspend fun getAllConstructors(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): MrDataResponse<ConstructorFetchingModel>
}
