package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.data.api.EspnApiService
import com.example.f1_kotlin.data.model.EspnArticleDto
import com.example.f1_kotlin.data.model.EspnEventDto
import com.example.f1_kotlin.data.model.EspnLinksDto
import com.example.f1_kotlin.data.model.EspnNewsResponseDto
import com.example.f1_kotlin.data.model.EspnScoreboardResponseDto
import com.example.f1_kotlin.data.model.EspnSearchResponseDto
import com.example.f1_kotlin.data.model.EspnHrefDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EspnRepositoryTest {
    private lateinit var api: EspnApiService
    private lateinit var repository: EspnRepository

    @Before
    fun setUp() {
        api = mockk()
        repository = EspnRepository(api)
    }

    @Test
    fun getNews_success_cachesAndIsFresh() = runTest {
        coEvery { api.getNews(any(), any()) } returns EspnNewsResponseDto(
            articles = listOf(
                EspnArticleDto(
                    id = 1,
                    headline = "Test",
                    description = "Desc",
                    links = EspnLinksDto(web = EspnHrefDto(href = "https://example.com/a")),
                ),
            ),
        )

        val first = repository.getNews(forceRefresh = true)
        assertTrue(first.isSuccess)
        assertEquals(1, first.getOrNull()!!.size)
        assertTrue(repository.isNewsFresh)
        assertEquals(1, repository.peekNews!!.size)

        val second = repository.getNews(forceRefresh = false)
        assertTrue(second.isSuccess)
        assertEquals(1, second.getOrNull()!!.size)
    }

    @Test
    fun getScoreboardEvent_emptyEvents_cachesNull() = runTest {
        coEvery { api.getScoreboard() } returns EspnScoreboardResponseDto(events = emptyList())

        val result = repository.getScoreboardEvent(forceRefresh = true)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertTrue(repository.isScoreboardFresh)
        assertNull(repository.peekScoreboard)
    }

    @Test
    fun getScoreboardEvent_mapsFirstEvent() = runTest {
        coEvery { api.getScoreboard() } returns EspnScoreboardResponseDto(
            events = listOf(
                EspnEventDto(
                    name = "Monaco Grand Prix",
                    shortName = "MON",
                ),
            ),
        )

        val result = repository.getScoreboardEvent(forceRefresh = true)
        assertTrue(result.isSuccess)
        assertEquals("Monaco Grand Prix", result.getOrNull()!!.name)
    }

    @Test
    fun driverCardData_noEspnId_returnsEmptyCached() = runTest {
        coEvery { api.searchPlayers(any(), any(), any(), any(), any()) } returns
            EspnSearchResponseDto(items = emptyList())

        val card = repository.driverCardData("Max", "Verstappen")
        assertNull(card.photoUrl)
        assertTrue(card.news.isEmpty())
        assertEquals(card, repository.driverCardData("Max", "Verstappen"))
    }

    @Test
    fun constructorNews_apiFailure_returnsEmpty() = runTest {
        coEvery { api.getNews(any(), any()) } throws RuntimeException("offline")

        val news = repository.constructorNews("red_bull", "Red Bull")
        assertTrue(news.isEmpty())
    }

    @Test
    fun constructorNews_knownTeam_loadsArticles() = runTest {
        coEvery { api.getNews(any(), any()) } returns EspnNewsResponseDto(
            articles = listOf(
                EspnArticleDto(
                    id = 7,
                    headline = "RB win",
                    description = "x",
                    links = EspnLinksDto(web = EspnHrefDto(href = "https://example.com/rb")),
                ),
            ),
        )

        val news = repository.constructorNews("red_bull", "Red Bull")
        assertEquals(1, news.size)
        assertEquals("RB win", news.single().headline)
        assertEquals(1, repository.constructorNews("red_bull", "Red Bull").size)
    }

    @Test
    fun clearCaches_resetsFreshFlags() = runTest {
        coEvery { api.getNews(any(), any()) } returns EspnNewsResponseDto(articles = emptyList())
        repository.getNews(forceRefresh = true)
        assertTrue(repository.isNewsFresh)

        repository.clearCaches()
        assertFalse(repository.isNewsFresh)
        assertNull(repository.peekNews)
        assertFalse(repository.isScoreboardFresh)
    }
}
