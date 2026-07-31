package com.example.f1_kotlin.domain

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ThemeControllerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("f1_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        ThemeController.init(context)
    }

    @Test
    fun init_defaultsToSystem() {
        assertEquals(AppThemePreference.System, ThemeController.preference.value)
        assertEquals("system", ThemeController.preferenceAnalyticsValue())
    }

    @Test
    fun cycle_systemToLightToDarkToSystem() {
        assertEquals(AppThemePreference.Light, ThemeController.cycle(context))
        assertEquals("light", ThemeController.preferenceAnalyticsValue())
        assertEquals(AppThemePreference.Dark, ThemeController.cycle(context))
        assertEquals("dark", ThemeController.preferenceAnalyticsValue())
        assertEquals(AppThemePreference.System, ThemeController.cycle(context))
    }

    @Test
    fun preferences_persistAcrossInit() {
        ThemeController.cycle(context) // light
        ThemeController.init(context)
        assertEquals(AppThemePreference.Light, ThemeController.preference.value)
    }
}
