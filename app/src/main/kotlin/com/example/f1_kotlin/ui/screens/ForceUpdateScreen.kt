package com.example.f1_kotlin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.F1Application
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.components.BlackButton
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.util.openUrl

private const val GITHUB_RELEASES_URL = "https://github.com/DaniilPavlov/f1_kotlin/releases"

/** Блокирующий экран: версия ниже минимума из Remote Config. */
@Composable
fun ForceUpdateScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val language by LocaleController.language.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = AppDimens.horizontalPadding.dp,
                vertical = AppDimens.verticalPadding.dp,
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    if (language == "en") R.string.locale_code_ru else R.string.locale_code_en,
                ),
                style = AppStyles.body.copy(color = F1Black, fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable {
                        (context.applicationContext as? F1Application)?.toggleLocale()
                    }
                    .padding(8.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.error_car),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(180.dp),
                contentScale = ContentScale.FillWidth,
            )
            Text(
                text = stringResource(R.string.force_update_title),
                style = AppStyles.h2,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            Text(
                text = stringResource(R.string.force_update_subtitle),
                style = AppStyles.h3,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            BlackButton(
                text = stringResource(R.string.force_update_button),
                onClick = { openUrl(context, GITHUB_RELEASES_URL) },
                modifier = Modifier.padding(horizontal = 50.dp),
            )
        }
    }
}
