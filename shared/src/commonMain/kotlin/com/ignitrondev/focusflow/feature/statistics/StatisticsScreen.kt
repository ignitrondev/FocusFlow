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
package com.ignitrondev.focusflow.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowShapes
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowSpacing
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.designsystem.theme.FocusTokens
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.DayFocus
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.feature.statistics.share.ShareStatisticsSheet
import com.ignitrondev.focusflow.platform.DateFormats
import com.ignitrondev.focusflow.platform.LocalDateFormats
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.completed_sessions
import com.ignitrondev.focusflow.resources.duration_minutes
import com.ignitrondev.focusflow.resources.statistics_average_session
import com.ignitrondev.focusflow.resources.statistics_bar_description
import com.ignitrondev.focusflow.resources.statistics_chart_empty
import com.ignitrondev.focusflow.resources.statistics_chart_summary
import com.ignitrondev.focusflow.resources.statistics_daily_average
import com.ignitrondev.focusflow.resources.statistics_metric_description
import com.ignitrondev.focusflow.resources.statistics_share
import com.ignitrondev.focusflow.resources.statistics_streak
import com.ignitrondev.focusflow.resources.statistics_subtitle
import com.ignitrondev.focusflow.resources.statistics_summary_description
import com.ignitrondev.focusflow.resources.statistics_this_week
import com.ignitrondev.focusflow.resources.statistics_title
import com.ignitrondev.focusflow.resources.statistics_today
import com.ignitrondev.focusflow.resources.streak_days
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object StatisticsRoute

fun NavGraphBuilder.statisticsScreen() {
    composable<StatisticsRoute> {
        val dependencies = LocalAppDependencies.current
        val viewModel = viewModel {
            StatisticsViewModel(
                focusSessionRepository = dependencies.focusSessionRepository,
                timeSource = dependencies.timeSource,
                userPreferencesRepository = dependencies.userPreferencesRepository,
                firstDayOfWeek = dependencies.platform::firstDayOfWeek,
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val shareRole by viewModel.shareRole.collectAsStateWithLifecycle()
        StatisticsScreen(uiState, shareRole = shareRole, onSaveShareRole = viewModel::saveShareRole)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    modifier: Modifier = Modifier,
    shareRole: String = "",
    onSaveShareRole: (String) -> Unit = {},
) {
    var sharing by rememberSaveable { mutableStateOf(false) }
    val statistics = (uiState as? StatisticsUiState.Success)?.statistics
    // Loading stays a title bar and an empty body. A successful load, including an empty week, uses the screen.
    Scaffold(
        modifier = modifier,
        containerColor = if (statistics != null) FocusTokens.screen else MaterialTheme.colorScheme.background,
        topBar = {
            if (statistics == null) {
                TopAppBar(title = { Text(stringResource(Res.string.statistics_title)) })
            }
        },
    ) { innerPadding ->
        if (sharing && statistics != null) {
            ShareStatisticsSheet(statistics, savedRole = shareRole, onSaveRole = onSaveShareRole, onDismiss = { sharing = false })
        }
        if (statistics != null) {
            StatisticsContent(
                statistics = statistics,
                onShare = { sharing = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun StatisticsContent(statistics: FocusStatistics, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxSize()) {
        val stack = maxWidth < 360.dp || fontScale >= 1.3f
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusFlowSpacing.lg, vertical = FocusFlowSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FocusFlowSpacing.sm),
            ) {
                Header(onShare)
                SummaryRow(statistics, stack)
                WeekChart(statistics, fontScale)
                MetricRow(statistics, stack)
                MetricCard(
                    title = stringResource(Res.string.statistics_streak),
                    value = pluralStringResource(Res.plurals.streak_days, statistics.currentStreakDays, statistics.currentStreakDays),
                    spokenValue = pluralStringResource(
                        Res.plurals.streak_days,
                        statistics.currentStreakDays,
                        statistics.currentStreakDays,
                    ),
                    icon = Icons.Rounded.EmojiEvents,
                    iconTint = FocusTokens.streak,
                    tag = "metricStreak",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Header(onShare: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FocusFlowSpacing.sm)) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.statistics_title),
                style = DisplayStyle,
                color = FocusTokens.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(Res.string.statistics_subtitle),
                style = SubtitleStyle,
                color = FocusTokens.textSecondary,
            )
        }
        ShareButton(onShare)
    }
}

@Composable
private fun ShareButton(onShare: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    IconButton(
        onClick = onShare,
        interactionSource = interaction,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = FocusTokens.textPrimary,
        ),
        modifier = Modifier
            .size(48.dp)
            .graphicsLayerScale(if (pressed) 0.96f else 1f)
            .clip(FocusFlowShapes.iconButton)
            .background(if (pressed) FocusTokens.cardPressed else FocusTokens.card)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = FocusTokens.focusRing,
                shape = FocusFlowShapes.iconButton,
            )
            .testTag("shareStatistics"),
    ) {
        Icon(Icons.Rounded.Share, contentDescription = stringResource(Res.string.statistics_share), modifier = Modifier.size(24.dp))
    }
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    },
)

