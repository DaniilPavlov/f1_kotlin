package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.H2hStats
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1GrayBg
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1TextGray
import com.example.f1_kotlin.ui.theme.F1White

@Composable
fun H2hFilterToggle(
    label: String,
    firstTitle: String,
    secondTitle: String,
    activeIndex: Int,
    onChanged: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = AppStyles.caption.copy(color = F1TextGray))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, F1StrokeGray, RoundedCornerShape(10.dp))
                .background(F1GrayBg),
        ) {
            H2hSegment(
                title = firstTitle,
                selected = activeIndex == 0,
                onClick = { onChanged(0) },
                modifier = Modifier.weight(1f),
            )
            H2hSegment(
                title = secondTitle,
                selected = activeIndex == 1,
                onClick = { onChanged(1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun H2hSegment(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) F1Red else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = AppStyles.caption.copy(
                color = if (selected) F1White else Color.Black,
            ),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun H2hCompareTable(
    nameA: String,
    nameB: String,
    statsA: H2hStats,
    statsB: H2hStats,
    season: String?,
) {
    val rows = listOf(
        Triple(stringResource(R.string.career_stat_races), statsA.races, statsB.races),
        Triple(stringResource(R.string.wins), statsA.wins, statsB.wins),
        Triple(stringResource(R.string.career_stat_podiums), statsA.podiums, statsB.podiums),
        Triple(stringResource(R.string.career_stat_poles), statsA.poles, statsB.poles),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!season.isNullOrEmpty()) {
            Text(
                stringResource(R.string.season_label, season),
                style = AppStyles.caption.copy(color = F1TextGray),
            )
            Spacer(Modifier.height(12.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(100.dp))
            Text(
                nameA,
                style = AppStyles.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                nameB,
                style = AppStyles.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        rows.forEach { (label, a, b) ->
            H2hCompareRow(label = label, valueA = a, valueB = b)
            Divider(color = F1StrokeGray)
        }
    }
}

@Composable
private fun H2hCompareRow(label: String, valueA: Int, valueB: Int) {
    val aWins = valueA > valueB
    val bWins = valueB > valueA
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = AppStyles.caption.copy(color = F1TextGray),
            modifier = Modifier.width(100.dp),
        )
        Text(
            "$valueA",
            style = AppStyles.h2.copy(color = if (aWins) F1Red else Color.Black),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$valueB",
            style = AppStyles.h2.copy(color = if (bWins) F1Red else Color.Black),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPickerField(
    label: String,
    driver: DriverModel?,
    enableSearch: Boolean,
    onChanged: (DriverModel) -> Unit,
    loadDrivers: suspend () -> Result<List<DriverModel>>,
) {
    var showSheet by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
    ) {
        OutlinedTextField(
            value = driver?.fullName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.select_driver)) },
            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null, tint = F1Red) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            EntityPickerSheet(
                enableSearch = enableSearch,
                searchHint = stringResource(R.string.h2h_search_driver),
                emptyText = stringResource(R.string.h2h_drivers_empty),
                loadErrorText = stringResource(R.string.drivers_load_error),
                loadItems = loadDrivers,
                itemTitle = { it.fullName },
                itemMatches = { item, query ->
                    val q = query.lowercase()
                    item.fullName.lowercase().contains(q) ||
                        item.code.orEmpty().lowercase().contains(q) ||
                        item.familyName.lowercase().contains(q)
                },
                onSelected = {
                    onChanged(it)
                    showSheet = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructorPickerField(
    label: String,
    constructor: ConstructorModel?,
    enableSearch: Boolean,
    onChanged: (ConstructorModel) -> Unit,
    loadConstructors: suspend () -> Result<List<ConstructorModel>>,
) {
    var showSheet by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
    ) {
        OutlinedTextField(
            value = constructor?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.select_constructor)) },
            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null, tint = F1Red) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            EntityPickerSheet(
                enableSearch = enableSearch,
                searchHint = stringResource(R.string.h2h_search_constructor),
                emptyText = stringResource(R.string.h2h_constructors_empty),
                loadErrorText = stringResource(R.string.constructors_load_error),
                loadItems = loadConstructors,
                itemTitle = { it.name },
                itemMatches = { item, query -> item.name.lowercase().contains(query.lowercase()) },
                onSelected = {
                    onChanged(it)
                    showSheet = false
                },
            )
        }
    }
}

@Composable
private fun <T> EntityPickerSheet(
    enableSearch: Boolean,
    searchHint: String,
    emptyText: String,
    loadErrorText: String,
    loadItems: suspend () -> Result<List<T>>,
    itemTitle: (T) -> String,
    itemMatches: (T, String) -> Boolean,
    onSelected: (T) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<T>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        error = false
        loadItems().onSuccess { items = it }.onFailure { error = true }
        loading = false
    }

    val filtered = remember(items, query) {
        if (!enableSearch || query.isBlank()) items else items.filter { itemMatches(it, query) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(horizontal = 16.dp),
    ) {
        if (enableSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(searchHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
        }
        when {
            loading -> LoadingIndicator(Modifier.fillMaxWidth().heightIn(min = 200.dp))
            error -> Text(loadErrorText, style = AppStyles.body, modifier = Modifier.padding(24.dp))
            filtered.isEmpty() -> Text(emptyText, style = AppStyles.body, modifier = Modifier.padding(24.dp))
            else -> LazyColumn {
                items(filtered) { item ->
                    Text(
                        text = itemTitle(item),
                        style = AppStyles.body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(item) }
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                    )
                    Divider(color = F1StrokeGray)
                }
            }
        }
    }
}
