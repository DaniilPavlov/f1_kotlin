package com.example.f1_kotlin.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class H2hScopeStateTest {
    @Test
    fun selectedSeason_careerScope_isNull() {
        val scope = H2hScopeState(scopeMode = 0, latestSeason = "2026")
        assertFalse(scope.isSeasonScope)
        assertNull(scope.selectedSeason)
        assertFalse(scope.showYearPicker)
    }

    @Test
    fun selectedSeason_currentSeasonMode_usesLatest() {
        val scope = H2hScopeState(
            scopeMode = 1,
            useCurrentSeason = true,
            latestSeason = "2026",
            pickedSeason = "2024",
        )
        assertTrue(scope.isSeasonScope)
        assertEquals("2026", scope.selectedSeason)
        assertFalse(scope.showYearPicker)
    }

    @Test
    fun selectedSeason_pickedYear_requiresFourDigits() {
        val incomplete = H2hScopeState(
            scopeMode = 1,
            useCurrentSeason = false,
            pickedSeason = "202",
        )
        assertNull(incomplete.selectedSeason)
        assertTrue(incomplete.showYearPicker)

        val complete = incomplete.copy(pickedSeason = "2024")
        assertEquals("2024", complete.selectedSeason)
    }

    @Test
    fun canCompare_rules() {
        val scope = H2hScopeState(scopeMode = 0)
        assertFalse(scope.canCompare(null, "b"))
        assertFalse(scope.canCompare("a", null))
        assertFalse(scope.canCompare("a", "a"))
        assertTrue(scope.canCompare("a", "b"))

        val season = H2hScopeState(scopeMode = 1, useCurrentSeason = false, pickedSeason = "")
        assertFalse(season.canCompare("a", "b"))
        assertTrue(season.copy(pickedSeason = "2025").canCompare("a", "b"))
    }
}