@Composable
private fun SummaryRow(statistics: FocusStatistics, stack: Boolean) {
    val todayMinutes = focusMinutes(statistics.today.focusMillis)
    val weekMinutes = focusMinutes(statistics.weekFocusMillis)
    AdaptivePair(
        stack = stack,
        first = { modifier ->
            SummaryCard(
                title = stringResource(Res.string.statistics_today),
                value = durationText(statistics.today.focusMillis),
                spokenValue = spokenMinutes(todayMinutes),
                caption = sessionsText(statistics.today.sessions),
                icon = Icons.Rounded.Timer,
                iconTint = FocusTokens.accent,
                glow = true,
                tag = "summaryToday",
                modifier = modifier,
            )
        },
        second = { modifier ->
            SummaryCard(
                title = stringResource(Res.string.statistics_this_week),
                value = durationText(statistics.weekFocusMillis),
                spokenValue = spokenMinutes(weekMinutes),
                caption = sessionsText(statistics.weekSessions),
                icon = Icons.Rounded.CalendarMonth,
                iconTint = FocusTokens.textSecondary,
                glow = false,
                tag = "summaryWeek",
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun MetricRow(statistics: FocusStatistics, stack: Boolean) {
    val dailyMinutes = focusMinutes(statistics.weekDailyAverageMillis)
    val sessionMinutes = focusMinutes(statistics.averageSessionMillis)
    AdaptivePair(
        stack = stack,
        first = { modifier ->
            MetricCard(
                title = stringResource(Res.string.statistics_daily_average),
                value = durationText(statistics.weekDailyAverageMillis),
                spokenValue = spokenMinutes(dailyMinutes),
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = FocusTokens.accent,
                tag = "metricDaily",
                modifier = modifier,
            )
        },
        second = { modifier ->
            MetricCard(
                title = stringResource(Res.string.statistics_average_session),
                value = durationText(statistics.averageSessionMillis),
                spokenValue = spokenMinutes(sessionMinutes),
                icon = Icons.Rounded.TrackChanges,
                iconTint = FocusTokens.session,
                tag = "metricSession",
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun AdaptivePair(stack: Boolean, first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    if (stack) {
        Column(verticalArrangement = Arrangement.spacedBy(FocusFlowSpacing.sm)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(FocusFlowSpacing.sm)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    spokenValue: String,
    caption: String,
    icon: ImageVector,
    iconTint: Color,
    glow: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.statistics_summary_description, title, spokenValue, caption)
    SurfaceCard(modifier = modifier.heightIn(min = 112.dp), description = description, tag = tag, glow = glow) {
        Column(Modifier.padding(FocusFlowSpacing.md)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(FocusFlowShapes.iconButton)
                    .background(FocusTokens.cardPressed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(FocusFlowSpacing.xs))
            Label(title)
            Text(value, style = ValueLargeStyle, color = FocusTokens.textPrimary)
            Text(caption, style = CaptionStyle, color = FocusTokens.textSecondary)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    spokenValue: String,
    icon: ImageVector,
    iconTint: Color,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.statistics_metric_description, title, spokenValue)
    SurfaceCard(modifier = modifier.heightIn(min = 96.dp), description = description, tag = tag, glow = false) {
        Row(
            Modifier.padding(FocusFlowSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FocusFlowSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            Column {
                Label(title)
                Text(value, style = ValueMediumStyle, color = FocusTokens.textPrimary)
            }
        }
    }
}

@Composable
private fun SurfaceCard(modifier: Modifier, description: String, tag: String, glow: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(FocusFlowShapes.card)
            .background(FocusTokens.card)
            .border(1.dp, FocusTokens.cardBorder, FocusFlowShapes.card)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) {
        if (glow) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(listOf(FocusTokens.accent.copy(alpha = 0.18f), Color.Transparent))),
            )
        }
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = LabelStyle,
        color = FocusTokens.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun WeekChart(statistics: FocusStatistics, fontScale: Float) {
    val dates = LocalDateFormats.current
    val minutes = statistics.weekDays.map { focusMinutes(it.focusMillis) }
    val axis = chartAxis(minutes)
    val plotHeight = if (fontScale >= 1.3f) 200.dp else 160.dp
    val gutter = if (fontScale >= 1.3f) 72.dp else 44.dp
    val totalSpoken = spokenMinutes(focusMinutes(statistics.weekFocusMillis))
    val summary = stringResource(
        Res.string.statistics_chart_summary,
        dates.dayMonth(statistics.weekStart),
        dates.dayMonth(statistics.weekEnd),
        totalSpoken,
    )
    SurfaceCard(modifier = Modifier.fillMaxWidth(), description = summary, tag = "weekChart", glow = false) {
        Column(Modifier.padding(FocusFlowSpacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FocusFlowSpacing.xs),
            ) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = FocusTokens.accent, modifier = Modifier.size(20.dp))
                Text(
                    weekRangeLabel(statistics.weekStart, statistics.weekEnd, dates),
                    style = LabelStyle,
                    color = FocusTokens.textSecondary,
                    modifier = Modifier.testTag("weekRange"),
                )
            }
            Spacer(Modifier.height(FocusFlowSpacing.xl))
            Row(Modifier.fillMaxWidth().height(plotHeight)) {
                Plot(statistics, minutes, axis, plotHeight, dates, Modifier.weight(1f).fillMaxHeight())
                YAxis(axis, Modifier.width(gutter).fillMaxHeight())
            }
            Spacer(Modifier.height(FocusFlowSpacing.xs))
            Row(Modifier.fillMaxWidth()) {
                Row(Modifier.weight(1f)) {
                    statistics.weekDays.forEachIndexed { index, day ->
                        DayLabel(day, statistics.today.date, dates, fontScale, index)
                    }
                }
                Spacer(Modifier.width(gutter))
            }
        }
    }
}

@Composable
private fun Plot(
    statistics: FocusStatistics,
    minutes: List<Int>,
    axis: ChartAxis,
    plotHeight: Dp,
    dates: DateFormats,
    modifier: Modifier,
) {
    val todayIndex = statistics.weekDays.indexOfFirst { it.date == statistics.today.date }
    BoxWithConstraints(modifier.testTag("chartPlot")) {
        val slot = if (minutes.isEmpty()) maxWidth else maxWidth / minutes.size
        val barWidth = 28.dp.coerceAtMost(slot).let { if (slot >= 20.dp) it.coerceAtLeast(20.dp) else slot }
        GridLines(axis, Modifier.matchParentSize())
        Row(Modifier.matchParentSize()) {
            statistics.weekDays.forEachIndexed { index, day ->
                val value = minutes[index]
                val description = stringResource(
                    Res.string.statistics_bar_description,
                    dates.weekdayShort(day.date.dayOfWeek),
                    spokenMinutes(value),
                )
                BarSlot(
                    minutes = value,
                    axisMax = axis.axisMaxMinutes,
                    plotHeight = plotHeight,
                    barWidth = barWidth,
                    highlighted = day.date == statistics.today.date,
                    description = description,
                )
            }
        }
        if (statistics.isEmpty) {
            Text(
                stringResource(Res.string.statistics_chart_empty),
                style = LabelStyle,
                color = FocusTokens.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).testTag("chartEmpty"),
            )
        }
        if (todayIndex >= 0 && minutes.getOrElse(todayIndex) { 0 } > 0) {
            val value = minutes[todayIndex]
            val fraction = value.toFloat() / axis.axisMaxMinutes
            // Keep the chip inside the plot so a Sunday bar does not cover the y-axis labels.
            val chipHalf = 40.dp
            val barCenter = slot * todayIndex + slot / 2
            val clamped = barCenter.coerceIn(chipHalf, (maxWidth - chipHalf).coerceAtLeast(chipHalf))
            Tooltip(
                text = durationText(statistics.weekDays[todayIndex].focusMillis),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(
                        x = clamped - maxWidth / 2,
                        y = -(plotHeight * fraction) - FocusFlowSpacing.xs,
                    ),
            )
        }
    }
}

@Composable
private fun GridLines(axis: ChartAxis, modifier: Modifier) {
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        axis.ticksMinutes.forEach { tick ->
            val y = size.height * (1f - tick.toFloat() / axis.axisMaxMinutes)
            drawLine(
                color = FocusTokens.gridline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (tick == 0) null else dash,
            )
        }
    }
}

@Composable
private fun RowScope.BarSlot(minutes: Int, axisMax: Int, plotHeight: Dp, barWidth: Dp, highlighted: Boolean, description: String) {
    val fraction = if (axisMax == 0) 0f else minutes.toFloat() / axisMax
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics { contentDescription = description }
            .focusable(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (minutes > 0) {
            Box(
                Modifier
                    .width(barWidth)
                    .height(plotHeight * fraction)
                    .clip(FocusFlowShapes.bar)
                    .background(if (highlighted) FocusTokens.accent else FocusTokens.accent.copy(alpha = 0.45f)),
            )
        } else {
            Box(
                Modifier
                    .size(4.dp)
                    .clip(FocusFlowShapes.iconButton)
                    .background(FocusTokens.baseline),
            )
        }
    }
}

@Composable
private fun Tooltip(text: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            style = TooltipStyle,
            color = FocusTokens.textPrimary,
            modifier = Modifier
                .clip(FocusFlowShapes.chip)
                .background(FocusTokens.cardPressed)
                .padding(horizontal = FocusFlowSpacing.xs, vertical = FocusFlowSpacing.xxs),
        )
        Spacer(Modifier.height(FocusFlowSpacing.xxs))
        Box(Modifier.size(4.dp).clip(FocusFlowShapes.iconButton).background(FocusTokens.accent))
    }
}

@Composable
private fun YAxis(axis: ChartAxis, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        axis.ticksMinutes.forEach { tick ->
            val fromTop = maxHeight * (1f - tick.toFloat() / axis.axisMaxMinutes)
            Text(
                chartTickLabel(tick),
                style = AxisStyle,
                color = FocusTokens.textSecondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth()
                    .offset(y = fromTop - FocusFlowSpacing.xs)
                    .testTag("tick_$tick"),
            )
        }
    }
}

@Composable
private fun RowScope.DayLabel(day: DayFocus, today: LocalDate, dates: DateFormats, fontScale: Float, index: Int) {
    val full = dates.weekdayShort(day.date.dayOfWeek)
    val narrow = dates.weekdayNarrow(day.date.dayOfWeek)
    var useNarrow by remember(full) { mutableStateOf(false) }
    val highlight = day.date == today
    Text(
        text = if (fontScale >= 1.3f && useNarrow) narrow else full,
        modifier = Modifier
            .weight(1f)
            .testTag("dayLabel_$index")
            .semantics { contentDescription = full },
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { layout ->
            if (fontScale >= 1.3f && !useNarrow && layout.hasVisualOverflow) useNarrow = true
        },
        style = AxisStyle.copy(fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal),
        color = if (highlight) FocusTokens.textPrimary else FocusTokens.textSecondary,
    )
}

@Composable
private fun sessionsText(count: Int) = pluralStringResource(Res.plurals.completed_sessions, count, count)

@Composable
private fun spokenMinutes(minutes: Int) = pluralStringResource(Res.plurals.duration_minutes, minutes, minutes)

private val DisplayStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)
private val SubtitleStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
private val ValueLargeStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
private val ValueMediumStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
private val LabelStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
private val CaptionStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
private val AxisStyle = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
private val TooltipStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)

@Preview
@Composable
private fun StatisticsScreenPreview() {
    val weekStart = LocalDate(2026, 11, 2)
    val today = LocalDate(2026, 11, 7)
    val days = (0..6).map { offset ->
        DayFocus(weekStart.plus(offset, DateTimeUnit.DAY), (offset % 4 + 1) * 25 * 60_000L, offset % 4 + 1)
    }
    FocusFlowTheme {
        StatisticsScreen(
            StatisticsUiState.Success(
                FocusStatistics(
                    today = days.first { it.date == today },
                    weekFocusMillis = 300 * 60_000L,
                    weekSessions = 12,
                    weekDailyAverageMillis = 60 * 60_000L,
                    averageSessionMillis = 25 * 60_000L,
                    totalSessions = 40,
                    totalFocusMillis = 1_000 * 60_000L,
                    weekDays = days,
                    weekStart = weekStart,
                    weekEnd = LocalDate(2026, 11, 8),
                ),
            ),
        )
    }
}
