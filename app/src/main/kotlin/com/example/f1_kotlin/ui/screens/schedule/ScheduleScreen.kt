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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.F1Calendar
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.ScheduleSessionCard
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.ScheduleViewModel

/**
 * Экран «Календарь».
 *
 * Сверху — [F1Calendar], снизу — карточки сессий на выбранный день.
 * Элемент с пустым [ScheduleSessionItem.title] рисуется как заголовок с названием гонки.
 */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val races by viewModel.races.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val userPickedDay by viewModel.userPickedDay.collectAsState()
    val focusedMonth by viewModel.focusedMonth.collectAsState()
    val scheduleItems by viewModel.scheduleItems.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        error != null && races.isError -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        races.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
        ) {
            F1Calendar(
                selectedDate = selectedDate,
                focusedMonth = focusedMonth,
                userPickedDay = userPickedDay,
                logoForDay = viewModel::logoForDay,
                onDaySelected = viewModel::onSelectDay,
                onMonthChanged = viewModel::onMonthChanged,
            )
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            scheduleItems.forEach { item ->
                if (item.title.isEmpty()) {
                    Text(item.raceName, style = AppStyles.h3, modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    ScheduleSessionCard(item.title, item.date.date, item.date.time)
                }
            }
        }
    }
}
