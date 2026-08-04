package com.example.f1_kotlin.ui.screens.h2h

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.components.ConstructorPickerField
import com.example.f1_kotlin.ui.components.DriverPickerField
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.H2hCompareTable
import com.example.f1_kotlin.ui.components.H2hFilterToggle
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.shimmer.H2hCompareShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.viewmodel.H2hConstructorsViewModel
import com.example.f1_kotlin.viewmodel.H2hDriversViewModel
import com.example.f1_kotlin.viewmodel.H2hScopeState

enum class H2hMode { Drivers, Constructors }

@Composable
fun H2hScreen(
    driversViewModel: H2hDriversViewModel,
    constructorsViewModel: H2hConstructorsViewModel,
    initialMode: H2hMode = H2hMode.Drivers,
) {
    var mode by rememberSaveable { mutableStateOf(initialMode.name) }
    val isDrivers = mode == H2hMode.Drivers.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        H2hFilterToggle(
            label = "",
            firstTitle = stringResource(R.string.h2h_mode_drivers),
            secondTitle = stringResource(R.string.h2h_mode_constructors),
            activeIndex = if (isDrivers) 0 else 1,
            onChanged = { mode = if (it == 0) H2hMode.Drivers.name else H2hMode.Constructors.name },
        )
        Spacer(Modifier.height(16.dp))
        if (isDrivers) {
            H2hDriversContent(viewModel = driversViewModel)
        } else {
            H2hConstructorsContent(viewModel = constructorsViewModel)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun H2hDriversContent(viewModel: H2hDriversViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Text(stringResource(R.string.h2h_subtitle), style = AppStyles.body)
    Spacer(Modifier.height(16.dp))
    H2hFiltersPanel(
        scope = uiState.scope,
        entityFilterLabel = stringResource(R.string.h2h_drivers_filter),
        entityCurrentTitle = stringResource(R.string.h2h_current_drivers),
        entityAllTitle = stringResource(R.string.h2h_all_drivers),
        onScopeModeChanged = viewModel::setScopeMode,
        onUseCurrentSeasonChanged = viewModel::setUseCurrentSeason,
        onSeasonPicked = viewModel::onSeasonPicked,
        loadSeasons = viewModel::loadSeasonYears,
        onCurrentOnlyChanged = viewModel::setCurrentOnly,
    )
    Spacer(Modifier.height(20.dp))
    DriverPickerField(
        label = stringResource(R.string.h2h_driver_a),
        driver = uiState.driverA,
        enableSearch = !uiState.currentOnly,
        onChanged = viewModel::setDriverA,
        loadDrivers = viewModel::loadDriversForPicker,
    )
    Spacer(Modifier.height(12.dp))
    DriverPickerField(
        label = stringResource(R.string.h2h_driver_b),
        driver = uiState.driverB,
        enableSearch = !uiState.currentOnly,
        onChanged = viewModel::setDriverB,
        loadDrivers = viewModel::loadDriversForPicker,
    )
    Spacer(Modifier.height(20.dp))
    BlackButton(
        text = stringResource(R.string.h2h_compare),
        enabled = uiState.canCompare && uiState.comparison !is AsyncValue.Loading,
        onClick = viewModel::compare,
    )
    Spacer(Modifier.height(24.dp))
    when (val state = uiState.comparison) {
        is AsyncValue.Loading -> H2hCompareShimmer()
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::compare,
        )
        is AsyncValue.Value -> state.value?.let { result ->
            H2hCompareTable(
                nameA = result.driverA.fullName,
                nameB = result.driverB.fullName,
                statsA = result.statsA,
                statsB = result.statsB,
                season = result.season,
                timeline = result.timeline,
                constructorIdA = result.constructorIdA,
                constructorIdB = result.constructorIdB,
            )
        }
    }
}

@Composable
private fun H2hConstructorsContent(viewModel: H2hConstructorsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Text(stringResource(R.string.h2h_constructors_subtitle), style = AppStyles.body)
    Spacer(Modifier.height(16.dp))
    H2hFiltersPanel(
        scope = uiState.scope,
        entityFilterLabel = stringResource(R.string.h2h_constructors_filter),
        entityCurrentTitle = stringResource(R.string.h2h_current_constructors),
        entityAllTitle = stringResource(R.string.h2h_all_constructors),
        onScopeModeChanged = viewModel::setScopeMode,
        onUseCurrentSeasonChanged = viewModel::setUseCurrentSeason,
        onSeasonPicked = viewModel::onSeasonPicked,
        loadSeasons = viewModel::loadSeasonYears,
        onCurrentOnlyChanged = viewModel::setCurrentOnly,
    )
    Spacer(Modifier.height(20.dp))
    ConstructorPickerField(
        label = stringResource(R.string.h2h_constructor_a),
        constructor = uiState.constructorA,
        enableSearch = !uiState.currentOnly,
        onChanged = viewModel::setConstructorA,
        loadConstructors = viewModel::loadConstructorsForPicker,
    )
    Spacer(Modifier.height(12.dp))
    ConstructorPickerField(
        label = stringResource(R.string.h2h_constructor_b),
        constructor = uiState.constructorB,
        enableSearch = !uiState.currentOnly,
        onChanged = viewModel::setConstructorB,
        loadConstructors = viewModel::loadConstructorsForPicker,
    )
    Spacer(Modifier.height(20.dp))
    BlackButton(
        text = stringResource(R.string.h2h_compare),
        enabled = uiState.canCompare && uiState.comparison !is AsyncValue.Loading,
        onClick = viewModel::compare,
    )
    Spacer(Modifier.height(24.dp))
    when (val state = uiState.comparison) {
        is AsyncValue.Loading -> H2hCompareShimmer()
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::compare,
        )
        is AsyncValue.Value -> state.value?.let { result ->
            H2hCompareTable(
                nameA = result.constructorA.name,
                nameB = result.constructorB.name,
                statsA = result.statsA,
                statsB = result.statsB,
                season = result.season,
                timeline = result.timeline,
                constructorIdA = result.constructorIdA,
                constructorIdB = result.constructorIdB,
            )
        }
    }
}

