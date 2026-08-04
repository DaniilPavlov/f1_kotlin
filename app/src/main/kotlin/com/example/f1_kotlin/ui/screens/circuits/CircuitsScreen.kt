package com.example.f1_kotlin.ui.screens.circuits

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.circuits.CircuitLayoutAssets
import com.example.f1_kotlin.domain.model.Circuit
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CareerListTile
import com.example.f1_kotlin.ui.components.CachedDataBanner
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LinkText
import com.example.f1_kotlin.ui.components.OnAppResumed
import com.example.f1_kotlin.ui.components.circuits.CircuitLayoutImage
import com.example.f1_kotlin.ui.components.circuits.CircuitStatsGrid
import com.example.f1_kotlin.ui.components.shimmer.CircuitScreenShimmer
import com.example.f1_kotlin.ui.components.shimmer.CircuitsShimmer
import com.example.f1_kotlin.ui.components.shimmer.ListRowsShimmer
import com.example.f1_kotlin.ui.components.CountryFlag
import com.example.f1_kotlin.ui.map.CircuitsTileSource
import com.example.f1_kotlin.ui.map.F1CircuitsClusterer
import com.example.f1_kotlin.ui.map.MapMarkerIcons
import com.example.f1_kotlin.ui.map.OsmdroidInitializer
import com.example.f1_kotlin.ui.map.configureCircuitsMapView
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.util.RegisterShareAction
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.util.rememberShareCircuitDeepLinkAction
import com.example.f1_kotlin.viewmodel.CircuitDetailViewModel
import com.example.f1_kotlin.viewmodel.CircuitsViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Экран «Трассы»: переключатель закреплён сверху, контент — в [Box] с [Modifier.weight].
 * Карта — [CircuitsMap] с OSMDroid ([configureCircuitsMapView] без повторения тайлов).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitsScreen(
    viewModel: CircuitsViewModel,
    onCircuitClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    OnAppResumed(onResumed = viewModel::dismissOfflineBannerIfOnline)

    when (val state = uiState.circuits) {
        is AsyncValue.Loading -> if (!uiState.isRefreshing) {
            CircuitsShimmer(modifier = Modifier.fillMaxSize())
        }
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        )
        is AsyncValue.Value -> Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.showingCachedData) {
                CachedDataBanner()
            }
            Spacer(Modifier.height(12.dp))
            CustomSwitcher(
                stringResource(R.string.on_map),
                stringResource(R.string.as_list),
                uiState.activePage,
                viewModel::changeActivePage,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (uiState.activePage) {
                    0 -> CircuitsMap(state.value, onCircuitClick)
                    else -> PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshAll,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CircuitsList(state.value, onCircuitClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircuitsList(circuits: List<Circuit>, onCircuitClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.horizontalPadding.dp)) {
        items(circuits) { circuit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, F1Red, RoundedCornerShape(20.dp))
                    .clickable { onCircuitClick(circuit.circuitId) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = circuit.circuitName,
                    style = AppStyles.h3,
                    modifier = Modifier.weight(1f),
                )
                if (CircuitLayoutAssets.hasLayout(circuit.circuitId)) {
                    Box(modifier = Modifier.width(72.dp).height(48.dp)) {
                        CircuitLayoutImage(
                            circuitId = circuit.circuitId,
                            height = 48.dp,
                            padding = 0.dp,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = F1Red,
                    )
                }
            }
        }
    }
}

/**
 * Карта трасс (OSMDroid): одна карта без повторения тайлов, пины 22 dp, красные кластеры.
 *
 * GoF Structural Bridge — абстракция Compose-[CircuitsMap] отделена от реализации:
 * OSMDroid [MapView] подключается через [AndroidView], не протекая в остальной UI.
 */
@Composable
private fun CircuitsMap(circuits: List<Circuit>, onCircuitClick: (String) -> Unit) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        OsmdroidInitializer.ensureInitialized(context)
    }

    DisposableEffect(mapView) {
        mapView?.onResume()
        onDispose { mapView?.onPause() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    setTileSource(CircuitsTileSource)
                    setMultiTouchControls(true)
                    val center = circuits.firstOrNull()?.let {
                        GeoPoint(it.location.lat.toDouble(), it.location.longitude.toDouble())
                    }
                    configureCircuitsMapView(this, center)
                }
            },
            update = { view ->
                populateCircuitsMap(view, circuits, onCircuitClick)
            },
        )
    }
}

