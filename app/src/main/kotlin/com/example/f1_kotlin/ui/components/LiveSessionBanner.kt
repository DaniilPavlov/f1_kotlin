package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.live.LiveWeekendController
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1OnChrome
import com.example.f1_kotlin.ui.theme.F1Red

/** Красный баннер live-сессии ESPN; тап ведёт на Results. */
@Composable
fun LiveSessionBanner(
    controller: LiveWeekendController,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreboard by controller.scoreboard.collectAsState()
    val live = scoreboard?.isLive == true
    if (!live) return
    val abbr = scoreboard?.highlightedSession?.abbreviation?.takeIf { it.isNotBlank() }
    val label = if (abbr == null) {
        stringResource(R.string.live_session_banner)
    } else {
        stringResource(R.string.live_session_banner_with_session, abbr)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(F1Red)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppStyles.body.copy(color = F1OnChrome),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = F1OnChrome,
        )
    }
}
