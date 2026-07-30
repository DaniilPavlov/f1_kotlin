package com.example.f1_kotlin.data.deeplink

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class F1PetDeepLinksTest {
    @Test
    fun builders_produceExpectedUris() {
        assertEquals("f1pet://driver/norris", F1PetDeepLinks.driver("norris").toString())
        assertEquals("f1pet://constructor/mclaren", F1PetDeepLinks.constructor("mclaren").toString())
        assertEquals("f1pet://circuit/monza", F1PetDeepLinks.circuit("monza").toString())
        assertEquals("f1pet://race/live", F1PetDeepLinks.raceLive().toString())
        assertEquals("f1pet://race/2026/5", F1PetDeepLinks.race("2026", "5").toString())
    }

    @Test
    fun toDeepLinkTarget_parsesHosts() {
        assertEquals(DeepLinkTarget.Driver("norris"), Uri.parse("f1pet://driver/norris").toDeepLinkTarget())
        assertEquals(
            DeepLinkTarget.Constructor("ferrari"),
            Uri.parse("f1pet://constructor/ferrari").toDeepLinkTarget(),
        )
        assertEquals(DeepLinkTarget.Circuit("spa"), Uri.parse("f1pet://circuit/spa").toDeepLinkTarget())
        assertEquals(DeepLinkTarget.RaceLive, Uri.parse("f1pet://race/live").toDeepLinkTarget())
        assertEquals(DeepLinkTarget.Race("2026", "3"), Uri.parse("f1pet://race/2026/3").toDeepLinkTarget())
    }

    @Test
    fun toDeepLinkTarget_rejectsInvalid() {
        assertNull(Uri.parse("https://example.com").toDeepLinkTarget())
        assertNull(Uri.parse("f1pet://unknown/x").toDeepLinkTarget())
        assertNull(Uri.parse("f1pet://driver").toDeepLinkTarget())
        assertNull(Uri.parse("f1pet://race").toDeepLinkTarget())
        assertNull(Uri.parse("f1pet://race/2026").toDeepLinkTarget())
    }

    @Test
    fun deepLinkBus_emitsTargets() {
        val bus = DeepLinkBus()
        var received: DeepLinkTarget? = null
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            bus.targets.collect { received = it }
        }
        bus.offer(Uri.parse("f1pet://driver/alonso"))
        assertTrue(received is DeepLinkTarget.Driver)
        assertEquals("alonso", (received as DeepLinkTarget.Driver).driverId)
        bus.offer(DeepLinkTarget.RaceLive)
        assertEquals(DeepLinkTarget.RaceLive, received)
        job.cancel()
    }
}
