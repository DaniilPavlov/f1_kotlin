package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceSession
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Unit-тесты сборки дня в [ScheduleViewModel] (сессии + иконки календаря). */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onSelectDay_qualifyingDay_buildsHeaderAndQualifyingSession() = runTest {
        val race = sampleRace()
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk(relaxed = true))
        advanceUntilIdle()

        viewModel.onSelectDay(LocalDate.parse("2026-05-24"))

        val items = viewModel.uiState.value.scheduleItems
        assertEquals(2, items.size)
        assertNull(items[0].titleRes) // header
        assertEquals("Monaco Grand Prix", items[0].raceName)
        assertEquals(R.string.qualifying, items[1].titleRes)
        assertEquals("2026-05-24", items[1].date.date)
    }

    @Test
    fun onSelectDay_raceDay_includesRaceSession() = runTest {
        val race = sampleRace()
        stubSchedule(listOf(race))

        val viewModel = ScheduleViewModel(repository, mockk(relaxed = true))
        advanceUntilIdle()

        viewModel.onSelectDay(LocalDate.parse("2026-05-25"))

        val items = viewModel.uiState.value.scheduleItems
        assertTrue(items.any { it.titleRes == R.string.race })
        assertTrue(items.any { it.titleRes == null }) // header
        assertEquals("Monaco Grand Prix", items.first { it.titleRes == R.string.race }.raceName)
    }

    @Test
    fun onSelectDay_emptyDay_clearsScheduleItems() = runTest {
        stubSchedule(listOf(sampleRace()))

        val viewModel = ScheduleViewModel(repository, mockk(relaxed = true))
        advanceUntilIdle()

        viewModel.onSelectDay(LocalDate.parse("2026-01-01"))

        assertTrue(viewModel.uiState.value.scheduleItems.isEmpty())
    }

    @Test
    fun logoForDay_raceDay_isFinish_sessionDay_isCar() = runTest {
        stubSchedule(listOf(sampleRace()))

        val viewModel = ScheduleViewModel(repository, mockk(relaxed = true))
        advanceUntilIdle()

        assertEquals(
            R.drawable.calendar_finish,
            viewModel.logoForDay(LocalDate.parse("2026-05-25")),
        )
        assertEquals(
            R.drawable.calendar_car,
            viewModel.logoForDay(LocalDate.parse("2026-05-24")),
        )
        assertNull(viewModel.logoForDay(LocalDate.parse("2026-01-01")))
    }

    @Test
    fun loadAllData_success_setsRacesValue() = runTest {
        stubSchedule(listOf(sampleRace()))

        val viewModel = ScheduleViewModel(repository, mockk(relaxed = true))
        advanceUntilIdle()

        val races = viewModel.uiState.value.races
        assertTrue(races is AsyncValue.Value)
        assertEquals(1, (races as AsyncValue.Value).value.size)
    }

    private fun stubSchedule(races: List<Race>) {
        coEvery { repository.peekScheduleCache() } returns races
        coEvery { repository.getCurrentSchedule() } returns Result.success(races)
    }

    private fun sampleRace() = Race(
        season = "2026",
        round = "8",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = Circuit(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = "2026-05-25",
        time = "13:00:00Z",
        firstPractice = RaceSession("2026-05-23", "11:30:00Z"),
        qualifying = RaceSession("2026-05-24", "14:00:00Z"),
    )
}
