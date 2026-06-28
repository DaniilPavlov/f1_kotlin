package com.example.f1kotlin.viewmodel

import com.example.f1kotlin.data.model.CircuitLocationModel
import com.example.f1kotlin.data.model.CircuitModel
import com.example.f1kotlin.data.model.RaceModel
import com.example.f1kotlin.data.repository.F1Repository
import com.example.f1kotlin.domain.AsyncValue
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
 * Отдельно проверяем валидацию полей ([RaceSearchViewModel.fieldsInputted]) —
 * это чистая логика UI без корутин, тест без [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RaceSearchViewModelTest {
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

    /**
     * Кнопка «Найти» активна только при 4 цифрах года и непустом раунде —
     * как во Flutter-версии приложения.
     */
    @Test
    fun checkFields_requiresFourDigitYearAndRound() {
        val viewModel = RaceSearchViewModel(repository)

        viewModel.onYearChanged("202")
        viewModel.onRoundChanged("1")
        assertEquals(false, viewModel.fieldsInputted.value)

        viewModel.onYearChanged("2026")
        assertEquals(true, viewModel.fieldsInputted.value)
    }

    /** После ввода года/раунда и [RaceSearchViewModel.loadRaceResults] — гонка в [AsyncValue.Value]. */
    @Test
    fun loadRaceResults_success_setsRace() = runTest {
        val race = sampleRace()
        coEvery { repository.getRaceResults("2026", "5") } returns Result.success(race)

        val viewModel = RaceSearchViewModel(repository)
        viewModel.onYearChanged("2026")
        viewModel.onRoundChanged("5")
        viewModel.loadRaceResults()
        advanceUntilIdle()

        val state = viewModel.searchedRace.value
        assertTrue(state is AsyncValue.Value)
        assertEquals("Monaco Grand Prix", (state as AsyncValue.Value).value?.raceName)
    }

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
