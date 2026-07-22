package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1GrayBg
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1White
import com.example.f1_kotlin.util.DateUtils

/**
 * Ширины как во Flutter `tournament_drivers_table`:
 * Fraction(0.05), Flex(0.2/0.3/0.15/0.05/0.25).
 */
private val DriversTableWeights = listOf(0.05f, 0.2f, 0.3f, 0.15f, 0.05f, 0.25f)

/** Flutter: Fraction(0.05), Flex(0.3/0.3/0.2/0.15). */
private val ConstructorsTableWeights = listOf(0.05f, 0.3f, 0.3f, 0.2f, 0.15f)

/** Flutter RaceInfoTable: Flex 1.15 / 1.35 / 1.1 / 0.55 / 0.9. */
private val RaceResultsTableWeights = listOf(1.15f, 1.35f, 1.1f, 0.55f, 0.9f)

/** Таблица чемпионата пилотов (Главная / Зал славы) — колонки как во Flutter. */
@Composable
fun TournamentDriversTable(
    drivers: List<DriverStandingsModel>,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            cells = listOf(
                "",
                stringResource(R.string.driver),
                stringResource(R.string.nationality),
                stringResource(R.string.points),
                stringResource(R.string.wins_short),
                stringResource(R.string.constructor),
            ),
            weights = DriversTableWeights,
        )
        drivers.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    TableCell.Text("${index + 1}"),
                    TableCell.Text("${item.driver.givenName}\n${item.driver.familyName}"),
                    TableCell.Flag(item.driver.nationality),
                    TableCell.Text(item.points),
                    TableCell.Text(item.wins),
                    TableCell.Text(item.constructors.firstOrNull()?.name.orEmpty()),
                ),
                index = index,
                weights = DriversTableWeights,
                onClick = onDriverClick?.let { { it(item.driver) } },
            )
        }
    }
}

/** Таблица чемпионата конструкторов — колонки как во Flutter. */
@Composable
fun TournamentConstructorsTable(
    constructors: List<ConstructorStandingsModel>,
    onConstructorClick: ((com.example.f1_kotlin.data.model.ConstructorModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            cells = listOf(
                "",
                stringResource(R.string.constructor),
                stringResource(R.string.country),
                stringResource(R.string.points),
                stringResource(R.string.wins),
            ),
            weights = ConstructorsTableWeights,
        )
        constructors.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    TableCell.Text("${index + 1}"),
                    TableCell.Text(item.constructor.name),
                    TableCell.Flag(item.constructor.nationality),
                    TableCell.Text(item.points),
                    TableCell.Text(item.wins),
                ),
                index = index,
                weights = ConstructorsTableWeights,
                onClick = onConstructorClick?.let { { it(item.constructor) } },
            )
        }
    }
}

/**
 * Таблица результатов гонки — как Flutter `RaceInfoTable`:
 * Driver(pos+name) · Constructor · Time/Status · Points · Best lap.
 */
