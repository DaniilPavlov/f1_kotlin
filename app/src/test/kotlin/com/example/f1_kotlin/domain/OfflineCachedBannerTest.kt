package com.example.f1_kotlin.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCachedBannerTest {
    @Test
    fun shouldShow_trueOnlyWhenCachedAndOffline() {
        val reachability = mockk<NetworkReachability>()
        every { reachability.isOffline() } returns true
        assertTrue(shouldShowOfflineCachedBanner(hasCachedContent = true, reachability = reachability))
        assertFalse(shouldShowOfflineCachedBanner(hasCachedContent = false, reachability = reachability))

        every { reachability.isOffline() } returns false
        assertFalse(shouldShowOfflineCachedBanner(hasCachedContent = true, reachability = reachability))
    }

    @Test
    fun clearIfOnline_returnsFalseWhenNetworkBack() {
        val reachability = mockk<NetworkReachability>(relaxed = true)
        every { reachability.isOffline() } returns false

        assertFalse(
            clearOfflineBannerIfOnline(currentlyShowing = true, reachability = reachability),
        )
        verify { reachability.clearMemo() }
    }

    @Test
    fun clearIfOnline_keepsTrueWhenStillOffline() {
        val reachability = mockk<NetworkReachability>(relaxed = true)
        every { reachability.isOffline() } returns true

        assertTrue(
            clearOfflineBannerIfOnline(currentlyShowing = true, reachability = reachability),
        )
    }

    @Test
    fun clearIfOnline_noopWhenNotShowing() {
        val reachability = mockk<NetworkReachability>(relaxed = true)
        assertFalse(
            clearOfflineBannerIfOnline(currentlyShowing = false, reachability = reachability),
        )
        verify(exactly = 0) { reachability.clearMemo() }
    }
}
