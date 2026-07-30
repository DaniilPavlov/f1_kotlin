package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.local.CacheDao
import com.example.f1_kotlin.data.local.CacheEntry
import com.example.f1_kotlin.data.local.CacheJsonMapper
import com.example.f1_kotlin.data.local.CacheKeys
import com.example.f1_kotlin.data.model.CircuitLocationModel
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.CircuitsModel
import com.example.f1_kotlin.data.model.CircuitTableModel
import com.example.f1_kotlin.data.model.ConstructorFetchingModel
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.ConstructorTableModel
import com.example.f1_kotlin.data.model.DriverFetchingModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.DriverStandingsCache
import com.example.f1_kotlin.data.model.FinishStatusDto
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.DriverTableModel
import com.example.f1_kotlin.data.model.MrDataResponse
import com.example.f1_kotlin.data.model.MrDataTotalModel
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.data.model.RaceTableModel
import com.example.f1_kotlin.data.model.ScheduleModel
import com.example.f1_kotlin.data.model.SeasonModel
import com.example.f1_kotlin.data.model.SeasonTableModel
import com.example.f1_kotlin.data.model.StandingsListsModel
import com.example.f1_kotlin.data.model.StandingsModel
import com.example.f1_kotlin.data.model.StandingsTableModel
import com.example.f1_kotlin.data.model.StatusTableModel
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class F1RepositoryTest {
    private lateinit var api: F1ApiService
    private lateinit var cacheDao: CacheDao
    private lateinit var repository: F1Repository
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapper = CacheJsonMapper(moshi)
    private val cache = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        api = mockk()
        cache.clear()
        cacheDao = mockk()
        coEvery { cacheDao.get(any()) } answers {
            val key = firstArg<String>()
            cache[key]?.let { CacheEntry(key, it, 0L) }
        }
        coEvery { cacheDao.insert(any()) } answers {
            val entry = firstArg<CacheEntry>()
            cache[entry.key] = entry.json
        }
        coEvery { cacheDao.clearAll() } answers { cache.clear() }
        repository = F1Repository(api, cacheDao, mapper)
    }

    @Test
    fun peekCurrentDriversCache_nullWhenEmpty() = runTest {
        assertNull(repository.peekCurrentDriversCache())
    }

    @Test
    fun getCurrentDriverStandings_success_cachesAndMaps() = runTest {
        coEvery { api.getCurrentDriverStandings(any()) } returns standingsDriversResponse()

        val result = repository.getCurrentDriverStandings()

        assertTrue(result.isSuccess)
        assertEquals("verstappen", result.getOrNull()!!.first.single().driver.driverId)
        assertEquals("2026", result.getOrNull()!!.second.season)
        assertNotNull(cache[CacheKeys.CURRENT_DRIVERS])
        assertEquals("verstappen", repository.peekCurrentDriversCache()!!.first.single().driver.driverId)
    }

    @Test
    fun getCurrentDriverStandings_networkFail_fallsBackToCache() = runTest {
        cache[CacheKeys.CURRENT_DRIVERS] = mapper.toJson(
            DriverStandingsCache(
                drivers = listOf(sampleDriverStanding()),
                season = "2026",
                round = "3",
            ),
            DriverStandingsCache::class.java,
        )
        coEvery { api.getCurrentDriverStandings(any()) } throws RuntimeException("offline")

        val result = repository.getCurrentDriverStandings()

        assertTrue(result.isSuccess)
        assertEquals("3", result.getOrNull()!!.second.round)
    }

    @Test
    fun getCurrentConstructorStandings_success() = runTest {
        coEvery { api.getCurrentConstructorStandings(any()) } returns standingsConstructorsResponse()

        val result = repository.getCurrentConstructorStandings()

        assertTrue(result.isSuccess)
        assertEquals("red_bull", result.getOrNull()!!.single().constructor.constructorId)
        assertNotNull(cache[CacheKeys.CURRENT_CONSTRUCTORS])
    }

    @Test
    fun getLastRace_and_getRaceResults() = runTest {
        coEvery { api.getLastRaceResults(any()) } returns scheduleResponse(listOf(sampleRace()))
        coEvery { api.getRaceResults("2026", "1", any()) } returns scheduleResponse(listOf(sampleRace()))

        assertEquals("Bahrain Grand Prix", repository.getLastRace().getOrNull()!!.raceName)
        assertEquals("Bahrain Grand Prix", repository.getRaceResults("2026", "1").getOrNull()!!.raceName)
        assertNotNull(repository.peekLastRaceCache())
    }

    @Test
    fun getSprintQualifyingPitStops_andSchedule() = runTest {
        coEvery { api.getSprintResults("2026", "1", any()) } returns scheduleResponse(
            listOf(sampleRace().copy(sprintResults = sampleRace().results, results = null)),
        )
        coEvery { api.getQualifyingResults("2026", "1", any()) } returns scheduleResponse(
            listOf(
                sampleRace().copy(
                    qualifyingResults = listOf(
                        QualifyingResultModel(number = "1", position = "1", driver = sampleDriver(), constructor = sampleConstructor()),
                    ),
                ),
            ),
        )
        coEvery { api.getPitStops("2026", "1", any()) } returns scheduleResponse(
            listOf(
                sampleRace().copy(
                    pitStops = listOf(PitStopModel(driverId = "verstappen", lap = "10", stop = "1", time = "12:00:00", duration = "2.3")),
                ),
            ),
        )
        coEvery { api.getDriver("verstappen", any()) } returns MrDataResponse(
            DriverFetchingModel(driverTable = DriverTableModel(listOf(sampleDriver()))),
        )
        coEvery { api.getCurrentSchedule(any()) } returns scheduleResponse(listOf(sampleRace()))

        assertEquals(1, repository.getSprintResults("2026", "1").getOrNull()!!.size)
        assertEquals(1, repository.getQualifyingResults("2026", "1").getOrNull()!!.size)
        assertEquals(1, repository.getPitStopsWithDriverNames("2026", "1").getOrNull()!!.size)
        assertEquals(1, repository.getCurrentSchedule().getOrNull()!!.size)
        assertNotNull(repository.peekScheduleCache())
    }

    @Test
    fun getHistoricalStandings_circuits_seasons_entities() = runTest {
        coEvery { api.getDriverStandings("2024", any()) } returns standingsDriversResponse(season = "2024")
        coEvery { api.getConstructorStandings("2024", any()) } returns standingsConstructorsResponse(season = "2024")
        coEvery { api.getCircuits(any()) } returns MrDataResponse(
            CircuitsModel(CircuitTableModel(listOf(sampleCircuit()))),
        )
        coEvery { api.getSeasons(any()) } returns MrDataResponse(
            MrDataTotalModel(
                seasonTable = SeasonTableModel(
                    listOf(SeasonModel("2024", ""), SeasonModel("2025", "")),
                ),
            ),
        )
        coEvery { api.getSeasonSchedule("2026", any()) } returns scheduleResponse(listOf(sampleRace()))
        coEvery { api.getDriver("verstappen", any()) } returns MrDataResponse(
            DriverFetchingModel(driverTable = DriverTableModel(listOf(sampleDriver()))),
        )
        coEvery { api.getConstructor("red_bull", any()) } returns MrDataResponse(
            ConstructorFetchingModel(constructorTable = ConstructorTableModel(listOf(sampleConstructor()))),
        )
        coEvery { api.getDriverStandingsAfterRound("2026", "1", any()) } returns standingsDriversResponse()
        coEvery { api.getConstructorStandingsAfterRound("2026", "1", any()) } returns standingsConstructorsResponse()
        coEvery { api.getSeasonStatus("2026", any()) } returns MrDataResponse(
            MrDataTotalModel(
                statusTable = StatusTableModel(
                    listOf(FinishStatusDto(statusId = "1", status = "Finished", count = "10")),
                ),
            ),
        )
        coEvery { api.getCurrentDrivers(any()) } returns MrDataResponse(
            DriverFetchingModel(driverTable = DriverTableModel(listOf(sampleDriver()))),
        )
        coEvery { api.getCurrentConstructors(any()) } returns MrDataResponse(
            ConstructorFetchingModel(constructorTable = ConstructorTableModel(listOf(sampleConstructor()))),
        )
        coEvery { api.getAllDrivers(any(), any()) } returns MrDataResponse(
            DriverFetchingModel(total = "1", driverTable = DriverTableModel(listOf(sampleDriver()))),
        )
        coEvery { api.getAllConstructors(any(), any()) } returns MrDataResponse(
            ConstructorFetchingModel(total = "1", constructorTable = ConstructorTableModel(listOf(sampleConstructor()))),
        )
        coEvery { api.getCircuitWinners("bahrain", any()) } returns scheduleResponse(listOf(sampleRace()))

        assertTrue(repository.getHistoricalStandings("2024").isSuccess)
        assertEquals("bahrain", repository.getCircuits().getOrNull()!!.single().circuitId)
        assertEquals("bahrain", repository.getCircuitById("bahrain").getOrNull()!!.circuitId)
        assertEquals(listOf("2025", "2024"), repository.getSeasonYears().getOrNull())
        assertEquals(1, repository.getSeasonRaces("2026").getOrNull()!!.size)
        assertEquals("verstappen", repository.getDriver("verstappen").getOrNull()!!.driverId)
        assertEquals("red_bull", repository.getConstructor("red_bull").getOrNull()!!.constructorId)
        assertTrue(repository.getStandingsAfterRound("2026", "1").isSuccess)
        assertEquals("Finished", repository.getSeasonFinishStatuses("2026").getOrNull()!!.single().status)
        assertEquals(1, repository.getCurrentDrivers().getOrNull()!!.size)
        assertEquals(1, repository.getCurrentConstructorsList().getOrNull()!!.size)
        assertEquals(1, repository.getAllDrivers().getOrNull()!!.size)
        assertEquals(1, repository.getAllConstructors().getOrNull()!!.size)
        assertEquals(1, repository.getCircuitWinners("bahrain").getOrNull()!!.size)
    }

    @Test
    fun currentConstructorsForDriver_readsStandingsCache() = runTest {
        cache[CacheKeys.CURRENT_DRIVERS] = mapper.toJson(
            DriverStandingsCache(listOf(sampleDriverStanding()), "2026", "1"),
            DriverStandingsCache::class.java,
        )
        assertEquals("red_bull", repository.currentConstructorsForDriver("verstappen").single().constructorId)
        assertEquals("verstappen", repository.currentDriversForConstructor("red_bull").single().driverId)
    }

    @Test
    fun careerAndH2h_delegateToCareerLoader() = runTest {
        stubH2hTotals()
        assertTrue(repository.getDriverH2hStats("verstappen", null).isSuccess)
        assertTrue(repository.getConstructorH2hStats("red_bull", "2026").isSuccess)
        assertTrue(repository.getDriverH2hRoundScores("verstappen", null).isSuccess)
        assertTrue(repository.getConstructorH2hRoundScores("red_bull", null).isSuccess)
        assertTrue(
            repository.getDriverCareerStats(
                "verstappen",
                listOf(Constructor("red_bull", "", "Red Bull", "Austrian")),
            ).isSuccess,
        )
        assertTrue(
            repository.getConstructorCareerStats(
                "red_bull",
                listOf(Driver("verstappen", "", "Max", "Verstappen", "", "Dutch")),
            ).isSuccess,
        )
        coVerify(atLeast = 1) { api.getMrDataTotal(any(), any(), any()) }
    }

    private fun stubH2hTotals() {
        coEvery { api.getMrDataTotal(any(), any(), any()) } answers {
            val path = firstArg<String>()
            when {
                path.endsWith("/constructors") || path.endsWith("/drivers") -> MrDataResponse(
                    MrDataTotalModel(
                        total = "1",
                        constructorTable = ConstructorTableModel(listOf(sampleConstructor())),
                        driverTable = DriverTableModel(listOf(sampleDriver())),
                    ),
                )
                path.contains("/results/") || path.contains("/qualifying/") -> MrDataResponse(
                    MrDataTotalModel(total = "1", raceTable = RaceTableModel(races = emptyList())),
                )
                else -> MrDataResponse(
                    MrDataTotalModel(total = "10", raceTable = RaceTableModel(races = emptyList())),
                )
            }
        }
    }

    private fun standingsDriversResponse(season: String = "2026") = MrDataResponse(
        StandingsModel(
            StandingsTableModel(
                listOf(
                    StandingsListsModel(
                        season = season,
                        round = "5",
                        driverStandings = listOf(sampleDriverStanding()),
                    ),
                ),
            ),
        ),
    )

    private fun standingsConstructorsResponse(season: String = "2026") = MrDataResponse(
        StandingsModel(
            StandingsTableModel(
                listOf(
                    StandingsListsModel(
                        season = season,
                        round = "5",
                        constructorStandings = listOf(
                            ConstructorStandingsModel(
                                "1", "1", "200", "6", sampleConstructor(),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun scheduleResponse(races: List<RaceModel>) = MrDataResponse(
        ScheduleModel(RaceTableModel(season = "2026", round = "1", races = races)),
    )

    private fun sampleDriverStanding() = DriverStandingsModel(
        position = "1",
        positionText = "1",
        points = "100",
        wins = "5",
        driver = sampleDriver(),
        constructors = listOf(sampleConstructor()),
    )

    private fun sampleDriver() = DriverModel(
        driverId = "verstappen",
        url = "",
        givenName = "Max",
        familyName = "Verstappen",
        dateOfBirth = "1997-09-30",
        nationality = "Dutch",
        code = "VER",
        permanentNumber = "1",
    )

    private fun sampleConstructor() = ConstructorModel("red_bull", "", "Red Bull", "Austrian")

    private fun sampleCircuit() = CircuitModel(
        circuitId = "bahrain",
        url = "",
        circuitName = "Bahrain International Circuit",
        location = CircuitLocationModel("26.0", "50.5", "Sakhir", "Bahrain"),
    )

    private fun sampleRace() = RaceModel(
        season = "2026",
        round = "1",
        url = "",
        raceName = "Bahrain Grand Prix",
        circuit = sampleCircuit(),
        date = "2026-03-01",
        results = listOf(
            RaceResultModel(
                number = "1",
                position = "1",
                positionText = "1",
                points = "25",
                driver = sampleDriver(),
                constructor = sampleConstructor(),
                grid = "1",
                laps = "57",
                status = "Finished",
            ),
        ),
    )
}
