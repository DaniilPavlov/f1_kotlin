package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HallOfFameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.getSeasonYears() } returns Result.success(listOf("2025", "2024"))
        coEvery { repository.peekHistoricalStandingsCache(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsHistoricalStandingsForLatestYear() = runTest {
        val drivers = listOf(sampleDriverStanding())
        val constructors = listOf(sampleConstructorStanding())
        coEvery { repository.getHistoricalStandings("2025") } returns Result.success(drivers to constructors)

        val viewModel = HallOfFameViewModel(repository)
        advanceUntilIdle()

        assertEquals("2025", viewModel.uiState.value.year)
        assertTrue(viewModel.uiState.value.fieldsInputted)
        assertTrue(viewModel.uiState.value.drivers is AsyncValue.Value)
        assertEquals(1, (viewModel.uiState.value.drivers as AsyncValue.Value).value.size)
        assertTrue(viewModel.uiState.value.constructors is AsyncValue.Value)
    }

    @Test
    fun onYearChanged_shortYear_doesNotLoad() = runTest {
        coEvery { repository.getHistoricalStandings(any()) } returns Result.success(
            emptyList<DriverStanding>() to emptyList(),
        )
        val viewModel = HallOfFameViewModel(repository)
        advanceUntilIdle()

        viewModel.onYearChanged("20")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.fieldsInputted)
    }

    @Test
    fun changeActiveTable_updatesIndex() = runTest {
        coEvery { repository.getHistoricalStandings(any()) } returns Result.success(
            emptyList<DriverStanding>() to emptyList(),
        )
        val viewModel = HallOfFameViewModel(repository)
        advanceUntilIdle()

        viewModel.changeActiveTable(1)
        assertEquals(1, viewModel.uiState.value.activeTable)
    }

    @Test
    fun loadFailure_setsError() = runTest {
        coEvery { repository.getHistoricalStandings("2025") } returns Result.failure(
            AppError("offline").asException(),
        )
        val viewModel = HallOfFameViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.drivers is AsyncValue.Error)
        assertTrue(viewModel.uiState.value.error != null)
    }

    private fun sampleDriverStanding() = DriverStanding(
        position = "1",
        positionText = "1",
        points = "100",
        wins = "5",
        driver = Driver("norris", "", "Lando", "Norris", "1999-01-01", "British"),
        constructors = emptyList(),
    )

    private fun sampleConstructorStanding() = ConstructorStanding(
        position = "1",
        positionText = "1",
        points = "200",
        wins = "8",
        constructor = Constructor("mclaren", "", "McLaren", "British"),
    )
}
