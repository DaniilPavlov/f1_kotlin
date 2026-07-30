package com.example.f1_kotlin.data.analytics

/**
 * Typed analytics events — names/params match Flutter `AnalyticsEvent` for cross-app parity.
 */
sealed class AnalyticsEvent {
    abstract val name: String
    open val params: Map<String, Any> get() = emptyMap()

    data class ScreenView(
        val screenName: String,
        val screenClass: String? = null,
    ) : AnalyticsEvent() {
        override val name = "screen_view"
        override val params: Map<String, Any>
            get() = buildMap {
                put("screen_name", screenName)
                screenClass?.let { put("screen_class", it) }
            }
    }

    data class TabSwitched(val tab: String) : AnalyticsEvent() {
        override val name = "tab_switched"
        override val params get() = mapOf("tab" to tab)
    }

    data class RaceOpened(
        val raceName: String,
        val season: String,
        val round: String,
    ) : AnalyticsEvent() {
        override val name = "race_opened"
        override val params get() = mapOf(
            "race_name" to raceName,
            "season" to season,
            "round" to round,
        )
    }

    data class H2hCompared(
        val driverA: String,
        val driverB: String,
        val scopeMode: String,
        val season: String? = null,
    ) : AnalyticsEvent() {
        override val name = "h2h_compared"
        override val params: Map<String, Any>
            get() = buildMap {
                put("driver_a", driverA)
                put("driver_b", driverB)
                put("scope", scopeMode)
                season?.let { put("season", it) }
            }
    }

    data class H2hConstructorsCompared(
        val constructorA: String,
        val constructorB: String,
        val scopeMode: String,
        val season: String? = null,
    ) : AnalyticsEvent() {
        override val name = "h2h_constructors_compared"
        override val params: Map<String, Any>
            get() = buildMap {
                put("constructor_a", constructorA)
                put("constructor_b", constructorB)
                put("scope", scopeMode)
                season?.let { put("season", it) }
            }
    }

    data class DriverOpened(
        val driverId: String,
        val driverName: String,
    ) : AnalyticsEvent() {
        override val name = "driver_opened"
        override val params get() = mapOf("driver_id" to driverId, "driver_name" to driverName)
    }

    data class ConstructorOpened(
        val constructorId: String,
        val constructorName: String,
    ) : AnalyticsEvent() {
        override val name = "constructor_opened"
        override val params get() = mapOf(
            "constructor_id" to constructorId,
            "constructor_name" to constructorName,
        )
    }

    data class CircuitOpened(
        val circuitId: String,
        val circuitName: String,
    ) : AnalyticsEvent() {
        override val name = "circuit_opened"
        override val params get() = mapOf("circuit_id" to circuitId, "circuit_name" to circuitName)
    }

    data class NewsOpened(val headline: String) : AnalyticsEvent() {
        override val name = "news_opened"
        override val params get() = mapOf("headline" to headline)
    }

    data object HallOfFameOpened : AnalyticsEvent() {
        override val name = "hall_of_fame_opened"
    }

    data object SeasonRewindOpened : AnalyticsEvent() {
        override val name = "season_rewind_opened"
    }

    data class ShareTapped(val contentType: String) : AnalyticsEvent() {
        override val name = "share_tapped"
        override val params get() = mapOf("content_type" to contentType)
    }

    data class ThemeChanged(val theme: String) : AnalyticsEvent() {
        override val name = "theme_changed"
        override val params get() = mapOf("theme" to theme)
    }

    data class LocaleChanged(val locale: String) : AnalyticsEvent() {
        override val name = "locale_changed"
        override val params get() = mapOf("locale" to locale)
    }

    data class RaceReminderToggled(val enabled: Boolean) : AnalyticsEvent() {
        override val name = "race_reminder_toggled"
        override val params get() = mapOf("enabled" to enabled)
    }

    data class RaceSearched(val query: String) : AnalyticsEvent() {
        override val name = "race_searched"
        override val params get() = mapOf("query" to query)
    }
}
