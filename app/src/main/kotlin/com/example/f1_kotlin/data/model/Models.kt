package com.example.f1_kotlin.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data-модели ответов Ergast API.
 *
 * Имена полей в JSON часто в PascalCase (`MRData`, `DriverStandings`),
 * а в Kotlin — camelCase. Аннотация [@Json] связывает их при парсинге Moshi.
 *
 * [@JsonClass(generateAdapter = true)] — Moshi может сгенерировать адаптер;
 * сейчас используется [KotlinJsonAdapterFactory] (рефлексия).
 */

/** Обёртка всех ответов API: `{ "MRData": { ... } }`. */
@JsonClass(generateAdapter = true)
data class MrDataResponse<T>(
    @Json(name = "MRData") val mrData: T,
)

@JsonClass(generateAdapter = true)
data class StandingsModel(
    @Json(name = "StandingsTable") val standingsTable: StandingsTableModel,
)

@JsonClass(generateAdapter = true)
data class StandingsTableModel(
    @Json(name = "StandingsLists") val standingsLists: List<StandingsListsModel>,
)

/** Один «срез» чемпионата: сезон, раунд и списки пилотов или конструкторов. */
@JsonClass(generateAdapter = true)
data class StandingsListsModel(
    val season: String,
    val round: String,
    @Json(name = "DriverStandings") val driverStandings: List<DriverStandingsModel>? = null,
    @Json(name = "ConstructorStandings") val constructorStandings: List<ConstructorStandingsModel>? = null,
)

@JsonClass(generateAdapter = true)
data class DriverStandingsModel(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @Json(name = "Driver") val driver: DriverModel,
    @Json(name = "Constructors") val constructors: List<ConstructorModel>,
)

@JsonClass(generateAdapter = true)
data class ConstructorStandingsModel(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @Json(name = "Constructor") val constructor: ConstructorModel,
)

@JsonClass(generateAdapter = true)
data class DriverModel(
    val driverId: String,
    val url: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String,
    val code: String? = null,
    val permanentNumber: String? = null,
) {
    /** Удобное поле для UI вместо склейки имени в каждой таблице. */
    val fullName: String get() = "$givenName $familyName"
}

@JsonClass(generateAdapter = true)
data class ConstructorModel(
    val constructorId: String,
    val url: String,
    val name: String,
    val nationality: String,
)

@JsonClass(generateAdapter = true)
data class ScheduleModel(
    @Json(name = "RaceTable") val raceTable: RaceTableModel,
)

@JsonClass(generateAdapter = true)
data class RaceTableModel(
    val season: String? = null,
    val round: String? = null,
    @Json(name = "Races") val races: List<RaceModel>,
)

/**
     * Гонка — центральная сущность приложения.
     * Может содержать расписание сессий, результаты, квалификацию и пит-стопы
     * в зависимости от того, какой эндпоинт её вернул.
    */
@JsonClass(generateAdapter = true)
data class RaceModel(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    @Json(name = "Circuit") val circuit: CircuitModel,
    val date: String,
    val time: String? = null,
    @Json(name = "FirstPractice") val firstPractice: RaceDateModel? = null,
    @Json(name = "SecondPractice") val secondPractice: RaceDateModel? = null,
    @Json(name = "ThirdPractice") val thirdPractice: RaceDateModel? = null,
    @Json(name = "Qualifying") val qualifying: RaceDateModel? = null,
    @Json(name = "Sprint") val sprint: RaceDateModel? = null,
    @Json(name = "Results") val results: List<RaceResultModel>? = null,
    @Json(name = "QualifyingResults") val qualifyingResults: List<QualifyingResultModel>? = null,
    @Json(name = "PitStops") val pitStops: List<PitStopModel>? = null,
) {
    /**
     * Находит лучшее время круга среди финишировавших.
     * Строки сравниваются лексикографически — для формата Ergast `M:SS.mmm` это работает.
     * `999999` — sentinel «круга ещё не было».
    */
    val fastestLapTime: String
        get() {
            var fastest = "999999"
            results?.forEach { result ->
                val lap = result.fastestLap?.time?.time
                if (lap != null && fastest > lap) {
                    fastest = lap
                }
            }
            return fastest
        }
}

/** Дата и время одной сессии (практика, квалификация, гонка). */
@JsonClass(generateAdapter = true)
data class RaceDateModel(
    val date: String,
    val time: String? = null,
)

@JsonClass(generateAdapter = true)
data class RaceResultModel(
    val number: String,
    val position: String,
    val positionText: String,
    val points: String,
    @Json(name = "Driver") val driver: DriverModel,
    @Json(name = "Constructor") val constructor: ConstructorModel,
    val grid: String,
    val laps: String,
    val status: String,
    @Json(name = "Time") val time: TimeModel? = null,
    @Json(name = "FastestLap") val fastestLap: FastestLapModel? = null,
)

@JsonClass(generateAdapter = true)
data class TimeModel(
    val millis: String? = null,
    val time: String,
)

@JsonClass(generateAdapter = true)
data class FastestLapModel(
    val rank: String,
    val lap: String,
    @Json(name = "Time") val time: TimeModel,
    @Json(name = "AverageSpeed") val averageSpeed: AverageSpeedModel? = null,
)

@JsonClass(generateAdapter = true)
data class AverageSpeedModel(
    val units: String,
    val speed: String,
)

/** Результат квалификации: три сегмента Q1/Q2/Q3. Пустые поля = пилот выбыл раньше. */
@JsonClass(generateAdapter = true)
data class QualifyingResultModel(
    val number: String,
    val position: String,
    val positionText: String? = null,
    @Json(name = "Driver") val driver: DriverModel,
    @Json(name = "Constructor") val constructor: ConstructorModel,
    val Q1: String? = null,
    val Q2: String? = null,
    val Q3: String? = null,
)

@JsonClass(generateAdapter = true)
data class PitStopModel(
    val driverId: String,
    val lap: String,
    val stop: String,
    val time: String,
    val duration: String,
)

@JsonClass(generateAdapter = true)
data class CircuitsModel(
    @Json(name = "CircuitTable") val circuitTable: CircuitTableModel,
)

@JsonClass(generateAdapter = true)
data class CircuitTableModel(
    @Json(name = "Circuits") val circuits: List<CircuitModel>,
)

@JsonClass(generateAdapter = true)
data class CircuitModel(
    val circuitId: String,
    val url: String,
    val circuitName: String,
    @Json(name = "Location") val location: CircuitLocationModel,
)

@JsonClass(generateAdapter = true)
data class CircuitLocationModel(
    val lat: String,
    @Json(name = "long") val longitude: String,
    val locality: String,
    val country: String,
)

@JsonClass(generateAdapter = true)
data class DriverFetchingModel(
    @Json(name = "DriverTable") val driverTable: DriverTableModel,
)

@JsonClass(generateAdapter = true)
data class DriverTableModel(
    @Json(name = "Drivers") val drivers: List<DriverModel>,
)
