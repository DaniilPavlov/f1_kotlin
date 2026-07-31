package com.example.f1_kotlin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R

/** Semantic palette — in dark mode [black] is light ink and [white] is dark surface. */
data class AppColors(
    val black: Color,
    val textGray: Color,
    val grayBg: Color,
    val shadowColor: Color,
    val strokeGray: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val pink: Color,
    val white: Color,
    val red: Color,
) {
    companion object {
        val Light = AppColors(
            black = Color(0xFF333333),
            textGray = Color(0xFFB6B6B6),
            grayBg = Color(0xFFF6F6F6),
            shadowColor = Color(0xFFD7D7D7),
            strokeGray = Color(0xFFD8D8D8),
            shimmerBase = Color(0xFFC8C8C8),
            shimmerHighlight = Color(0xFFE0E0E0),
            pink = Color(0xFFF3B2AE),
            white = Color(0xFFFFFFFF),
            red = Color(0xFFE1271E),
        )

        val Dark = AppColors(
            black = Color(0xFFE8E8E8),
            textGray = Color(0xFF9A9A9A),
            grayBg = Color(0xFF1E1E1E),
            shadowColor = Color(0xFF2A2A2A),
            strokeGray = Color(0xFF3A3A3A),
            shimmerBase = Color(0xFF2C2C2C),
            shimmerHighlight = Color(0xFF404040),
            pink = Color(0xFFC48B87),
            white = Color(0xFF121212),
            red = Color(0xFFE1271E),
        )
    }
}

val LocalAppColors = staticCompositionLocalOf { AppColors.Light }

@Composable
@ReadOnlyComposable
fun appColors(): AppColors = LocalAppColors.current

/** Chrome (app bar / nav) stays F1 black regardless of theme. */
val F1Chrome = Color(0xFF333333)
val F1OnChrome = Color(0xFFFFFFFF)

/**
 * Fixed light-palette aliases for share cards, red/chrome overlays, and other
 * surfaces that must stay light regardless of app theme.
 */
val F1Black = AppColors.Light.black
val F1Red = AppColors.Light.red
val F1Pink = AppColors.Light.pink
val F1White = AppColors.Light.white
val F1GrayBg = AppColors.Light.grayBg
val F1StrokeGray = AppColors.Light.strokeGray
val F1TextGray = AppColors.Light.textGray
val F1ShadowColor = AppColors.Light.shadowColor
val F1ShimmerBase = AppColors.Light.shimmerBase
val F1ShimmerHighlight = AppColors.Light.shimmerHighlight

val HelveticaBold = FontFamily(Font(R.font.helvetica_neue_cyr_bold, FontWeight.Bold))
val InterRegular = FontFamily(Font(R.font.inter_regular, FontWeight.Normal))

/** Typography — ink color follows [appColors].black (inverts in dark mode). */
object AppStyles {
    val h1: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(fontFamily = HelveticaBold, fontSize = 34.sp, color = appColors().black)

    val h2: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(fontFamily = HelveticaBold, fontSize = 30.sp, color = appColors().black)

    val h3: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(fontFamily = HelveticaBold, fontSize = 25.sp, color = appColors().black)

    val body: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = InterRegular,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = appColors().black,
        )

    val caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = InterRegular,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = appColors().black,
        )

    val navBar: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = TextStyle(
            fontFamily = InterRegular,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = appColors().black,
        )
}

@Composable
@ReadOnlyComposable
fun themedH1() = AppStyles.h1

@Composable
@ReadOnlyComposable
fun themedH2() = AppStyles.h2

@Composable
@ReadOnlyComposable
fun themedH3() = AppStyles.h3

@Composable
@ReadOnlyComposable
fun themedBody() = AppStyles.body

@Composable
@ReadOnlyComposable
fun themedCaption() = AppStyles.caption

@Composable
@ReadOnlyComposable
fun themedNavBar() = AppStyles.navBar

object AppDimens {
    const val horizontalPadding = 12f
    const val verticalPadding = 20f
}
