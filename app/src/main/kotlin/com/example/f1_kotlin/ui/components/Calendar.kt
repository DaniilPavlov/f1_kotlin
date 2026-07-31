package com.example.f1_kotlin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.domain.LocaleController
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.ui.theme.F1Black
import com.example.f1_kotlin.ui.theme.F1OnChrome
import com.example.f1_kotlin.ui.theme.F1Red
import com.example.f1_kotlin.ui.theme.F1White
import com.example.f1_kotlin.ui.theme.appColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Кастомный месячный календарь для расписания F1.
 *
 * Не используем готовый Material DatePicker — нужны иконки на днях с сессиями/гонками.
 * Сетка строится вручную: считаем смещение первого дня месяца и заполняем ячейки.
 *
 * @param logoForDay колбэк из ViewModel: какую иконку показать на дате (или null)
 */
@Composable
fun F1Calendar(
    selectedDate: LocalDate,
    focusedMonth: YearMonth,
    userPickedDay: Boolean,
    logoForDay: (LocalDate) -> Int?,
    onDaySelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = appColors()
    val language by LocaleController.language.collectAsState()
    val locale = remember(language) { LocaleController.currentLocale() }
    val daysInMonth = focusedMonth.lengthOfMonth()
    val firstDayOfMonth = focusedMonth.atDay(1)
    // Смещение: календарь начинается с понедельника (ISO), не с воскресенья
    val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(colors.shadowColor)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonthChanged(focusedMonth.minusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = colors.black,
                )
            }
            Text(
                text = focusedMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
                    .replaceFirstChar { it.titlecase(locale) } + " ${focusedMonth.year}",
                style = AppStyles.body,
            )
            IconButton(onClick = { onMonthChanged(focusedMonth.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.black,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    style = AppStyles.body,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        var dayCounter = 1
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < startOffset || dayCounter > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = focusedMonth.atDay(dayCounter)
                        val today = date == LocalDate.now()
                        val selected = date == selectedDate && (!today || userPickedDay)
                        CalendarDay(
                            date = date,
                            selected = selected,
                            today = today,
                            logoRes = logoForDay(date),
                            onClick = { onDaySelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

/**
 * Одна ячейка дня в календаре.
 * Сегодня — красный круг; выбранный день (в т.ч. сегодня после клика) — белый.
 */
@Composable
private fun CalendarDay(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    logoRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selection / today pills stay fixed contrast (true white / red), not theme surfaces.
    val background = when {
        selected -> F1White
        today -> F1Red
        else -> Color.Transparent
    }
    val textStyle = when {
        selected -> AppStyles.body.copy(color = F1Black)
        today -> AppStyles.body.copy(color = F1OnChrome)
        else -> AppStyles.body
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(text = date.dayOfMonth.toString(), style = textStyle)
        }
    }
}
