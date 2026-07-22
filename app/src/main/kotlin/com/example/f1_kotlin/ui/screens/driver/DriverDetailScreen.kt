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
import androidx.compose.material3.Text
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
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.data.model.ConstructorModel
import com.example.f1_kotlin.data.model.DriverModel
import com.example.f1_kotlin.data.model.EspnDriverCardData
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
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.util.rememberShareCareerAction
import com.example.f1_kotlin.viewmodel.DriverDetailViewModel
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DriverDetailScreen(
    viewModel: DriverDetailViewModel,
    onConstructorClick: (ConstructorModel) -> Unit,
    onCircuitClick: (CircuitModel) -> Unit,
) {
    val driver by viewModel.driver.collectAsState()
    val career by viewModel.careerStats.collectAsState()
    val espnCard by viewModel.espnCard.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    when {
        driver.isLoading || career.isLoading -> CareerScreenShimmer(modifier = Modifier.fillMaxSize())
        error != null && driver !is AsyncValue.Value -> ErrorBody(
            error?.title,
            error?.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        driver is AsyncValue.Value && career is AsyncValue.Value -> {
            DriverContent(
                driver = (driver as AsyncValue.Value).value,
                stats = (career as AsyncValue.Value).value,
                espnCard = espnCard,
                onConstructorClick = onConstructorClick,
                onCircuitClick = onCircuitClick,
                onWikipediaClick = { openUrl(context, (driver as AsyncValue.Value).value.url) },
            )
        }
    }
}

@Composable
private fun DriverContent(
    driver: DriverModel,
    stats: com.example.f1_kotlin.data.model.CareerStats<ConstructorModel>,
    espnCard: EspnDriverCardData,
    onConstructorClick: (ConstructorModel) -> Unit,
    onCircuitClick: (CircuitModel) -> Unit,
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
        espnCard.photoUrl?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(url),
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
        Text(stringResource(R.string.driver_teams_title), style = AppStyles.h2)
        Spacer(Modifier.height(12.dp))
        stats.related.forEach { constructor ->
            CareerListTile(
                title = constructor.name,
                subtitle = "",
                onClick = { onConstructorClick(constructor) },
                trailing = { CountryFlag(countryOrNationality = constructor.nationality) },
            )
        }
        if (espnCard.news.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.driver_news_title), style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            espnCard.news.forEach { article ->
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

private fun formatBirthDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
}.getOrElse { displayValue(value) }
