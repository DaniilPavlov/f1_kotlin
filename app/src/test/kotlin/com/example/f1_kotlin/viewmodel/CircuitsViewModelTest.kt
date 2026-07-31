package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CircuitsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var appDataRefresh: AppDataRefresh

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        appDataRefresh = mockk(relaxed = true)
        coEvery { repository.peekCircuitsCache() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsCircuits() = runTest {
        val circuits = listOf(sampleCircuit("monaco"), sampleCircuit("monza"))
        coEvery { repository.getCircuits() } returns Result.success(circuits)

        val viewModel = CircuitsViewModel(repository, appDataRefresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value.circuits
        assertTrue(state is AsyncValue.Value)
        assertEquals(2, (state as AsyncValue.Value).value.size)
    }

    @Test
    fun changeActivePage_updatesIndex() = runTest {
        coEvery { repository.getCircuits() } returns Result.success(emptyList())
        val viewModel = CircuitsViewModel(repository, appDataRefresh)
        advanceUntilIdle()

        viewModel.changeActivePage(3)
        assertEquals(3, viewModel.uiState.value.activePage)
    }

    @Test
    fun refreshAll_clearsCachesThenReloads() = runTest {
        coEvery { repository.getCircuits() } returns Result.success(listOf(sampleCircuit("spa")))
        val viewModel = CircuitsViewModel(repository, appDataRefresh)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        coVerify { appDataRefresh.clearAll() }
        assertTrue(viewModel.uiState.value.circuits is AsyncValue.Value)
    }

    @Test
    fun loadCircuits_failure_setsError() = runTest {
        coEvery { repository.getCircuits() } returns Result.failure(
            AppError("offline").asException(),
        )
        val viewModel = CircuitsViewModel(repository, appDataRefresh)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.circuits is AsyncValue.Error)
    }

    @Test
    fun peekCache_showsCachedBeforeNetwork() = runTest {
        val cached = listOf(sampleCircuit("silverstone"))
        coEvery { repository.peekCircuitsCache() } returns cached
        coEvery { repository.getCircuits() } returns Result.success(cached)

        val viewModel = CircuitsViewModel(repository, appDataRefresh)
        advanceUntilIdle()

        assertEquals("silverstone", (viewModel.uiState.value.circuits as AsyncValue.Value).value[0].circuitId)
    }

    private fun sampleCircuit(id: String) = Circuit(
        circuitId = id,
        url = "",
        circuitName = id,
        location = CircuitLocation("0", "0", "City", "Country"),
    )
}
