package com.example.f1_kotlin.ui.screens.rewind

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R
import java.util.Locale
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.domain.model.Race
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.SeasonPickerField
import com.example.f1_kotlin.ui.components.shimmer.SeasonRewindShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.ConstructorColors
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.viewmodel.SeasonRewindBarEntry
import com.example.f1_kotlin.viewmodel.SeasonRewindUiState
import com.example.f1_kotlin.viewmodel.SeasonRewindViewModel
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonRewindScreen(viewModel: SeasonRewindViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    SeasonRewindScreenContent(
        uiState = uiState,
        onRefresh = viewModel::refreshAll,
        onSeasonSelected = viewModel::onSeasonChanged,
        loadSeasons = viewModel::loadSeasonYears,
        onSelectRound = viewModel::selectRound,
        onTogglePlayback = viewModel::togglePlayback,
        onStopPlayback = viewModel::stopPlayback,
        onTableChanged = viewModel::onTableChanged,
        onRetryStandings = { viewModel.selectRound(uiState.selectedRoundIndex) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonRewindScreenContent(
    uiState: SeasonRewindUiState,
    onRefresh: () -> Unit,
    onSeasonSelected: (String) -> Unit,
    loadSeasons: suspend () -> Result<List<String>>,
    onSelectRound: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onTableChanged: (Int) -> Unit,
    onRetryStandings: () -> Unit,
) {
    val colors = appColors()

    PullToRefreshBox(
        isRefreshing = uiState.races is AsyncValue.Loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val racesState = uiState.races) {
            is AsyncValue.Error -> ErrorBody(
                racesState.message,
                racesState.subtitle,
                onRetry = onRefresh,
                modifier = Modifier.padding(AppDimens.horizontalPadding.dp),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AppDimens.horizontalPadding.dp,
                        vertical = AppDimens.verticalPadding.dp,
                    ),
            ) {
                Text(
                    stringResource(R.string.season_rewind_subtitle),
                    style = AppStyles.caption.copy(color = colors.textGray),
                )
                Spacer(Modifier.height(16.dp))
                SeasonPickerField(
                    value = uiState.year,
                    label = stringResource(R.string.season),
                    hint = stringResource(R.string.select_season),
                    onSeasonSelected = onSeasonSelected,
                    loadSeasons = loadSeasons,
                )
                Spacer(Modifier.height(20.dp))

                val races = (racesState as? AsyncValue.Value)?.value.orEmpty()
                if (racesState is AsyncValue.Loading && races.isEmpty()) {
                    SeasonRewindShimmer(showScrubber = true)
                } else if (races.isEmpty()) {
                    Text(
                        stringResource(R.string.season_rewind_empty),
                        style = AppStyles.body.copy(color = colors.black),
                    )
                } else {
                    SeasonRewindScrubber(
                        races = races,
                        selectedIndex = uiState.selectedRoundIndex,
                        isPlaying = uiState.isPlaying,
                        canPlay = uiState.canPlay,
                        onCommitRound = onSelectRound,
                        onTogglePlayback = onTogglePlayback,
                        onDragStart = onStopPlayback,
                    )
                    Spacer(Modifier.height(16.dp))

                    when {
                        // Только первичная загрузка — при scrub оставляем график.
                        uiState.chartLoading && !uiState.hasChartData -> {
                            SeasonRewindShimmer(showScrubber = false)
                        }
                        uiState.hasChartData -> {
                            CustomSwitcher(
                                firstTitle = stringResource(R.string.drivers),
                                secondTitle = stringResource(R.string.constructors),
                                activeValue = uiState.activeTable,
                                onChanged = onTableChanged,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.season_rewind_chart_hint),
                                style = AppStyles.caption.copy(color = colors.textGray),
                            )
                            Spacer(Modifier.height(12.dp))
                            SeasonRewindRacingChart(entries = uiState.activeBars)
                        }
                        uiState.chartLoadFailed || uiState.isChartStale -> {
                            ErrorBody(
                                title = uiState.chartError?.title
                                    ?: stringResource(R.string.season_rewind_load_error),
                                subtitle = uiState.chartError?.subtitle
                                    ?: stringResource(R.string.season_rewind_retry_subtitle),
                                onRetry = onRetryStandings,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Слайдер раундов. Thumb во время драга — в локальном state, commit с debounce
 * иначе rebuild/429 ломают загрузку 1-го раунда.
 */
@Composable
private fun SeasonRewindScrubber(
    races: List<Race>,
    selectedIndex: Int,
    isPlaying: Boolean,
    canPlay: Boolean,
    onCommitRound: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onDragStart: () -> Unit,
) {
    val colors = appColors()
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    var commitJob by remember { mutableStateOf<Job?>(null) }
    val maxIndex = (races.size - 1).coerceAtLeast(0)
    val thumb = (dragIndex ?: selectedIndex).coerceIn(0, maxIndex)
    val race = races.getOrNull(thumb)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = race?.raceName.orEmpty(),
            style = AppStyles.body.copy(color = colors.black),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.season_rewind_race_of, thumb + 1, races.size),
            style = AppStyles.caption.copy(color = colors.textGray),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTogglePlayback, enabled = canPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = F1Red,
                )
            }
            Slider(
                value = thumb.toFloat(),
                onValueChange = { value ->
                    if (dragIndex == null) onDragStart()
                    dragIndex = value.roundToInt().coerceIn(0, maxIndex)
                },
                onValueChangeFinished = {
                    val index = (dragIndex ?: selectedIndex).coerceIn(0, maxIndex)
                    dragIndex = null
                    // 120ms debounce— меньше параллельных Jolpica-запросов.
                    commitJob?.cancel()
                    commitJob = scope.launch {
                        delay(120)
                        onCommitRound(index)
                    }
                },
                valueRange = 0f..maxIndex.toFloat(),
                steps = (maxIndex - 1).coerceAtLeast(0),
                enabled = maxIndex > 0,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.round_label, races.first().round),
                style = AppStyles.caption.copy(color = colors.textGray),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.round_label, races.last().round),
                style = AppStyles.caption.copy(color = colors.textGray),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SeasonRewindRacingChart(
    entries: List<SeasonRewindBarEntry>,
    modifier: Modifier = Modifier,
    durationMs: Int = 650,
    rowHeightDp: Float = 44f,
) {
    var from by remember { mutableStateOf<Map<String, SeasonRewindBarEntry>>(emptyMap()) }
    var to by remember { mutableStateOf<Map<String, SeasonRewindBarEntry>>(emptyMap()) }
    val progress = remember { Animatable(1f) }
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(entries) {
        if (!seeded) {
            seeded = true
            to = entries.associateBy { it.id }
            if (entries.isEmpty()) {
                from = to
                progress.snapTo(1f)
                return@LaunchedEffect
            }
            // Первый показ: полосы заполняются с нуля.
            from = emptyMap()
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
            )
            return@LaunchedEffect
        }
        if (sameEntries(to.values.toList(), entries)) return@LaunchedEffect

        // progress уже с easing из tween — не трансформируем повторно.
        from = lerpAll(from, to, progress.value).associateBy { it.id }
        to = entries.associateBy { it.id }
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        )
    }

    if (entries.isEmpty() && to.isEmpty()) return

    val lerped = lerpAll(from, to, progress.value)
    val visibleCount = max(to.size, 1)
    val maxPoints = lerped.maxOfOrNull { it.points }?.takeIf { it > 0 } ?: 1.0
    val rowHeight = rowHeightDp.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * visibleCount),
    ) {
        lerped.forEach { entry ->
            RacingBarRow(
                entry = entry,
                maxPoints = maxPoints,
                isLeader = entry.rank < 0.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .offset(y = rowHeight * entry.rank),
            )
        }
    }
}

@Composable
private fun RacingBarRow(
    entry: SeasonRewindBarEntry,
    maxPoints: Double,
    isLeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val base = ConstructorColors.forConstructorId(entry.constructorId)
    val widthFactor = (entry.points / maxPoints).toFloat().coerceIn(0f, 1f)
    val pointsLabel = formatPoints(entry.points)

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${entry.rank.roundToInt() + 1}",
            style = AppStyles.caption.copy(
                fontWeight = FontWeight.Bold,
                color = if (isLeader) base else colors.textGray,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        Row(
            modifier = Modifier.width(88.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.label,
                style = AppStyles.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.black,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (entry.tag.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = entry.tag,
                    style = AppStyles.caption.copy(
                        fontSize = 10.sp,
                        color = colors.textGray,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        RacingBarTrack(
            widthFactor = widthFactor,
            barColor = base,
            trackColor = colors.grayBg,
            isLeader = isLeader,
            modifier = Modifier
                .weight(1f)
                .height(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pointsLabel,
            style = AppStyles.caption.copy(
                fontWeight = FontWeight.Bold,
                color = if (isLeader) base else colors.black,
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun RacingBarTrack(
    widthFactor: Float,
    barColor: Color,
    trackColor: Color,
    isLeader: Boolean,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(trackColor, RoundedCornerShape(6.dp)),
            )
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .then(
                        if (isLeader) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(6.dp),
                                ambientColor = barColor.copy(alpha = 0.4f),
                                spotColor = barColor.copy(alpha = 0.4f),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.85f), barColor),
                        ),
                        shape = RoundedCornerShape(6.dp),
                    ),
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val track = measurables[0].measure(constraints)
        val barMax = constraints.maxWidth
        val barWidth = max(8, (barMax * widthFactor).roundToInt()).coerceAtMost(barMax)
        val bar = measurables[1].measure(
            constraints.copy(minWidth = barWidth, maxWidth = barWidth),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            track.placeRelative(0, 0)
            bar.placeRelative(0, 0)
        }
    }
}

private fun formatPoints(points: Double): String {
    val asInt = points.roundToInt()
    return if (kotlin.math.abs(points - asInt) < 0.05) {
        asInt.toString()
    } else {
        String.format(Locale.US, "%.1f", points)
    }
}

private fun sameEntries(
    a: List<SeasonRewindBarEntry>,
    b: List<SeasonRewindBarEntry>,
): Boolean {
    if (a.size != b.size) return false
    return a.indices.all { a[it] == b[it] }
}

private fun lerpAll(
    from: Map<String, SeasonRewindBarEntry>,
    to: Map<String, SeasonRewindBarEntry>,
    t: Float,
): List<SeasonRewindBarEntry> {
    val ids = from.keys + to.keys
    val exitRank = max(from.size, to.size).toFloat()
    val lerped = ArrayList<SeasonRewindBarEntry>(ids.size)
    for (id in ids) {
        val a = from[id]
        val b = to[id]
        when {
            a == null && b == null -> Unit
            a == null && b != null -> lerped.add(
                b.copy(points = b.points * t, rank = b.rank),
            )
            a != null && b == null -> lerped.add(
                a.copy(points = a.points * (1 - t), rank = exitRank),
            )
            a != null && b != null -> lerped.add(
                SeasonRewindBarEntry(
                    id = b.id,
                    constructorId = b.constructorId,
                    label = b.label,
                    tag = b.tag,
                    points = a.points + (b.points - a.points) * t,
                    rank = a.rank + (b.rank - a.rank) * t,
                ),
            )
        }
    }
    return lerped.filter { it.points > 0.05 || to.containsKey(it.id) }
}
