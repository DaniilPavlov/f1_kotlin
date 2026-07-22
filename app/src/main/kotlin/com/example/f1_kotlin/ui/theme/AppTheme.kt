package com.example.f1_kotlin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R

/** Брендовые цвета F1 — используются в Compose напрямую, не через colors.xml. */
val F1Black = Color(0xFF333333)
val F1Red = Color(0xFFE1271E)
val F1Pink = Color(0xFFF3B2AE)
val F1White = Color(0xFFFFFFFF)
val F1GrayBg = Color(0xFFF6F6F6)
val F1StrokeGray = Color(0xFFD8D8D8)
val F1TextGray = Color(0xFFB6B6B6)
val F1ShadowColor = Color(0xFFD7D7D7)
val F1ShimmerBase = Color(0xFFC8C8C8)
val F1ShimmerHighlight = Color(0xFFE0E0E0)

/** Шрифты из res/font — подключаются к TextStyle в Compose. */
val HelveticaBold = FontFamily(Font(R.font.helvetica_neue_cyr_bold, FontWeight.Bold))
val InterRegular = FontFamily(Font(R.font.inter_regular, FontWeight.Normal))

/**
 * Типографика приложения.
 * Composable'ы берут готовые стили, чтобы не дублировать fontSize/color.
 */
object AppStyles {
    val h1 = TextStyle(fontFamily = HelveticaBold, fontSize = 34.sp, color = Color.Black)
    val h2 = TextStyle(fontFamily = HelveticaBold, fontSize = 30.sp, color = Color.Black)
    val h3 = TextStyle(fontFamily = HelveticaBold, fontSize = 25.sp, color = Color.Black)
    val body = TextStyle(fontFamily = InterRegular, fontSize = 16.sp, lineHeight = 20.sp, color = Color.Black)
    val caption = TextStyle(fontFamily = InterRegular, fontSize = 12.sp, lineHeight = 14.sp, color = Color.Black)
    val navBar = TextStyle(fontFamily = InterRegular, fontSize = 10.sp, lineHeight = 12.sp, color = Color.Black)
}

/** Отступы по умолчанию. */
object AppDimens {
    const val horizontalPadding = 12f
    const val verticalPadding = 20f
}
