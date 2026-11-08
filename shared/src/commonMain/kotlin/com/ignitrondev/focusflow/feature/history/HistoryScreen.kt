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
package com.ignitrondev.focusflow.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.EmptyState
import com.ignitrondev.focusflow.core.designsystem.component.screenContentInsets
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.core.ui.labelRes
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.platform.DateFormats
import com.ignitrondev.focusflow.platform.PreviewDateFormats
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.history_day_total
import com.ignitrondev.focusflow.resources.history_empty_body
import com.ignitrondev.focusflow.resources.history_empty_title
import com.ignitrondev.focusflow.resources.history_interrupted
import com.ignitrondev.focusflow.resources.history_item_description
import com.ignitrondev.focusflow.resources.history_time_range
import com.ignitrondev.focusflow.resources.history_title
import com.ignitrondev.focusflow.resources.history_today
import com.ignitrondev.focusflow.resources.history_yesterday
import com.ignitrondev.focusflow.resources.timer_mode_focus
import com.ignitrondev.focusflow.resources.timer_mode_long_break
import com.ignitrondev.focusflow.resources.timer_mode_short_break
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object HistoryRoute

fun NavGraphBuilder.historyScreen() {
    composable<HistoryRoute> {
        val dependencies = LocalAppDependencies.current
        val viewModel = viewModel { HistoryViewModel(dependencies.focusSessionRepository, dependencies.timeSource) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HistoryScreen(uiState, dates = dependencies.platform.dates)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(uiState: HistoryUiState, dates: DateFormats, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = screenContentInsets(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.history_title)) }) },
    ) { innerPadding ->
        when {
            uiState is HistoryUiState.Success && uiState.days.isNotEmpty() -> HistoryList(uiState.days, dates, innerPadding)

            uiState is HistoryUiState.Success -> EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(Res.string.history_empty_title),
                body = stringResource(Res.string.history_empty_body),
                modifier = Modifier.padding(innerPadding),
            )

            else -> Unit
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(days: List<HistoryDay>, dates: DateFormats, contentPadding: PaddingValues) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            days.forEach { day ->
                stickyHeader(key = day.date.toString()) { DayHeader(day, dates) }
                items(day.items, key = { it.id }) { item -> SessionRow(item, dates) }
            }
        }
    }
}

@Composable
private fun DayHeader(day: HistoryDay, dates: DateFormats) {
    val title = when {
        day.isToday -> stringResource(Res.string.history_today)
        day.isYesterday -> stringResource(Res.string.history_yesterday)
        else -> dates.fullDate(day.date)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        if (day.focusMillis > 0) {
            Text(
                stringResource(Res.string.history_day_total, durationText(day.focusMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionRow(item: HistoryItem, dates: DateFormats) {
    val typeLabel = stringResource(item.type.labelRes())
    val range = stringResource(Res.string.history_time_range, dates.shortTime(item.startTime), dates.shortTime(item.endTime))
    val duration = durationText(item.durationMillis)
    val status = if (item.completed) null else stringResource(Res.string.history_interrupted)
    val description = stringResource(Res.string.history_item_description, typeLabel, duration, range) +
        (status?.let { ", $it" } ?: "")
    val accent = if (item.type == SessionType.Focus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    ListItem(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.type == SessionType.Focus) Icons.Rounded.SelfImprovement else Icons.Rounded.Coffee,
                    contentDescription = null,
                    tint = accent,
                )
            }
        },
        headlineContent = { Text(typeLabel) },
        supportingContent = { Text(if (status != null) "$range · $status" else range) },
        trailingContent = { Text(duration, style = MaterialTheme.typography.labelLarge) },
    )
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate(2026, 11, 7)
    FocusFlowTheme {
        HistoryScreen(
            HistoryUiState.Success(
                listOf(
                    HistoryDay(
                        date = today,
                        isToday = true,
                        isYesterday = false,
                        focusMillis = 50 * 60_000L,
                        items = listOf(
                            HistoryItem("3", SessionType.Focus, LocalTime(10, 5), LocalTime(10, 30), 25 * 60_000L, true),
                            HistoryItem("2", SessionType.ShortBreak, LocalTime(9, 55), LocalTime(10, 0), 5 * 60_000L, true),
                            HistoryItem("1", SessionType.Focus, LocalTime(9, 30), LocalTime(9, 42), 12 * 60_000L, false),
                        ),
                    ),
                ),
            ),
            dates = PreviewDateFormats,
        )
    }
}
