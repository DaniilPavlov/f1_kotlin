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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.DriverInfoBottomSheet
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.TournamentConstructorsTable
import com.example.f1_kotlin.ui.components.TournamentDriversTable
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.HomeViewModel

/**
 * Экран «Главная» — турнирная таблица текущего сезона.
 *
 * **Как связаны View и ViewModel:**
 * 1. `collectAsState()` подписывает Composable на [StateFlow] — при смене данных UI перерисуется.
 * 2. `when` по [AsyncValue] выбирает: загрузка / ошибка / контент.
 *
 * ViewModel создаётся через `viewModel()` в NavGraph и переживает поворот экрана.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val selectedDriver = remember { mutableStateOf<com.example.f1_kotlin.data.model.DriverModel?>(null) }
    val drivers by viewModel.drivers.collectAsState()
    val constructors by viewModel.constructors.collectAsState()
    val season by viewModel.season.collectAsState()
    val round by viewModel.round.collectAsState()
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
                    Text(stringResource(R.string.home_standings_title), style = AppStyles.h1)
                    Spacer(Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.season_label, season), style = AppStyles.h2, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.round_label, round), style = AppStyles.h2)
                    }
                }
                Spacer(Modifier.height(32.dp))
                CustomSwitcher(stringResource(R.string.drivers), stringResource(R.string.constructors), activeTable, viewModel::changeActiveTable)
                Spacer(Modifier.height(8.dp))
                if (activeTable == 0) {
                    TournamentDriversTable(driversList) { selectedDriver.value = it }
                } else {
                    TournamentConstructorsTable(constructorsList)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    selectedDriver.value?.let { DriverInfoBottomSheet(it) { selectedDriver.value = null } }
}
