package com.example.f1_kotlin.domain.model

/**
 * Domain-модели Jolpica/Ergast — без Moshi/`@Json`.
 * UI и ViewModel работают только с ними; парсинг JSON остаётся в `data.model` (DTO).
 */

data class Driver(
    val driverId: String,
    val url: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String,
    val code: String? = null,
    val permanentNumber: String? = null,
) {
    val fullName: String get() = "$givenName $familyName"
}

data class Constructor(
    val constructorId: String,
    val url: String,
    val name: String,
    val nationality: String,
)

data class DriverStanding(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    val driver: Driver,
    val constructors: List<Constructor>,
)

data class ConstructorStanding(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    val constructor: Constructor,
)

/** Сезон/раунд текущего среза standings (без сырых списков API). */
data class StandingsMeta(
    val season: String,
    val round: String,
)

data class CircuitLocation(
    val lat: String,
    val longitude: String,
    val locality: String,
    val country: String,
)

data class Circuit(
    val circuitId: String,
    val url: String,
    val circuitName: String,
    val location: CircuitLocation,
)

data class RaceSession(
    val date: String,
    val time: String? = null,
)

data class RaceTime(
    val millis: String? = null,
    val time: String,
)

data class AverageSpeed(
    val units: String,
    val speed: String,
)

data class FastestLap(
    val rank: String,
    val lap: String,
    val time: RaceTime,
    val averageSpeed: AverageSpeed? = null,
)

data class RaceResult(
    val number: String,
    val position: String,
    val positionText: String,
    val points: String,
    val driver: Driver,
    val constructor: Constructor,
    val grid: String,
    val laps: String,
    val status: String,
    val time: RaceTime? = null,
    val fastestLap: FastestLap? = null,
)

data class QualifyingResult(
    val number: String,
    val position: String,
    val positionText: String? = null,
    val driver: Driver,
    val constructor: Constructor,
    val q1: String? = null,
    val q2: String? = null,
    val q3: String? = null,
)

data class PitStop(
    val driverId: String,
    val lap: String,
    val stop: String,
    val time: String,
    val duration: String,
)

data class Race(
    val season: String,
    val round: String,
    val url: String,
    val raceName: String,
    val circuit: Circuit,
    val date: String,
    val time: String? = null,
    val firstPractice: RaceSession? = null,
    val secondPractice: RaceSession? = null,
    val thirdPractice: RaceSession? = null,
    val sprintQualifying: RaceSession? = null,
    val qualifying: RaceSession? = null,
    val sprint: RaceSession? = null,
    val results: List<RaceResult>? = null,
    val sprintResults: List<RaceResult>? = null,
    val qualifyingResults: List<QualifyingResult>? = null,
    val pitStops: List<PitStop>? = null,
) {
    val fastestLapTime: String
        get() {
            var fastest = "999999"
            results?.forEach { result ->
                val lap = result.fastestLap?.time?.time
                if (lap != null && fastest > lap) fastest = lap
            }
            return fastest
        }
}
