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
package com.ignitrondev.focusflow.domain.timer

/**
 * A session that ended, either because its time ran out ([completed] = true) or because the user
 * reset or skipped it after it had been running for a while.
 *
 * @property id the [TimerState.runId] of the run; unique per run.
 * @property actualDurationMillis running time, excluding pauses.
 */
data class FinishedSession(
    val id: String,
    val sessionType: SessionType,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val plannedDurationMillis: Long,
    val actualDurationMillis: Long,
    val completed: Boolean,
)

/** Result of a timer transition: the new state and any sessions that ended on the way. */
data class TimerUpdate(val state: TimerState, val finishedSessions: List<FinishedSession> = emptyList())

/** A session end that will happen at [atEpochMillis] if the timer is left running. */
data class UpcomingSessionEnd(
    val sessionType: SessionType,
    val atEpochMillis: Long,
    val nextSessionType: SessionType,
    val nextStartsAutomatically: Boolean,
)
