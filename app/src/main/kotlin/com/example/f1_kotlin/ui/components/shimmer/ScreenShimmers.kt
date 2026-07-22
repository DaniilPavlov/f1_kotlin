package com.example.f1_kotlin.ui.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.F1ShimmerBase
import com.example.f1_kotlin.ui.theme.F1ShimmerHighlight
import com.example.f1_kotlin.ui.theme.F1StrokeGray

@Composable
fun CareerScreenShimmer(showPhoto: Boolean = true, modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.horizontalPadding.dp,
                    vertical = AppDimens.verticalPadding.dp,
                ),
        ) {
            if (showPhoto) {
                ShimmerSkeleton(
                    radius = 20.dp,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f),
                )
                Spacer(Modifier.height(20.dp))
            }
            ShimmerTextLine(height = 24.dp, width = 200.dp, bottomGap = 16.dp)
            ShimmerTextLine(width = 160.dp)
            ShimmerTextLine(width = 200.dp)
            ShimmerTextLine(width = 120.dp, bottomGap = 28.dp)
            ShimmerTextLine(height = 18.dp, width = 100.dp, bottomGap = 16.dp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerSkeleton(height = 72.dp, radius = 12.dp, modifier = Modifier.weight(1f))
                ShimmerSkeleton(height = 72.dp, radius = 12.dp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerSkeleton(height = 72.dp, radius = 12.dp, modifier = Modifier.weight(1f))
                ShimmerSkeleton(height = 72.dp, radius = 12.dp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            ShimmerTextLine(height = 18.dp, width = 120.dp, bottomGap = 14.dp)
            repeat(4) {
                ShimmerSkeleton(height = 44.dp, bottomPadding = 12.dp)
            }
        }
    }
}

@Composable
fun TournamentTablesShimmer(showHeader: Boolean = true, modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.horizontalPadding.dp),
        ) {
            if (showHeader) {
                Spacer(Modifier.height(AppDimens.verticalPadding.dp))
                ShimmerTextLine(height = 24.dp, width = 180.dp, bottomGap = 24.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShimmerTextLine(height = 16.dp, width = 100.dp, bottomGap = 0.dp)
                    ShimmerTextLine(height = 16.dp, width = 80.dp, bottomGap = 0.dp)
                }
                Spacer(Modifier.height(24.dp))
            }
            ShimmerSkeleton(height = 40.dp, radius = 10.dp, bottomPadding = 16.dp)
            repeat(10) {
                ShimmerSkeleton(height = 40.dp, bottomPadding = 12.dp)
            }
        }
    }
}

@Composable
fun ScheduleShimmer(modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.horizontalPadding.dp,
                    vertical = AppDimens.verticalPadding.dp,
                ),
        ) {
            ShimmerSkeleton(height = 280.dp, radius = 16.dp)
            Spacer(Modifier.height(AppDimens.verticalPadding.dp))
            ShimmerSkeleton(height = 200.dp, radius = 20.dp)
        }
    }
}

