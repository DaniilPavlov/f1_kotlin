package com.example.f1_kotlin.ui.screens.halloffame

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.DriverInfoBottomSheet
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.TournamentConstructorsTable
import com.example.f1_kotlin.ui.components.TournamentDriversTable
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.HallOfFameViewModel

/**
 * Экран «Зал славы» — итоговые таблицы за выбранный год.
 *
 * Похож на [HomeScreen], но данные загружаются по кнопке «Поиск»,
 * а не автоматически при каждом изменении поля (кроме первого запуска с 2026).
 */
@Composable
fun HallOfFameScreen(viewModel: HallOfFameViewModel) {
    val selectedDriver = remember { mutableStateOf<com.example.f1_kotlin.data.model.DriverModel?>(null) }
    val drivers by viewModel.drivers.collectAsState()
    val constructors by viewModel.constructors.collectAsState()
    val year by viewModel.year.collectAsState()
    val fieldsInputted by viewModel.fieldsInputted.collectAsState()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.season), style = AppStyles.caption)
                            OutlinedTextField(
                                value = year,
                                onValueChange = viewModel::onYearChanged,
                                placeholder = { Text(stringResource(R.string.year_hint)) },
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                            )
                        }
                        Spacer(Modifier.height(0.dp).weight(0.05f))
                        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                            Spacer(Modifier.height(18.dp))
                            BlackButton(text = stringResource(R.string.search), enabled = fieldsInputted, onClick = viewModel::loadAllData)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
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
