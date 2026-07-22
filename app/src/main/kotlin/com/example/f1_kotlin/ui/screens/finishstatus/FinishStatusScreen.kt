package com.example.f1_kotlin.ui.screens.finishstatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.FinishStatusItem
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1TextGray
import com.example.f1_kotlin.viewmodel.FinishStatusViewModel

@Composable
fun FinishStatusScreen(viewModel: FinishStatusViewModel) {
    val year by viewModel.year.collectAsState()
    val statuses by viewModel.statuses.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(stringResource(R.string.finish_status_subtitle), style = AppStyles.body)
        Spacer(Modifier.height(16.dp))
        SeasonPickerField(
            value = year,
            label = stringResource(R.string.season),
            hint = stringResource(R.string.select_season),
            onSeasonSelected = viewModel::onYearChanged,
            loadSeasons = viewModel::loadSeasonYears,
        )
        Spacer(Modifier.height(20.dp))
        when (val state = statuses) {
            is AsyncValue.Loading -> ListRowsShimmer(rowCount = 8)
            is AsyncValue.Error -> ErrorBody(
                state.message,
                state.subtitle,
                onRetry = viewModel::loadAllData,
            )
            is AsyncValue.Value -> StatusList(state.value)
        }
    }
}

@Composable
private fun StatusList(items: List<FinishStatusItem>) {
    if (items.isEmpty()) {
        Text(
            stringResource(R.string.finish_status_empty),
            style = AppStyles.body,
            modifier = Modifier.padding(vertical = 24.dp),
        )
        return
    }
    val total = items.sumOf { it.count }
    items.forEach { item ->
        val color = if (item.isHighlight) F1Red else Color.Black
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Text(
                item.status,
                style = AppStyles.body.copy(color = color),
                modifier = Modifier.weight(1f),
            )
            Text("${item.count}", style = AppStyles.h3.copy(color = color))
            if (total > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${((item.count.toDouble() / total) * 100).toInt()}%",
                    style = AppStyles.caption.copy(color = F1TextGray),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        Divider(color = F1StrokeGray)
    }
}
