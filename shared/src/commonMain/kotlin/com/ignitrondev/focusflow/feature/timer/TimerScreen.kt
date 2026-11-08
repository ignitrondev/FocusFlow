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
package com.ignitrondev.focusflow.feature.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.designsystem.component.screenContentInsets
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.designsystem.theme.TimerTextStyle
import com.ignitrondev.focusflow.core.time.formatMinutesSeconds
import com.ignitrondev.focusflow.core.ui.labelRes
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.app_name
import com.ignitrondev.focusflow.resources.completed_sessions
import com.ignitrondev.focusflow.resources.duration_minutes
import com.ignitrondev.focusflow.resources.timer_action_pause
import com.ignitrondev.focusflow.resources.timer_action_reset
import com.ignitrondev.focusflow.resources.timer_action_resume
import com.ignitrondev.focusflow.resources.timer_action_skip
import com.ignitrondev.focusflow.resources.timer_action_start
import com.ignitrondev.focusflow.resources.timer_cycle_position
import com.ignitrondev.focusflow.resources.timer_dial_description
import com.ignitrondev.focusflow.resources.timer_mode_focus
import com.ignitrondev.focusflow.resources.timer_mode_long_break
import com.ignitrondev.focusflow.resources.timer_mode_short_break
import com.ignitrondev.focusflow.resources.timer_today_summary
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(uiState: TimerUiState, onAction: (TimerAction) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = screenContentInsets(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isWideLandscape = maxWidth > maxHeight && maxWidth >= 600.dp
            val landscapeDialMaxWidth = maxHeight * 0.85f
            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimerDial(
                        uiState = uiState,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = landscapeDialMaxWidth),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        ModeSelector(uiState.mode, uiState.canSelectMode, onSelect = { onAction(TimerAction.SelectMode(it)) })
                        TimerControls(uiState.isRunning, uiState.isPaused, onAction)
                        TodaySummary(uiState)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ModeSelector(uiState.mode, uiState.canSelectMode, onSelect = { onAction(TimerAction.SelectMode(it)) })
                    Spacer(Modifier.height(40.dp))
                    TimerDial(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .widthIn(max = 360.dp),
                    )
                    Spacer(Modifier.height(48.dp))
                    TimerControls(uiState.isRunning, uiState.isPaused, onAction)
                    Spacer(Modifier.weight(1f, fill = true))
                    Spacer(Modifier.height(32.dp))
                    TodaySummary(uiState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(selected: SessionType, enabled: Boolean, onSelect: (SessionType) -> Unit) {
    val modes = SessionType.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                icon = {},
                label = { FittedLabel(stringResource(mode.labelRes())) },
            )
        }
    }
}

@Composable
private fun TimerDial(uiState: TimerUiState, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(targetValue = uiState.remainingFraction, label = "timerProgress")
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = uiState.mode.accentColor()
    val remaining = formatMinutesSeconds(uiState.remainingSeconds)
    val modeLabel = stringResource(uiState.mode.labelRes())
    val dialDescription = stringResource(Res.string.timer_dial_description, modeLabel, remaining)

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .testTag("timerDial")
            .semantics(mergeDescendants = true) {
                contentDescription = dialDescription
                progressBarRangeInfo = ProgressBarRangeInfo(current = progress, range = 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.035f
            val inset = strokeWidth / 2
            val arcSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        // The digits follow the dial, so they fill it on a tablet as they do on a phone. Derived
        // from the dial width rather than the font scale: they are already display-sized.
        val digitsSize = with(LocalDensity.current) { (maxWidth * 0.21f).coerceIn(48.dp, 180.dp).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = remaining,
                style = TimerTextStyle.copy(fontSize = digitsSize, lineHeight = digitsSize * 1.1f),
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = modeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = progressColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.timer_cycle_position, uiState.cyclePosition, uiState.cycleLength),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimerControls(isRunning: Boolean, isPaused: Boolean, onAction: (TimerAction) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = { onAction(TimerAction.Reset) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.timer_action_reset))
        }
        LargeFloatingActionButton(
            onClick = { onAction(TimerAction.StartOrPause) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    when {
                        isRunning -> Res.string.timer_action_pause
                        isPaused -> Res.string.timer_action_resume
                        else -> Res.string.timer_action_start
                    },
                ),
                modifier = Modifier.size(36.dp),
            )
        }
        FilledTonalIconButton(
            onClick = { onAction(TimerAction.Skip) },
            modifier = Modifier.size(56.dp),
        ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(Res.string.timer_action_skip))
        }
    }
}

@Composable
private fun TodaySummary(uiState: TimerUiState) {
    val minutes = pluralStringResource(
        Res.plurals.duration_minutes,
        uiState.todayFocusMinutes.toInt(),
        uiState.todayFocusMinutes,
    )
    val sessions = pluralStringResource(
        Res.plurals.completed_sessions,
        uiState.todayCompletedSessions,
        uiState.todayCompletedSessions,
    )
    Text(
        text = stringResource(Res.string.timer_today_summary, minutes, sessions),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SessionType.accentColor() = when (this) {
    SessionType.Focus -> MaterialTheme.colorScheme.primary
    SessionType.ShortBreak, SessionType.LongBreak -> MaterialTheme.colorScheme.tertiary
}

@PreviewScreenSizes
@Composable
private fun TimerScreenPreview() {
    FocusFlowTheme {
        TimerScreen(
            uiState = TimerUiState(remainingSeconds = 17 * 60L + 42, isRunning = true, canSelectMode = false, cyclePosition = 2),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TimerScreenDarkPreview() {
    FocusFlowTheme(darkTheme = true) {
        TimerScreen(uiState = TimerUiState(mode = SessionType.ShortBreak, totalSeconds = 300, remainingSeconds = 120), onAction = {})
    }
}
