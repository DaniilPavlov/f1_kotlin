package com.example.f1_kotlin.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFormatTest {
    @Test
    fun shortRaceName_stripsGrandPrixSuffix() {
        assertEquals("Monaco", WidgetFormat.shortRaceName("Monaco Grand Prix"))
        assertEquals("Bahrain", WidgetFormat.shortRaceName("  Bahrain Grand Prix  "))
        assertEquals("Sprint", WidgetFormat.shortRaceName("Sprint"))
        assertEquals("Grand Prix", WidgetFormat.shortRaceName("Grand Prix"))
    }

    @Test
    fun driverLabel_prefersCode() {
        assertEquals("VER", WidgetFormat.driverLabel("VER", "Verstappen"))
        assertEquals("VERSTAPPEN", WidgetFormat.driverLabel(null, "Verstappen"))
        assertEquals("VERSTAPPEN", WidgetFormat.driverLabel("none", "Verstappen"))
        assertEquals("VERSTAPPEN", WidgetFormat.driverLabel("  ", "Verstappen"))
    }
}
