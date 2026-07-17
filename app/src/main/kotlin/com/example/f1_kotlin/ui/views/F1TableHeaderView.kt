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

    /**
     * Обновляет заголовки колонок. [requestLayout] + [invalidate] перерисуют View
     * при следующем кадре — вызывается из Compose `update` блока [AndroidView].
     */
    fun setColumns(values: List<String>) {
        columns = values
        requestLayout()
        invalidate()
    }

    /** Высота строки зависит от размера текста и padding. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (paddingTop + paddingBottom + textPaint.textSize * 2.2f).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    /** Рисует красный фон и текст колонок с равной шириной ячеек. */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (columns.isEmpty()) return

        val columnWidth = width.toFloat() / columns.size
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        columns.forEachIndexed { index, title ->
            val x = columnWidth * index + CELL_PADDING
            val maxWidth = columnWidth - CELL_PADDING * 2
            val clipped = ellipsize(title, maxWidth)
            canvas.drawText(clipped, x, textY, textPaint)
        }
    }

    /** Обрезает длинный текст многоточием, если не помещается в колонку. */
    private fun ellipsize(text: String, maxWidth: Float): String {
        if (textPaint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && textPaint.measureText(text, 0, end) + textPaint.measureText("…") > maxWidth) {
            end--
        }
        return if (end <= 0) "" else text.substring(0, end) + "…"
    }

    private companion object {
        const val CELL_PADDING = 8f
    }
}
