package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.model.EspnDriverCardData
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.ui.navigation.DriverDetail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
class DriverDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: IF1Repository
    private lateinit var espnRepository: IEspnRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        espnRepository = mockk()
        savedStateHandle = mockk(relaxed = true)
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        every {
            savedStateHandle.toRoute(DriverDetail::class, any())
        } returns DriverDetail(driverId = "norris")
        coEvery { repository.currentConstructorsForDriver("norris") } returns emptyList()
        coEvery { espnRepository.driverCardData(any(), any()) } returns EspnDriverCardData()
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsDriverAndCareer() = runTest {
        val driver = sampleDriver()
        val stats = CareerStats<Constructor>(
            races = 100,
            wins = 10,
            podiums = 30,
            poles = 5,
            current = emptyList(),
            related = emptyList(),
        )
        coEvery { repository.getDriver("norris") } returns Result.success(driver)
        coEvery { repository.getDriverCareerStats("norris", any()) } returns Result.success(stats)

        val viewModel = DriverDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.driver is AsyncValue.Value)
        assertEquals("Norris", (viewModel.uiState.value.driver as AsyncValue.Value).value.familyName)
        assertTrue(viewModel.uiState.value.careerStats is AsyncValue.Value)
        assertEquals(10, (viewModel.uiState.value.careerStats as AsyncValue.Value).value.wins)
    }

    @Test
    fun getDriver_failure_setsError() = runTest {
        coEvery { repository.getDriver("norris") } returns Result.failure(
            AppError("offline").asException(),
        )

        val viewModel = DriverDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.driver is AsyncValue.Error)
        assertTrue(viewModel.uiState.value.error != null)
    }

    @Test
    fun getDriver_null_setsNotFound() = runTest {
        coEvery { repository.getDriver("norris") } returns Result.success(null)

        val viewModel = DriverDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.driver is AsyncValue.Error)
    }

    private fun sampleDriver() = Driver(
        driverId = "norris",
        url = "",
        givenName = "Lando",
        familyName = "Norris",
        dateOfBirth = "1999-11-13",
        nationality = "British",
        code = "NOR",
    )
}
