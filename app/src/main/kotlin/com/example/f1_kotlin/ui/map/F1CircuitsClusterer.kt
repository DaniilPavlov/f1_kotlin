package com.example.f1_kotlin.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.f1_kotlin.R
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.max
import kotlin.math.min

/** Высота чёрного пина на карте в dp (фиксированный размер, не масштаб PNG). */
private const val PIN_HEIGHT_DP = 25f

/** Множитель и границы радиуса красного кластера (увеличены относительно Flutter-оригинала). */
private const val CLUSTER_RADIUS_MULTIPLIER = 9
private const val CLUSTER_MIN_RADIUS_PX = 45
private const val CLUSTER_MAX_RADIUS_PX = 72

/**
 * Кластеры трасс на карте — красный круг с прозрачностью 75% и белой цифрой.
 *
 * Размер кластера больше, чем во Flutter: `min(max(size × 9, 45), 72)` px.
 * Отдельные пины — фиксированная высота [PIN_HEIGHT_DP] dp через [MapMarkerIcons.scaledPinIcon].
 */
class F1CircuitsClusterer(context: Context) : RadiusMarkerClusterer(context) {

    init {
        setMaxClusteringZoomLevel(15)
        setRadius(50)
        mTextPaint.color = Color.WHITE
        mTextPaint.textSize = 14f * context.resources.displayMetrics.density
        mAnchorU = Marker.ANCHOR_CENTER
        mAnchorV = Marker.ANCHOR_CENTER
    }

    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = Marker(mapView)
        marker.position = cluster.position
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.infoWindow = null
        marker.icon = BitmapDrawable(mapView.context.resources, buildClusterBitmap(cluster.size))

        marker.setOnMarkerClickListener { _, _ ->
            mapView.controller.animateTo(cluster.position, mapView.zoomLevelDouble + 2, 500L)
            true
        }
        return marker
    }

    private fun buildClusterBitmap(clusterSize: Int): Bitmap {
        val radius = min(
            max(clusterSize * CLUSTER_RADIUS_MULTIPLIER, CLUSTER_MIN_RADIUS_PX),
            CLUSTER_MAX_RADIUS_PX,
        )
        val diameter = radius * 2
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E1271E")
            alpha = (255 * 0.75).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), fillPaint)

        val text = clusterSize.toString()
        val textHeight = mTextPaint.descent() + mTextPaint.ascent()
        canvas.drawText(text, radius.toFloat(), radius - textHeight / 2f, mTextPaint)
        return bitmap
    }
}

/** Подготовка иконок маркеров OSMDroid: уменьшенный чёрный пин [R.drawable.pin_unselected]. */
object MapMarkerIcons {

    /**
     * Уменьшает [pin_unselected] до [PIN_HEIGHT_DP] dp по высоте.
     * Якорь маркера — [Marker.ANCHOR_BOTTOM]: «острие» пина на координате трассы.
     */
    fun scaledPinIcon(context: Context): Drawable? {
        val source = ContextCompat.getDrawable(context, R.drawable.pin_unselected) ?: return null
        val bitmap = source.toBitmap()
        val density = context.resources.displayMetrics.density
        val targetHeightPx = (PIN_HEIGHT_DP * density).toInt().coerceAtLeast(1)
        val targetWidthPx = (bitmap.width.toFloat() / bitmap.height * targetHeightPx).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidthPx, targetHeightPx, true)
        return BitmapDrawable(context.resources, scaled)
    }
}
