package com.example.f1_kotlin.ui.screens.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.F1Calendar
import com.example.f1_kotlin.ui.components.ScheduleSessionCard
import com.example.f1_kotlin.ui.components.shimmer.ScheduleShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.ScheduleViewModel

/**
 * Экран «Календарь».
 *
 * Сверху — [F1Calendar], снизу — карточки сессий на выбранный день
 * или [ScheduleRaceFeaturedCard] с countdown, если день пустой и есть upcoming race.
 */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var sessionsRace by remember { mutableStateOf<Race?>(null) }

    when {
        uiState.error != null && uiState.races.isError -> ErrorBody(
            uiState.error?.title,
            uiState.error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        uiState.races.isLoading -> ScheduleShimmer(modifier = Modifier.fillMaxSize())
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
        ) {
            F1Calendar(
                selectedDate = uiState.selectedDate,
                focusedMonth = uiState.focusedMonth,
                userPickedDay = uiState.userPickedDay,
                logoForDay = viewModel::logoForDay,
                onDaySelected = viewModel::onSelectDay,
                onMonthChanged = viewModel::onMonthChanged,
            )
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            if (uiState.scheduleItems.isNotEmpty()) {
                uiState.scheduleItems.forEach { item ->
                    if (item.titleRes == null) {
                        Text(item.raceName, style = AppStyles.h3, modifier = Modifier.padding(bottom = 12.dp))
                    } else {
                        ScheduleSessionCard(stringResource(item.titleRes), item.date.date, item.date.time)
                    }
                }
            } else {
                uiState.upcomingRace?.let { race ->
                    ScheduleRaceFeaturedCard(
                        race = race,
                        onViewSessions = { sessionsRace = race },
                    )
                }
            }
        }
    }

    sessionsRace?.let { race ->
        ScheduleRaceSessionsSheet(
            race = race,
            onDismiss = { sessionsRace = null },
        )
    }
}
