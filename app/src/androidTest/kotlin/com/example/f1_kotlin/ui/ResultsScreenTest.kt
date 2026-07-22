package com.example.f1_kotlin.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.CircuitLocation
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.screens.results.ResultsScreenContent
import com.example.f1_kotlin.viewmodel.ResultsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsLastRaceName() {
        val race = Race(
            season = "2026",
            round = "5",
            url = "",
            raceName = "Monaco Grand Prix",
            circuit = Circuit(
                circuitId = "monaco",
                url = "",
                circuitName = "Monaco",
                location = CircuitLocation("43.7", "7.4", "Monte Carlo", "Monaco"),
            ),
            date = "2026-05-25",
            results = emptyList(),
        )

        composeRule.setContent {
            ResultsScreenContent(
                uiState = ResultsUiState(
                    lastRace = AsyncValue.Value(race),
                    scoreboard = AsyncValue.Value(null),
                ),
            )
        }

        composeRule.onNodeWithText("Monaco Grand Prix").assertIsDisplayed()
    }
}
