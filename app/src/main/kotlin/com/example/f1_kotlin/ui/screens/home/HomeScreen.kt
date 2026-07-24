package com.example.f1_kotlin.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.f1_kotlin.ui.components.TournamentConstructorsTable
import com.example.f1_kotlin.ui.components.TournamentDriversTable
import com.example.f1_kotlin.ui.components.shimmer.TournamentTablesShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.HomeUiState
import com.example.f1_kotlin.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDriverClick: (Driver) -> Unit,
    onConstructorClick: (Constructor) -> Unit,
) {
    // GoF Behavioral Observer — вид подписывается на StateFlow ViewModel
    // и перестраивается при изменении standings / ошибок.
    val uiState by viewModel.uiState.collectAsState()
    HomeScreenContent(
        uiState = uiState,
        onRetry = viewModel::refreshAll,
        onRefresh = viewModel::refreshAll,
        onChangeTable = viewModel::changeActiveTable,
        onDriverClick = onDriverClick,
        onConstructorClick = onConstructorClick,
    )
}

/** Testable content — no Hilt / ViewModel required. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onChangeTable: (Int) -> Unit,
    onDriverClick: (Driver) -> Unit = {},
    onConstructorClick: (Constructor) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val drivers = uiState.drivers
    val constructors = uiState.constructors

    when {
        drivers.isLoading || constructors.isLoading -> TournamentTablesShimmer(modifier = Modifier.fillMaxSize())
        uiState.error != null -> ErrorBody(
            uiState.error?.title,
            uiState.error?.subtitle,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
        drivers is AsyncValue.Value && constructors is AsyncValue.Value -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = AppDimens.verticalPadding.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                        Text(stringResource(R.string.home_standings_title), style = AppStyles.h1)
                        Spacer(Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.season_label, uiState.season),
                                style = AppStyles.h2,
                                modifier = Modifier.weight(1f),
                            )
                            Text(stringResource(R.string.round_label, uiState.round), style = AppStyles.h2)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    CustomSwitcher(
                        stringResource(R.string.drivers),
                        stringResource(R.string.constructors),
                        uiState.activeTable,
                        onChangeTable,
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
    }
}
