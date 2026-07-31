package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.CareerRaceResult
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors

@Composable
fun CareerInfoRow(
    label: String,
    value: String = "",
    valueContent: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = AppStyles.caption, modifier = Modifier.width(140.dp))
        if (valueContent != null) {
            valueContent()
        } else {
            Text(value, style = AppStyles.body)
        }
    }
}

@Composable
fun CareerStatsGrid(
    races: Int,
    wins: Int,
    podiums: Int,
    poles: Int,
    onWinsTap: (() -> Unit)? = null,
    onPodiumsTap: (() -> Unit)? = null,
    onPolesTap: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareerStatCell(
                label = stringResource(R.string.career_stat_races),
                value = races,
                onTap = null,
                modifier = Modifier.weight(1f),
            )
            CareerStatCell(
                label = stringResource(R.string.wins),
                value = wins,
                onTap = onWinsTap,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareerStatCell(
                label = stringResource(R.string.career_stat_podiums),
                value = podiums,
                onTap = onPodiumsTap,
                modifier = Modifier.weight(1f),
            )
            CareerStatCell(
                label = stringResource(R.string.career_stat_poles),
                value = poles,
                onTap = onPolesTap,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CareerStatCell(
    label: String,
    value: Int,
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val tappable = onTap != null && value > 0
    Column(
        modifier = modifier
            .border(1.dp, F1Red, RoundedCornerShape(8.dp))
            .then(if (tappable) Modifier.clickable(onClick = onTap!!) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$value", style = AppStyles.h2, modifier = Modifier.weight(1f))
            if (tappable) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = F1Red)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppStyles.body.copy(color = colors.textGray), maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerRaceResultsSheet(
    title: String,
    races: List<CareerRaceResult>,
    showPosition: Boolean,
    onDismiss: () -> Unit,
    onCircuitClick: (Circuit) -> Unit,
) {
    val colors = appColors()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(title, style = AppStyles.h2)
            Spacer(Modifier.height(16.dp))
            if (races.isEmpty()) {
                Text(
                    stringResource(R.string.career_race_list_empty),
                    style = AppStyles.body,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn {
                    items(races) { race ->
                        val subtitle = if (showPosition) {
                            "P${race.position} · ${race.entityName}"
                        } else {
                            race.entityName
                        }
                        CareerListTile(
                            title = "${race.season} · ${race.raceName}",
                            subtitle = subtitle,
                            onClick = {
                                onDismiss()
                                onCircuitClick(race.circuit)
                            },
                        )
                        Divider(color = colors.strokeGray)
                    }
                }
            }
        }
    }
}

@Composable
fun CareerListTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppStyles.body)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = AppStyles.caption)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun WikipediaLink(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.open_in_wikipedia),
        style = AppStyles.body.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

fun displayValue(value: String?): String =
    value?.takeUnless { it.isBlank() || it.equals("none", true) }
        ?: "—"
