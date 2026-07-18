package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1_kotlin.F1Application
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1GrayBg
import com.example.f1_kotlin.ui.theme.F1Pink
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1StrokeGray
import com.example.f1_kotlin.ui.theme.F1White
import com.example.f1_kotlin.ui.views.F1TableHeaderView

/**
 * Верхняя панель приложения.
 *
 * Без параметров — логотип (на вкладках). С [title] и [onBack] — экран деталей со стрелкой назад.
 */
@Composable
fun F1AppBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val language by LocaleController.language.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Black)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.horizontalPadding.dp)
            .padding(top = 8.dp, bottom = 14.dp)
            .heightIn(min = 48.dp),
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = F1White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (title != null) {
            Text(
                text = title,
                style = AppStyles.body.copy(color = F1White),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .height(28.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = stringResource(
                    if (language == "en") R.string.locale_code_en else R.string.locale_code_ru,
                ),
                style = AppStyles.body.copy(color = F1White),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .clickable {
                        (context.applicationContext as? F1Application)?.toggleLocale()
                            ?: LocaleController.toggle(context)
                    }
                    .padding(8.dp),
            )
        }
    }
}

/** Центрированный индикатор загрузки в фирменном красном. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = F1Red)
    }
}

/**
 * Экран/блок ошибки: иллюстрация, текст, кнопка повтора.
 * Используется при сетевых сбоях и ошибках API.
 */
@Composable
fun ErrorBody(
    title: String?,
    subtitle: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.verticalPadding.dp),
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
            text = title ?: stringResource(R.string.no_connection),
            style = AppStyles.h2,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Text(
            text = subtitle ?: stringResource(R.string.no_connection_subtitle),
            style = AppStyles.h3,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        BlackButton(
            text = stringResource(R.string.refresh),
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = 50.dp),
        )
    }
}

/** Основная чёрная кнопка действия. [enabled] = false блокирует клик и меняет фон. */
@Composable
fun BlackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) F1Black else F1StrokeGray)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppStyles.body.copy(color = F1White))
    }
}

/**
 * Переключатель из двух вкладок (пилоты/конструкторы, карта/список).
 * [activeValue] — 0 или 1; подчёркивание красное у активной вкладки.
 */
@Composable
fun CustomSwitcher(
    firstTitle: String,
    secondTitle: String,
    activeValue: Int,
    onChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        SwitcherTab(firstTitle, activeValue == 0) { onChanged(0) }
        SwitcherTab(secondTitle, activeValue == 1) { onChanged(1) }
    }
}

@Composable
private fun RowScope.SwitcherTab(title: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppStyles.h3.copy(color = if (active) F1Red else F1Pink),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(15.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (active) F1Red else F1Pink),
        )
    }
}

/**
 * Красная шапка таблицы — мост между Compose и классическим [F1TableHeaderView].
 *
 * [AndroidView.factory] создаёт View один раз; [AndroidView.update] вызывается при
 * каждой рекомпозиции и передаёт новый список заголовков в [F1TableHeaderView.setColumns].
 */
@Composable
fun TableHeaderRow(cells: List<String>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context -> F1TableHeaderView(context) },
        update = { view -> view.setColumns(cells) },
    )
}

/**
 * Строка данных таблицы с зеброй (чётные строки на сером фоне).
 * [highlight] подсвечивает ячейки красным (например, лучший круг).
 */
@Composable
fun TableDataRow(
    cells: List<String>,
    index: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (index % 2 == 1) F1GrayBg else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        cells.forEachIndexed { cellIndex, cell ->
            Text(
                text = cell,
                style = if (highlight && cellIndex > 0) AppStyles.caption.copy(color = F1Red) else AppStyles.caption,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(F1StrokeGray),
    )
}

/** Кликабельный текст со подчёркиванием (ссылка на Wikipedia). */
@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = AppStyles.body.copy(textDecoration = TextDecoration.Underline),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
