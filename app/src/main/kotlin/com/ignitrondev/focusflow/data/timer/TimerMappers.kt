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
package com.ignitrondev.focusflow.data.timer

import com.ignitrondev.focusflow.data.database.FocusSessionEntity
import com.ignitrondev.focusflow.data.database.TimerStateEntity
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerInstant
import com.ignitrondev.focusflow.domain.timer.TimerState
import com.ignitrondev.focusflow.domain.timer.TimerStatus

internal fun TimerState.toEntity(updatedAtEpochMillis: Long) = TimerStateEntity(
    runId = runId,
    sessionType = sessionType.name,
    status = status.name,
    plannedDurationMillis = plannedDurationMillis,
    remainingMillis = remainingMillis,
    anchorElapsedRealtimeMillis = anchor?.elapsedRealtimeMillis,
    anchorEpochMillis = anchor?.epochMillis,
    anchorBootCount = anchor?.bootCount,
    startedAtEpochMillis = startedAtEpochMillis,
    completedFocusInCycle = completedFocusInCycle,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

/**
 * Returns null for a row that cannot be trusted (unknown enum values, inconsistent fields), so the
 * caller starts from a fresh timer instead of crashing.
 */
internal fun TimerStateEntity.toDomainOrNull(): TimerState? = runCatching {
    val status = TimerStatus.valueOf(status)
    val anchor = if (status == TimerStatus.Running) {
        TimerInstant(
            elapsedRealtimeMillis = checkNotNull(anchorElapsedRealtimeMillis),
            epochMillis = checkNotNull(anchorEpochMillis),
            bootCount = anchorBootCount,
        )
    } else {
        null
    }
    TimerState(
        runId = runId,
        sessionType = SessionType.valueOf(sessionType),
        status = status,
        plannedDurationMillis = plannedDurationMillis,
        remainingMillis = remainingMillis,
        anchor = anchor,
        startedAtEpochMillis = startedAtEpochMillis,
        completedFocusInCycle = completedFocusInCycle,
    )
}.getOrNull()

internal fun FinishedSession.toEntity() = FocusSessionEntity(
    id = id,
    sessionType = sessionType.name,
    startedAt = startedAtEpochMillis,
    endedAt = endedAtEpochMillis,
    plannedDurationSeconds = plannedDurationMillis.toRoundedSeconds(),
    actualDurationSeconds = actualDurationMillis.toRoundedSeconds(),
    completed = completed,
)

private fun Long.toRoundedSeconds(): Long = (this + 500) / 1000
