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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.ignitrondev.focusflow.domain.timer.SessionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

fun NavGraphBuilder.historyScreen() {
    composable<HistoryRoute> {
        val viewModel: HistoryViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        HistoryScreen(uiState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(uiState: HistoryUiState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
    ) { innerPadding ->
        when {
            uiState is HistoryUiState.Success && uiState.days.isNotEmpty() -> HistoryList(uiState.days, innerPadding)

            uiState is HistoryUiState.Success -> EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.history_empty_title),
                body = stringResource(R.string.history_empty_body),
                modifier = Modifier.padding(innerPadding),
            )

            else -> Unit
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(days: List<HistoryDay>, contentPadding: PaddingValues) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    val timeFormat = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            days.forEach { day ->
                stickyHeader(key = day.date.toString()) { DayHeader(day, dateFormat) }
                items(day.items, key = { it.id }) { item -> SessionRow(item, timeFormat) }
            }
        }
    }
}

@Composable
private fun DayHeader(day: HistoryDay, dateFormat: DateTimeFormatter) {
    val title = when {
        day.isToday -> stringResource(R.string.history_today)
        day.isYesterday -> stringResource(R.string.history_yesterday)
        else -> day.date.format(dateFormat)
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
                stringResource(R.string.history_day_total, durationText(day.focusMillis)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionRow(item: HistoryItem, timeFormat: DateTimeFormatter) {
    val typeLabel = stringResource(item.type.labelRes())
    val range = stringResource(R.string.history_time_range, item.startTime.format(timeFormat), item.endTime.format(timeFormat))
    val duration = durationText(item.durationMillis)
    val status = if (item.completed) null else stringResource(R.string.history_interrupted)
    val description = stringResource(R.string.history_item_description, typeLabel, duration, range) +
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

private fun SessionType.labelRes(): Int = when (this) {
    SessionType.Focus -> R.string.timer_mode_focus
    SessionType.ShortBreak -> R.string.timer_mode_short_break
    SessionType.LongBreak -> R.string.timer_mode_long_break
}

@Preview
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate.of(2026, 11, 7)
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
                            HistoryItem("3", SessionType.Focus, LocalTime.of(10, 5), LocalTime.of(10, 30), 25 * 60_000L, true),
                            HistoryItem("2", SessionType.ShortBreak, LocalTime.of(9, 55), LocalTime.of(10, 0), 5 * 60_000L, true),
                            HistoryItem("1", SessionType.Focus, LocalTime.of(9, 30), LocalTime.of(9, 42), 12 * 60_000L, false),
                        ),
                    ),
                ),
            ),
        )
    }
}
