package com.example.f1_kotlin.domain

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocaleControllerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("f1_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        LocaleController.init(context)
    }

    @Test
    fun init_defaultsToRu() {
        assertEquals("ru", LocaleController.language.value)
    }

    @Test
    fun toggle_switchesBetweenRuAndEn() {
        assertEquals("en", LocaleController.toggle(context))
        assertEquals("en", LocaleController.language.value)
        assertEquals("ru", LocaleController.toggle(context))
        assertEquals("ru", LocaleController.language.value)
    }

    @Test
    fun preferences_persistAcrossInit() {
        LocaleController.toggle(context)
        LocaleController.init(context)
        assertEquals("en", LocaleController.language.value)
    }

    @Test
    fun currentLocale_followsLanguage() {
        LocaleController.init(context)
        assertEquals("ru", LocaleController.currentLocale().language)
        LocaleController.toggle(context)
        assertEquals("en", LocaleController.currentLocale().language)
    }
}
