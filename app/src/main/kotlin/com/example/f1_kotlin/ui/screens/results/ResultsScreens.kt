package com.example.f1_kotlin.ui.screens.results

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.PitStopsTable
import com.example.f1_kotlin.ui.components.QualifyingTable
import com.example.f1_kotlin.ui.components.RacePickerField
import com.example.f1_kotlin.ui.components.RaceResultsTable
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.SectionHeader
import com.example.f1_kotlin.ui.components.TableHeaderRow
import com.example.f1_kotlin.ui.components.shimmer.LastRaceSectionShimmer
import com.example.f1_kotlin.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kotlin.ui.components.shimmer.RaceInfoShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.util.RegisterShareAction
import com.example.f1_kotlin.util.rememberShareRaceAction
import com.example.f1_kotlin.viewmodel.RaceInfoScreenViewModel
import com.example.f1_kotlin.viewmodel.RaceSearchViewModel
import com.example.f1_kotlin.viewmodel.ResultsUiState
import com.example.f1_kotlin.viewmodel.ResultsViewModel

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onSearchRace: () -> Unit,
    onHallOfFame: () -> Unit,
    onSeasonRewind: () -> Unit = {},
    onH2hDrivers: () -> Unit,
    onH2hConstructors: () -> Unit,
    onFinishStatus: () -> Unit,
    onRaceDetails: (Race) -> Unit,
    onDriverClick: (Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    ResultsScreenContent(
        uiState = uiState,
        onRetry = viewModel::refreshAll,
        onRefresh = viewModel::refreshAll,
        onSearchRace = onSearchRace,
        onHallOfFame = onHallOfFame,
        onSeasonRewind = onSeasonRewind,
        onH2hDrivers = onH2hDrivers,
        onH2hConstructors = onH2hConstructors,
        onFinishStatus = onFinishStatus,
        onRaceDetails = onRaceDetails,
        onDriverClick = onDriverClick,
    )
}

/** Testable content — no Hilt / ViewModel required. */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
fun ResultsScreenContent(
    uiState: ResultsUiState,
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSearchRace: () -> Unit = {},
    onHallOfFame: () -> Unit = {},
    onSeasonRewind: () -> Unit = {},
    onH2hDrivers: () -> Unit = {},
    onH2hConstructors: () -> Unit = {},
    onFinishStatus: () -> Unit = {},
    onRaceDetails: (Race) -> Unit = {},
    onDriverClick: (Driver) -> Unit = {},
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = AppDimens.verticalPadding.dp),
        ) {
            WeekendScoreboardSection(uiState.scoreboard)

            when (val state = uiState.lastRace) {
                is AsyncValue.Loading -> LastRaceSectionShimmer()
                is AsyncValue.Error -> ErrorBody(
                    state.message,
                    state.subtitle,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp),
                )
                is AsyncValue.Value -> Column(
                    modifier = Modifier.padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                        Text(stringResource(R.string.last_race), style = AppStyles.h2)
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        Text(state.value.raceName, style = AppStyles.h2)
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.season_label, state.value.season),
                                style = AppStyles.h2,
                                modifier = Modifier.weight(1f),
                            )
                            Text(stringResource(R.string.round_label, state.value.round), style = AppStyles.h2)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    RaceResultsTable(
                        race = state.value,
                        maxRows = 3,
                        onDetailsClick = { onRaceDetails(state.value) },
                        onDriverClick = onDriverClick,
                    )
                }
            }

            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            BoxedAction(title = stringResource(R.string.choose_specific_race), onClick = onSearchRace)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(R.string.hall_of_fame_title), onClick = onHallOfFame)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(R.string.season_rewind_title), onClick = onSeasonRewind)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(R.string.h2h_title), onClick = onH2hDrivers)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(R.string.h2h_constructors_title), onClick = onH2hConstructors)
            Spacer(Modifier.height(12.dp))
            BoxedAction(title = stringResource(R.string.finish_status_title), onClick = onFinishStatus)
        }
    }
}

