package com.example.f1_kotlin.domain

import android.content.Context
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.data.analytics.AnalyticsGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Локальные предпочтения напоминаний (SharedPreferences).
 */
@Singleton
class NotificationsPreference @Inject constructor(
    @ApplicationContext context: Context,
    private val analytics: AnalyticsGateway,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _raceRemindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_RACE, true))
    val raceRemindersEnabled: StateFlow<Boolean> = _raceRemindersEnabled.asStateFlow()

    private val _practiceRemindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_PRACTICE, true))
    val practiceRemindersEnabled: StateFlow<Boolean> = _practiceRemindersEnabled.asStateFlow()

    val canTogglePractice: Boolean
        get() = _raceRemindersEnabled.value

    val effectivelyEnabled: Boolean
        get() = _raceRemindersEnabled.value

    val practiceRemindersEffectivelyEnabled: Boolean
        get() = effectivelyEnabled && _practiceRemindersEnabled.value

    fun setRaceRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RACE, enabled).apply()
        _raceRemindersEnabled.update { enabled }
        analytics.log(AnalyticsEvent.RaceReminderToggled(enabled))
    }

    fun setPracticeRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRACTICE, enabled).apply()
        _practiceRemindersEnabled.update { enabled }
        analytics.log(AnalyticsEvent.PracticeReminderToggled(enabled))
    }

    companion object {
        private const val PREFS = "f1_preferences"
        private const val KEY_RACE = "race_reminders_enabled"
        private const val KEY_PRACTICE = "practice_reminders_enabled"
    }
}
