package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.example.f1_kotlin.data.model.CareerStats
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AppError
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.ui.navigation.ConstructorDetail
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
class ConstructorDetailViewModelTest {
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
            savedStateHandle.toRoute(ConstructorDetail::class, any())
        } returns ConstructorDetail(constructorId = "mclaren")
        coEvery { repository.currentDriversForConstructor("mclaren") } returns emptyList()
        coEvery { espnRepository.constructorNews(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.navigation.SavedStateHandleKt")
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_success_setsConstructorAndCareer() = runTest {
        val ctor = Constructor("mclaren", "", "McLaren", "British")
        val stats = CareerStats<Driver>(
            races = 200,
            wins = 50,
            podiums = 120,
            poles = 40,
            current = emptyList(),
            related = emptyList(),
        )
        coEvery { repository.getConstructor("mclaren") } returns Result.success(ctor)
        coEvery { repository.getConstructorCareerStats("mclaren", any()) } returns Result.success(stats)

        val viewModel = ConstructorDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.constructor is AsyncValue.Value)
        assertEquals("McLaren", (viewModel.uiState.value.constructor as AsyncValue.Value).value.name)
        assertTrue(viewModel.uiState.value.careerStats is AsyncValue.Value)
        assertEquals(50, (viewModel.uiState.value.careerStats as AsyncValue.Value).value.wins)
    }

    @Test
    fun getConstructor_failure_setsError() = runTest {
        coEvery { repository.getConstructor("mclaren") } returns Result.failure(
            AppError("offline").asException(),
        )

        val viewModel = ConstructorDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.constructor is AsyncValue.Error)
    }

    @Test
    fun getConstructor_null_setsNotFound() = runTest {
        coEvery { repository.getConstructor("mclaren") } returns Result.success(null)

        val viewModel = ConstructorDetailViewModel(savedStateHandle, repository, espnRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.constructor is AsyncValue.Error)
    }
}
