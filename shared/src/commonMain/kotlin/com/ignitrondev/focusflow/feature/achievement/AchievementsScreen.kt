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
package com.ignitrondev.focusflow.feature.achievement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.domain.achievement.AchievementMetric
import com.ignitrondev.focusflow.domain.achievement.AchievementStatus
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.achievement_focus_hours
import com.ignitrondev.focusflow.resources.achievement_sessions
import com.ignitrondev.focusflow.resources.achievement_streak_days
import com.ignitrondev.focusflow.resources.achievements_back
import com.ignitrondev.focusflow.resources.achievements_earned
import com.ignitrondev.focusflow.resources.achievements_no_completed_sessions
import com.ignitrondev.focusflow.resources.achievements_not_earned
import com.ignitrondev.focusflow.resources.achievements_open
import com.ignitrondev.focusflow.resources.achievements_progress_days
import com.ignitrondev.focusflow.resources.achievements_progress_hours
import com.ignitrondev.focusflow.resources.achievements_progress_hours_minutes
import com.ignitrondev.focusflow.resources.achievements_progress_sessions
import com.ignitrondev.focusflow.resources.achievements_title
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object AchievementsRoute

/**
 * Entry on Statistics. Position is not a final design; it is a plain button in reading order.
 * Returning from [AchievementsRoute] moves input focus back here.
 */
@Composable
fun AchievementsEntryButton(
    onOpen: () -> Unit,
    restoreFocus: Boolean = false,
    onFocusRestored: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // Captured for this composition. The click that navigates away must not consume the flag.
    val restoreOnEntry = remember { restoreFocus }
    LaunchedEffect(restoreOnEntry) {
        if (!restoreOnEntry) return@LaunchedEffect
        withFrameNanos { }
        focusRequester.requestFocus()
        onFocusRestored()
    }
    Button(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRequester(focusRequester)
            .testTag("openAchievements"),
    ) {
        Text(stringResource(Res.string.achievements_open))
    }
}

@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val dependencies = LocalAppDependencies.current
    val viewModel = viewModel {
        AchievementsViewModel(
            sessions = dependencies.focusSessionRepository.observeSessions(),
            unlocks = dependencies.achievementRepository.observeUnlocks(),
            timeSource = dependencies.timeSource,
            firstDayOfWeek = dependencies.platform::firstDayOfWeek,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AchievementsContent(uiState, onBack)
}

@Composable
private fun AchievementsContent(uiState: AchievementsUiState, onBack: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = 48.dp).testTag("achievementsBack"),
            ) {
                Text(stringResource(Res.string.achievements_back))
            }
            Text(
                stringResource(Res.string.achievements_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }.testTag("achievementsTitle"),
            )
            if (uiState is AchievementsUiState.Ready && uiState.noCompletedSessions) {
                Text(
                    stringResource(Res.string.achievements_no_completed_sessions),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("achievementsEmpty"),
                )
            }
            if (uiState is AchievementsUiState.Ready) {
                uiState.items.forEach { item ->
                    AchievementRow(item)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementStatus) {
    val criteria = criteriaLabel(item)
    val earned = item.earned
    val status = stringResource(if (earned) Res.string.achievements_earned else Res.string.achievements_not_earned)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("achievement_${item.definition.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (earned) Icons.Rounded.Check else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(criteria, style = MaterialTheme.typography.titleMedium)
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("achievement_${item.definition.id}_status"),
            )
            if (!earned) {
                Text(
                    progressText(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun criteriaLabel(item: AchievementStatus): String {
    val count = item.definition.threshold
    return when (item.definition.metric) {
        AchievementMetric.CompletedSessions -> pluralStringResource(Res.plurals.achievement_sessions, count, count)
        AchievementMetric.StreakDays -> pluralStringResource(Res.plurals.achievement_streak_days, count, count)
        AchievementMetric.FocusHours -> pluralStringResource(Res.plurals.achievement_focus_hours, count, count)
    }
}

@Composable
private fun progressText(item: AchievementStatus): String {
    val whole = item.currentWholeUnits.toDisplayInt()
    val threshold = item.definition.threshold
    return when (item.definition.metric) {
        AchievementMetric.CompletedSessions -> stringResource(Res.string.achievements_progress_sessions, whole, threshold)

        AchievementMetric.StreakDays -> stringResource(Res.string.achievements_progress_days, whole, threshold)

        AchievementMetric.FocusHours -> {
            val minutes = item.remainderMinutes.toDisplayInt()
            if (minutes == 0) {
                stringResource(Res.string.achievements_progress_hours, whole, threshold)
            } else {
                stringResource(Res.string.achievements_progress_hours_minutes, whole, minutes, threshold)
            }
        }
    }
}

private fun Long.toDisplayInt(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
