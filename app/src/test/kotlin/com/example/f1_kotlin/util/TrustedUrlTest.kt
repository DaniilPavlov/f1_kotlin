package com.example.f1_kotlin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit-тесты [TrustedUrl] — allowlist внешних ссылок. */
class TrustedUrlTest {

    @Test
    fun parse_acceptsHttpsWikipedia() {
        val url = TrustedUrl.parse("https://en.wikipedia.org/wiki/Monaco_Grand_Prix")
        assertNotNull(url)
        assertEquals("https://en.wikipedia.org/wiki/Monaco_Grand_Prix", url)
    }

    @Test
    fun parse_upgradesHttpToHttpsForAllowedHosts() {
        val url = TrustedUrl.parse("http://en.wikipedia.org/wiki/Monaco_Grand_Prix")
        assertNotNull(url)
        assertEquals("https://en.wikipedia.org/wiki/Monaco_Grand_Prix", url)
    }

    @Test
    fun parse_acceptsEspnAndGithub() {
        assertTrue(
            TrustedUrl.parse("https://www.espn.com/f1/story/_/id/123")!!
                .contains("www.espn.com"),
        )
        assertTrue(
            TrustedUrl.parse("https://github.com/DaniilPavlov/f1_kotlin/releases")!!
                .contains("github.com"),
        )
    }

    @Test
    fun parse_rejectsUnknownHostsAndNonHttpSchemes() {
        assertNull(TrustedUrl.parse("http://example.com/page"))
        assertNull(TrustedUrl.parse("javascript:alert(1)"))
        assertNull(TrustedUrl.parse(""))
    }

    @Test
    fun preferHttps_upgradesWithoutHostCheck() {
        assertEquals(
            "https://cdn.example.com/img.jpg",
            TrustedUrl.preferHttps("http://cdn.example.com/img.jpg"),
        )
        assertEquals(
            "https://cdn.example.com/img.jpg",
            TrustedUrl.preferHttps("https://cdn.example.com/img.jpg"),
        )
    }

    @Test
    fun isWikipediaHost() {
        assertTrue(TrustedUrl.isWikipediaHost("en.wikipedia.org"))
        assertTrue(TrustedUrl.isWikipediaHost("wikipedia.org"))
    }
}
