package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.model.CircuitRaceWin
import com.example.f1_kotlin.data.model.FinishStatusItem
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.domain.model.PitStop
import com.example.f1_kotlin.domain.model.QualifyingResult
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceResult
import com.example.f1_kotlin.domain.model.StandingsMeta

interface IF1Repository {
    suspend fun peekCurrentDriversCache(): Pair<List<DriverStanding>, StandingsMeta>?

    suspend fun peekCurrentConstructorsCache(): List<ConstructorStanding>?

    suspend fun peekLastRaceCache(): Race?

    suspend fun peekScheduleCache(): List<Race>?

    suspend fun peekCircuitsCache(): List<Circuit>?

    suspend fun peekHistoricalStandingsCache(
        year: String,
    ): Pair<List<DriverStanding>, List<ConstructorStanding>>?

    suspend fun getCurrentDriverStandings(): Result<Pair<List<DriverStanding>, StandingsMeta>>

    suspend fun getCurrentConstructorStandings(): Result<List<ConstructorStanding>>

    suspend fun getLastRace(): Result<Race>

    suspend fun getRaceResults(year: String, round: String): Result<Race?>

    suspend fun getSprintResults(year: String, round: String): Result<List<RaceResult>>

    suspend fun getQualifyingResults(year: String, round: String): Result<List<QualifyingResult>>

    suspend fun getPitStopsWithDriverNames(year: String, round: String): Result<List<PitStop>>

    suspend fun getCurrentSchedule(): Result<List<Race>>

    suspend fun getHistoricalStandings(
        year: String,
    ): Result<Pair<List<DriverStanding>, List<ConstructorStanding>>>

    suspend fun getCircuits(): Result<List<Circuit>>

    suspend fun getCircuitById(circuitId: String): Result<Circuit?>

    suspend fun getSeasonYears(): Result<List<String>>

    suspend fun getSeasonRaces(year: String): Result<List<Race>>

    suspend fun getDriver(driverId: String): Result<Driver?>

    suspend fun getConstructor(constructorId: String): Result<Constructor?>

    suspend fun getDriverCareerStats(
        driverId: String,
        currentConstructors: List<Constructor> = emptyList(),
    ): Result<CareerStats<Constructor>>

    suspend fun getConstructorCareerStats(
        constructorId: String,
        currentDrivers: List<Driver> = emptyList(),
    ): Result<CareerStats<Driver>>

    suspend fun getDriverH2hStats(driverId: String, season: String? = null): Result<H2hStats>

    suspend fun getConstructorH2hStats(constructorId: String, season: String? = null): Result<H2hStats>

    suspend fun getSeasonFinishStatuses(year: String): Result<List<FinishStatusItem>>

    suspend fun getStandingsAfterRound(
        year: String,
        round: String,
    ): Result<Pair<List<DriverStanding>, List<ConstructorStanding>>>

    suspend fun getDriverH2hRoundScores(driverId: String, season: String? = null): Result<List<com.example.f1_kotlin.viewmodel.H2hRoundScore>>

    suspend fun getConstructorH2hRoundScores(constructorId: String, season: String? = null): Result<List<com.example.f1_kotlin.viewmodel.H2hRoundScore>>

    suspend fun getCurrentDrivers(): Result<List<Driver>>

    suspend fun getAllDrivers(): Result<List<Driver>>

    suspend fun getCurrentConstructorsList(): Result<List<Constructor>>

    suspend fun getAllConstructors(): Result<List<Constructor>>

    suspend fun getCircuitWinners(circuitId: String): Result<List<CircuitRaceWin>>

    suspend fun currentConstructorsForDriver(driverId: String): List<Constructor>

    suspend fun currentDriversForConstructor(constructorId: String): List<Driver>

    fun clearInMemoryCaches()
}
