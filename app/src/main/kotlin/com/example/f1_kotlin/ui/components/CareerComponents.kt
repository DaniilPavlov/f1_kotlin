package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red

@Composable
fun CareerInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = AppStyles.caption, modifier = Modifier.width(140.dp))
        Text(value, style = AppStyles.body)
    }
}

@Composable
fun CareerStatsGrid(races: Int, wins: Int, podiums: Int, poles: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareerStatCell(stringResource(R.string.career_stat_races), races, Modifier.weight(1f))
            CareerStatCell(stringResource(R.string.wins), wins, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareerStatCell(stringResource(R.string.career_stat_podiums), podiums, Modifier.weight(1f))
            CareerStatCell(stringResource(R.string.career_stat_poles), poles, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CareerStatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = AppStyles.caption)
        Spacer(Modifier.height(4.dp))
        Text("$value", style = AppStyles.h2.copy(color = F1Red))
    }
}

@Composable
fun CareerListTile(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = AppStyles.body)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = AppStyles.caption)
        }
    }
}

@Composable
fun WikipediaLink(onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.open_in_wikipedia),
        style = AppStyles.body.copy(textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

fun displayValue(value: String?): String =
    value?.takeUnless { it.isBlank() || it.equals("none", true) }
        ?: "—"
