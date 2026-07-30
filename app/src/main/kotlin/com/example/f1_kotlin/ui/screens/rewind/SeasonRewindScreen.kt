package com.example.f1_kotlin.ui.screens.rewind

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.viewmodel.SeasonRewindBarEntry
import com.example.f1_kotlin.viewmodel.SeasonRewindViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonRewindScreen(viewModel: SeasonRewindViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = appColors()

    PullToRefreshBox(
        isRefreshing = uiState.races is AsyncValue.Loading,
        onRefresh = viewModel::refreshAll,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val racesState = uiState.races) {
            is AsyncValue.Error -> ErrorBody(
                racesState.message,
                racesState.subtitle,
                onRetry = viewModel::refreshAll,
                modifier = Modifier.padding(AppDimens.horizontalPadding.dp),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
            ) {
                Text(
                    stringResource(R.string.season_rewind_subtitle),
                    style = AppStyles.caption.copy(color = colors.textGray),
                )
                Spacer(Modifier.height(16.dp))
                SeasonPickerField(
                    value = uiState.year,
                    label = stringResource(R.string.season),
                    hint = stringResource(R.string.select_season),
                    onSeasonSelected = viewModel::onSeasonChanged,
                    loadSeasons = viewModel::loadSeasonYears,
                )
                Spacer(Modifier.height(20.dp))

                val races = (racesState as? AsyncValue.Value)?.value.orEmpty()
                if (racesState is AsyncValue.Loading && races.isEmpty()) {
                    LoadingIndicator()
                } else if (races.isEmpty()) {
                    Text(
                        stringResource(R.string.season_rewind_empty),
                        style = AppStyles.body.copy(color = colors.black),
                    )
                } else {
                    val race = uiState.selectedRace
                    Text(
                        text = race?.let { "R${it.round} · ${it.raceName}" } ?: "",
                        style = AppStyles.h3.copy(color = colors.black),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::togglePlayback, enabled = uiState.canPlay) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = F1Red,
                            )
                        }
                        Slider(
                            value = uiState.selectedRoundIndex.toFloat(),
                            onValueChange = { viewModel.selectRound(it.toInt()) },
                            valueRange = 0f..(races.size - 1).coerceAtLeast(0).toFloat(),
                            steps = (races.size - 2).coerceAtLeast(0),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (uiState.chartLoading && uiState.driverBars.isEmpty()) {
                        LoadingIndicator()
                    } else {
                        Text(stringResource(R.string.drivers), style = AppStyles.body.copy(color = colors.black))
                        Spacer(Modifier.height(8.dp))
                        RacingBars(uiState.driverBars)
                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(R.string.constructors), style = AppStyles.body.copy(color = colors.black))
                        Spacer(Modifier.height(8.dp))
                        RacingBars(uiState.constructorBars)
                    }
                }
            }
        }
    }
}

@Composable
private fun RacingBars(entries: List<SeasonRewindBarEntry>) {
    val colors = appColors()
    val maxPoints = entries.maxOfOrNull { it.points }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.take(10).forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${entry.rank + 1}",
                    style = AppStyles.caption.copy(color = colors.textGray),
                    modifier = Modifier.width(24.dp),
                )
                Text(
                    text = entry.tag.ifBlank { entry.label }.take(3).uppercase(),
                    style = AppStyles.caption.copy(color = colors.black),
                    modifier = Modifier.width(40.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .background(colors.grayBg),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((entry.points / maxPoints).toFloat().coerceIn(0.02f, 1f))
                            .height(18.dp)
                            .background(F1Red),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.points.toInt().toString(),
                    style = AppStyles.caption.copy(color = colors.black),
                    modifier = Modifier.width(36.dp),
                )
            }
        }
    }
}
