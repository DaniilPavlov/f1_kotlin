package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.FinishStatusItem
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
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
class FinishStatusViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2026", "2025"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsFirstYearStatuses_asValue() = runTest {
        val items = listOf(FinishStatusItem("1", "Finished", 20))
        coEvery { repository.getSeasonFinishStatuses("2026") } returns Result.success(items)

        val viewModel = FinishStatusViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026", state.year)
        assertTrue(state.statuses is AsyncValue.Value)
        val statuses = (state.statuses as AsyncValue.Value).value
        assertEquals(1, statuses.size)
        assertEquals("Finished", statuses.first().status)
    }

    @Test
    fun getSeasonFinishStatuses_failure_setsStatusesError() = runTest {
        coEvery { repository.getSeasonFinishStatuses("2026") } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )

        val viewModel = FinishStatusViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.statuses is AsyncValue.Error)
    }

    @Test
    fun onYearChanged_loadsStatusesForNewYear() = runTest {
        coEvery { repository.getSeasonFinishStatuses("2026") } returns Result.success(emptyList())
        val items2025 = listOf(FinishStatusItem("3", "Accident", 2))
        coEvery { repository.getSeasonFinishStatuses("2025") } returns Result.success(items2025)

        val viewModel = FinishStatusViewModel(repository)
        advanceUntilIdle()

        viewModel.onYearChanged("2025")
        advanceUntilIdle()

        coVerify { repository.getSeasonFinishStatuses("2025") }
        val state = viewModel.uiState.value
        assertEquals("2025", state.year)
        assertTrue(state.statuses is AsyncValue.Value)
        assertEquals("Accident", (state.statuses as AsyncValue.Value).value.first().status)
    }
}
