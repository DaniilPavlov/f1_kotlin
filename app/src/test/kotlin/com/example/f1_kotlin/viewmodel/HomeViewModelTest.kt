package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.StandingsListsModel
import com.example.f1_kotlin.data.repository.F1Repository
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
 * **MockK** подменяет [F1Repository]: задаём ответ через [coEvery], ViewModel не знает, что это не настоящий API.
 *
 * **StandardTestDispatcher** + [Dispatchers.setMain] — корутины ViewModel выполняются синхронно
 * в тесте; [advanceUntilIdle] дожидается завершения [viewModelScope.launch].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: F1Repository

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
     * - положить список пилотов в [HomeViewModel.drivers] как [AsyncValue.Value].
     */
    @Test
    fun loadAllData_success_setsSeasonAndDrivers() = runTest {
        val drivers = listOf(
            DriverStandingsModel(
                position = "1",
                positionText = "1",
                points = "100",
                wins = "5",
                driver = DriverModel("verstappen", "", "Max", "Verstappen", "", "Dutch"),
                constructors = listOf(ConstructorModel("red_bull", "", "Red Bull", "Austrian")),
            ),
        )
        val meta = StandingsListsModel("2026", "5", drivers, null)
        val constructors = listOf(
            ConstructorStandingsModel(
                position = "1",
                positionText = "1",
                points = "200",
                wins = "6",
                constructor = ConstructorModel("red_bull", "", "Red Bull", "Austrian"),
            ),
        )

        coEvery { repository.peekCurrentDriversCache() } returns null
        coEvery { repository.peekCurrentConstructorsCache() } returns null
        coEvery { repository.getCurrentDriverStandings() } returns Result.success(Pair(drivers, meta))
        coEvery { repository.getCurrentConstructorStandings() } returns Result.success(constructors)

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertEquals("2026", viewModel.season.value)
        assertEquals("5", viewModel.round.value)
        assertTrue(viewModel.drivers.value is AsyncValue.Value)
        assertEquals(1, (viewModel.drivers.value as AsyncValue.Value).value.size)
    }
}
