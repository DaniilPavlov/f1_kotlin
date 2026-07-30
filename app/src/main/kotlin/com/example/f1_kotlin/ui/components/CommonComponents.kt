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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1_kotlin.F1Application
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.analytics.AnalyticsEvent
import com.example.f1_kotlin.di.AppEntryPoint
import com.example.f1_kotlin.domain.AppThemePreference
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.domain.ThemeController
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Chrome
import com.example.f1_kotlin.ui.theme.F1OnChrome
import com.example.f1_kotlin.ui.theme.F1Pink
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.appColors
import com.example.f1_kotlin.ui.views.F1TableHeaderView
import dagger.hilt.android.EntryPointAccessors

/**
 * Верхняя панель приложения.
 *
 * Без параметров — логотип (на вкладках). С [title] и [onBack] — экран деталей со стрелкой назад.
 */
@Composable
fun F1AppBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(F1Chrome)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.horizontalPadding.dp)
            .padding(top = 8.dp, bottom = 14.dp)
            .heightIn(min = 48.dp),
    ) {
        if (onBack != null) {
            F1AppBarIconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = F1OnChrome,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (title != null) {
            Text(
                text = title,
                style = AppStyles.body.copy(color = F1OnChrome),
                modifier = Modifier.align(Alignment.Center),
            )
            if (onShare != null) {
                F1AppBarIconButton(
                    onClick = onShare,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = F1OnChrome,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .height(28.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
            F1AppBarHomeActions(modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun F1AppBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun F1AppBarHomeActions(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val language by LocaleController.language.collectAsState()
    val themePreference by ThemeController.preference.collectAsState()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = when (themePreference) {
                AppThemePreference.System -> Icons.Filled.BrightnessAuto
                AppThemePreference.Light -> Icons.Filled.LightMode
                AppThemePreference.Dark -> Icons.Filled.DarkMode
            },
            contentDescription = stringResource(R.string.theme_toggle),
            tint = F1OnChrome,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable {
                    val next = ThemeController.cycle(context)
                    runCatching {
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            AppEntryPoint::class.java,
                        ).analyticsGateway().log(
                            AnalyticsEvent.ThemeChanged(
                                when (next) {
                                    AppThemePreference.System -> "system"
                                    AppThemePreference.Light -> "light"
                                    AppThemePreference.Dark -> "dark"
                                },
                            ),
                        )
                    }
                }
                .padding(6.dp),
        )
        Text(
            text = stringResource(
                if (language == "en") R.string.locale_code_en else R.string.locale_code_ru,
            ),
            style = AppStyles.body.copy(color = F1OnChrome),
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    val next = (context.applicationContext as? F1Application)?.toggleLocale()
                        ?: LocaleController.toggle(context)
                    runCatching {
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            AppEntryPoint::class.java,
                        ).analyticsGateway().log(AnalyticsEvent.LocaleChanged(next))
                    }
                }
                .padding(8.dp),
        )
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
 *
 * GoF Behavioral Command — действие повтора инкапсулировано в [onRetry]
 * (`refreshAll` / reload); вид только вызывает команду.
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
            style = AppStyles.h2.copy(color = appColors().black),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Text(
            text = subtitle ?: stringResource(R.string.no_connection_subtitle),
            style = AppStyles.h3.copy(color = appColors().black),
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
    val colors = appColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) colors.black else colors.strokeGray)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppStyles.body.copy(color = colors.white))
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
 * @param weights относительные ширины колонок (как [Modifier.weight]); `null` — равные.
 * Заголовки могут содержать `\n`.
 */
@Composable
fun TableHeaderRow(
    cells: List<String>,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context -> F1TableHeaderView(context) },
        update = { view -> view.setColumns(cells, weights) },
    )
}

/** Ячейка строки таблицы. */
sealed interface TableCell {
    data class Text(val value: String, val color: Color? = null) : TableCell
    data class Flag(val countryOrNationality: String) : TableCell
    data class PlaceAndName(
        val place: String,
        val name: String,
        val placeColor: Color? = null,
    ) : TableCell
}

/**
 * Строка данных таблицы с зеброй.
 * [weights] — относительные ширины; `null` — равные колонки.
 */
@Composable
fun TableDataRow(
    cells: List<TableCell>,
    index: Int,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = appColors()
    val rowDescription = cells.joinToString(separator = ", ") { cell ->
        when (cell) {
            is TableCell.Text -> cell.value
            is TableCell.Flag -> cell.countryOrNationality
            is TableCell.PlaceAndName -> "${cell.place} ${cell.name.replace('\n', ' ')}"
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (index % 2 == 1) colors.grayBg else Color.Transparent)
            .semantics { contentDescription = rowDescription }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { cellIndex, cell ->
            val weight = weights?.getOrNull(cellIndex) ?: 1f
            Box(
                modifier = Modifier.weight(weight),
                contentAlignment = Alignment.Center,
            ) {
                when (cell) {
                    is TableCell.Text -> Text(
                        text = cell.value,
                        style = if (cell.color != null) {
                            AppStyles.caption.copy(color = cell.color)
                        } else {
                            AppStyles.caption.copy(color = colors.black)
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    is TableCell.Flag -> CountryFlag(countryOrNationality = cell.countryOrNationality)
                    is TableCell.PlaceAndName -> PlaceAndNameContent(cell)
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.strokeGray),
    )
}

@Composable
private fun PlaceAndNameContent(cell: TableCell.PlaceAndName) {
    val colors = appColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = cell.place,
            style = if (cell.placeColor != null) {
                AppStyles.caption.copy(color = cell.placeColor)
            } else {
                AppStyles.caption.copy(color = colors.black)
            },
        )
        Text(
            text = cell.name,
            style = AppStyles.caption.copy(color = colors.black),
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Удобный overload: все ячейки — строки; [flagCellIndices] → флаги. */
@Composable
fun TableDataRow(
    cells: List<String>,
    index: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    flagCellIndices: Set<Int> = emptySet(),
    weights: List<Float>? = null,
    onClick: (() -> Unit)? = null,
) {
    TableDataRow(
        cells = cells.mapIndexed { i, value ->
            when {
                i in flagCellIndices -> TableCell.Flag(value)
                highlight && i > 0 -> TableCell.Text(value, color = F1Red)
                else -> TableCell.Text(value)
            }
        },
        index = index,
        modifier = modifier,
        weights = weights,
        onClick = onClick,
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
