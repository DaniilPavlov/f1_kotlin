package com.example.f1_kotlin.data.model

import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data-модели (DTO) ответов Ergast / Jolpica API.
 *
 * Парсятся Moshi. В UI / ViewModel не пробрасывать напрямую —
 * маппить через [com.example.f1_kotlin.data.mapper] → [com.example.f1_kotlin.domain.model].
 *
 * Имена полей в JSON часто в PascalCase (`MRData`, `DriverStandings`),
 * а в Kotlin — camelCase. Аннотация [@Json] связывает их при парсинге Moshi.
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
    // Jolpica иногда не отдаёт position (например Stroll после R1 2026 с positionText="-").
    val position: String = "",
    val positionText: String = "",
    val points: String,
    val wins: String,
    @Json(name = "Driver") val driver: DriverModel,
    @Json(name = "Constructors") val constructors: List<ConstructorModel>,
)

@JsonClass(generateAdapter = true)
data class ConstructorStandingsModel(
    val position: String = "",
    val positionText: String = "",
    val points: String,
    val wins: String,
    @Json(name = "Constructor") val constructor: ConstructorModel,
)

@JsonClass(generateAdapter = true)
data class DriverModel(
    val driverId: String,
    val givenName: String,
    val familyName: String,
    /** Jolpica иногда отдаёт резервных пилотов без url/DoB/nationality. */
    val url: String = "",
    val dateOfBirth: String = "",
    val nationality: String = "",
    val code: String? = null,
    val permanentNumber: String? = null,
) {
    /** Удобное поле для UI вместо склейки имени в каждой таблице. */
    val fullName: String get() = "$givenName $familyName"
}

@JsonClass(generateAdapter = true)
data class ConstructorModel(
    val constructorId: String,
    val url: String = "",
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
    val url: String = "",
    val raceName: String,
    @Json(name = "Circuit") val circuit: CircuitModel,
    val date: String,
    val time: String? = null,
    @Json(name = "FirstPractice") val firstPractice: RaceDateModel? = null,
    @Json(name = "SecondPractice") val secondPractice: RaceDateModel? = null,
    @Json(name = "ThirdPractice") val thirdPractice: RaceDateModel? = null,
    @Json(name = "SprintQualifying") val sprintQualifying: RaceDateModel? = null,
    @Json(name = "Qualifying") val qualifying: RaceDateModel? = null,
    @Json(name = "Sprint") val sprint: RaceDateModel? = null,
    @Json(name = "Results") val results: List<RaceResultModel>? = null,
    @Json(name = "SprintResults") val sprintResults: List<RaceResultModel>? = null,
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
    /** Jolpica sprint results often omit rank. */
    val rank: String = "",
    val lap: String = "",
    @Json(name = "Time") val time: TimeModel? = null,
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
    @Json(name = "Q1") val q1: String? = null,
    @Json(name = "Q2") val q2: String? = null,
    @Json(name = "Q3") val q3: String? = null,
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
    val url: String = "",
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
    val total: String? = null,
    @Json(name = "DriverTable") val driverTable: DriverTableModel,
)

@JsonClass(generateAdapter = true)
data class DriverTableModel(
    @Json(name = "Drivers") val drivers: List<DriverModel>,
)

@JsonClass(generateAdapter = true)
data class ConstructorFetchingModel(
    val total: String? = null,
    @Json(name = "ConstructorTable") val constructorTable: ConstructorTableModel,
)

@JsonClass(generateAdapter = true)
data class ConstructorTableModel(
    @Json(name = "Constructors") val constructors: List<ConstructorModel>,
)

@JsonClass(generateAdapter = true)
data class MrDataTotalModel(
    val total: String? = null,
    @Json(name = "RaceTable") val raceTable: RaceTableModel? = null,
    @Json(name = "ConstructorTable") val constructorTable: ConstructorTableModel? = null,
    @Json(name = "DriverTable") val driverTable: DriverTableModel? = null,
    @Json(name = "SeasonTable") val seasonTable: SeasonTableModel? = null,
    @Json(name = "StatusTable") val statusTable: StatusTableModel? = null,
)

@JsonClass(generateAdapter = true)
data class StatusTableModel(
    @Json(name = "Status") val status: List<FinishStatusDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FinishStatusDto(
    val statusId: String? = null,
    val status: String? = null,
    val count: String? = null,
)

/** Статус финиша сезона из Jolpica `/{year}/status`. */
data class FinishStatusItem(
    val statusId: String,
    val status: String,
    val count: Int,
) {
    /** Retired / DNF-подобные и дисквалификации. */
    val isHighlight: Boolean
        get() {
            val lower = status.lowercase()
            return lower.contains("retir") ||
                lower.contains("disqual") ||
                lower.contains("accident") ||
                lower.contains("collision") ||
                lower.contains("did not start") ||
                lower.contains("dns") ||
                lower.contains("dnf") ||
                lower.startsWith("+") ||
                lower.contains("lapped") ||
                lower.contains("not classified")
        }
}

/** Метрики для сравнения H2H (пилот или конструктор). */
data class H2hStats(
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
)

/** Stats + round scores из одного прохода results/sprint (для графика H2H). */
data class H2hEntityCompareData(
    val stats: H2hStats,
    val scores: List<com.example.f1_kotlin.viewmodel.H2hRoundScore>,
)

@JsonClass(generateAdapter = true)
data class SeasonTableModel(
    @Json(name = "Seasons") val seasons: List<SeasonModel>,
)

@JsonClass(generateAdapter = true)
data class SeasonModel(
    val season: String,
    val url: String,
)

/** Финиш в конкретной гонке (победа / подиум / поул). */
data class CareerRaceResult(
    val season: String,
    val round: String,
    val raceName: String,
    val position: Int,
    val constructor: Constructor,
    val circuit: Circuit,
    val driver: Driver? = null,
) {
    /** Подзаголовок строки: пилот (если есть) или конструктор. */
    val entityName: String
        get() = driver?.fullName?.trim()?.takeIf { it.isNotEmpty() } ?: constructor.name
}

/** Карьерная статистика пилота или конструктора. */
data class CareerStats<T>(
    val races: Int,
    val wins: Int,
    val podiums: Int,
    val poles: Int,
    val current: List<T>,
    val related: List<T>,
    val winRaces: List<CareerRaceResult> = emptyList(),
    val podiumRaces: List<CareerRaceResult> = emptyList(),
    val poleRaces: List<CareerRaceResult> = emptyList(),
)

/** Победа на трассе (история ГП). */
data class CircuitRaceWin(
    val season: String,
    val round: String,
    val raceName: String,
    val driver: Driver,
    val constructor: Constructor,
)

/** Кэш списка сезонов (обновляется раз в сутки). */
@JsonClass(generateAdapter = true)
data class SeasonsCache(
    val dayKey: String,
    val years: List<String>,
)