/**
 * Добавляет маркеры на [MapView]: [MapMarkerIcons.scaledPinIcon] (22 dp по высоте)
 * и [F1CircuitsClusterer] с увеличенными красными кругами.
 */
private fun populateCircuitsMap(
    mapView: MapView,
    circuits: List<Circuit>,
    onCircuitClick: (String) -> Unit,
) {
    mapView.overlays.removeAll { it is F1CircuitsClusterer }

    val pinIcon = MapMarkerIcons.scaledPinIcon(mapView.context) ?: return
    val clusterer = F1CircuitsClusterer(mapView.context)

    circuits.forEach { circuit ->
        val marker = Marker(mapView)
        marker.position = GeoPoint(circuit.location.lat.toDouble(), circuit.location.longitude.toDouble())
        marker.icon = pinIcon
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = circuit.circuitName
        marker.relatedObject = circuit.circuitId
        marker.setOnMarkerClickListener { clicked, _ ->
            onCircuitClick(clicked.relatedObject as String)
            true
        }
        clusterer.add(marker)
    }

    mapView.overlays.add(clusterer)
    mapView.invalidate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitDetailScreen(
    viewModel: CircuitDetailViewModel,
    onDriverClick: (com.example.f1_kotlin.domain.model.Driver) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val circuit = (uiState.circuit as? AsyncValue.Value)?.value
    val shareAction = if (circuit != null) {
        rememberShareCircuitDeepLinkAction(circuit.circuitId, circuit.circuitName)
    } else {
        null
    }
    RegisterShareAction(shareAction)

    when (val state = uiState.circuit) {
        is AsyncValue.Loading -> if (!uiState.isRefreshing) {
            CircuitScreenShimmer(modifier = Modifier.fillMaxSize())
        }
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::loadAllData,
            modifier = Modifier.fillMaxSize(),
        )
        is AsyncValue.Value -> PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = AppDimens.horizontalPadding.dp,
                        vertical = AppDimens.verticalPadding.dp,
                    ),
            ) {
                if (CircuitLayoutAssets.hasLayout(state.value.circuitId)) {
                    CircuitLayoutImage(circuitId = state.value.circuitId, height = 220.dp)
                    Spacer(Modifier.height(16.dp))
                }
                Text(state.value.circuitName, style = AppStyles.h1)
                uiState.stats?.let {
                    Spacer(Modifier.height(16.dp))
                    CircuitStatsGrid(stats = it)
                }
                Spacer(Modifier.height(16.dp))
                LinkText(stringResource(R.string.read_on_wikipedia)) { openUrl(context, state.value.url) }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${stringResource(R.string.country)}: ", style = AppStyles.h3)
                    CountryFlag(
                        countryOrNationality = state.value.location.country,
                        fontSize = 28.sp,
                        fallbackStyle = AppStyles.h3,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.city_label, state.value.location.locality), style = AppStyles.h3)
                Spacer(Modifier.height(28.dp))
                Text(stringResource(R.string.circuit_winners_title), style = AppStyles.h2)
                Spacer(Modifier.height(12.dp))
                when (val winners = uiState.winners) {
                    is AsyncValue.Loading -> if (!uiState.isRefreshing) ListRowsShimmer(rowCount = 4)
                    is AsyncValue.Error -> Text(winners.message, style = AppStyles.body)
                    is AsyncValue.Value -> {
                        if (winners.value.isEmpty()) {
                            Text(stringResource(R.string.circuit_winners_empty), style = AppStyles.body)
                        } else {
                            winners.value.forEach { win ->
                                CareerListTile(
                                    title = "${win.season} · ${win.raceName}",
                                    subtitle = "${win.driver.fullName} · ${win.constructor.name}",
                                    onClick = { onDriverClick(win.driver) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
