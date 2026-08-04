package com.example.f1_kotlin.ui.screens.predictor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.predictor.PredictorGridKind
import com.example.f1_kotlin.domain.predictor.PredictorSeasonSummary
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kotlin.domain.predictor.predictorDriverLabel
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.ConstructorColors
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.util.CountdownParts
import com.example.f1_kotlin.viewmodel.PredictorUiState
import com.example.f1_kotlin.viewmodel.PredictorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictorScreen(
    viewModel: PredictorViewModel,
    onGoSignIn: () -> Unit,
    onGoProfile: () -> Unit,
    onOpenLeaderboard: (year: String, myPoints: Int) -> Unit,
    onOpenWeekend: (season: String, weekend: PredictorWeekendPrediction) -> Unit,
    onOpenSeason: (year: String) -> Unit,
) {
    PredictorAuthGate(
        asTabRoot = true,
        onGoSignIn = onGoSignIn,
        onGoProfile = onGoProfile,
    ) {
        val uiState by viewModel.uiState.collectAsState()
        when {
            uiState.isLoading && uiState.races.isEmpty() ->
                LoadingIndicator(Modifier.fillMaxSize())
            uiState.error != null && uiState.races.isEmpty() -> ErrorBody(
                title = uiState.error?.title,
                subtitle = uiState.error?.subtitle,
                onRetry = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            )
            else -> PredictorBody(
                viewModel = viewModel,
                uiState = uiState,
                onOpenLeaderboard = onOpenLeaderboard,
                onOpenWeekend = onOpenWeekend,
                onOpenSeason = onOpenSeason,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PredictorBody(
    viewModel: PredictorViewModel,
    uiState: PredictorUiState,
    onOpenLeaderboard: (year: String, myPoints: Int) -> Unit,
    onOpenWeekend: (season: String, weekend: PredictorWeekendPrediction) -> Unit,
    onOpenSeason: (year: String) -> Unit,
) {
    val year = viewModel.seasonYear(uiState) ?: "—"
    val race = viewModel.upcomingRace(uiState)
    val history = viewModel.historyWeekends(uiState)
    val archived = viewModel.archivedSeasonSummaries(uiState)
    val locked = viewModel.computeIsLocked(uiState)
    val prediction = viewModel.currentPrediction(uiState)
    val waitingResults = locked &&
        prediction != null &&
        prediction.qualiPoints == null &&
        prediction.racePoints == null
    val order = viewModel.activeDraft(uiState)
    var moveFromIndex by remember { mutableStateOf<Int?>(null) }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshAll,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp)
                .padding(vertical = AppDimens.verticalPadding.dp),
        ) {
            uiState.error?.let { err ->
                Text(
                    text = listOfNotNull(err.title, err.subtitle).joinToString("\n"),
                    style = AppStyles.caption.copy(color = F1Red),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = stringResource(
                    R.string.predictor_season_points,
                    year,
                    viewModel.seasonTotalPoints(uiState),
                ),
                style = AppStyles.h3.copy(color = appColors().black),
            )
            viewModel.seasonYear(uiState)?.let { seasonYear ->
                Spacer(Modifier.height(12.dp))
                BlackButton(
                    text = stringResource(R.string.predictor_leaderboard_open),
                    onClick = {
                        onOpenLeaderboard(seasonYear, viewModel.seasonTotalPoints(uiState))
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            if (race == null) {
                Text(
                    text = stringResource(R.string.predictor_no_upcoming),
                    style = AppStyles.body.copy(color = appColors().black),
                )
            } else {
                WeekendHeader(
                    race = race,
                    isLocked = locked,
                    missingQualifyingTime = viewModel.missingQualifyingTime(uiState),
                    lockCountdown = viewModel.lockCountdown(uiState),
                    waitingResults = waitingResults,
                )
                Spacer(Modifier.height(12.dp))
                CustomSwitcher(
                    firstTitle = stringResource(R.string.qualifying),
                    secondTitle = stringResource(R.string.race),
                    activeValue = if (uiState.selectedGrid == PredictorGridKind.Qualifying) 0 else 1,
                    onChanged = {
                        viewModel.selectGrid(
                            if (it == 0) PredictorGridKind.Qualifying else PredictorGridKind.Race,
                        )
                    },
                )
                if (!locked && uiState.selectedGrid == PredictorGridKind.Race) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.predictor_copy_qualifying_to_race),
                        style = AppStyles.body.copy(color = F1Red),
                        modifier = Modifier
                            .clickable { viewModel.copyQualifyingToRace() }
                            .padding(vertical = 4.dp),
                    )
                }
                prediction?.let { pred ->
                    if (pred.hasAnyPoints) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = formatWeekendPoints(pred),
                            style = AppStyles.caption.copy(color = appColors().black),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                order.forEachIndexed { index, driverId ->
                    DriverTile(
                        position = index + 1,
                        driver = uiState.driversById[driverId],
                        constructor = uiState.constructorsByDriverId[driverId],
                        driverId = driverId,
                        locked = locked,
                        onClick = if (locked) null else ({ moveFromIndex = index }),
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.predictor_history_title),
                style = AppStyles.h3.copy(color = appColors().black),
            )
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.predictor_history_empty),
                    style = AppStyles.body.copy(color = appColors().black),
                )
            } else {
                history.forEach { weekend ->
                    HistoryTile(
                        weekend = weekend,
                        onClick = {
                            viewModel.seasonYear(uiState)?.let { y ->
                                onOpenWeekend(y, weekend)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.predictor_past_seasons_title),
                    style = AppStyles.h3.copy(color = appColors().black),
                )
                Spacer(Modifier.height(8.dp))
                archived.forEach { summary ->
                    PastSeasonButton(summary = summary, onClick = { onOpenSeason(summary.year) })
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    moveFromIndex?.let { from ->
        MovePositionDialog(
            driverLabel = predictorDriverLabel(
                uiState.driversById[order.getOrNull(from)],
                order.getOrNull(from),
            ),
            size = order.size,
            currentIndex = from,
            onDismiss = { moveFromIndex = null },
            onPick = { to ->
                viewModel.moveDraftTo(from, to)
                moveFromIndex = null
            },
        )
    }
}

@Composable
private fun WeekendHeader(
    race: Race,
    isLocked: Boolean,
    missingQualifyingTime: Boolean,
    lockCountdown: CountdownParts,
    waitingResults: Boolean,
) {
    val status = when {
        missingQualifyingTime -> stringResource(R.string.predictor_missing_quali_time)
        isLocked && waitingResults ->
            "${stringResource(R.string.predictor_locked)} · ${stringResource(R.string.predictor_waiting_results)}"
        isLocked -> stringResource(R.string.predictor_locked)
        else -> stringResource(R.string.predictor_lock_in, formatCountdown(lockCountdown))
    }
    Column {
        Text(text = race.raceName, style = AppStyles.h3.copy(color = appColors().black))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isLocked) F1Red.copy(alpha = 0.2f) else appColors().grayBg,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = status, style = AppStyles.caption.copy(color = appColors().black))
        }
    }
}

@Composable
private fun DriverTile(
    position: Int,
    driver: Driver?,
    constructor: Constructor?,
    driverId: String,
    locked: Boolean,
    onClick: (() -> Unit)?,
) {
    val accent = ConstructorColors.forConstructorId(constructor?.constructorId.orEmpty())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(appColors().grayBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "P$position",
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.width(40.dp),
        )
        Text(
            text = predictorDriverLabel(driver, driverId),
            style = AppStyles.body.copy(color = appColors().black),
            modifier = Modifier.weight(1f),
        )
        if (locked) {
            Text(
                text = stringResource(R.string.predictor_locked_suffix).trimStart(',', ' '),
                style = AppStyles.caption.copy(color = appColors().black.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun HistoryTile(
    weekend: PredictorWeekendPrediction,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, F1Red, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = weekend.raceName.ifBlank { "R${weekend.round}" },
            style = AppStyles.body.copy(color = appColors().black),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatWeekendPoints(weekend),
            style = AppStyles.caption.copy(color = appColors().black),
        )
    }
}

@Composable
private fun PastSeasonButton(
    summary: PredictorSeasonSummary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, F1Red, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(
                R.string.predictor_season_button,
                summary.year,
                summary.totalPoints,
                summary.weekendCount,
            ),
            style = AppStyles.body.copy(color = appColors().black),
        )
    }
}

@Composable
private fun MovePositionDialog(
    driverLabel: String,
    size: Int,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.predictor_move_to_title, driverLabel)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.predictor_move_to_hint),
                    style = AppStyles.caption.copy(color = appColors().black),
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(size) { index ->
                        TextButton(
                            onClick = { onPick(index) },
                            enabled = index != currentIndex,
                        ) {
                            Text("P${index + 1}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun formatWeekendPoints(weekend: PredictorWeekendPrediction): String {
    val pending = stringResource(R.string.predictor_pending_points)
    val quali = weekend.qualiPoints?.toString() ?: pending
    val race = weekend.racePoints?.toString() ?: pending
    return stringResource(
        R.string.predictor_weekend_points,
        quali,
        race,
        weekend.totalPoints,
    )
}

private fun formatCountdown(parts: CountdownParts): String =
    "%02d:%02d:%02d:%02d".format(parts.days, parts.hours, parts.minutes, parts.seconds)
