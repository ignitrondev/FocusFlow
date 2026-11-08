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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.DayFocus
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.feature.statistics.weekRangeLabel
import com.ignitrondev.focusflow.platform.DateFormats
import com.ignitrondev.focusflow.platform.LocalDateFormats
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.app_name
import com.ignitrondev.focusflow.resources.share_card_daily_average
import com.ignitrondev.focusflow.resources.share_card_empty
import com.ignitrondev.focusflow.resources.share_card_focus_time
import com.ignitrondev.focusflow.resources.share_card_footer
import com.ignitrondev.focusflow.resources.share_card_sessions
import com.ignitrondev.focusflow.resources.share_card_streak
import com.ignitrondev.focusflow.resources.share_period_today
import com.ignitrondev.focusflow.resources.share_period_week
import com.ignitrondev.focusflow.resources.streak_days
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

enum class SharePeriod { Today, Week }

/**
 * Where the card is going: an Instagram feed post (4:5, 1080x1350) or a full-screen story (9:16,
 * 1080x1920). A story fills the screen, but Instagram draws the account name and music over the
 * top and the reply bar over the bottom, so the content keeps clear of those areas (fractions of
 * the width, measured on a 1080 px story).
 */
enum class ShareFormat(val aspectRatio: Float, val topSafeArea: Float, val bottomSafeArea: Float) {
    Post(aspectRatio = 4f / 5f, topSafeArea = 0f, bottomSafeArea = 0f),
    Story(aspectRatio = 9f / 16f, topSafeArea = 270f / 1080f, bottomSafeArea = 340f / 1080f),
}

// The card is an image for other people's feeds: fixed brand colors, independent of the app theme.
private val CardGradient = Brush.linearGradient(listOf(Color(0xFF151A5C), Color(0xFF3B44B8), Color(0xFF00605A)))
private val OnCard = Color.White
private val OnCardMuted = Color.White.copy(alpha = 0.72f)
private val BarTrack = Color.White.copy(alpha = 0.16f)
private val BarToday = Color(0xFF9EF2E6)
private val PhotoScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.55f),
    0.2f to Color.Black.copy(alpha = 0.15f),
    0.35f to Color.Black.copy(alpha = 0.35f),
    0.6f to Color.Black.copy(alpha = 0.65f),
    1f to Color.Black.copy(alpha = 0.85f),
)
private val PhotoTextShadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 12f)
private val LocalCardTextShadow = staticCompositionLocalOf<Shadow?> { null }

@Composable
private fun TextStyle.onCard(): TextStyle = copy(shadow = LocalCardTextShadow.current)

/**
 * A Strava-style summary of the user's focus, in a 4:5 portrait card (an Instagram feed post).
 * Rendered at a fixed font scale so the shared image always looks the same. Square-cornered and
 * full-bleed, as social apps expect; the preview rounds the corners on screen only.
 */