@Composable
fun RaceSearchScreen(
    viewModel: RaceSearchViewModel,
    onRaceDetails: (Race) -> Unit,
    onDriverClick: (Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.horizontalPadding.dp),
    ) {
        Text(
            stringResource(R.string.race_search_info),
            style = AppStyles.body,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        SeasonPickerField(
            value = uiState.year,
            label = stringResource(R.string.season),
            hint = stringResource(R.string.select_season),
            onSeasonSelected = viewModel::onYearChanged,
            loadSeasons = viewModel::loadSeasonYears,
        )
        Spacer(Modifier.height(12.dp))
        RacePickerField(
            displayValue = uiState.raceDisplay,
            seasonYear = uiState.year,
            label = stringResource(R.string.race),
            hint = stringResource(R.string.select_race),
            disabledHint = stringResource(R.string.select_season_first),
            onRacePicked = { viewModel.onRacePicked(it.round, it.title) },
            loadRaces = viewModel::loadSeasonRaces,
        )
        Spacer(Modifier.height(16.dp))
        BlackButton(
            text = stringResource(R.string.search),
            enabled = uiState.fieldsInputted,
            onClick = viewModel::loadRaceResults,
        )
        if (uiState.errorMessage.isNotEmpty()) {
            Text(uiState.errorMessage, style = AppStyles.body, modifier = Modifier.padding(top = 16.dp))
        }
        if (!uiState.dataLoaded) {
            LoadingIndicator(Modifier.padding(top = 24.dp))
        }
        when (val state = uiState.searchedRace) {
            is AsyncValue.Value -> state.value?.let { race ->
                Spacer(Modifier.height(24.dp))
                Text(race.raceName, style = AppStyles.h2)
                RaceResultsTable(
                    race = race,
                    maxRows = 3,
                    onDetailsClick = { onRaceDetails(race) },
                    onDriverClick = onDriverClick,
                )
            }
            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RaceInfoScreen(
    viewModel: RaceInfoScreenViewModel,
    onDriverClick: (Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val race = uiState.race
    val qualifying = uiState.qualifying
    val pitStops = uiState.pitStops
    val sprint = uiState.sprint
    val error = uiState.error

    when {
        error != null && race.isError -> ErrorBody(
            error.title,
            error.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        race.isLoading -> RaceInfoShimmer(modifier = Modifier.fillMaxSize())
        race is AsyncValue.Value -> {
            val raceData = race.value
            RegisterShareAction(rememberShareRaceAction(raceData))
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                // pinned section app bars
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppDimens.horizontalPadding.dp),
                ) {
                    item {
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        Text(raceData.raceName, style = AppStyles.h2)
                        RowInfo(
                            stringResource(R.string.season_label, raceData.season),
                            stringResource(R.string.round_label, raceData.round),
                        )
                    }
                    stickyHeader {
                        RaceInfoPinnedHeader(
                            title = stringResource(R.string.race),
                            headerCells = listOf(
                                stringResource(R.string.driver),
                                stringResource(R.string.constructor),
                                stringResource(R.string.time),
                                stringResource(R.string.points),
                                stringResource(R.string.best_lap),
                            ),
                            weights = RaceResultsStickyWeights,
                        )
                    }
                    item {
                        RaceResultsTable(
                            race = raceData,
                            showHeader = false,
                            timeHeaderRes = R.string.time,
                            onDriverClick = onDriverClick,
                        )
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                    }
                    if (sprint is AsyncValue.Value && sprint.value.isNotEmpty()) {
                        stickyHeader {
                            RaceInfoPinnedHeader(
                                title = stringResource(R.string.sprint),
                                headerCells = listOf(
                                    stringResource(R.string.driver),
                                    stringResource(R.string.constructor),
                                    stringResource(R.string.time),
                                    stringResource(R.string.points),
                                    stringResource(R.string.best_lap),
                                ),
                                weights = RaceResultsStickyWeights,
                            )
                        }
                        item {
                            RaceResultsTable(
                                race = raceData.copy(results = sprint.value),
                                showHeader = false,
                                timeHeaderRes = R.string.time,
                                onDriverClick = onDriverClick,
                            )
                            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                        }
                    }
                    stickyHeader {
                        RaceInfoPinnedHeader(
                            title = stringResource(R.string.qualifying),
                            headerCells = listOf(
                                stringResource(R.string.driver),
                                stringResource(R.string.constructor),
                                "Q1",
                                "Q2",
                                "Q3",
                            ),
                        )
                    }
                    item {
                        when (val q = qualifying) {
                            is AsyncValue.Loading -> ListRowsShimmer(rowCount = 8)
                            is AsyncValue.Error -> Text(q.message, style = AppStyles.body)
                            is AsyncValue.Value -> QualifyingTable(
                                results = q.value,
                                showHeader = false,
                                onDriverClick = onDriverClick,
                            )
                        }
                        Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                    }
                    stickyHeader {
                        RaceInfoPinnedHeader(
                            title = stringResource(R.string.pit_stops),
                            headerCells = listOf(
                                stringResource(R.string.driver),
                                stringResource(R.string.lap),
                                stringResource(R.string.stop_number),
                                stringResource(R.string.stop_time),
                                stringResource(R.string.race_time),
                            ),
                        )
                    }
                    item {
                        when (val p = pitStops) {
                            is AsyncValue.Loading -> ListRowsShimmer(rowCount = 6)
                            is AsyncValue.Error -> Text(p.message, style = AppStyles.body)
                            is AsyncValue.Value -> PitStopsTable(stops = p.value, showHeader = false)
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

/** Flex weights. */
private val RaceResultsStickyWeights = listOf(1.15f, 1.35f, 1.1f, 0.55f, 0.9f)

/** Section title + column headers — sticks while that section is scrolling. */
@Composable
private fun RaceInfoPinnedHeader(
    title: String,
    headerCells: List<String>,
    weights: List<Float>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(appColors().white),
    ) {
        SectionHeader(title)
        TableHeaderRow(cells = headerCells, weights = weights)
    }
}

@Composable
private fun RowInfo(left: String, right: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(left, style = AppStyles.h2, modifier = Modifier.weight(1f))
        Text(right, style = AppStyles.h2)
    }
}

@Composable
private fun BoxedAction(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.horizontalPadding.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, style = AppStyles.h3)
    }
}