@Composable
private fun H2hFiltersPanel(
    scope: H2hScopeState,
    entityFilterLabel: String,
    entityCurrentTitle: String,
    entityAllTitle: String,
    onScopeModeChanged: (Int) -> Unit,
    onUseCurrentSeasonChanged: (Boolean) -> Unit,
    onSeasonPicked: (String) -> Unit,
    loadSeasons: suspend () -> Result<List<String>>,
    onCurrentOnlyChanged: (Boolean) -> Unit,
) {
    val colors = appColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.strokeGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.h2h_filters_title), style = AppStyles.body)
        Spacer(Modifier.height(16.dp))
        H2hFilterToggle(
            label = stringResource(R.string.h2h_period_filter),
            firstTitle = stringResource(R.string.career_title),
            secondTitle = stringResource(R.string.season),
            activeIndex = scope.scopeMode,
            onChanged = onScopeModeChanged,
        )
        if (scope.isSeasonScope) {
            Spacer(Modifier.height(14.dp))
            H2hFilterToggle(
                label = stringResource(R.string.h2h_season_filter),
                firstTitle = stringResource(R.string.h2h_current_season),
                secondTitle = stringResource(R.string.h2h_pick_year),
                activeIndex = if (scope.useCurrentSeason) 0 else 1,
                onChanged = { onUseCurrentSeasonChanged(it == 0) },
            )
            if (scope.showYearPicker) {
                Spacer(Modifier.height(12.dp))
                SeasonPickerField(
                    value = scope.pickedSeason,
                    label = stringResource(R.string.season),
                    hint = stringResource(R.string.select_season),
                    onSeasonSelected = onSeasonPicked,
                    loadSeasons = loadSeasons,
                )
            } else if (scope.latestSeason.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.season_label, scope.latestSeason),
                    style = AppStyles.caption.copy(color = colors.textGray),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        H2hFilterToggle(
            label = entityFilterLabel,
            firstTitle = entityCurrentTitle,
            secondTitle = entityAllTitle,
            activeIndex = if (scope.currentOnly) 0 else 1,
            onChanged = { onCurrentOnlyChanged(it == 0) },
        )
    }
}
