package com.example.f1_kotlin.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.EspnScoreboardEvent
import com.example.f1_kotlin.data.model.EspnScoreboardSession
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.components.CountryFlag
import com.example.f1_kotlin.ui.components.shimmer.WeekendScoreboardSectionShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1GrayBg
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1TextGray
import com.example.f1_kotlin.ui.theme.F1White
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.sp

/** ESPN weekend scoreboard card on Results. Hides silently on failure / null. */
@Composable
fun WeekendScoreboardSection(scoreboard: AsyncValue<EspnScoreboardEvent?>) {
    val event = scoreboard.getOrNull()
    if (event != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppDimens.horizontalPadding.dp,
                    end = AppDimens.horizontalPadding.dp,
                    top = AppDimens.verticalPadding.dp,
                ),
        ) {
            Text(stringResource(R.string.home_weekend_title), style = AppStyles.h1)
            Spacer(Modifier.height(16.dp))
            ScoreboardCard(event)
        }
        return
    }
    if (scoreboard.isLoading) {
        WeekendScoreboardSectionShimmer()
    }
}

@Composable
private fun ScoreboardCard(event: EspnScoreboardEvent) {
    val language by LocaleController.language.collectAsState()
    val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("ru")
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
    }
    val highlighted = event.highlightedSession
    var sheetSession by remember { mutableStateOf<EspnScoreboardSession?>(null) }
    val locationParts = listOfNotNull(
        event.circuitCity?.takeIf { it.isNotEmpty() },
        event.circuitCountry?.takeIf { it.isNotEmpty() },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = event.shortName.ifEmpty { event.name },
                style = AppStyles.h3.copy(fontSize = 20.sp, lineHeight = 24.sp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            StatusChip(event, highlighted)
        }
        if (!event.circuitName.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(event.circuitName, style = AppStyles.body)
        }
        if (locationParts.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                event.circuitCountry?.takeIf { it.isNotEmpty() }?.let { country ->
                    CountryFlag(countryOrNationality = country, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    locationParts.joinToString(", "),
                    style = AppStyles.caption.copy(color = F1TextGray),
                )
            }
        }
        if (highlighted != null) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(F1GrayBg)
                    .clickable { sheetSession = highlighted }
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        highlighted.abbreviation,
                        style = AppStyles.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = F1TextGray,
                        modifier = Modifier.size(20.dp),
                    )
                }
                highlighted.date?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    Text(dateFormat.format(date), style = AppStyles.caption.copy(color = F1TextGray))
                }
                highlighted.leaderName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (highlighted.isWinner) {
                            stringResource(R.string.home_weekend_winner, name)
                        } else {
                            stringResource(R.string.home_weekend_leader, name)
                        },
                        style = AppStyles.body,
                    )
                }
            }
        }
        if (event.sessions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            event.sessions.forEach { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sheetSession = session }
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        session.abbreviation,
                        style = AppStyles.caption.copy(
                            color = if (session === highlighted) F1Red else F1Black,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        ),
                        modifier = Modifier.width(48.dp),
                    )
                    Text(
                        session.date?.let { dateFormat.format(it) } ?: session.statusDetail,
                        style = AppStyles.caption.copy(color = F1TextGray),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        session.statusDetail,
                        style = AppStyles.caption.copy(
                            color = if (session.isLive) F1Red else F1TextGray,
                        ),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = F1TextGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    sheetSession?.let { session ->
        WeekendSessionResultsSheet(
            session = session,
            onDismiss = { sheetSession = null },
        )
    }
}

@Composable
private fun StatusChip(event: EspnScoreboardEvent, highlighted: EspnScoreboardSession?) {
    val isLive = event.isLive || (highlighted?.isLive == true)
    val label = when {
        isLive -> stringResource(R.string.home_weekend_live)
        !highlighted?.statusDetail.isNullOrEmpty() -> highlighted!!.statusDetail
        else -> event.statusDetail
    }
    if (label.isEmpty()) return
    Text(
        text = label,
        style = AppStyles.caption.copy(color = F1White),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isLive) F1Red else F1Black)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekendSessionResultsSheet(
    session: EspnScoreboardSession,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = F1White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                stringResource(R.string.weekend_session_results_title, session.abbreviation),
                style = AppStyles.h2,
            )
            if (session.statusDetail.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(session.statusDetail, style = AppStyles.caption.copy(color = F1TextGray))
            }
            Spacer(Modifier.height(16.dp))
            if (session.hasResults) {
                LazyColumn {
                    items(session.results, key = { "${it.position}-${it.displayName}" }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${entry.position}",
                                style = AppStyles.body.copy(
                                    color = if (entry.isWinner) F1Red else F1Black,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                ),
                                modifier = Modifier.width(32.dp),
                            )
                            Text(
                                entry.displayName,
                                style = AppStyles.body.copy(
                                    color = if (entry.isWinner) F1Red else F1Black,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            entry.country?.let { country ->
                                CountryFlag(countryOrNationality = country, fontSize = 20.sp)
                            }
                        }
                        HorizontalDivider(color = F1StrokeGray)
                    }
                }
            } else {
                Text(
                    stringResource(R.string.weekend_session_results_empty),
                    style = AppStyles.body.copy(color = F1TextGray),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}
