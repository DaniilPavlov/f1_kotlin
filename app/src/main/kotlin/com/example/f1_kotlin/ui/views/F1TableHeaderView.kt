package com.example.f1_kotlin.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.f1_kotlin.R

/**
 * Классический Custom View (не Compose): рисует красную шапку таблицы через [Canvas].
 *
 * Пример для вакансии «Custom Views» — в остальном UI на Jetpack Compose.
 * Встраивается через [androidx.compose.ui.viewinterop.AndroidView] в [com.example.f1_kotlin.ui.components.TableHeaderRow].
 */
class F1TableHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.f1_red)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.f1_white)
        textSize = resources.displayMetrics.scaledDensity * 12f
        typeface = Typeface.DEFAULT
    }
    private var columns: List<String> = emptyList()
    private var weights: List<Float> = emptyList()
    private var maxLines = 1

    /**
     * @param columnWeights относительные ширины; пусто / другой размер — равные колонки.
     * Заголовки могут содержать `\n` (как во Flutter).
     */
    fun setColumns(values: List<String>, columnWeights: List<Float>? = null) {
        columns = values
        weights = if (columnWeights != null && columnWeights.size == values.size) {
            columnWeights
        } else {
            emptyList()
        }
        maxLines = columns.maxOfOrNull { it.split('\n').size }?.coerceAtLeast(1) ?: 1
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val lineHeight = textPaint.textSize * 1.15f
        val height = (paddingTop + paddingBottom + lineHeight * maxLines + textPaint.textSize * 0.6f).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height.coerceAtLeast((textPaint.textSize * 2.2f).toInt()))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (columns.isEmpty()) return

        val totalWeight = if (weights.isEmpty()) columns.size.toFloat() else weights.sum()
        val lineHeight = textPaint.textSize * 1.15f
        var x = 0f
        columns.forEachIndexed { index, title ->
            val w = if (weights.isEmpty()) 1f else weights[index]
            val columnWidth = width * (w / totalWeight)
            val maxWidth = (columnWidth - CELL_PADDING * 2).coerceAtLeast(0f)
            val lines = title.split('\n').map { ellipsize(it, maxWidth) }
            val blockHeight = lineHeight * lines.size
            var textY = (height - blockHeight) / 2f - textPaint.ascent()
            lines.forEach { line ->
                val textWidth = textPaint.measureText(line)
                val textX = x + ((columnWidth - textWidth) / 2f).coerceAtLeast(CELL_PADDING)
                canvas.drawText(line, textX, textY, textPaint)
                textY += lineHeight
            }
            x += columnWidth
        }
    }

    private fun ellipsize(text: String, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (textPaint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && textPaint.measureText(text, 0, end) + textPaint.measureText("…") > maxWidth) {
            end--
        }
        return if (end <= 0) "" else text.substring(0, end) + "…"
    }

    private companion object {
        const val CELL_PADDING = 4f
    }
}
