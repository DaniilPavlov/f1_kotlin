package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.domain.model.Race
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonRewindViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var analytics: AnalyticsGateway

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        analytics = mockk(relaxed = true)
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2025", "2024"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsCompletedRacesAndStandings_startsAtLastRound() = runTest {
        val r1 = sampleRace(round = "1", date = LocalDate.now().minusDays(60).toString())
        val r2 = sampleRace(round = "2", date = LocalDate.now().minusDays(30).toString())
        val future = sampleRace(round = "3", date = LocalDate.now().plusDays(30).toString())
        coEvery { repository.getSeasonRaces("2025") } returns Result.success(listOf(r1, r2, future))
        coEvery { repository.getStandingsAfterRound("2025", "2") } returns Result.success(
            listOf(driverStanding("norris", "Norris", "25", "mclaren")) to
                listOf(constructorStanding("mclaren", "McLaren", "40")),
        )

        val viewModel = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()

        verify { analytics.log(AnalyticsEvent.SeasonRewindOpened) }
        val state = viewModel.uiState.value
        assertEquals("2025", state.year)
        assertTrue(state.races is AsyncValue.Value)
        assertEquals(2, (state.races as AsyncValue.Value).value.size)
        assertEquals(1, state.selectedRoundIndex)
        assertEquals("2", state.chartRound)
        assertFalse(state.isChartStale)
        assertEquals(1, state.driverBars.size)
        assertEquals("mclaren", state.driverBars[0].constructorId)
        assertEquals(25.0, state.driverBars[0].points, 0.0)
        assertEquals(1, state.constructorBars.size)
        assertTrue(state.canPlay)
    }

    @Test
    fun selectRound_reloadsStandings_firstRoundPoints() = runTest {
        val r1 = sampleRace(round = "1", date = LocalDate.now().minusDays(60).toString())
        val r2 = sampleRace(round = "2", date = LocalDate.now().minusDays(30).toString())
        coEvery { repository.getSeasonRaces("2025") } returns Result.success(listOf(r1, r2))
        coEvery { repository.getStandingsAfterRound("2025", "2") } returns Result.success(
            listOf(driverStanding("verstappen", "Verstappen", "50", "red_bull")) to
                listOf(constructorStanding("red_bull", "Red Bull", "50")),
        )
        coEvery { repository.getStandingsAfterRound("2025", "1") } returns Result.success(
            listOf(driverStanding("norris", "Norris", "25", "mclaren")) to
                listOf(constructorStanding("mclaren", "McLaren", "25")),
        )

        val viewModel = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedRoundIndex)

        viewModel.selectRound(0)
        assertTrue(viewModel.uiState.value.isChartStale)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedRoundIndex)
        assertEquals("1", viewModel.uiState.value.chartRound)
        assertFalse(viewModel.uiState.value.isChartStale)
        assertEquals("Norris", viewModel.uiState.value.driverBars.first().label)
        assertEquals(25.0, viewModel.uiState.value.driverBars.first().points, 0.0)
    }

    @Test
    fun onTableChanged_switchesActiveBars() = runTest {
        val r1 = sampleRace(round = "1", date = LocalDate.now().minusDays(30).toString())
        coEvery { repository.getSeasonRaces("2025") } returns Result.success(listOf(r1))
        coEvery { repository.getStandingsAfterRound("2025", "1") } returns Result.success(
            listOf(driverStanding("norris", "Norris", "25", "mclaren")) to
                listOf(constructorStanding("mclaren", "McLaren", "40")),
        )

        val viewModel = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.activeTable)
        assertEquals("Norris", viewModel.uiState.value.activeBars.first().label)

        viewModel.onTableChanged(1)
        assertEquals(1, viewModel.uiState.value.activeTable)
        assertEquals("McLaren", viewModel.uiState.value.activeBars.first().label)
    }

    @Test
    fun onSeasonChanged_invalidYear_setsError() = runTest {
        coEvery { repository.getSeasonRaces(any()) } returns Result.success(emptyList())
        coEvery { repository.getStandingsAfterRound(any(), any()) } returns Result.success(
            emptyList<DriverStanding>() to emptyList(),
        )

        val viewModel = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()

        viewModel.onSeasonChanged("20")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.races is AsyncValue.Error)
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun togglePlayback_fromLastRound_restartsAtFirst() = runTest {
        val r1 = sampleRace(round = "1", date = LocalDate.now().minusDays(60).toString())
        val r2 = sampleRace(round = "2", date = LocalDate.now().minusDays(30).toString())
        coEvery { repository.getSeasonRaces("2025") } returns Result.success(listOf(r1, r2))
        coEvery { repository.getStandingsAfterRound("2025", "2") } returns Result.success(
            listOf(driverStanding("verstappen", "Verstappen", "50", "red_bull")) to
                listOf(constructorStanding("red_bull", "Red Bull", "50")),
        )
        coEvery { repository.getStandingsAfterRound("2025", "1") } returns Result.success(
            listOf(driverStanding("norris", "Norris", "25", "mclaren")) to
                listOf(constructorStanding("mclaren", "McLaren", "25")),
        )

        val viewModel = SeasonRewindViewModel(repository, analytics)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedRoundIndex)

        viewModel.togglePlayback()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPlaying)
        assertEquals(1, viewModel.uiState.value.selectedRoundIndex)
    }

    private fun sampleRace(round: String, date: String) = Race(
        season = "2025",
        round = round,
        url = "",
        raceName = "GP $round",
        circuit = Circuit(
            circuitId = "c$round",
            url = "",
            circuitName = "Circuit",
            location = CircuitLocation("0", "0", "City", "Country"),
        ),
        date = date,
        results = emptyList(),
    )

    private fun driverStanding(
        id: String,
        family: String,
        points: String,
        constructorId: String,
    ) = DriverStanding(
        position = "1",
        positionText = "1",
        points = points,
        wins = "1",
        driver = Driver(id, "", "X", family, "1990-01-01", "British", code = family.take(3).uppercase()),
        constructors = listOf(Constructor(constructorId, "", constructorId, "British")),
    )

    private fun constructorStanding(id: String, name: String, points: String) = ConstructorStanding(
        position = "1",
        positionText = "1",
        points = points,
        wins = "1",
        constructor = Constructor(id, "", name, "British"),
    )
}
