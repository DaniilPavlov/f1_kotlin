package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.internalToRoute
import com.example.f1_kotlin.data.circuits.CircuitStats
import com.example.f1_kotlin.data.circuits.CircuitStatsRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.ui.navigation.CircuitDetail
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CircuitDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var circuitStatsRepository: CircuitStatsRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        circuitStatsRepository = mockk()
        savedStateHandle = mockk(relaxed = true)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            savedStateHandle.internalToRoute(CircuitDetail::class, any())
        } returns CircuitDetail(circuitId = "monaco")
        coEvery { repository.peekCircuitsCache() } returns null
        coEvery { circuitStatsRepository.of("monaco") } returns CircuitStats(
            lengthKm = 3.337,
            laps = 78,
            turns = 19,
            topSpeedKmh = 290.0,
            elevationM = 42.0,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsCircuitAndWinners() = runTest {
        val circuit = sampleCircuit()
        coEvery { repository.getCircuitById("monaco") } returns Result.success(circuit)
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel(savedStateHandle, repository, circuitStatsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.circuit is AsyncValue.Value)
        assertEquals("Monaco", (state.circuit as AsyncValue.Value).value.circuitName)
        assertTrue(state.winners is AsyncValue.Value)
        assertEquals(0, (state.winners as AsyncValue.Value).value.size)
    }

    @Test
    fun getCircuitById_failure_setsCircuitError() = runTest {
        coEvery { repository.getCircuitById("monaco") } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel(savedStateHandle, repository, circuitStatsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuit is AsyncValue.Error)
    }

    @Test
    fun getCircuitById_null_setsCircuitNotFoundError() = runTest {
        coEvery { repository.getCircuitById("monaco") } returns Result.success(null)
        coEvery { repository.getCircuitWinners("monaco") } returns Result.success(emptyList())

        val viewModel = CircuitDetailViewModel(savedStateHandle, repository, circuitStatsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuit is AsyncValue.Error)
    }

    private fun sampleCircuit() = Circuit(
        circuitId = "monaco",
        url = "",
        circuitName = "Monaco",
        location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
    )
}
