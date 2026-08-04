package com.example.f1_kotlin.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConstructorColorsTest {
    @Test
    fun knownIds_matchPalette() {
        assertEquals(Color(0xFFA51010), ConstructorColors.forConstructorId("ferrari"))
        assertEquals(Color(0xFF006F62), ConstructorColors.forConstructorId("Mercedes"))
        assertEquals(Color(0xFF1E2E5A), ConstructorColors.forConstructorId("red_bull"))
        assertEquals(Color(0xFF6B9AC4), ConstructorColors.forConstructorId("rb"))
        assertEquals(Color(0xFFFF8700), ConstructorColors.forConstructorId("mclaren"))
        assertEquals(Color(0xFFE85A5A), ConstructorColors.forConstructorId("audi"))
        assertEquals(Color(0xFF8A8D8F), ConstructorColors.forConstructorId("cadillac"))
        assertEquals(Color(0xFF2B2B2B), ConstructorColors.forConstructorId("haas"))
        assertEquals(Color(0xFF229971), ConstructorColors.forConstructorId("aston_martin"))
        assertEquals(Color(0xFFFF69B4), ConstructorColors.forConstructorId("alpine"))
        assertEquals(Color(0xFF00A0DE), ConstructorColors.forConstructorId("williams"))
    }

    @Test
    fun aliases_shareColor() {
        assertEquals(
            ConstructorColors.forConstructorId("sauber"),
            ConstructorColors.forConstructorId("audi"),
        )
        assertEquals(
            ConstructorColors.forConstructorId("racing_bulls"),
            ConstructorColors.forConstructorId("rb"),
        )
    }

    @Test
    fun unknownIds_areStableAndDistinctFromKnown() {
        val a = ConstructorColors.forConstructorId("lotus")
        val b = ConstructorColors.forConstructorId("lotus")
        val c = ConstructorColors.forConstructorId("jordan")
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals(a, ConstructorColors.forConstructorId("ferrari"))
    }
}
