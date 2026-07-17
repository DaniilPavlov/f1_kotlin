package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.CircuitLocationModel
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.domain.AppException
import com.example.f1_kotlin.domain.AsyncValue
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты [ResultsViewModel] — экран «Последняя гонка».
 *
 * ViewModel в [init] сразу вызывает [ResultsViewModel.loadAllData], поэтому достаточно
 * создать mock Repository, затем ViewModel — загрузка стартует автоматически.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: F1Repository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Успешный [F1Repository.getLastRace] → [AsyncValue.Value] с названием гонки. */
    @Test
    fun loadAllData_success_updatesLastRace() = runTest {
        val race = sampleRace()
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)

        val viewModel = ResultsViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.lastRace.value
        assertTrue(state is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state as AsyncValue.Value).value.raceName)
    }

    /** Ошибка сети/сервера → [AsyncValue.Error], UI покажет [com.example.f1_kotlin.ui.components.ErrorBody]. */
    @Test
    fun loadAllData_failure_setsError() = runTest {
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.failure(
            AppException("Соединение отсутствует"),
        )

        val viewModel = ResultsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.lastRace.value is AsyncValue.Error)
    }

    /** Минимальная заготовка [RaceModel] — не тянем полный JSON из API. */
    private fun sampleRace() = RaceModel(
        season = "2026",
        round = "5",
        url = "",
        raceName = "Monaco Grand Prix",
        circuit = CircuitModel(
            circuitId = "monaco",
            url = "",
            circuitName = "Monaco",
            location = CircuitLocationModel("43.7", "7.4", "Monte Carlo", "Monaco"),
        ),
        date = "2026-05-25",
        results = emptyList(),
    )
}