@Composable
fun LastRaceSectionShimmer(modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(modifier = modifier.padding(top = AppDimens.verticalPadding.dp)) {
            Column(modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp)) {
                ShimmerTextLine(
                    height = 18.dp,
                    width = 220.dp,
                    bottomGap = 0.dp,
                    modifier = Modifier.padding(vertical = AppDimens.verticalPadding.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ShimmerTextLine(height = 16.dp, width = 100.dp, bottomGap = 0.dp)
                    ShimmerTextLine(height = 16.dp, width = 80.dp, bottomGap = 0.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            repeat(3) {
                ShimmerSkeleton(
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = AppDimens.horizontalPadding.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun RaceInfoShimmer(modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.horizontalPadding.dp,
                    vertical = AppDimens.verticalPadding.dp,
                ),
        ) {
            ShimmerTextLine(height = 20.dp, width = 200.dp, bottomGap = 14.dp)
            ShimmerTextLine(width = 140.dp, bottomGap = 24.dp)
            ShimmerTextLine(height = 16.dp, width = 100.dp, bottomGap = 14.dp)
            repeat(8) { ShimmerSkeleton(height = 36.dp, bottomPadding = 12.dp) }
            Spacer(Modifier.height(12.dp))
            ShimmerTextLine(height = 16.dp, width = 120.dp, bottomGap = 14.dp)
            repeat(6) { ShimmerSkeleton(height = 36.dp, bottomPadding = 12.dp) }
        }
    }
}

@Composable
fun WeekendScoreboardSectionShimmer(modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier.padding(
                start = AppDimens.horizontalPadding.dp,
                end = AppDimens.horizontalPadding.dp,
                top = AppDimens.verticalPadding.dp,
            ),
        ) {
            ShimmerSkeleton(height = 34.dp, width = 160.dp, radius = 4.dp)
            Spacer(Modifier.height(16.dp))
            WeekendScoreboardShimmer()
        }
    }
}

@Composable
fun WeekendScoreboardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, F1StrokeGray, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ShimmerSkeleton(height = 24.dp, radius = 4.dp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ShimmerSkeleton(height = 24.dp, width = 72.dp)
        }
        Spacer(Modifier.height(8.dp))
        ShimmerSkeleton(height = 20.dp, width = 220.dp, radius = 4.dp)
        Spacer(Modifier.height(6.dp))
        Row {
            ShimmerSkeleton(height = 20.dp, width = 28.dp, radius = 4.dp)
            Spacer(Modifier.width(8.dp))
            ShimmerSkeleton(height = 14.dp, radius = 4.dp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(F1ShimmerBase)
                .padding(12.dp),
        ) {
            Row {
                ShimmerSkeleton(
                    height = 20.dp,
                    radius = 4.dp,
                    color = F1ShimmerHighlight,
                    modifier = Modifier.weight(1f),
                )
                ShimmerSkeleton(height = 20.dp, width = 20.dp, radius = 4.dp, color = F1ShimmerHighlight)
            }
            Spacer(Modifier.height(4.dp))
            ShimmerSkeleton(height = 14.dp, width = 140.dp, radius = 4.dp, color = F1ShimmerHighlight)
            Spacer(Modifier.height(6.dp))
            ShimmerSkeleton(height = 20.dp, width = 200.dp, radius = 4.dp, color = F1ShimmerHighlight)
        }
        Spacer(Modifier.height(14.dp))
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                ShimmerSkeleton(height = 14.dp, width = 48.dp, radius = 4.dp)
                Spacer(Modifier.width(8.dp))
                ShimmerSkeleton(height = 14.dp, radius = 4.dp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                ShimmerSkeleton(height = 14.dp, width = 56.dp, radius = 4.dp)
                Spacer(Modifier.width(4.dp))
                ShimmerSkeleton(height = 16.dp, width = 16.dp, radius = 4.dp)
            }
        }
    }
}

@Composable
fun NewsListShimmer(itemCount: Int = 4, modifier: Modifier = Modifier) {
    ScreenShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppDimens.horizontalPadding.dp,
                    end = AppDimens.horizontalPadding.dp,
                    top = 12.dp,
                    bottom = AppDimens.verticalPadding.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(itemCount) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, F1StrokeGray, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    ShimmerSkeleton(
                        radius = 0.dp,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        ShimmerTextLine(height = 16.dp, width = 220.dp, bottomGap = 14.dp)
                        ShimmerTextLine()
                        ShimmerTextLine(width = 180.dp)
                        ShimmerTextLine(height = 10.dp, width = 100.dp, bottomGap = 0.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ListRowsShimmer(
    rowCount: Int = 12,
    modifier: Modifier = Modifier,
) {
    ScreenShimmer {
        Column(
            modifier = modifier.padding(
                horizontal = AppDimens.horizontalPadding.dp,
                vertical = 12.dp,
            ),
        ) {
            repeat(rowCount) {
                ShimmerSkeleton(height = 44.dp, radius = 10.dp, bottomPadding = 12.dp)
            }
        }
    }
}

@Composable
fun CircuitsShimmer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.height(12.dp))
        ScreenShimmer {
            ShimmerSkeleton(
                height = 40.dp,
                radius = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.horizontalPadding.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        ListRowsShimmer(modifier = Modifier.fillMaxWidth())
    }
}
