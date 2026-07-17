package com.example.f1_kotlin.data.api

import com.example.f1_kotlin.data.model.CircuitsModel
import com.example.f1_kotlin.data.model.DriverFetchingModel
import com.example.f1_kotlin.data.model.MrDataResponse
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
}
