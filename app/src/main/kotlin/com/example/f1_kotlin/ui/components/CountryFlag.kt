package com.example.f1_kotlin.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.util.CountryFlagCodes

/** Country / nationality Jolpica string → flag emoji (or fallback text). */
@Composable
fun CountryFlag(
    countryOrNationality: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    fallbackStyle: TextStyle? = null,
    showFallbackText: Boolean = true,
) {
    val raw = countryOrNationality?.trim().orEmpty()
    val emoji = CountryFlagCodes.emojiFor(raw)
    if (emoji != null) {
        Text(
            text = emoji,
            style = TextStyle(fontSize = fontSize, lineHeight = fontSize),
            modifier = modifier.semantics { contentDescription = raw.ifEmpty { emoji } },
        )
        return
    }
    if (!showFallbackText || raw.isEmpty()) return
    Text(
        text = raw,
        style = fallbackStyle ?: AppStyles.caption.copy(color = appColors().textGray),
        modifier = modifier,
    )
}
