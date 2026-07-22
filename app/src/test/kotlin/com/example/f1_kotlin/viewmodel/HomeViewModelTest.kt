package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.domain.model.StandingsMeta
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
 * Unit-тесты [HomeViewModel] — проверяем логику без Android-фреймворка и реальной сети.
 *
 * **MockK** подменяет [IF1Repository]: задаём ответ через [coEvery], ViewModel не знает, что это не настоящий API.
 *
 * **StandardTestDispatcher** + [Dispatchers.setMain] — корутины ViewModel выполняются синхронно
 * в тесте; [advanceUntilIdle] дожидается завершения [viewModelScope.launch].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository

    /** Перед каждым тестом: подменяем Main-дispatcher и создаём mock Repository. */
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    /** Возвращаем Main-dispatcher, чтобы не влиять на другие тесты. */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * При успешном ответе Repository ViewModel должен:
     * - записать сезон и раунд из метаданных API;
     * - положить список пилотов в [HomeUiState.drivers] как [AsyncValue.Value].
     */
    @Test
    fun loadAllData_success_setsSeasonAndDrivers() = runTest {
        val drivers = listOf(
            DriverStanding(
                position = "1",
                positionText = "1",
                points = "100",
                wins = "5",
                driver = Driver("verstappen", "", "Max", "Verstappen", "", "Dutch"),
                constructors = listOf(Constructor("red_bull", "", "Red Bull", "Austrian")),
            ),
        )
        val meta = StandingsMeta("2026", "5")
        val constructors = listOf(
            ConstructorStanding(
                position = "1",
                positionText = "1",
                points = "200",
                wins = "6",
                constructor = Constructor("red_bull", "", "Red Bull", "Austrian"),
            ),
        )

        coEvery { repository.peekCurrentDriversCache() } returns null
        coEvery { repository.peekCurrentConstructorsCache() } returns null
        coEvery { repository.getCurrentDriverStandings() } returns Result.success(Pair(drivers, meta))
        coEvery { repository.getCurrentConstructorStandings() } returns Result.success(constructors)

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026", state.season)
        assertEquals("5", state.round)
        assertTrue(state.drivers is AsyncValue.Value)
        assertEquals(1, (state.drivers as AsyncValue.Value).value.size)
    }
}
