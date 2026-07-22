package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonPickerField(
    value: String,
    label: String,
    hint: String,
    onSeasonSelected: (String) -> Unit,
    loadSeasons: suspend () -> Result<List<String>>,
) {
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(hint) },
            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null, tint = F1Red) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            SeasonPickerSheet(
                selected = value,
                loadSeasons = loadSeasons,
                onSelected = { year ->
                    onSeasonSelected(year)
                    showSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonPickerSheet(
    selected: String,
    loadSeasons: suspend () -> Result<List<String>>,
    onSelected: (String) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var years by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        error = false
        loadSeasons().onSuccess { years = it }.onFailure { error = true }
        loading = false
    }

    when {
        loading -> LoadingIndicator(Modifier.fillMaxWidth().heightIn(min = 200.dp))
        error -> Text(
            stringResource(R.string.seasons_load_error),
            style = AppStyles.body,
            modifier = Modifier.padding(24.dp),
        )
        else -> LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            items(years) { year ->
                val isSelected = year == selected
                Text(
                    text = year,
                    style = if (isSelected) AppStyles.body.copy(color = F1Red) else AppStyles.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(year) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
                Divider()
            }
        }
    }
}

data class RacePick(val round: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RacePickerField(
    displayValue: String,
    seasonYear: String,
    label: String,
    hint: String,
    disabledHint: String,
    onRacePicked: (RacePick) -> Unit,
    loadRaces: suspend (String) -> Result<List<Race>>,
) {
    val enabled = seasonYear.length == 4
    var showSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showSheet = true },
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(if (enabled) hint else disabledHint) },
            trailingIcon = {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (enabled) F1Red else F1StrokeGray,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            RacePickerSheet(
                seasonYear = seasonYear,
                loadRaces = loadRaces,
                onSelected = { pick ->
                    onRacePicked(pick)
                    showSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RacePickerSheet(
    seasonYear: String,
    loadRaces: suspend (String) -> Result<List<Race>>,
    onSelected: (RacePick) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var races by remember { mutableStateOf<List<Race>>(emptyList()) }

    LaunchedEffect(seasonYear) {
        loading = true
        error = false
        loadRaces(seasonYear).onSuccess { races = it }.onFailure { error = true }
        loading = false
    }

    when {
        loading -> LoadingIndicator(Modifier.fillMaxWidth().heightIn(min = 200.dp))
        error -> Text(
            stringResource(R.string.races_load_error),
            style = AppStyles.body,
            modifier = Modifier.padding(24.dp),
        )
        else -> LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
            items(races) { race ->
                val title = "${race.round}. ${race.raceName}"
                Text(
                    text = title,
                    style = AppStyles.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(RacePick(race.round, title)) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
                Divider()
            }
        }
    }
}
