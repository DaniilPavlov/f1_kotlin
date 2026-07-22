package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.internalToRoute
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.navigation.RaceInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RaceInfoScreenViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        savedStateHandle = mockk(relaxed = true)
        // toRoute is inline → calls Bundle; mock internalToRoute for JVM unit tests
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            savedStateHandle.internalToRoute(RaceInfo::class, any())
        } returns RaceInfo(season = "2026", round = "5")
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsRaceAndExtraSections() = runTest {
        val race = sampleRace()
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(race)
        coEvery { repository.getQualifyingResults("2026", "5") } returns Result.success(emptyList())
        coEvery { repository.getPitStopsWithDriverNames("2026", "5") } returns Result.success(emptyList())
        coEvery { repository.getSprintResults("2026", "5") } returns Result.success(emptyList())

        val viewModel = RaceInfoScreenViewModel(savedStateHandle, repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.race is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state.race as AsyncValue.Value).value.raceName)
        assertTrue(state.qualifying is AsyncValue.Value)
        assertTrue(state.pitStops is AsyncValue.Value)
        assertTrue(state.sprint is AsyncValue.Value)
    }

    @Test
    fun getRaceResults_failure_setsRaceError() = runTest {
        coEvery { repository.getRaceResults("2026", "5") } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )

        val viewModel = RaceInfoScreenViewModel(savedStateHandle, repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.race is AsyncValue.Error)
        assertNotNull(state.error)
    }

    @Test
    fun getRaceResults_null_setsRaceNotFoundError() = runTest {
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(null)

        val viewModel = RaceInfoScreenViewModel(savedStateHandle, repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.race is AsyncValue.Error)
        assertNotNull(state.error)
    }

    private fun sampleRace() = Race(
        season = "2026",
        round = "5",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = Circuit(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = "2026-05-25",
        results = emptyList(),
    )
}
