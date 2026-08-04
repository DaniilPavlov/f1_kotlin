package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NotificationsPreferenceTest {
    private lateinit var analytics: AnalyticsGateway
    private lateinit var prefs: NotificationsPreference

    @Before
    fun setUp() {
        analytics = mockk(relaxed = true)
        prefs = NotificationsPreference(RuntimeEnvironment.getApplication(), analytics)
    }

    @Test
    fun defaults_enabled() {
        assertTrue(prefs.effectivelyEnabled)
        assertTrue(prefs.practiceRemindersEffectivelyEnabled)
    }

    @Test
    fun setRaceRemindersDisabled_disablesPracticeGate() {
        prefs.setRaceRemindersEnabled(false)
        assertFalse(prefs.effectivelyEnabled)
        assertFalse(prefs.canTogglePractice)
        assertFalse(prefs.practiceRemindersEffectivelyEnabled)
        verify { analytics.log(AnalyticsEvent.RaceReminderToggled(false)) }
    }

    @Test
    fun setPracticeReminders_logsAnalytics() {
        prefs.setPracticeRemindersEnabled(false)
        assertFalse(prefs.practiceRemindersEffectivelyEnabled)
        verify { analytics.log(AnalyticsEvent.PracticeReminderToggled(false)) }
    }
}
