package com.example.f1_kotlin.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.ConstructorStanding
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.DriverStanding
import com.example.f1_kotlin.ui.screens.home.HomeScreenContent
import com.example.f1_kotlin.viewmodel.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsSeasonAndDriverName() {
        val drivers = listOf(
            DriverStanding(
                position = "1",
                positionText = "1",
                points = "100",
                wins = "5",
                driver = Driver("verstappen", "", "Max", "Verstappen", "", "Dutch"),
                constructors = listOf(Constructor("red_bull", "", "Red Bull", "Austrian")),
            ),
        )
        val constructors = listOf(
            ConstructorStanding(
                position = "1",
                positionText = "1",
                points = "200",
                wins = "6",
                constructor = Constructor("red_bull", "", "Red Bull", "Austrian"),
            ),
        )

        composeRule.setContent {
            HomeScreenContent(
                uiState = HomeUiState(
                    drivers = AsyncValue.Value(drivers),
                    constructors = AsyncValue.Value(constructors),
                    season = "2026",
                    round = "5",
                ),
                onRetry = {},
                onChangeTable = {},
            )
        }

        composeRule.onNodeWithText("2026", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Verstappen", substring = true).assertIsDisplayed()
    }
}
