package com.example.f1_kotlin.ui.screens.constructor

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
import com.example.f1_kotlin.viewmodel.ConstructorDetailViewModel

@Composable
fun ConstructorDetailScreen(
    viewModel: ConstructorDetailViewModel,
    onDriverClick: (DriverModel) -> Unit,
) {
    val constructor by viewModel.constructor.collectAsState()
    val career by viewModel.careerStats.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    when {
        constructor.isLoading || career.isLoading -> LoadingIndicator(Modifier.fillMaxSize())
        error != null -> ErrorBody(error?.title, error?.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        constructor is AsyncValue.Value && career is AsyncValue.Value -> {
            val model = (constructor as AsyncValue.Value).value
            val stats = (career as AsyncValue.Value).value
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
            ) {
                Text(model.name, style = AppStyles.h1)
                Spacer(Modifier.height(16.dp))
                CareerInfoRow(stringResource(R.string.nationality), displayValue(model.nationality))
                if (stats.current.isNotEmpty()) {
                    CareerInfoRow(
                        stringResource(R.string.current_drivers),
                        stats.current.joinToString(", ") { it.fullName },
                    )
                }
                if (model.url.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    WikipediaLink { openUrl(context, model.url) }
                }
                Spacer(Modifier.height(28.dp))
                Text(stringResource(R.string.career_title), style = AppStyles.h2)
                Spacer(Modifier.height(16.dp))
                CareerStatsGrid(stats.races, stats.wins, stats.podiums, stats.poles)
                Spacer(Modifier.height(28.dp))
                Text(stringResource(R.string.constructor_drivers_title), style = AppStyles.h2)
                Spacer(Modifier.height(12.dp))
                stats.related.forEach { driver ->
                    CareerListTile(
                        title = driver.fullName,
                        subtitle = displayValue(driver.nationality),
                        onClick = { onDriverClick(driver) },
                    )
                }
            }
        }
    }
}
