package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import io.mockk.coEvery
import io.mockk.every
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

/**
 * Unit-тесты [ResultsViewModel] — экран «Последняя гонка».
 *
 * ViewModel в [init] сразу вызывает [ResultsViewModel.loadAllData], поэтому достаточно
 * создать mock Repository, затем ViewModel — загрузка стартует автоматически.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var espnRepository: IEspnRepository
    private lateinit var appDataRefresh: AppDataRefresh

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        espnRepository = mockk(relaxed = true)
        appDataRefresh = mockk(relaxed = true)
        every { espnRepository.isScoreboardFresh } returns false
        every { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(any()) } returns Result.success(null)
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

        val viewModel = ResultsViewModel(repository, espnRepository, appDataRefresh, mockk(relaxed = true))
        advanceUntilIdle()

        val state = viewModel.uiState.value.lastRace
        assertTrue(state is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state as AsyncValue.Value).value.raceName)
    }

    /** Ошибка сети/сервера → [AsyncValue.Error], UI покажет [com.example.f1_kotlin.ui.components.ErrorBody]. */
    @Test
    fun loadAllData_failure_setsError() = runTest {
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )

        val viewModel = ResultsViewModel(repository, espnRepository, appDataRefresh, mockk(relaxed = true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastRace is AsyncValue.Error)
    }

    /** ESPN scoreboard: сеть упала — скрываем блок (Value(null)), Results остаётся рабочим. */
    @Test
    fun scoreboard_networkFailure_setsValueNull_notError() = runTest {
        val race = sampleRace()
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)
        every { espnRepository.isScoreboardFresh } returns false
        every { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(any()) } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )

        val viewModel = ResultsViewModel(repository, espnRepository, appDataRefresh, mockk(relaxed = true))
        advanceUntilIdle()

        val scoreboard = viewModel.uiState.value.scoreboard
        assertTrue(scoreboard is AsyncValue.Value)
        assertNull((scoreboard as AsyncValue.Value).value)
        assertTrue(viewModel.uiState.value.lastRace is AsyncValue.Value)
    }

    /** Уже показанный scoreboard не затирается ошибкой forceRefresh. */
    @Test
    fun scoreboard_forceRefreshFailure_keepsPreviousValue() = runTest {
        val race = sampleRace()
        val event = EspnScoreboardEvent(
            name = "Monaco GP",
            shortName = "MON",
            statusState = "pre",
            statusDetail = "Scheduled",
        )
        coEvery { repository.peekLastRaceCache() } returns null
        coEvery { repository.getLastRace() } returns Result.success(race)
        every { espnRepository.isScoreboardFresh } returns false
        every { espnRepository.peekScoreboard } returns null
        coEvery { espnRepository.getScoreboardEvent(forceRefresh = false) } returns Result.success(event)

        val viewModel = ResultsViewModel(repository, espnRepository, appDataRefresh, mockk(relaxed = true))
        advanceUntilIdle()

        coEvery { espnRepository.getScoreboardEvent(forceRefresh = true) } returns Result.failure(
            AppError("Соединение отсутствует").asException(),
        )
        viewModel.loadScoreboard(forceRefresh = true)
        advanceUntilIdle()

        val scoreboard = viewModel.uiState.value.scoreboard
        assertTrue(scoreboard is AsyncValue.Value)
        assertEquals("Monaco GP", (scoreboard as AsyncValue.Value).value?.name)
    }

    /** Минимальная заготовка [Race] — не тянем полный JSON из API. */
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
