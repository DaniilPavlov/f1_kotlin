package com.example.f1_kotlin.ui.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.circuits.CircuitLayoutAssets
import com.example.f1_kotlin.domain.model.RaceSession
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.components.ScheduleSessionCard
import com.example.f1_kotlin.ui.components.circuits.CircuitLayoutImage
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.util.CountdownParts
import com.example.f1_kotlin.util.RaceDateTimeHelper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/** Крупная карточка ближайшей гонки со схемой и countdown. */
@Composable
fun ScheduleRaceFeaturedCard(
    race: Race,
    onViewSessions: () -> Unit,
    modifier: Modifier = Modifier,
    showCountdown: Boolean = true,
) {
    val colors = appColors()
    val language by LocaleController.language.collectAsState()
    val locale = remember(language) { LocaleController.currentLocale() }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = ZonedDateTime.now()
        }
    }
    val countdown = remember(now, race) {
        CountdownParts.until(RaceDateTimeHelper.countdownTarget(race), now)
    }

    val start = RaceDateTimeHelper.weekendStart(race)
    val end = RaceDateTimeHelper.raceLocal(race)
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("d", locale) }
    val longFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    }
    val dateRange = if (start.toLocalDate() == end.toLocalDate()) {
        end.format(longFormatter)
    } else {
        "${start.format(dayFormatter)} – ${end.format(longFormatter)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.schedule_round, race.round),
            style = AppStyles.caption.copy(color = colors.textGray),
        )
        Spacer(Modifier.height(4.dp))
        Text(race.raceName, style = AppStyles.h2)
        Spacer(Modifier.height(4.dp))
        Text(race.circuit.circuitName, style = AppStyles.body.copy(color = F1Red))
        Spacer(Modifier.height(4.dp))
        Text(dateRange, style = AppStyles.body)
        if (CircuitLayoutAssets.hasLayout(race.circuit.circuitId)) {
            Spacer(Modifier.height(12.dp))
            CircuitLayoutImage(
                circuitId = race.circuit.circuitId,
                height = 140.dp,
                padding = 0.dp,
            )
        }
        if (showCountdown) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.schedule_countdown_title),
                style = AppStyles.caption.copy(color = colors.textGray),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CountdownCell(
                    value = countdown.days.toString(),
                    label = stringResource(R.string.schedule_days),
                    modifier = Modifier.weight(1f),
                )
                CountdownCell(
                    value = countdown.hours.toString(),
                    label = stringResource(R.string.schedule_hours),
                    modifier = Modifier.weight(1f),
                )
                CountdownCell(
                    value = countdown.minutes.toString(),
                    label = stringResource(R.string.schedule_minutes),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BlackButton(
            text = stringResource(R.string.schedule_view_sessions),
            onClick = onViewSessions,
        )
    }
}

@Composable
private fun CountdownCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppStyles.h3)
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppStyles.caption.copy(color = appColors().textGray))
    }
}

/** Нижний лист со всеми сессиями выбранного ГП. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRaceSessionsSheet(
    race: Race,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessions = remember(race) { raceWeekendSessions(race) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(race.raceName, style = AppStyles.h2)
            Spacer(Modifier.height(4.dp))
            Text(race.circuit.circuitName, style = AppStyles.body)
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            sessions.forEach { (titleRes, date) ->
                ScheduleSessionCard(stringResource(titleRes), date.date, date.time)
            }
        }
    }
}

/** Все сессии уикенда для bottom sheet / «View sessions». */
fun raceWeekendSessions(race: Race): List<Pair<Int, RaceSession>> = buildList {
    race.firstPractice?.let { add(R.string.first_practice to it) }
    race.secondPractice?.let { add(R.string.second_practice to it) }
    race.thirdPractice?.let { add(R.string.third_practice to it) }
    race.sprintQualifying?.let { add(R.string.sprint_qualifying to it) }
    race.sprint?.let { add(R.string.sprint to it) }
    race.qualifying?.let { add(R.string.qualifying to it) }
    add(R.string.race to RaceSession(date = race.date, time = race.time))
}