@Composable
fun FocusShareCard(
    statistics: FocusStatistics,
    period: SharePeriod,
    modifier: Modifier = Modifier,
    role: String = "",
    photo: ImageBitmap? = null,
    format: ShareFormat = ShareFormat.Post,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1f),
        // A soft shadow under the text only when it sits on a photo, where the background is unknown.
        LocalCardTextShadow provides if (photo != null) PhotoTextShadow else null,
    ) {
        val dates = LocalDateFormats.current
        val focusMillis = if (period == SharePeriod.Today) statistics.today.focusMillis else statistics.weekFocusMillis
        val sessions = if (period == SharePeriod.Today) statistics.today.sessions else statistics.weekSessions
        val periodLabel = when (period) {
            SharePeriod.Today -> stringResource(Res.string.share_period_today) + " · " +
                dates.mediumDate(statistics.today.date)

            SharePeriod.Week -> stringResource(Res.string.share_period_week) + " · " +
                weekRangeLabel(statistics.weekStart, statistics.weekEnd, dates)
        }
        val awaitingFirstSession = focusMillis == 0L
        BoxWithConstraints(modifier.aspectRatio(format.aspectRatio).background(CardGradient)) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Keeps white text readable on any photo: darker at the top (header) and much
                // darker at the bottom, where the numbers are.
                Box(Modifier.fillMaxSize().background(PhotoScrim))
            }
            Column(
                Modifier.fillMaxSize().padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp + maxWidth * format.topSafeArea,
                    bottom = 24.dp + maxWidth * format.bottomSafeArea,
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.app_name),
                        color = OnCard,
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold).onCard(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(periodLabel, color = OnCardMuted, style = TextStyle(fontSize = 13.sp).onCard())
                Spacer(Modifier.weight(1f))
                if (role.isNotBlank()) {
                    Text(
                        role.trim(),
                        color = OnCard,
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp).onCard(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (awaitingFirstSession) {
                    Text(
                        stringResource(Res.string.share_card_empty),
                        color = OnCard,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Center,
                        ).onCard(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("shareCardEmpty"),
                    )
                    Spacer(Modifier.weight(1f))
                } else {
                    Text(stringResource(Res.string.share_card_focus_time).uppercase(), color = OnCardMuted, style = LabelStyle.onCard())
                    FittedCardText(durationText(focusMillis), FocusTimeStyle.onCard(), OnCard, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    // Equal columns: with three metrics the last one used to be squeezed until its
                    // label broke mid-word ("STRE / AK") and its value lost a word ("14").
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Metric(stringResource(Res.string.share_card_sessions), sessions.toString(), Modifier.weight(1f))
                        if (period == SharePeriod.Week) {
                            Metric(
                                stringResource(Res.string.share_card_daily_average),
                                durationText(statistics.weekDailyAverageMillis),
                                Modifier.weight(1f),
                            )
                        }
                        Metric(
                            stringResource(Res.string.share_card_streak),
                            pluralStringResource(Res.plurals.streak_days, statistics.currentStreakDays, statistics.currentStreakDays),
                            Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    WeekBars(statistics.weekDays, statistics.today.date, dates)
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    stringResource(Res.string.share_card_footer),
                    color = OnCardMuted,
                    style = TextStyle(fontSize = 12.sp).onCard(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private val FocusTimeStyle = TextStyle(fontSize = 50.sp, fontWeight = FontWeight.Bold, lineHeight = 54.sp, fontFeatureSettings = "tnum")
private val LabelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        FittedCardText(label.uppercase(), LabelStyle.onCard(), OnCardMuted)
        FittedCardText(value, MetricValueStyle.onCard(), OnCard)
    }
}

private val MetricValueStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

/**
 * One line that shrinks to fit its width. A plain `Text` with `maxLines = 1` hides the words that
 * wrap ("9 h 35 min" showed as "9 h 35"), and without it a long word breaks mid-word.
 */
@Composable
private fun FittedCardText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val maxSize = style.fontSize
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = maxSize),
    )
}

@Composable
private fun WeekBars(days: List<DayFocus>, today: LocalDate, dates: DateFormats) {
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
                        color = if (day.date == today) BarToday else OnCard,
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
                    dates.weekdayNarrow(day.date.dayOfWeek),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = OnCardMuted,
                    style = TextStyle(fontSize = 11.sp).onCard(),
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
    val weekStart = LocalDate(2026, 11, 2)
    val today = LocalDate(2026, 11, 7)
    val days = remember {
        (0..6).map { offset ->
            DayFocus(weekStart.plus(offset, DateTimeUnit.DAY), (offset % 4 + 1) * 25 * 60_000L, offset % 4 + 1)
        }
    }
    FocusShareCard(
        modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(28.dp)),
        statistics = FocusStatistics(
            today = days.first { it.date == today },
            weekFocusMillis = 450 * 60_000L,
            weekSessions = 18,
            weekDailyAverageMillis = 64 * 60_000L,
            averageSessionMillis = 25 * 60_000L,
            totalSessions = 40,
            totalFocusMillis = 1_000 * 60_000L,
            weekDays = days,
            weekStart = weekStart,
            weekEnd = LocalDate(2026, 11, 8),
            currentStreakDays = 5,
        ),
        role = "Software Engineer",
        period = SharePeriod.Week,
    )
}
