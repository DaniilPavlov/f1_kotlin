package com.example.f1_kotlin.ui.screens.driver

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CareerInfoRow
import com.example.f1_kotlin.ui.components.CareerListTile
import com.example.f1_kotlin.ui.components.CareerStatsGrid
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.components.WikipediaLink
import com.example.f1_kotlin.ui.components.displayValue
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.viewmodel.DriverDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DriverDetailScreen(
    viewModel: DriverDetailViewModel,
    onConstructorClick: (ConstructorModel) -> Unit,
) {
    val driver by viewModel.driver.collectAsState()
    val career by viewModel.careerStats.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    when {
        driver.isLoading || career.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        error != null -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        driver is AsyncValue.Value && career is AsyncValue.Value -> {
            DriverContent(
                driver = (driver as AsyncValue.Value).value,
                stats = (career as AsyncValue.Value).value,
                onConstructorClick = onConstructorClick,
                onWikipediaClick = { openUrl(context, (driver as AsyncValue.Value).value.url) },
            )
        }
    }
}

@Composable
private fun DriverContent(
    driver: DriverModel,
    stats: com.example.f1_kotlin.data.model.CareerStats<ConstructorModel>,
    onConstructorClick: (ConstructorModel) -> Unit,
    onWikipediaClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(driver.fullName, style = AppStyles.h1)
        Spacer(Modifier.height(16.dp))
        CareerInfoRow(stringResource(R.string.driver_code), displayValue(driver.code))
        CareerInfoRow(stringResource(R.string.driver_number), displayValue(driver.permanentNumber))
        CareerInfoRow(stringResource(R.string.nationality), displayValue(driver.nationality))
        CareerInfoRow(stringResource(R.string.date_of_birth), formatBirthDate(driver.dateOfBirth))
        if (stats.current.isNotEmpty()) {
            CareerInfoRow(
                stringResource(R.string.current_team),
                stats.current.joinToString(", ") { it.name },
            )
        }
        if (driver.url.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            WikipediaLink(onWikipediaClick)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.career_title), style = AppStyles.h2)
        Spacer(Modifier.height(16.dp))
        CareerStatsGrid(stats.races, stats.wins, stats.podiums, stats.poles)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.driver_teams_title), style = AppStyles.h2)
        Spacer(Modifier.height(12.dp))
        stats.related.forEach { constructor ->
            CareerListTile(
                title = constructor.name,
                subtitle = displayValue(constructor.nationality),
                onClick = { onConstructorClick(constructor) },
            )
        }
    }
}

private fun formatBirthDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
}.getOrElse { displayValue(value) }
