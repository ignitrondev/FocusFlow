/*
 * Copyright 2026 IgnitronDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ignitrondev.focusflow.feature.statistics.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.DayFocus
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle

enum class SharePeriod { Today, Week }

// The card is an image for other people's feeds: fixed brand colors, independent of the app theme.
private val CardGradient = Brush.linearGradient(listOf(Color(0xFF151A5C), Color(0xFF3B44B8), Color(0xFF00605A)))
private val OnCard = Color.White
private val OnCardMuted = Color.White.copy(alpha = 0.72f)
private val BarTrack = Color.White.copy(alpha = 0.16f)
private val BarToday = Color(0xFF9EF2E6)

/**
 * A Strava-style summary of the user's focus, in a 4:5 portrait card (an Instagram feed post).
 * Rendered at a fixed font scale so the shared image always looks the same. Square-cornered and
 * full-bleed, as social apps expect; the preview rounds the corners on screen only.
 */
@Composable
fun FocusShareCard(statistics: FocusStatistics, period: SharePeriod, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        val locale = LocalConfiguration.current.locales[0]
        val focusMillis = if (period == SharePeriod.Today) statistics.today.focusMillis else statistics.weekFocusMillis
        val sessions = if (period == SharePeriod.Today) statistics.today.sessions else statistics.weekSessions
        val periodLabel = when (period) {
            SharePeriod.Today -> stringResource(R.string.share_period_today) + " · " +
                statistics.today.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

            SharePeriod.Week -> stringResource(R.string.share_period_week) + " · " + stringResource(
                R.string.share_card_week_range,
                statistics.weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale)),
                statistics.today.date.format(DateTimeFormatter.ofPattern("d MMM", locale)),
            )
        }
        Box(
            modifier
                .aspectRatio(4f / 5f)
                .background(CardGradient)
                .padding(24.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.app_name),
                        color = OnCard,
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(periodLabel, color = OnCardMuted, style = TextStyle(fontSize = 13.sp))
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.share_card_focus_time).uppercase(locale), color = OnCardMuted, style = LabelStyle)
                Text(
                    durationText(focusMillis),
                    color = OnCard,
                    style = TextStyle(fontSize = 50.sp, fontWeight = FontWeight.Bold, lineHeight = 54.sp, fontFeatureSettings = "tnum"),
                    maxLines = 1,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Metric(stringResource(R.string.share_card_sessions), sessions.toString())
                    if (period == SharePeriod.Week) {
                        Metric(stringResource(R.string.share_card_daily_average), durationText(statistics.weekDailyAverageMillis))
                    }
                    Metric(
                        stringResource(R.string.share_card_streak),
                        pluralStringResource(R.plurals.streak_days, statistics.currentStreakDays, statistics.currentStreakDays),
                    )
                }
                Spacer(Modifier.height(24.dp))
                WeekBars(statistics.lastSevenDays, locale)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.share_card_footer),
                    color = OnCardMuted,
                    style = TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private val LabelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = OnCardMuted, style = LabelStyle)
        Text(value, color = OnCard, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun WeekBars(days: List<DayFocus>, locale: java.util.Locale) {
    val max = days.maxOf { it.focusMillis }.coerceAtLeast(1)
    Column {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val slot = size.width / days.size
            val barWidth = slot * 0.42f
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            days.forEachIndexed { index, day ->
                val left = slot * index + (slot - barWidth) / 2
                drawRoundRect(BarTrack, Offset(left, 0f), Size(barWidth, size.height), radius)
                val height = (size.height * day.focusMillis / max).coerceAtLeast(if (day.focusMillis > 0) barWidth else 0f)
                if (height > 0f) {
                    drawRoundRect(
                        color = if (index == days.lastIndex) BarToday else OnCard,
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = OnCardMuted,
                    style = TextStyle(fontSize = 11.sp),
                )
            }
        }
    }
}

/** The FocusFlow mark (three-quarter ring and dot), as in the launcher icon. */
@Composable
private fun LogoMark(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val inset = stroke / 2
        drawArc(
            color = OnCard,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawCircle(OnCard, radius = size.minDimension * 0.12f, center = center)
    }
}

@Preview(widthDp = 360)
@Composable
private fun FocusShareCardPreview() {
    val today = LocalDate.of(2026, 11, 7)
    val days = remember { (6L downTo 0L).mapIndexed { i, ago -> DayFocus(today.minusDays(ago), (i % 4 + 1) * 25 * 60_000L, i % 4 + 1) } }
    FocusShareCard(
        modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(28.dp)),
        statistics = FocusStatistics(
            today = days.last(),
            weekFocusMillis = 450 * 60_000L,
            weekSessions = 18,
            weekDailyAverageMillis = 64 * 60_000L,
            averageSessionMillis = 25 * 60_000L,
            totalSessions = 40,
            totalFocusMillis = 1_000 * 60_000L,
            lastSevenDays = days,
            weekStart = today.minusDays(6),
            currentStreakDays = 5,
        ),
        period = SharePeriod.Week,
    )
}
