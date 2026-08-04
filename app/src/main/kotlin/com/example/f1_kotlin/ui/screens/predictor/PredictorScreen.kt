package com.example.f1_kotlin.ui.screens.predictor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles

/** Placeholder вкладки «Предикт» до этапа local/cloud predictor. */
@Composable
fun PredictorScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.horizontalPadding.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.predictor_placeholder),
            style = AppStyles.body,
            textAlign = TextAlign.Center,
        )
    }
}
