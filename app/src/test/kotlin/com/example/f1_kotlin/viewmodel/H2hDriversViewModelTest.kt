package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Driver
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit-тесты [H2hDriversViewModel.compare] и правил [H2hDriversUiState.canCompare]. */
@OptIn(ExperimentalCoroutinesApi::class)
class H2hDriversViewModelTest {
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
    fun canCompare_requiresTwoDifferentDrivers() = runTest {
        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.canCompare)

        viewModel.setDriverA(driver("hamilton"))
        assertFalse(viewModel.canCompare)

        viewModel.setDriverB(driver("hamilton"))
        assertFalse(viewModel.canCompare)

        viewModel.setDriverB(driver("norris"))
        assertTrue(viewModel.canCompare)
    }

    @Test
    fun canCompare_seasonScope_requiresSelectedSeason() = runTest {
        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()

        viewModel.setDriverA(driver("hamilton"))
        viewModel.setDriverB(driver("norris"))
        viewModel.setScopeMode(1)
        viewModel.setUseCurrentSeason(false)
        viewModel.onSeasonPicked("")

        assertNull(viewModel.selectedSeason)
        assertFalse(viewModel.canCompare)

        viewModel.onSeasonPicked("2024")
        assertEquals("2024", viewModel.selectedSeason)
        assertTrue(viewModel.canCompare)
    }

    @Test
    fun compare_success_setsComparisonValue() = runTest {
        val statsA = H2hStats(races = 10, wins = 3, podiums = 5, poles = 2)
        val statsB = H2hStats(races = 12, wins = 1, podiums = 4, poles = 0)
        coEvery { repository.getDriverH2hStats("hamilton", null) } returns Result.success(statsA)
        coEvery { repository.getDriverH2hStats("norris", null) } returns Result.success(statsB)

        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()
        viewModel.setDriverA(driver("hamilton"))
        viewModel.setDriverB(driver("norris"))
        viewModel.compare()
        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertTrue(comparison is AsyncValue.Value)
        val result = (comparison as AsyncValue.Value).value
        assertEquals("hamilton", result?.driverA?.driverId)
        assertEquals("norris", result?.driverB?.driverId)
        assertEquals(statsA, result?.statsA)
        assertEquals(statsB, result?.statsB)
        assertNull(result?.season)
    }

    @Test
    fun compare_failure_setsComparisonError() = runTest {
        coEvery { repository.getDriverH2hStats("hamilton", null) } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )
        coEvery { repository.getDriverH2hStats("norris", null) } returns Result.success(
            H2hStats(1, 0, 0, 0),
        )

        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()
        viewModel.setDriverA(driver("hamilton"))
        viewModel.setDriverB(driver("norris"))
        viewModel.compare()
        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertTrue(comparison is AsyncValue.Error)
        assertEquals("Соединение отсутствует", (comparison as AsyncValue.Error).message)
    }

    @Test
    fun compare_passesSeasonWhenSeasonScope() = runTest {
        val stats = H2hStats(22, 4, 9, 1)
        coEvery { repository.getDriverH2hStats("hamilton", "2026") } returns Result.success(stats)
        coEvery { repository.getDriverH2hStats("norris", "2026") } returns Result.success(stats)

        val viewModel = H2hDriversViewModel(repository)
        advanceUntilIdle()
        viewModel.setScopeMode(1)
        viewModel.setUseCurrentSeason(true)
        viewModel.setDriverA(driver("hamilton"))
        viewModel.setDriverB(driver("norris"))
        viewModel.compare()
        advanceUntilIdle()

        val result = (viewModel.uiState.value.comparison as AsyncValue.Value).value
        assertEquals("2026", result?.season)
    }

    private fun driver(id: String) = Driver(
        driverId = id,
        url = "",
        givenName = id.replaceFirstChar { it.uppercase() },
        familyName = "Test",
        dateOfBirth = "",
        nationality = "British",
    )
}
