package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.data.repository.IF1Repository
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
 * Unit-тесты [RaceSearchViewModel] — поиск гонки по году и раунду.
 *
 * Отдельно проверяем валидацию полей ([RaceSearchUiState.fieldsInputted]) —
 * это чистая логика UI без корутин, тест без [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RaceSearchViewModelTest {
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

    /** Кнопка «Найти» активна только при 4 цифрах года и непустом раунде. */
    @Test
    fun checkFields_requiresFourDigitYearAndRound() {
        val viewModel = RaceSearchViewModel(repository)

        viewModel.onYearChanged("202")
        viewModel.onRacePicked("1", "1. Bahrain Grand Prix")
        assertEquals(false, viewModel.uiState.value.fieldsInputted)

        viewModel.onYearChanged("2026")
        viewModel.onRacePicked("5", "5. Monaco Grand Prix")
        assertEquals(true, viewModel.uiState.value.fieldsInputted)
    }

    /** После ввода года/раунда и [RaceSearchViewModel.loadRaceResults] — гонка в [AsyncValue.Value]. */
    @Test
    fun loadRaceResults_success_setsRace() = runTest {
        val race = sampleRace()
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(race)

        val viewModel = RaceSearchViewModel(repository)
        viewModel.onYearChanged("2026")
        viewModel.onRacePicked("5", "5. Monaco Grand Prix")
        viewModel.loadRaceResults()
        advanceUntilIdle()

        val state = viewModel.uiState.value.searchedRace
        assertTrue(state is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state as AsyncValue.Value).value?.raceName)
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
