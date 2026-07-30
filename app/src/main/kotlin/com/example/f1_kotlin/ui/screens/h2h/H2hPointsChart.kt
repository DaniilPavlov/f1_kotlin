package com.example.f1_kotlin.ui.screens.h2h

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.viewmodel.H2hPointsTimeline
import com.example.f1_kotlin.viewmodel.H2hTimelinePoint

@Composable
fun H2hPointsChart(
    timeline: H2hPointsTimeline,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
    height: Float = 220f,
) {
    val colors = appColors()
    val axisColor = colors.textGray
    val gridColor = colors.strokeGray.copy(alpha = 0.5f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
    ) {
        val points = timeline.points
        if (points.isEmpty()) return@Canvas

        val leftPad = 36.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val chartLeft = leftPad
        val chartTop = topPad
        val chartRight = size.width - rightPad
        val chartBottom = size.height - bottomPad
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val niceMax = niceCeil(max(timeline.maxCumulative, 1.0)).toFloat()
        val yTicks = 4

        for (i in 0..yTicks) {
            val t = i / yTicks.toFloat()
            val y = chartBottom - chartHeight * t
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1f)
            val label = formatPoints(niceMax * t)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                chartLeft - 4.dp.toPx(),
                y + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                },
            )
        }
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), strokeWidth = 1f)

        fun pointAt(index: Int, cumulative: Double): Offset {
            val x = if (points.size == 1) {
                chartLeft + chartWidth / 2f
            } else {
                chartLeft + chartWidth * (index / (points.size - 1).toFloat())
            }
            val y = chartBottom - chartHeight * (cumulative.toFloat() / niceMax)
            return Offset(x, y)
        }

                        fun drawSeries(color: Color, values: (H2hTimelinePoint) -> Double) {
            val path = Path()
            points.forEachIndexed { index, p ->
                val pt = pointAt(index, values(p))
                if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            points.forEachIndexed { index, p ->
                val pt = pointAt(index, values(p))
                drawCircle(color, radius = 3.dp.toPx(), center = pt)
            }
        }

        drawSeries(colorA) { it.cumulativeA }
        drawSeries(colorB) { it.cumulativeB }

        val labelStep = max(1, points.size / 6)
        points.forEachIndexed { index, p ->
            if (index % labelStep != 0 && index != points.lastIndex) return@forEachIndexed
            val x = pointAt(index, 0.0).x
            drawContext.canvas.nativeCanvas.drawText(
                p.label,
                x,
                size.height - 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 9.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                },
            )
        }
    }
}

private fun niceCeil(value: Double): Double {
    if (value <= 0) return 1.0
    val exp = kotlin.math.floor(kotlin.math.log10(value))
    val base = 10.0.pow(exp)
    val fraction = value / base
    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * base
}

private fun formatPoints(value: Float): String {
    return if (value >= 100) ceil(value.toDouble()).toInt().toString()
    else if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format("%.0f", value)
}
