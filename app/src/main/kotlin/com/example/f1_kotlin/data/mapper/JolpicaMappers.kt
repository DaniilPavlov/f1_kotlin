package com.example.f1_kotlin.data.mapper

import com.example.f1_kotlin.data.model.AverageSpeedModel
import com.example.f1_kotlin.data.model.CircuitLocationModel
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.FastestLapModel
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceDateModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.data.model.StandingsListsModel
import com.example.f1_kotlin.data.model.TimeModel
import com.example.f1_kotlin.domain.model.AverageSpeed
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.domain.model.FastestLap
import com.example.f1_kotlin.domain.model.PitStop
import com.example.f1_kotlin.domain.model.QualifyingResult
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceResult
import com.example.f1_kotlin.domain.model.RaceSession
import com.example.f1_kotlin.domain.model.RaceTime
import com.example.f1_kotlin.domain.model.StandingsMeta

/**
 * Jolpica DTO (`data.model`) → domain (`domain.model`).
 *
 * GoF Structural Adapter — чужой JSON Jolpica (DTO) → domain ([Driver], [Race], …).
 */

fun DriverModel.toDomain() = Driver(
    driverId = driverId,
    url = url,
    givenName = givenName,
    familyName = familyName,
    dateOfBirth = dateOfBirth,
    nationality = nationality,
    code = code,
    permanentNumber = permanentNumber,
)

fun ConstructorModel.toDomain() = Constructor(
    constructorId = constructorId,
    url = url,
    name = name,
    nationality = nationality,
)

fun DriverStandingsModel.toDomain() = DriverStanding(
    position = position,
    positionText = positionText,
    points = points,
    wins = wins,
    driver = driver.toDomain(),
    constructors = constructors.map { it.toDomain() },
)

fun ConstructorStandingsModel.toDomain() = ConstructorStanding(
    position = position,
    positionText = positionText,
    points = points,
    wins = wins,
    constructor = constructor.toDomain(),
)

fun StandingsListsModel.toMeta() = StandingsMeta(season = season, round = round)

fun CircuitLocationModel.toDomain() = CircuitLocation(
    lat = lat,
    longitude = longitude,
    locality = locality,
    country = country,
)

fun CircuitModel.toDomain() = Circuit(
    circuitId = circuitId,
    url = url,
    circuitName = circuitName,
    location = location.toDomain(),
)

fun RaceDateModel.toDomain() = RaceSession(date = date, time = time)

fun TimeModel.toDomain() = RaceTime(millis = millis, time = time)

fun AverageSpeedModel.toDomain() = AverageSpeed(units = units, speed = speed)

fun FastestLapModel.toDomain() = FastestLap(
    rank = rank,
    lap = lap,
    time = time?.toDomain() ?: RaceTime(millis = null, time = ""),
    averageSpeed = averageSpeed?.toDomain(),
)

fun RaceResultModel.toDomain() = RaceResult(
    number = number,
    position = position,
    positionText = positionText,
    points = points,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
    grid = grid,
    laps = laps,
    status = status,
    time = time?.toDomain(),
    fastestLap = fastestLap?.toDomain(),
)

fun QualifyingResultModel.toDomain() = QualifyingResult(
    number = number,
    position = position,
    positionText = positionText,
    driver = driver.toDomain(),
    constructor = constructor.toDomain(),
    q1 = q1,
    q2 = q2,
    q3 = q3,
)

fun PitStopModel.toDomain() = PitStop(
    driverId = driverId,
    lap = lap,
    stop = stop,
    time = time,
    duration = duration,
)

fun RaceModel.toDomain() = Race(
    season = season,
    round = round,
    url = url,
    raceName = raceName,
    circuit = circuit.toDomain(),
    date = date,
    time = time,
    firstPractice = firstPractice?.toDomain(),
    secondPractice = secondPractice?.toDomain(),
    thirdPractice = thirdPractice?.toDomain(),
    sprintQualifying = sprintQualifying?.toDomain(),
    qualifying = qualifying?.toDomain(),
    sprint = sprint?.toDomain(),
    results = results?.map { it.toDomain() },
    sprintResults = sprintResults?.map { it.toDomain() },
    qualifyingResults = qualifyingResults?.map { it.toDomain() },
    pitStops = pitStops?.map { it.toDomain() },
)

fun List<DriverStandingsModel>.toDriverStandingDomain() = map { it.toDomain() }
fun List<ConstructorStandingsModel>.toConstructorStandingDomain() = map { it.toDomain() }
fun List<RaceModel>.toRaceDomain() = map { it.toDomain() }
