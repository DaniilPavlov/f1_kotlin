package com.example.f1_kotlin.ui.screens.circuits

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.model.CircuitModel
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.CareerListTile
import com.example.f1_kotlin.ui.components.CustomSwitcher
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.LinkText
import com.example.f1_kotlin.ui.components.LoadingIndicator
import com.example.f1_kotlin.ui.map.CircuitsTileSource
import com.example.f1_kotlin.ui.map.F1CircuitsClusterer
import com.example.f1_kotlin.ui.map.MapMarkerIcons
import com.example.f1_kotlin.ui.map.OsmdroidInitializer
import com.example.f1_kotlin.ui.map.configureCircuitsMapView
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.util.openUrl
import com.example.f1_kotlin.viewmodel.CircuitDetailViewModel
import com.example.f1_kotlin.viewmodel.CircuitsViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Экран «Трассы»: переключатель закреплён сверху, контент — в [Box] с [Modifier.weight].
 * Карта — [CircuitsMap] с OSMDroid ([configureCircuitsMapView] без повторения тайлов).
 */
@Composable
fun CircuitsScreen(
    viewModel: CircuitsViewModel,
    onCircuitClick: (String) -> Unit,
) {
    val circuits by viewModel.circuits.collectAsState()
    val activePage by viewModel.activePage.collectAsState()

    when (val state = circuits) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadCircuits, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(12.dp))
            CustomSwitcher(
                stringResource(R.string.on_map),
                stringResource(R.string.as_list),
                activePage,
                viewModel::changeActivePage,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (activePage) {
                    0 -> CircuitsMap(state.value, onCircuitClick)
                    else -> CircuitsList(state.value, onCircuitClick)
                }
            }
        }
    }
}

@Composable
private fun CircuitsList(circuits: List<CircuitModel>, onCircuitClick: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.horizontalPadding.dp)) {
        items(circuits) { circuit ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, F1Red, RoundedCornerShape(20.dp))
                    .clickable { onCircuitClick(circuit.circuitId) }
                    .padding(16.dp),
            ) {
                Text(circuit.circuitName, style = AppStyles.h3)
            }
        }
    }
}

/**
 * Карта трасс (OSMDroid): одна карта без повторения тайлов, пины 22 dp, красные кластеры.
 * [configureCircuitsMapView] отключает «мини-карты» при отдалении.
 */
@Composable
private fun CircuitsMap(circuits: List<CircuitModel>, onCircuitClick: (String) -> Unit) {
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
    circuits: List<CircuitModel>,
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

@Composable
fun CircuitDetailScreen(
    viewModel: CircuitDetailViewModel,
    onDriverClick: (com.example.f1_kotlin.data.model.DriverModel) -> Unit,
) {
    val circuitState by viewModel.circuit.collectAsState()
    val winnersState by viewModel.winners.collectAsState()
    val context = LocalContext.current

    when (val state = circuitState) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadAllData, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
        ) {
            Text(state.value.circuitName, style = AppStyles.h1)
            Spacer(Modifier.height(20.dp))
            LinkText(stringResource(R.string.read_on_wikipedia)) { openUrl(context, state.value.url) }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.country_label, state.value.location.country), style = AppStyles.h3)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.city_label, state.value.location.locality), style = AppStyles.h3)
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.circuit_winners_title), style = AppStyles.h2)
            Spacer(Modifier.height(12.dp))
            when (val winners = winnersState) {
                is AsyncValue.Loading -> LoadingIndicator(Modifier.padding(vertical = 16.dp))
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
