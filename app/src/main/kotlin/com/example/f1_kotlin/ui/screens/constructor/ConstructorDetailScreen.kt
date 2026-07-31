package com.example.f1_kotlin.ui.screens.constructor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.CareerRaceResult
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.data.model.NewsArticle
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CareerInfoRow
import com.example.f1_kotlin.ui.components.CareerListTile
import com.example.f1_kotlin.ui.components.CareerRaceResultsSheet
import com.example.f1_kotlin.ui.components.CareerStatsGrid
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.NewsArticleTile
import com.example.f1_kotlin.ui.components.WikipediaLink
import com.example.f1_kotlin.ui.components.displayValue
import com.example.f1_kotlin.ui.components.CountryFlag
import com.example.f1_kotlin.ui.components.shimmer.CareerScreenShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.util.RegisterShareAction
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.util.rememberShareCareerAction
import com.example.f1_kotlin.viewmodel.ConstructorDetailViewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructorDetailScreen(
    viewModel: ConstructorDetailViewModel,
    onDriverClick: (Driver) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val constructor = uiState.constructor
    val career = uiState.careerStats
    val context = LocalContext.current

    when {
        !uiState.isRefreshing && (constructor.isLoading || career.isLoading) -> CareerScreenShimmer(
            showPhoto = false,
            modifier = Modifier.fillMaxSize(),
        )
        constructor is AsyncValue.Value && career is AsyncValue.Value -> {
            val model = constructor.value
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                ConstructorContent(
                    name = model.name,
                    nationality = model.nationality,
                    url = model.url,
                    stats = career.value,
                    news = uiState.news,
                    onDriverClick = onDriverClick,
                    onCircuitClick = onCircuitClick,
                    onWikipediaClick = { openUrl(context, model.url) },
                )
            }
        }
        else -> {
            val asyncError = (constructor as? AsyncValue.Error)
                ?: (career as? AsyncValue.Error)
            ErrorBody(
                uiState.error?.title ?: asyncError?.message,
                uiState.error?.subtitle ?: asyncError?.subtitle,
                onRetry = viewModel::loadAllData,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConstructorContent(
    name: String,
    nationality: String,
    url: String,
    stats: com.example.f1_kotlin.data.model.CareerStats<Driver>,
    news: List<NewsArticle>,
    onDriverClick: (Driver) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
    onWikipediaClick: () -> Unit,
) {
    var sheetTitle by remember { mutableStateOf<String?>(null) }
    var sheetRaces by remember { mutableStateOf<List<CareerRaceResult>>(emptyList()) }
    var sheetShowPosition by remember { mutableStateOf(false) }
    val winsTitle = stringResource(R.string.wins)
    val podiumsTitle = stringResource(R.string.career_stat_podiums)
    val polesTitle = stringResource(R.string.career_stat_poles)

    RegisterShareAction(
        rememberShareCareerAction(
            title = name,
            races = stats.races,
            wins = stats.wins,
            podiums = stats.podiums,
            poles = stats.poles,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
    ) {
        Text(name, style = AppStyles.h1)
        Spacer(Modifier.height(16.dp))
        CareerInfoRow(stringResource(R.string.nationality)) {
            CountryFlag(countryOrNationality = nationality, fontSize = 28.sp)
        }
        if (stats.current.isNotEmpty()) {
            CareerInfoRow(
                stringResource(R.string.current_drivers),
                stats.current.joinToString(", ") { it.fullName },
            )
        }
        if (url.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            WikipediaLink(onWikipediaClick)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.career_title), style = AppStyles.h2)
        Spacer(Modifier.height(16.dp))
        CareerStatsGrid(
            races = stats.races,
            wins = stats.wins,
            podiums = stats.podiums,
            poles = stats.poles,
            onWinsTap = {
                sheetTitle = winsTitle
                sheetRaces = stats.winRaces
                sheetShowPosition = false
            },
            onPodiumsTap = {
                sheetTitle = podiumsTitle
                sheetRaces = stats.podiumRaces
                sheetShowPosition = true
            },
            onPolesTap = {
                sheetTitle = polesTitle
                sheetRaces = stats.poleRaces
                sheetShowPosition = false
            },
        )
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.constructor_drivers_title), style = AppStyles.h2)
        Spacer(Modifier.height(12.dp))
        stats.related.forEach { driver ->
            CareerListTile(
                title = driver.fullName,
                subtitle = "",
                onClick = { onDriverClick(driver) },
                trailing = { CountryFlag(countryOrNationality = driver.nationality) },
            )
        }
        if (news.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.driver_news_title), style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            news.forEach { article ->
                NewsArticleTile(article)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    sheetTitle?.let { title ->
        CareerRaceResultsSheet(
            title = title,
            races = sheetRaces,
            showPosition = sheetShowPosition,
            onDismiss = { sheetTitle = null },
            onCircuitClick = onCircuitClick,
        )
    }
}
