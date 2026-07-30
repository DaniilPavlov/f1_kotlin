package com.example.f1_kotlin.ui.screens.halloffame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.TournamentConstructorsTable
import com.example.f1_kotlin.ui.components.TournamentDriversTable
import com.example.f1_kotlin.ui.components.shimmer.TournamentTablesShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.HallOfFameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallOfFameScreen(
    viewModel: HallOfFameViewModel,
    onDriverClick: (Driver) -> Unit,
    onConstructorClick: (Constructor) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val drivers = uiState.drivers
    val constructors = uiState.constructors

    when {
        !uiState.isRefreshing && (drivers.isLoading || constructors.isLoading) -> TournamentTablesShimmer(
            showHeader = false,
            modifier = Modifier.fillMaxSize(),
        )
        uiState.error != null && drivers !is AsyncValue.Value -> ErrorBody(
            uiState.error?.title,
            uiState.error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        drivers is AsyncValue.Value && constructors is AsyncValue.Value -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                        Text(stringResource(R.string.hall_of_fame_title), style = AppStyles.h1)
                        Spacer(Modifier.height(16.dp))
                        Row {
                            Column(modifier = Modifier.width(240.dp)) {
                                SeasonPickerField(
                                    value = uiState.year,
                                    label = stringResource(R.string.season),
                                    hint = stringResource(R.string.select_season),
                                    onSeasonSelected = viewModel::onYearChanged,
                                    loadSeasons = viewModel::loadSeasonYears,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    CustomSwitcher(
                        stringResource(R.string.drivers),
                        stringResource(R.string.constructors),
                        uiState.activeTable,
                        viewModel::changeActiveTable,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (uiState.activeTable == 0) {
                        TournamentDriversTable(drivers.value, onDriverClick = onDriverClick)
                    } else {
                        TournamentConstructorsTable(constructors.value, onConstructorClick = onConstructorClick)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
        else -> TournamentTablesShimmer(showHeader = false, modifier = Modifier.fillMaxSize())
    }
}
