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

import kotlinx.coroutines.flow.Flow

/**
 * Owns the persisted timer. Every operation first settles the stored state against the clocks
 * (see [TimerEngine.settle]) and runs atomically, so concurrent callers (the UI, and from Phase 3
 * an alarm) can never record the same session twice.
 */
interface TimerRepository {
    val state: Flow<TimerState>

    /** Recovers the timer and records sessions whose time ran out while nobody was watching. */
    suspend fun refresh()

    suspend fun start()

    suspend fun pause()

    suspend fun reset()

    suspend fun skip()

    suspend fun select(type: SessionType)
}

/**
 * Told about sessions at the moment they are recorded as completed. Called once per session, after
 * the database transaction committed, whichever caller (screen, alarm) noticed the deadline.
 */
fun interface SessionCompletionListener {
    fun onSessionsCompleted(sessions: List<FinishedSession>, next: TimerState)
}

/** Source of the timer configuration. Phase 2 uses the defaults; settings arrive in Phase 5. */
interface TimerConfigRepository {
    val config: Flow<TimerConfig>
}
