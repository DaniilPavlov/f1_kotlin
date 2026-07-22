package com.example.f1_kotlin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryFlagCodesTest {
    @Test
    fun resolveNationalityAndCountry() {
        assertEquals("gb", CountryFlagCodes.resolve("British"))
        assertEquals("gb", CountryFlagCodes.resolve("United Kingdom"))
        assertEquals("us", CountryFlagCodes.resolve("american"))
        assertEquals("mc", CountryFlagCodes.resolve("Monegasque"))
        assertNull(CountryFlagCodes.resolve("Atlantis"))
    }

    @Test
    fun toEmojiRegionalIndicators() {
        assertEquals("🇬🇧", CountryFlagCodes.toEmoji("gb"))
        assertEquals("🇺🇸", CountryFlagCodes.toEmoji("us"))
    }
}
