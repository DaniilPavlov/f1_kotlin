package com.example.f1_kotlin.ui.map

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Минимальный zoom: 2 — предельное отдаление (бывший «предпоследний» шаг).
 * Zoom 1 даёт белые полосы по бокам при отключённом повторении тайлов.
 */
private const val MAP_MIN_ZOOM = 2.0
private const val MAP_INITIAL_ZOOM = 2.5
private const val MAP_MAX_ZOOM = 18.0

/**
 * Базовые настройки [MapView] для экрана «Трассы».
 *
 * Повторение тайлов выключено — одна карта без «мини-карт».
 * [MAP_MIN_ZOOM] = 2: дальше отдалять нельзя, чтобы не появлялись белые поля по краям.
 */
fun configureCircuitsMapView(mapView: MapView, initialCenter: GeoPoint?) {
    mapView.setHorizontalMapRepetitionEnabled(false)
    mapView.setVerticalMapRepetitionEnabled(false)

    val tileSystem = MapView.getTileSystem()
    mapView.setScrollableAreaLimitLatitude(tileSystem.maxLatitude, tileSystem.minLatitude, 0)
    mapView.setScrollableAreaLimitLongitude(tileSystem.minLongitude, tileSystem.maxLongitude, 0)

    mapView.minZoomLevel = MAP_MIN_ZOOM
    mapView.maxZoomLevel = MAP_MAX_ZOOM
    mapView.controller.setZoom(MAP_INITIAL_ZOOM)
    initialCenter?.let { mapView.controller.setCenter(it) }
}
