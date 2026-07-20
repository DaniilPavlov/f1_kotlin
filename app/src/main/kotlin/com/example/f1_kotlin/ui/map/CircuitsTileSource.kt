package com.example.f1_kotlin.ui.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource

/**
 * Carto Voyager вместо MAPNIK: tile.openstreetmap.org часто блокирует приложения
 * без корректного UA (403 / «not following tile usage policy»).
 */
val CircuitsTileSource: OnlineTileSourceBase = XYTileSource(
    "CartoVoyager",
    0,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "© OpenStreetMap, © CARTO",
)
