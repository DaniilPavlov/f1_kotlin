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
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.data.model.ConstructorStandingsModel
import com.example.f1_kotlin.data.model.DriverStandingsModel
import com.example.f1_kotlin.data.model.PitStopModel
import com.example.f1_kotlin.data.model.QualifyingResultModel
import com.example.f1_kotlin.data.model.RaceModel
import com.example.f1_kotlin.data.model.RaceResultModel
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1White
import com.example.f1_kotlin.util.DateUtils

/** Таблица чемпионата пилотов (вкладки Главная и Зал славы). */
@Composable
fun TournamentDriversTable(drivers: List<DriverStandingsModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(listOf("#", "Пилот", "Страна", "Очки", "Победы", "Команда"))
        drivers.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.driver.fullName,
                    item.driver.nationality,
                    item.points,
                    item.wins,
                    item.constructors.firstOrNull()?.name.orEmpty(),
                ),
                index = index,
            )
        }
    }
}

/** Таблица чемпионата конструкторов. */
@Composable
fun TournamentConstructorsTable(constructors: List<ConstructorStandingsModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(listOf("#", "Команда", "Страна", "Очки", "Победы"))
        constructors.forEachIndexed { index, item ->
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.constructor.name,
                    item.constructor.nationality,
                    item.points,
                    item.wins,
                ),
                index = index,
            )
        }
    }
}

/**
 * Таблица результатов гонки.
 *
 * @param maxRows ограничить число строк (на главной результатов — топ-3)
 * @param showHeader показывать ли красную шапку с колонками
 * @param onDetailsClick если задан — рисуем строку «Подробная информация» внизу таблицы
 */
@Composable
fun RaceResultsTable(
    race: RaceModel,
    maxRows: Int? = null,
    showHeader: Boolean = true,
    onDetailsClick: (() -> Unit)? = null,
) {
    val results = race.results.orEmpty()
    val rows = maxRows?.let { results.take(it) } ?: results
    val fastest = race.fastestLapTime

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showHeader) {
            TableHeaderRow(listOf("#", "Пилот", "Команда", "Очки", "Круги", "Время"))
        }
        rows.forEachIndexed { index, result ->
            val isFastest = result.fastestLap?.time?.time == fastest && fastest != "999999"
            RaceResultRow(result, index + 1, isFastest)
        }
        if (onDetailsClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.f1_kotlin.ui.theme.F1GrayBg)
                    .clickable(onClick = onDetailsClick)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Подробная информация", style = AppStyles.caption)
                Icon(Icons.Default.Flag, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RaceResultRow(result: RaceResultModel, position: Int, isFastest: Boolean) {
    val timeText = buildString {
        append(result.time?.time ?: result.status)
        if (isFastest) append(" самый быстрый")
    }
    TableDataRow(
        cells = listOf(
            "$position",
            result.driver.fullName,
            result.constructor.name,
            result.points,
            result.laps,
            timeText,
        ),
        index = position,
        highlight = isFastest,
    )
}

/**
 * Таблица квалификации Q1/Q2/Q3.
 * Прочерк «-» ставится, если пилот не прошёл в следующий сегмент (позиция 16+ / 11+).
 */
@Composable
fun QualifyingTable(results: List<QualifyingResultModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(listOf("#", "Пилот", "Команда", "Q1", "Q2", "Q3"))
        results.forEachIndexed { index, item ->
            val position = item.position.toIntOrNull() ?: (index + 1)
            TableDataRow(
                cells = listOf(
                    "${index + 1}",
                    item.driver.fullName,
                    item.constructor.name,
                    item.Q1 ?: if (position >= 16) "-" else "",
                    item.Q2 ?: if (position >= 11) "-" else "",
                    item.Q3 ?: "-",
                ),
                index = index,
            )
        }
    }
}

/** Таблица пит-стопов. В [PitStopModel.driverId] уже подставлено ФИО из Repository. */
@Composable
fun PitStopsTable(stops: List<PitStopModel>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableHeaderRow(listOf("Пилот", "Круг", "Стоп", "Время", "Длительность"))
        stops.forEachIndexed { index, stop ->
            TableDataRow(
                cells = listOf(stop.driverId, stop.lap, stop.stop, stop.time, stop.duration),
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
