package com.example.f1_kotlin.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.domain.model.RaceResult
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1GrayBg
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1TextGray
import com.example.f1_kotlin.ui.theme.F1White
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ShareCareerCard(
    title: String,
    races: Int,
    wins: Int,
    podiums: Int,
    poles: Int,
) {
    val items = listOf(
        stringResource(R.string.career_stat_races) to races,
        stringResource(R.string.wins) to wins,
        stringResource(R.string.career_stat_podiums) to podiums,
        stringResource(R.string.career_stat_poles) to poles,
    )
    ShareCardShell {
        Text(title, style = AppStyles.h2.copy(fontSize = 26.sp, lineHeight = 30.sp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.career_title), style = AppStyles.body.copy(color = F1TextGray))
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (label, value) ->
                        ShareStatCell(
                            label = label,
                            value = "$value",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        ShareFooter()
    }
}

@Composable
fun ShareRaceResultsCard(race: Race, topN: Int = 10) {
    val results = race.results.orEmpty()
    val rows = results.take(topN)
    ShareCardShell {
        Text(race.raceName, style = AppStyles.h2.copy(fontSize = 24.sp, lineHeight = 28.sp))
        Spacer(Modifier.height(6.dp))
        Text(
            "${race.season} · ${stringResource(R.string.round_label, race.round)}",
            style = AppStyles.body.copy(color = F1TextGray),
        )
        Spacer(Modifier.height(16.dp))
        if (rows.isEmpty()) {
            Text(stringResource(R.string.share_no_results), style = AppStyles.body)
        } else {
            rows.forEachIndexed { index, result ->
                if (index > 0) HorizontalDivider(color = F1StrokeGray)
                ShareResultRow(result)
            }
        }
        if (results.size > topN) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.share_and_more, results.size - topN),
                style = AppStyles.caption.copy(color = F1TextGray),
            )
        }
        Spacer(Modifier.height(20.dp))
        ShareFooter()
    }
}

@Composable
private fun ShareCardShell(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .width(360.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(F1White)
            .border(2.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(24.dp),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(F1Red),
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ShareStatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(F1GrayBg)
            .border(1.dp, F1StrokeGray, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(value, style = AppStyles.h3.copy(fontSize = 22.sp, color = F1Red))
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppStyles.caption.copy(color = F1TextGray))
    }
}

@Composable
private fun ShareResultRow(result: RaceResult) {
    val classified = result.time != null || result.status.equals("Finished", ignoreCase = true)
    val timeOrStatus = result.time?.time ?: result.status
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            result.positionText,
            style = AppStyles.body.copy(
                color = if (classified) F1Black else F1Red,
            ),
            modifier = Modifier.width(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(result.driver.fullName, style = AppStyles.body)
            Text(result.constructor.name, style = AppStyles.caption.copy(color = F1TextGray))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            timeOrStatus,
            style = AppStyles.caption.copy(color = if (classified) F1Black else F1Red),
        )
    }
}

@Composable
fun ShareWeekendSummaryCard(event: EspnScoreboardEvent) {
    val title = event.shortName.ifBlank { event.name }
    val highlighted = event.highlightedSession
    val podiumSession = event.sessions.firstOrNull {
        it.abbreviation.contains("race", ignoreCase = true) && it.results.isNotEmpty()
    } ?: highlighted?.takeIf { it.results.isNotEmpty() }
    val podium = podiumSession?.results?.take(3).orEmpty()
    val isLive = event.isLive || (highlighted?.isLive == true)
    val statusLabel = when {
        isLive -> stringResource(R.string.home_weekend_live)
        !highlighted?.statusDetail.isNullOrEmpty() -> highlighted!!.statusDetail
        else -> event.statusDetail
    }

    ShareCardShell {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                title,
                style = AppStyles.h2.copy(fontSize = 24.sp, lineHeight = 28.sp),
                modifier = Modifier.weight(1f),
            )
            if (statusLabel.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLive) F1Red else F1Black)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(statusLabel, style = AppStyles.caption.copy(color = F1White))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.home_weekend_title), style = AppStyles.body.copy(color = F1TextGray))
        event.circuitName?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = AppStyles.body.copy(color = F1TextGray))
        }
        if (event.sessions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            event.sessions.forEachIndexed { index, session ->
                if (index > 0) HorizontalDivider(color = F1StrokeGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(session.abbreviation, style = AppStyles.body)
                    Text(
                        session.statusDetail.ifBlank { if (session.isLive) "LIVE" else "" },
                        style = AppStyles.caption.copy(color = if (session.isLive) F1Red else F1TextGray),
                    )
                }
            }
        }
        if (podium.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.share_weekend_podium),
                style = AppStyles.body.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(8.dp))
            podium.forEachIndexed { index, entry ->
                Text("${index + 1}. ${entry.displayName}", style = AppStyles.caption)
            }
        }
        Spacer(Modifier.height(20.dp))
        ShareFooter()
    }
}

@Composable
private fun ShareFooter() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.size(width = 40.dp, height = 18.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.app_name), style = AppStyles.caption.copy(color = F1TextGray))
    }
}
