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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.designsystem.component.EmptyState
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.DayFocus
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.feature.statistics.share.ShareStatisticsSheet
import java.time.LocalDate
import java.time.format.TextStyle
import kotlinx.serialization.Serializable

@Serializable
data object StatisticsRoute

fun NavGraphBuilder.statisticsScreen() {
    composable<StatisticsRoute> {
        val viewModel: StatisticsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        StatisticsScreen(uiState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(uiState: StatisticsUiState, modifier: Modifier = Modifier) {
    var sharing by rememberSaveable { mutableStateOf(false) }
    val statistics = (uiState as? StatisticsUiState.Success)?.statistics?.takeUnless { it.isEmpty }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                actions = {
                    if (statistics != null) {
                        IconButton(onClick = { sharing = true }, modifier = Modifier.testTag("shareStatistics")) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.statistics_share))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (sharing && statistics != null) {
            ShareStatisticsSheet(statistics, onDismiss = { sharing = false })
        }
        when {
            uiState is StatisticsUiState.Success && !uiState.statistics.isEmpty -> StatisticsContent(
                statistics = uiState.statistics,
                modifier = Modifier.padding(innerPadding),
            )

            uiState is StatisticsUiState.Success -> EmptyState(
                icon = Icons.Rounded.Insights,
                title = stringResource(R.string.statistics_empty_title),
                body = stringResource(R.string.statistics_empty_body),
                modifier = Modifier.padding(innerPadding),
            )

            else -> Unit
        }
    }
}

@Composable
private fun StatisticsContent(statistics: FocusStatistics, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = stringResource(R.string.statistics_today),
                    value = durationText(statistics.today.focusMillis),
                    detail = sessionsText(statistics.today.sessions),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = stringResource(R.string.statistics_this_week),
                    value = durationText(statistics.weekFocusMillis),
                    detail = sessionsText(statistics.weekSessions),
                    modifier = Modifier.weight(1f),
                )
            }
            ChartCard(statistics.lastSevenDays)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = stringResource(R.string.statistics_daily_average),
                    value = durationText(statistics.weekDailyAverageMillis),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = stringResource(R.string.statistics_average_session),
                    value = durationText(statistics.averageSessionMillis),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = stringResource(R.string.statistics_streak),
                    value = pluralStringResource(R.plurals.streak_days, statistics.currentStreakDays, statistics.currentStreakDays),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = stringResource(R.string.statistics_all_time),
                    value = durationText(statistics.totalFocusMillis),
                    detail = sessionsText(statistics.totalSessions),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun sessionsText(count: Int) = pluralStringResource(R.plurals.completed_sessions, count, count)

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, detail: String? = null) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Bars of focus time per day. TalkBack reads the values instead of the drawing. */
@Composable
private fun ChartCard(days: List<DayFocus>) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = days.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale) }
    val durations = days.map { durationText(it.focusMillis) }
    val description = stringResource(
        R.string.statistics_chart_description,
        labels.zip(durations).joinToString { (day, duration) -> "$day $duration" },
    )
    val maxMillis = days.maxOf { it.focusMillis }.coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.statistics_last_seven_days),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                val slot = size.width / days.size
                val barWidth = slot * 0.5f
                val radius = CornerRadius(barWidth / 2, barWidth / 2)
                days.forEachIndexed { index, day ->
                    val left = slot * index + (slot - barWidth) / 2
                    drawRoundRect(trackColor, Offset(left, 0f), Size(barWidth, size.height), radius)
                    val height = size.height * day.focusMillis / maxMillis
                    if (height > 0f) {
                        drawRoundRect(
                            color = if (index == days.lastIndex) todayColor else barColor,
                            topLeft = Offset(left, size.height - height),
                            size = Size(barWidth, height),
                            cornerRadius = radius,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
                labels.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun StatisticsScreenPreview() {
    val today = LocalDate.of(2026, 11, 7)
    val days = (6L downTo 0L).mapIndexed { i, ago -> DayFocus(today.minusDays(ago), (i % 4 + 1) * 25 * 60_000L, i % 4 + 1) }
    FocusFlowTheme {
        StatisticsScreen(
            StatisticsUiState.Success(
                FocusStatistics(
                    today = days.last(),
                    weekFocusMillis = 300 * 60_000L,
                    weekSessions = 12,
                    weekDailyAverageMillis = 60 * 60_000L,
                    averageSessionMillis = 25 * 60_000L,
                    totalSessions = 40,
                    totalFocusMillis = 1_000 * 60_000L,
                    lastSevenDays = days,
                ),
            ),
        )
    }
}
