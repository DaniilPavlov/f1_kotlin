package com.example.f1_kotlin.data.career

import com.example.f1_kotlin.data.api.F1ApiService
import com.example.f1_kotlin.data.model.CircuitLocationModel
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.ConstructorTableModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.MrDataResponse
import com.example.f1_kotlin.data.model.MrDataTotalModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.data.model.RaceTableModel
import com.example.f1_kotlin.domain.model.Constructor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit-тесты [CareerLoader] — агрегация totals / подиумов без реальной сети. */
class CareerLoaderTest {
    private lateinit var api: F1ApiService

    @Before
    fun setUp() {
        api = mockk()
    }

    @Test
    fun loadH2hStats_sumsPodiumsFromPositionTotals() = runTest {
        stubTotalsByPathSuffix(
            "/results" to "100",
            "/results/1" to "20",
            "/results/2" to "10",
            "/results/3" to "5",
            "/qualifying/1" to "15",
        )

        val stats = CareerLoader.loadH2hStats(api, "drivers/hamilton")

        assertEquals(100, stats.races)
        assertEquals(20, stats.wins)
        assertEquals(35, stats.podiums)
        assertEquals(15, stats.poles)
    }

    @Test
    fun loadH2hStats_withSeason_prefixesPath() = runTest {
        stubTotalsByPathSuffix(
            "2024/drivers/norris/results" to "22",
            "2024/drivers/norris/results/1" to "4",
            "2024/drivers/norris/results/2" to "3",
            "2024/drivers/norris/results/3" to "2",
            "2024/drivers/norris/qualifying/1" to "1",
            matchFullPath = true,
        )

        val stats = CareerLoader.loadH2hStats(api, "drivers/norris", season = "2024")

        assertEquals(22, stats.races)
        assertEquals(4, stats.wins)
        assertEquals(9, stats.podiums)
        assertEquals(1, stats.poles)
    }

    @Test
    fun loadDriverCareer_usesTotalsAndRelatedConstructors() = runTest {
        val related = ConstructorModel("mercedes", "", "Mercedes", "German")
        coEvery { api.getMrDataTotal(any(), any(), any()) } answers {
            val path = firstArg<String>()
            when {
                path.endsWith("/constructors") -> MrDataResponse(
                    MrDataTotalModel(
                        total = "1",
                        constructorTable = ConstructorTableModel(listOf(related)),
                    ),
                )
                path.endsWith("/results/1") -> pageWithOptionalRace(
                    total = "3",
                    race = sampleWinRace(),
                )
                path.endsWith("/results/2") -> emptyPage("2")
                path.endsWith("/results/3") -> emptyPage("1")
                path.endsWith("/qualifying/1") -> pageWithPole(total = "4")
                path.endsWith("/results") -> emptyPage("50")
                else -> emptyPage("0")
            }
        }

        val current = listOf(Constructor("mercedes", "", "Mercedes", "German"))
        val career = CareerLoader.loadDriverCareer(api, "hamilton", current)

        assertEquals(50, career.races)
        assertEquals(3, career.wins)
        assertEquals(6, career.podiums) // 3+2+1
        assertEquals(4, career.poles)
        assertEquals(current, career.current)
        assertEquals(listOf("mercedes"), career.related.map { it.constructorId })
        assertEquals(1, career.winRaces.size)
        assertEquals("Bahrain Grand Prix", career.winRaces.first().raceName)
        assertTrue(career.poleRaces.isNotEmpty())
    }

    private fun stubTotalsByPathSuffix(
        vararg pairs: Pair<String, String>,
        matchFullPath: Boolean = false,
    ) {
        val map = pairs.toMap()
        coEvery { api.getMrDataTotal(any(), any(), any()) } answers {
            val path = firstArg<String>()
            val total = if (matchFullPath) {
                map.entries.firstOrNull { path == it.key || path.endsWith(it.key) }?.value
            } else {
                map.entries.firstOrNull { path.endsWith(it.key) }?.value
            } ?: "0"
            emptyPage(total)
        }
    }

    private fun emptyPage(total: String) = MrDataResponse(
        MrDataTotalModel(
            total = total,
            raceTable = RaceTableModel(races = emptyList()),
        ),
    )

    private fun pageWithOptionalRace(total: String, race: RaceModel) = MrDataResponse(
        MrDataTotalModel(
            total = total,
            raceTable = RaceTableModel(races = listOf(race)),
        ),
    )

    private fun pageWithPole(total: String) = MrDataResponse(
        MrDataTotalModel(
            total = total,
            raceTable = RaceTableModel(
                races = listOf(
                    RaceModel(
                        season = "2024",
                        round = "1",
                        url = "",
                        raceName = "Bahrain Grand Prix",
                        circuit = sampleCircuit(),
                        date = "2024-03-02",
                        qualifyingResults = listOf(
                            QualifyingResultModel(
                                number = "44",
                                position = "1",
                                driver = sampleDriver(),
                                constructor = ConstructorModel("mercedes", "", "Mercedes", "German"),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun sampleWinRace() = RaceModel(
        season = "2024",
        round = "1",
        url = "",
        raceName = "Bahrain Grand Prix",
        circuit = sampleCircuit(),
        date = "2024-03-02",
        results = listOf(
            RaceResultModel(
                number = "44",
                position = "1",
                positionText = "1",
                points = "25",
                driver = sampleDriver(),
                constructor = ConstructorModel("mercedes", "", "Mercedes", "German"),
                grid = "1",
                laps = "57",
                status = "Finished",
            ),
        ),
    )

    private fun sampleDriver() = DriverModel(
        driverId = "hamilton",
        url = "",
        givenName = "Lewis",
        familyName = "Hamilton",
        dateOfBirth = "1985-01-07",
        nationality = "British",
    )

    private fun sampleCircuit() = CircuitModel(
        circuitId = "bahrain",
        url = "",
        circuitName = "Bahrain International Circuit",
        location = CircuitLocationModel("26.0", "50.5", "Sakhir", "Bahrain"),
    )
}
