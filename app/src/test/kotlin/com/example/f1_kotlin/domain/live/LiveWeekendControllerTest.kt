package com.example.f1_kotlin.domain.live

import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.data.model.EspnScoreboardSession
import com.example.f1_kotlin.data.repository.IEspnRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveWeekendControllerTest {
    private lateinit var espnRepository: IEspnRepository

    @Before
    fun setUp() {
        espnRepository = mockk()
    }

    @Test
    fun loadScoreboard_success_updatesStateAndLiveFlag() {
        val latch = CountDownLatch(1)
        val event = liveEvent()
        coEvery { espnRepository.getScoreboardEvent(any()) } answers {
            latch.countDown()
            Result.success(event)
        }

        val controller = LiveWeekendController(espnRepository)
        controller.loadScoreboard()
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        // Allow StateFlow update on IO
        Thread.sleep(50)

        assertEquals(event, controller.scoreboard.value)
        assertTrue(controller.isLive)
        assertEquals("Race", controller.liveSessionAbbreviation)
        controller.stopLivePolling()
    }

    @Test
    fun loadScoreboard_notLive_clearsAbbreviation() {
        val latch = CountDownLatch(1)
        val event = EspnScoreboardEvent(
            name = "Monaco GP",
            shortName = "MON",
            statusState = "post",
            statusDetail = "Final",
            sessions = listOf(
                EspnScoreboardSession(
                    abbreviation = "Race",
                    statusState = "post",
                    statusDetail = "Final",
                ),
            ),
        )
        coEvery { espnRepository.getScoreboardEvent(any()) } answers {
            latch.countDown()
            Result.success(event)
        }

        val controller = LiveWeekendController(espnRepository)
        controller.loadScoreboard()
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        Thread.sleep(50)

        assertFalse(controller.isLive)
        assertNull(controller.liveSessionAbbreviation)
    }

    @Test
    fun onAppBackground_stopsPolling_onForegroundReloads() {
        val latch = CountDownLatch(1)
        coEvery { espnRepository.getScoreboardEvent(any()) } answers {
            latch.countDown()
            Result.success(null)
        }

        val controller = LiveWeekendController(espnRepository)
        controller.onAppBackground()
        controller.stopLivePolling()
        controller.onAppForeground()
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        coVerify(atLeast = 1) { espnRepository.getScoreboardEvent(forceRefresh = true) }
    }

    private fun liveEvent() = EspnScoreboardEvent(
        name = "Monaco GP",
        shortName = "MON",
        statusState = "in",
        statusDetail = "Lap 12",
        sessions = listOf(
            EspnScoreboardSession(
                abbreviation = "Race",
                statusState = "in",
                statusDetail = "Lap 12",
            ),
        ),
    )
}