@Composable
fun RaceResultsTable(
    race: RaceModel,
    maxRows: Int? = null,
    showHeader: Boolean = true,
    onDetailsClick: (() -> Unit)? = null,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    val results = race.results.orEmpty()
    val rows = maxRows?.let { results.take(it) } ?: results
    val fastest = race.fastestLapTime

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showHeader) {
            TableHeaderRow(
                cells = listOf(
                    stringResource(R.string.driver),
                    stringResource(R.string.constructor),
                    stringResource(R.string.time_status),
                    stringResource(R.string.points),
                    stringResource(R.string.best_lap),
                ),
                weights = RaceResultsTableWeights,
            )
        }
        rows.forEachIndexed { index, result ->
            RaceResultRow(result, index, fastest, onDriverClick)
        }
        if (onDetailsClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(F1GrayBg)
                    .clickable(onClick = onDetailsClick)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.detailed_info), style = AppStyles.caption)
                Icon(Icons.Default.Flag, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RaceResultRow(
    result: RaceResultModel,
    index: Int,
    fastestLap: String,
    onDriverClick: ((DriverModel) -> Unit)?,
) {
    val classified = result.time != null || result.status.equals("Finished", ignoreCase = true)
    val timeOrStatus = result.time?.time ?: result.status
    val lapTime = result.fastestLap?.time?.time
    val isFastest = lapTime != null && lapTime == fastestLap && fastestLap != "999999"
    val noneLabel = stringResource(R.string.none_short)
    val fastestLabel = lapTime?.let { stringResource(R.string.fastest_lap_label, it) }
    val bestLapText = when {
        lapTime == null -> noneLabel
        isFastest -> fastestLabel.orEmpty()
        else -> lapTime
    }

    TableDataRow(
        cells = listOf(
            TableCell.PlaceAndName(
                place = result.positionText,
                name = "${result.driver.givenName}\n${result.driver.familyName}",
                placeColor = if (classified) null else F1Red,
            ),
            TableCell.Text(result.constructor.name),
            TableCell.Text(timeOrStatus, color = if (classified) null else F1Red),
            TableCell.Text(result.points),
            TableCell.Text(bestLapText, color = if (isFastest) F1Red else null),
        ),
        index = index,
        weights = RaceResultsTableWeights,
        onClick = onDriverClick?.let { { it(result.driver) } },
    )
}

/**
 * Квалификация — как Flutter: Driver(place+name) · Constructor · Q1 · Q2 · Q3 (равные колонки).
 */
@Composable
fun QualifyingTable(
    results: List<QualifyingResultModel>,
    onDriverClick: ((DriverModel) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            cells = listOf(
                stringResource(R.string.driver),
                stringResource(R.string.constructor),
                "Q1",
                "Q2",
                "Q3",
            ),
        )
        results.forEachIndexed { index, item ->
            val position = item.position.toIntOrNull() ?: (index + 1)
            val q2 = item.Q2 ?: if (position < 16) "-" else ""
            val q3 = item.Q3 ?: if (position < 11) "-" else ""
            TableDataRow(
                cells = listOf(
                    TableCell.PlaceAndName(
                        place = "${index + 1}",
                        name = "${item.driver.givenName}\n${item.driver.familyName}",
                    ),
                    TableCell.Text(item.constructor.name),
                    TableCell.Text(item.Q1.orEmpty()),
                    TableCell.Text(q2),
                    TableCell.Text(q3),
                ),
                index = index,
                onClick = onDriverClick?.let { { it(item.driver) } },
            )
        }
    }
}

/**
 * Пит-стопы — как Flutter: Driver(place+name) · Lap · Stop number · Stop time(duration) · Race time.
 */
@Composable
fun PitStopsTable(stops: List<PitStopModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(
            cells = listOf(
                stringResource(R.string.driver),
                stringResource(R.string.lap),
                stringResource(R.string.stop_number),
                stringResource(R.string.stop_time),
                stringResource(R.string.race_time),
            ),
        )
        stops.forEachIndexed { index, stop ->
            TableDataRow(
                cells = listOf(
                    TableCell.PlaceAndName(place = "${index + 1}", name = stop.driverId),
                    TableCell.Text(stop.lap),
                    TableCell.Text(stop.stop),
                    TableCell.Text(stop.duration),
                    TableCell.Text(stop.time),
                ),
                index = index,
            )
        }
    }
}

/** Красный заголовок секции на экране детальной гонки. */
@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Red)
            .padding(vertical = 10.dp, horizontal = AppDimens.horizontalPadding.dp),
    ) {
        Text(text = title, style = AppStyles.body.copy(color = F1White))
    }
}

/**
 * Карточка одной сессии в календаре (практика, квалификация, гонка).
 * Время конвертируется из UTC в локальное через [DateUtils.toLocalDateTime].
 */
@Composable
fun ScheduleSessionCard(
    title: String,
    date: String,
    time: String?,
) {
    val localDateTime = DateUtils.toLocalDateTime(date, time)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppDimens.horizontalPadding.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, style = AppStyles.h3)
                if (localDateTime != null) {
                    Text(
                        "${localDateTime.dayOfMonth} ${DateUtils.monthName(localDateTime.monthValue)} ${localDateTime.year}",
                        style = AppStyles.body,
                        modifier = Modifier.padding(vertical = 5.dp),
                    )
                    Text(DateUtils.formatHourMinute(localDateTime), style = AppStyles.body)
                }
            }
            Icon(Icons.Default.Flag, contentDescription = null, tint = F1Red)
        }
    }
}
