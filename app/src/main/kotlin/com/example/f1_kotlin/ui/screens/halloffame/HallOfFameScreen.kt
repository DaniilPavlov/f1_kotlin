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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.TournamentConstructorsTable
import com.example.f1_kotlin.ui.components.TournamentDriversTable
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.HallOfFameViewModel

@Composable
fun HallOfFameScreen(
    viewModel: HallOfFameViewModel,
    onDriverClick: (DriverModel) -> Unit,
    onConstructorClick: (ConstructorModel) -> Unit,
) {
    val drivers by viewModel.drivers.collectAsState()
    val constructors by viewModel.constructors.collectAsState()
    val year by viewModel.year.collectAsState()
    val activeTable by viewModel.activeTable.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        drivers.isLoading || constructors.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        error != null -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        drivers is AsyncValue.Value && constructors is AsyncValue.Value -> {
            val driversList = (drivers as AsyncValue.Value).value
            val constructorsList = (constructors as AsyncValue.Value).value
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
                                value = year,
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
                    activeTable,
                    viewModel::changeActiveTable,
                )
                Spacer(Modifier.height(8.dp))
                if (activeTable == 0) {
                    TournamentDriversTable(driversList, onDriverClick = onDriverClick)
                } else {
                    TournamentConstructorsTable(constructorsList, onConstructorClick = onConstructorClick)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
        else -> LoadingIndicator(Modifier.fillMaxSize())
    }
}
