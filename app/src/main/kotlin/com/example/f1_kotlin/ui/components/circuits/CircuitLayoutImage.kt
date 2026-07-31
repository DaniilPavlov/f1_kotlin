package com.example.f1_kotlin.ui.components.circuits

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.data.circuits.CircuitLayoutAssets
import com.example.f1_kotlin.ui.theme.appColors

/** Локальная схема трассы; при отсутствии ассета — ничего не рисует. Tint по умолчанию — ink. */
@Composable
fun CircuitLayoutImage(
    circuitId: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    tint: Color? = null,
    padding: Dp = 12.dp,
) {
    val path = CircuitLayoutAssets.assetPath(circuitId) ?: return
    val context = LocalContext.current
    val resolvedTint = tint ?: appColors().black
    val bitmap = remember(path) {
        runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    } ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(padding),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(height),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(resolvedTint),
        )
    }
}
