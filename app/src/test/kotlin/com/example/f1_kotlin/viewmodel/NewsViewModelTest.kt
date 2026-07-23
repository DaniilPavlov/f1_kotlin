package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.model.NewsArticle
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AsyncValue
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты [NewsViewModel] — вкладка «Новости» (ESPN).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var espnRepository: IEspnRepository
    private lateinit var appDataRefresh: AppDataRefresh

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        espnRepository = mockk(relaxed = true)
        appDataRefresh = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Свежий in-memory кэш → сразу [AsyncValue.Value], сеть не дергаем. */
    @Test
    fun loadArticles_cacheHitFresh_skipsNetwork() = runTest {
        val cached = listOf(sampleArticle())
        every { espnRepository.peekNews } returns cached
        every { espnRepository.isNewsFresh } returns true

        val viewModel = NewsViewModel(espnRepository, appDataRefresh)
        advanceUntilIdle()

        val articles = viewModel.uiState.value.articles
        assertTrue(articles is AsyncValue.Value)
        assertEquals("Test Headline", (articles as AsyncValue.Value).value.first().headline)
        coVerify(exactly = 0) { espnRepository.getNews(any()) }
    }

    /** Нет кэша → [getNews] успешно заполняет [NewsUiState.articles]. */
    @Test
    fun loadArticles_networkSuccess_setsArticles() = runTest {
        val articles = listOf(sampleArticle(id = 2, headline = "Network Headline"))
        every { espnRepository.peekNews } returns null
        coEvery { espnRepository.getNews(forceRefresh = false) } returns Result.success(articles)

        val viewModel = NewsViewModel(espnRepository, appDataRefresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value.articles
        assertTrue(state is AsyncValue.Value)
        assertEquals("Network Headline", (state as AsyncValue.Value).value.first().headline)
    }

    /** Pull-to-refresh → [getNews] с forceRefresh, [AppDataRefresh.clearAll], isRefreshing=false. */
    @Test
    fun refreshAll_fetchesFromNetwork() = runTest {
        val initial = listOf(sampleArticle())
        val refreshed = listOf(sampleArticle(id = 3, headline = "Refreshed"))
        every { espnRepository.peekNews } returns initial
        every { espnRepository.isNewsFresh } returns true
        coEvery { espnRepository.getNews(forceRefresh = true) } returns Result.success(refreshed)

        val viewModel = NewsViewModel(espnRepository, appDataRefresh)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.articles is AsyncValue.Value)
        assertEquals("Refreshed", (state.articles as AsyncValue.Value).value.first().headline)
        assertFalse(state.isRefreshing)
        coVerify { espnRepository.getNews(forceRefresh = true) }
        coVerify { appDataRefresh.clearAll() }
    }

    private fun sampleArticle(id: Int = 1, headline: String = "Test Headline") = NewsArticle(
        id = id,
        headline = headline,
        description = "Description",
        webUrl = "https://example.com",
    )
}
