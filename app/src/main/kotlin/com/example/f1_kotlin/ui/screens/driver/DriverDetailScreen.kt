package com.example.f1_kotlin.ui.screens.driver

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.CareerRaceResult
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.model.Constructor
import com.example.f1_kotlin.domain.model.Driver
import com.example.f1_kotlin.data.model.EspnDriverCardData
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
import com.example.f1_kotlin.ui.components.shimmer.CareerScreenShimmer
import com.example.f1_kotlin.ui.components.CountryFlag
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.util.RegisterShareAction
import com.example.f1_kotlin.util.TrustedUrl
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.util.rememberShareCareerAction
import com.example.f1_kotlin.viewmodel.DriverDetailViewModel
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    viewModel: DriverDetailViewModel,
    onConstructorClick: (Constructor) -> Unit,
    onCircuitClick: (Circuit) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val driver = uiState.driver
    val career = uiState.careerStats
    val context = LocalContext.current

    when {
        !uiState.isRefreshing && (driver.isLoading || career.isLoading) -> CareerScreenShimmer(
            modifier = Modifier.fillMaxSize(),
        )
        driver is AsyncValue.Value && career is AsyncValue.Value -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                DriverContent(
                    driver = driver.value,
                    stats = career.value,
                    espnCard = uiState.espnCard,
                    onConstructorClick = onConstructorClick,
                    onCircuitClick = onCircuitClick,
                    onWikipediaClick = { openUrl(context, driver.value.url) },
                )
            }
        }
        else -> {
            val asyncError = (driver as? AsyncValue.Error)
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
private fun DriverContent(
    driver: Driver,
    stats: com.example.f1_kotlin.data.model.CareerStats<Constructor>,
    espnCard: EspnDriverCardData,
    onConstructorClick: (Constructor) -> Unit,
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
            title = driver.fullName,
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
        DriverHeaderBlock(
            driver = driver,
            currentTeams = stats.current,
            photoUrl = espnCard.photoUrl,
            onWikipediaClick = onWikipediaClick,
        )
        Spacer(Modifier.height(28.dp))
        DriverCareerStatsSection(
            stats = stats,
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
        DriverTeamsSection(
            teams = stats.related,
            onConstructorClick = onConstructorClick,
        )
        if (espnCard.news.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            DriverNewsSection(articles = espnCard.news)
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

@Composable
private fun DriverHeaderBlock(
    driver: Driver,
    currentTeams: List<Constructor>,
    photoUrl: String?,
    onWikipediaClick: () -> Unit,
) {
    photoUrl?.let { url ->
        Image(
            painter = rememberAsyncImagePainter(TrustedUrl.preferHttps(url)),
            contentDescription = driver.fullName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(16.dp))
    }
    Text(driver.fullName, style = AppStyles.h1)
    Spacer(Modifier.height(16.dp))
    CareerInfoRow(stringResource(R.string.driver_code), displayValue(driver.code))
    CareerInfoRow(stringResource(R.string.driver_number), displayValue(driver.permanentNumber))
    CareerInfoRow(stringResource(R.string.nationality)) {
        CountryFlag(countryOrNationality = driver.nationality, fontSize = 28.sp)
    }
    CareerInfoRow(stringResource(R.string.date_of_birth), formatBirthDate(driver.dateOfBirth))
    if (currentTeams.isNotEmpty()) {
        CareerInfoRow(
            stringResource(R.string.current_team),
            currentTeams.joinToString(", ") { it.name },
        )
    }
    if (driver.url.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        WikipediaLink(onWikipediaClick)
    }
}

@Composable
private fun DriverCareerStatsSection(
    stats: com.example.f1_kotlin.data.model.CareerStats<Constructor>,
    onWinsTap: () -> Unit,
    onPodiumsTap: () -> Unit,
    onPolesTap: () -> Unit,
) {
    Text(stringResource(R.string.career_title), style = AppStyles.h2)
    Spacer(Modifier.height(16.dp))
    CareerStatsGrid(
        races = stats.races,
        wins = stats.wins,
        podiums = stats.podiums,
        poles = stats.poles,
        onWinsTap = onWinsTap,
        onPodiumsTap = onPodiumsTap,
        onPolesTap = onPolesTap,
    )
}

@Composable
private fun DriverTeamsSection(
    teams: List<Constructor>,
    onConstructorClick: (Constructor) -> Unit,
) {
    Text(stringResource(R.string.driver_teams_title), style = AppStyles.h2)
    Spacer(Modifier.height(12.dp))
    teams.forEach { constructor ->
        CareerListTile(
            title = constructor.name,
            subtitle = "",
            onClick = { onConstructorClick(constructor) },
            trailing = { CountryFlag(countryOrNationality = constructor.nationality) },
        )
    }
}

@Composable
private fun DriverNewsSection(articles: List<NewsArticle>) {
    Text(stringResource(R.string.driver_news_title), style = AppStyles.h2)
    Spacer(Modifier.height(12.dp))
    articles.forEach { article ->
        NewsArticleTile(article)
        Spacer(Modifier.height(12.dp))
    }
}

private fun formatBirthDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
}.getOrElse { displayValue(value) }
