package com.example.f1_kotlin.ui.components.shimmer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.ui.theme.F1ShimmerBase
import com.example.f1_kotlin.ui.theme.F1ShimmerHighlight

internal val LocalShimmerBrush = staticCompositionLocalOf<Brush> {
    error("ScreenShimmer not provided")
}

@Composable
fun ScreenShimmer(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    val brush = Brush.linearGradient(
        colors = listOf(F1ShimmerBase, F1ShimmerHighlight, F1ShimmerBase),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 300f),
    )
    CompositionLocalProvider(LocalShimmerBrush provides brush) {
        content()
    }
}

@Composable
fun ShimmerSkeleton(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    width: Dp? = null,
    radius: Dp = 8.dp,
    color: Color? = null,
    leftPadding: Dp = 0.dp,
    rightPadding: Dp = 0.dp,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    val brush = if (color == null) LocalShimmerBrush.current else null
    Spacer(
        modifier = modifier
            .padding(start = leftPadding, end = rightPadding, top = topPadding, bottom = bottomPadding)
            .then(if (width != null) Modifier.width(width) else Modifier)
            .then(if (height != null) Modifier.height(height) else Modifier)
            .then(if (width == null && height != null) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(radius))
            .then(
                if (brush != null) Modifier.background(brush)
                else Modifier.background(color!!)
            ),
    )
}

@Composable
fun ShimmerTextLine(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 12.dp,
    bottomGap: Dp = 12.dp,
    radius: Dp = 4.dp,
) {
    ShimmerSkeleton(
        height = height,
        width = width,
        radius = radius,
        bottomPadding = bottomGap,
        modifier = modifier,
    )
}
