package com.example.f1_kotlin.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDataStoreKeysTest {
    @Test
    fun driverIndexes_areStable() {
        assertEquals("standings_d1_code", WidgetDataStore.driverCode(1))
        assertEquals("standings_d3_points", WidgetDataStore.driverPoints(3))
        assertEquals("f1_app_widgets", WidgetDataStore.PREFS_NAME)
        assertEquals("next_gp_has_data", WidgetDataStore.NEXT_GP_HAS_DATA)
        assertEquals("standings_has_data", WidgetDataStore.STANDINGS_HAS_DATA)
    }
}
