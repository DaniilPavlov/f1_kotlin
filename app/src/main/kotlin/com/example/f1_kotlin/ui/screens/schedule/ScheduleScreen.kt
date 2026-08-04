package com.example.f1_kotlin.ui.screens.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.components.CachedDataBanner
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.F1Calendar
import com.example.f1_kotlin.ui.components.OnAppResumed
import com.example.f1_kotlin.ui.components.ScheduleSessionCard
import com.example.f1_kotlin.ui.components.shimmer.ScheduleShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.viewmodel.ScheduleViewModel

/**
 * Экран «Календарь».
 *
 * Сверху — [F1Calendar], снизу — карточки сессий на выбранный день
 * или [ScheduleRaceFeaturedCard] с countdown, если день пустой и есть upcoming race.
 * Внизу — переход к списку трасс (бывшая вкладка Circuits).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onCircuits: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var sessionsRace by remember { mutableStateOf<Race?>(null) }

    OnAppResumed(onResumed = viewModel::dismissOfflineBannerIfOnline)

    when {
        uiState.error != null && uiState.races.isError -> ErrorBody(
            uiState.error?.title,
            uiState.error?.subtitle,
            onRetry = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        )
        uiState.races.isLoading -> ScheduleShimmer(modifier = Modifier.fillMaxSize())
        else -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (uiState.showingCachedData) {
                    CachedDataBanner()
                }
                Column(
                    modifier = Modifier.padding(
                        horizontal = AppDimens.horizontalPadding.dp,
                        vertical = AppDimens.verticalPadding.dp,
                    ),
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
                    Spacer(Modifier.height(12.dp))
                    CircuitsEntry(onClick = onCircuits)
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

@Composable
private fun CircuitsEntry(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.nav_circuits), style = AppStyles.h3)
    }
}
