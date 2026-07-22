package com.example.f1_kotlin.ui.components.circuits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.circuits.CircuitStats
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1TextGray
import com.example.f1_kotlin.ui.theme.HelveticaBold

/** Сетка характеристик трассы: длина, круги, повороты, скорость, перепад. */
@Composable
fun CircuitStatsGrid(
    stats: CircuitStats,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        stringResource(R.string.circuit_stat_length) to stats.lengthLabel,
        stringResource(R.string.circuit_stat_laps) to stats.lapsLabel,
        stringResource(R.string.circuit_stat_turns) to stats.turnsLabel,
        stringResource(R.string.circuit_stat_top_speed) to stats.topSpeedLabel,
        stringResource(R.string.circuit_stat_elevation) to stats.elevationLabel,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, F1Red, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(F1StrokeGray),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    style = AppStyles.body.copy(
                        fontFamily = HelveticaBold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = AppStyles.caption.copy(color = F1TextGray),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
