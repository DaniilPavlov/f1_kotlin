package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.H2hEntityCompareData
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
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
class H2hConstructorsViewModelTest {
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
    fun canCompare_requiresTwoDifferentConstructors() = runTest {
        val viewModel = H2hConstructorsViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.canCompare)
        viewModel.setConstructorA(ctor("mclaren"))
        assertFalse(viewModel.canCompare)
        viewModel.setConstructorB(ctor("mclaren"))
        assertFalse(viewModel.canCompare)
        viewModel.setConstructorB(ctor("ferrari"))
        assertTrue(viewModel.canCompare)
    }

    @Test
    fun compare_success_setsComparison() = runTest {
        val statsA = H2hStats(races = 20, wins = 5, podiums = 10, poles = 3)
        val statsB = H2hStats(races = 20, wins = 4, podiums = 8, poles = 2)
        coEvery { repository.getConstructorH2hCompareData("mclaren", null) } returns Result.success(
            H2hEntityCompareData(statsA, emptyList()),
        )
        coEvery { repository.getConstructorH2hCompareData("ferrari", null) } returns Result.success(
            H2hEntityCompareData(statsB, emptyList()),
        )

        val viewModel = H2hConstructorsViewModel(repository)
        advanceUntilIdle()
        viewModel.setConstructorA(ctor("mclaren"))
        viewModel.setConstructorB(ctor("ferrari"))
        viewModel.compare()
        advanceUntilIdle()

        val comparison = viewModel.uiState.value.comparison
        assertTrue(comparison is AsyncValue.Value)
        val result = (comparison as AsyncValue.Value).value
        assertEquals("mclaren", result?.constructorA?.constructorId)
        assertEquals(statsA, result?.statsA)
        assertEquals(statsB, result?.statsB)
    }

    @Test
    fun setScopeMode_andSeason_clearComparison() = runTest {
        val viewModel = H2hConstructorsViewModel(repository)
        advanceUntilIdle()

        viewModel.setScopeMode(1)
        viewModel.setUseCurrentSeason(false)
        viewModel.onSeasonPicked("2024")
        assertEquals("2024", viewModel.selectedSeason)
        assertTrue(viewModel.isSeasonScope)
        assertTrue(viewModel.showYearPicker)

        viewModel.setCurrentOnly(true)
        assertTrue(viewModel.uiState.value.currentOnly)
        assertEquals(null, viewModel.uiState.value.constructorA)
    }

    private fun ctor(id: String) = Constructor(id, "", id.replaceFirstChar { it.uppercase() }, "British")
}
