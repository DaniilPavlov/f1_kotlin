package com.example.f1kotlin.ui.screens.results

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1kotlin.data.model.RaceModel
import com.example.f1kotlin.domain.AsyncValue
import com.example.f1kotlin.ui.components.ErrorBody
import com.example.f1kotlin.ui.components.LoadingIndicator
import com.example.f1kotlin.ui.components.PitStopsTable
import com.example.f1kotlin.ui.components.QualifyingTable
import com.example.f1kotlin.ui.components.RaceResultsTable
import com.example.f1kotlin.ui.components.SectionHeader
import com.example.f1kotlin.ui.theme.AppDimens
import com.example.f1kotlin.ui.theme.AppStyles
import com.example.f1kotlin.ui.theme.F1Red
import com.example.f1kotlin.viewmodel.RaceInfoScreenViewModel
import com.example.f1kotlin.viewmodel.RaceSearchViewModel
import com.example.f1kotlin.viewmodel.ResultsViewModel

@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    onSearchRace: () -> Unit,
    onRaceDetails: (RaceModel) -> Unit,
) {
    val lastRace by viewModel.lastRace.collectAsState()

    when (val state = lastRace) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = AppDimens.verticalPadding.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                Text("Последняя гонка:", style = AppStyles.h2)
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Text(state.value.raceName, style = AppStyles.h2)
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Сезон: ${state.value.season}", style = AppStyles.h2, modifier = Modifier.weight(1f))
                    Text("Раунд: ${state.value.round}", style = AppStyles.h2)
                }
            }
            Spacer(Modifier.height(10.dp))
            RaceResultsTable(
                race = state.value,
                maxRows = 3,
                onDetailsClick = { onRaceDetails(state.value) },
            )
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            BoxedAction(title = "Выбрать конкретную гонку", onClick = onSearchRace)
        }
    }
}

@Composable
fun RaceSearchScreen(
    viewModel: RaceSearchViewModel,
    onRaceDetails: (RaceModel) -> Unit,
) {
    val year by viewModel.year.collectAsState()
    val round by viewModel.round.collectAsState()
    val fieldsInputted by viewModel.fieldsInputted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchedRace by viewModel.searchedRace.collectAsState()
    val dataLoaded by viewModel.dataLoaded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.horizontalPadding.dp),
    ) {
        Text("Введите год и номер гонки", style = AppStyles.body, modifier = Modifier.padding(vertical = 16.dp))
        OutlinedField("Сезон", "Год", year, viewModel::onYearChanged)
        Spacer(Modifier.height(12.dp))
        OutlinedField("Раунд", "Номер", round, viewModel::onRoundChanged)
        Spacer(Modifier.height(16.dp))
        com.example.f1kotlin.ui.components.BlackButton(
            text = "Поиск",
            enabled = fieldsInputted,
            onClick = viewModel::loadRaceResults,
        )
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, style = AppStyles.body, modifier = Modifier.padding(top = 16.dp))
        }
        if (!dataLoaded) {
            LoadingIndicator(Modifier.padding(top = 24.dp))
        }
        when (val state = searchedRace) {
            is AsyncValue.Value -> state.value?.let { race ->
                Spacer(Modifier.height(24.dp))
                Text(race.raceName, style = AppStyles.h2)
                RaceResultsTable(
                    race = race,
                    maxRows = 3,
                    onDetailsClick = { onRaceDetails(race) },
                )
            }
            else -> Unit
        }
    }
}

@Composable
fun RaceInfoScreen(viewModel: RaceInfoScreenViewModel) {
    val race by viewModel.race.collectAsState()
    val qualifying by viewModel.qualifying.collectAsState()
    val pitStops by viewModel.pitStops.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        error != null && race.isError -> ErrorBody(
            error?.title,
            error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        race.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        race is AsyncValue.Value -> {
            val raceData = (race as AsyncValue.Value).value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.horizontalPadding.dp),
            ) {
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                Text(raceData.raceName, style = AppStyles.h2)
                RowInfo("Сезон: ${raceData.season}", "Раунд: ${raceData.round}")
                SectionHeader("Результаты гонки")
                RaceResultsTable(race = raceData, showHeader = false)
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                SectionHeader("Квалификация")
                when (val q = qualifying) {
                    is AsyncValue.Loading -> LoadingIndicator(Modifier.padding(vertical = 16.dp))
                    is AsyncValue.Error -> Text(q.message, style = AppStyles.body)
                    is AsyncValue.Value -> QualifyingTable(q.value)
                }
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                SectionHeader("Пит-стопы")
                when (val p = pitStops) {
                    is AsyncValue.Loading -> LoadingIndicator(Modifier.padding(vertical = 16.dp))
                    is AsyncValue.Error -> Text(p.message, style = AppStyles.body)
                    is AsyncValue.Value -> PitStopsTable(p.value)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
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

@Composable
private fun OutlinedField(label: String, hint: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, style = AppStyles.caption)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}
