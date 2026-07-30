package com.example.f1_kotlin.data.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {
    @Test
    fun screenView_params() {
        val event = AnalyticsEvent.ScreenView("home", "HomeScreen")
        assertEquals("screen_view", event.name)
        assertEquals("home", event.params["screen_name"])
        assertEquals("HomeScreen", event.params["screen_class"])
    }

    @Test
    fun tabAndEntityEvents_namesAndParams() {
        assertEquals("tab_switched", AnalyticsEvent.TabSwitched("results").name)
        assertEquals(
            mapOf("race_name" to "Monaco", "season" to "2026", "round" to "8"),
            AnalyticsEvent.RaceOpened("Monaco", "2026", "8").params,
        )
        assertEquals("driver_opened", AnalyticsEvent.DriverOpened("norris", "Lando Norris").name)
        assertEquals("constructor_opened", AnalyticsEvent.ConstructorOpened("mclaren", "McLaren").name)
        assertEquals("circuit_opened", AnalyticsEvent.CircuitOpened("monza", "Monza").name)
        assertEquals("news_opened", AnalyticsEvent.NewsOpened("headline").name)
    }

    @Test
    fun h2hEvents_optionalSeason() {
        val career = AnalyticsEvent.H2hCompared("a", "b", "career")
        assertEquals("career", career.params["scope"])
        assertTrue(!career.params.containsKey("season"))

        val season = AnalyticsEvent.H2hCompared("a", "b", "season", "2026")
        assertEquals("2026", season.params["season"])

        val ctors = AnalyticsEvent.H2hConstructorsCompared("mclaren", "ferrari", "season", "2025")
        assertEquals("h2h_constructors_compared", ctors.name)
        assertEquals("ferrari", ctors.params["constructor_b"])
    }

    @Test
    fun preferenceAndShareEvents() {
        assertEquals("hall_of_fame_opened", AnalyticsEvent.HallOfFameOpened.name)
        assertEquals("season_rewind_opened", AnalyticsEvent.SeasonRewindOpened.name)
        assertEquals("career_card", AnalyticsEvent.ShareTapped("career_card").params["content_type"])
        assertEquals("dark", AnalyticsEvent.ThemeChanged("dark").params["theme"])
        assertEquals("en", AnalyticsEvent.LocaleChanged("en").params["locale"])
        assertEquals("true", AnalyticsEvent.RaceReminderToggled(true).params["enabled"].toString())
        assertEquals("monaco", AnalyticsEvent.RaceSearched("monaco").params["query"])
    }
}
