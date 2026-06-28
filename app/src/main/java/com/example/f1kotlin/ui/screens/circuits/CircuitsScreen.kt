package com.example.f1kotlin.ui.screens.circuits

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1kotlin.data.model.CircuitModel
import com.example.f1kotlin.domain.AsyncValue
import com.example.f1kotlin.ui.components.CustomSwitcher
import com.example.f1kotlin.ui.components.ErrorBody
import com.example.f1kotlin.ui.components.LinkText
import com.example.f1kotlin.ui.components.LoadingIndicator
import com.example.f1kotlin.ui.map.F1CircuitsClusterer
import com.example.f1kotlin.ui.map.MapMarkerIcons
import com.example.f1kotlin.ui.map.configureCircuitsMapView
import com.example.f1kotlin.ui.theme.AppDimens
import com.example.f1kotlin.ui.theme.AppStyles
import com.example.f1kotlin.ui.theme.F1Red
import com.example.f1kotlin.util.openUrl
import com.example.f1kotlin.viewmodel.CircuitDetailViewModel
import com.example.f1kotlin.viewmodel.CircuitsViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
            CustomSwitcher("На карте", "Списком", activePage, viewModel::changeActivePage)
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
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
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
                    setTileSource(TileSourceFactory.MAPNIK)
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
fun CircuitDetailScreen(viewModel: CircuitDetailViewModel) {
    val circuitState by viewModel.circuit.collectAsState()
    val context = LocalContext.current

    when (val state = circuitState) {
        is AsyncValue.Loading -> LoadingIndicator(Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(state.message, state.subtitle, onRetry = viewModel::loadCircuit, modifier = Modifier.fillMaxSize())
        is AsyncValue.Value -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp, vertical = AppDimens.verticalPadding.dp),
        ) {
            Text(state.value.circuitName, style = AppStyles.h1)
            Spacer(Modifier.height(20.dp))
            LinkText("Прочитать информацию в википедии") { openUrl(context, state.value.url) }
            Spacer(Modifier.height(20.dp))
            Text("Страна: ${state.value.location.country}", style = AppStyles.h3)
            Spacer(Modifier.height(10.dp))
            Text("Город: ${state.value.location.locality}", style = AppStyles.h3)
        }
    }
}
