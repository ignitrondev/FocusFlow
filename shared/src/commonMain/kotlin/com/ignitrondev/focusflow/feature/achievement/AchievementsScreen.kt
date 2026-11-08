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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.screenContentInsets
import com.ignitrondev.focusflow.core.designsystem.component.withoutBottom
import com.ignitrondev.focusflow.domain.achievement.AchievementMetric
import com.ignitrondev.focusflow.domain.achievement.AchievementStatus
import com.ignitrondev.focusflow.platform.LocalDateFormats
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.achievement_focus_hours
import com.ignitrondev.focusflow.resources.achievement_sessions
import com.ignitrondev.focusflow.resources.achievement_streak_days
import com.ignitrondev.focusflow.resources.achievements_back
import com.ignitrondev.focusflow.resources.achievements_no_completed_sessions
import com.ignitrondev.focusflow.resources.achievements_open
import com.ignitrondev.focusflow.resources.achievements_progress_count
import com.ignitrondev.focusflow.resources.achievements_progress_days
import com.ignitrondev.focusflow.resources.achievements_progress_days_short
import com.ignitrondev.focusflow.resources.achievements_progress_hours
import com.ignitrondev.focusflow.resources.achievements_progress_hours_minutes
import com.ignitrondev.focusflow.resources.achievements_progress_hours_short
import com.ignitrondev.focusflow.resources.achievements_progress_sessions
import com.ignitrondev.focusflow.resources.achievements_title
import com.ignitrondev.focusflow.resources.achievements_unlocked
import com.ignitrondev.focusflow.resources.achievements_unlocked_on
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object AchievementsRoute

/**
 * Entry at the end of Statistics: a plain button in reading order.
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
    val zone = LocalAppDependencies.current.timeSource.zone()
    val fontScale = LocalDensity.current.fontScale
    Scaffold(contentWindowInsets = screenContentInsets()) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding.withoutBottom())) {
            val columns = when {
                maxWidth < 360.dp && fontScale >= 1.3f -> 1
                maxWidth >= 840.dp -> 4
                maxWidth >= 560.dp -> 3
                else -> 2
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // A round tonal button, in the wallpaper colors when Material You is on.
                    FilledTonalIconButton(
                        onClick = onBack,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp).testTag("achievementsBack"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.achievements_back),
                        )
                    }
                    Text(
                        stringResource(Res.string.achievements_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() }.testTag("achievementsTitle"),
                    )
                }
                if (uiState is AchievementsUiState.Ready && uiState.noCompletedSessions) {
                    Text(
                        stringResource(Res.string.achievements_no_completed_sessions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("achievementsEmpty"),
                    )
                }
                if (uiState is AchievementsUiState.Ready) {
                    uiState.items.chunked(columns).forEach { row ->
                        // Cards in a row share the tallest card's height, so badges and progress line up.
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { item -> AchievementCard(item, zone, Modifier.weight(1f).fillMaxHeight()) }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One badge card: the hexagon, the English name, what it takes, and either when it was unlocked
 * or how far along it is. Screen readers hear the card as one item.
 */
@Composable
private fun AchievementCard(item: AchievementStatus, zone: TimeZone, modifier: Modifier = Modifier) {
    val look = remember(item.definition.id) { item.definition.look() }
    val tier = look.tier
    val scheme = MaterialTheme.colorScheme
    val darkTheme = scheme.surface.luminance() < 0.5f
    val legendary = tier == AchievementTier.Legendary
    val container = if (legendary) lerp(scheme.surfaceContainer, tier.accent, 0.07f) else scheme.surfaceContainer
    val border = if (legendary) tier.accent.copy(alpha = 0.35f) else scheme.outlineVariant.copy(alpha = 0.5f)
    val id = item.definition.id
    Column(
        modifier
            .clip(CardShape)
            .background(container)
            .border(1.dp, border, CardShape)
            .semantics(mergeDescendants = true) {}
            .testTag("achievement_$id")
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HexBadge(look.icon, tier, item.earned)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(look.title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            criteriaLabel(item),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        if (item.earned) {
            val unlockedOn = item.unlockedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }
            Text(
                if (unlockedOn != null) {
                    stringResource(Res.string.achievements_unlocked_on, LocalDateFormats.current.mediumDate(unlockedOn))
                } else {
                    stringResource(Res.string.achievements_unlocked)
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = tier.ink(darkTheme),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("achievement_${id}_status"),
            )
        } else {
            val tierName = stringResource(tier.label)
            val spoken = progressText(item)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = "$tierName. $spoken"
                        testTag = "achievement_${id}_progress"
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProgressTrack(progressFraction(item), tier.accent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tierName, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
                    Text(
                        progressShort(item),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

/** Share of the threshold reached; hours count their leftover minutes too. */
private fun progressFraction(item: AchievementStatus): Float {
    val threshold = item.definition.threshold
    if (threshold <= 0) return 1f
    val done = when (item.definition.metric) {
        AchievementMetric.FocusHours -> item.currentWholeUnits + item.remainderMinutes / 60f
        else -> item.currentWholeUnits.toFloat()
    }
    return (done / threshold).coerceIn(0f, 1f)
}

@Composable
private fun progressShort(item: AchievementStatus): String {
    val whole = item.currentWholeUnits.toDisplayInt()
    val threshold = item.definition.threshold
    return when (item.definition.metric) {
        AchievementMetric.CompletedSessions -> stringResource(Res.string.achievements_progress_count, whole, threshold)
        AchievementMetric.StreakDays -> stringResource(Res.string.achievements_progress_days_short, whole, threshold)
        AchievementMetric.FocusHours -> stringResource(Res.string.achievements_progress_hours_short, whole, threshold)
    }
}

private val CardShape = RoundedCornerShape(24.dp)

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
