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

import androidx.room.withTransaction
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.data.database.TimerStateDao
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionCompletionListener
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerInstant
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.domain.timer.TimerState
import com.ignitrondev.focusflow.domain.timer.TimerUpdate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class RoomTimerRepository @Inject constructor(
    private val database: FocusFlowDatabase,
    private val timerStateDao: TimerStateDao,
    private val focusSessionDao: FocusSessionDao,
    private val configRepository: TimerConfigRepository,
    private val timeSource: TimeSource,
    private val engine: TimerEngine,
    private val completionListener: SessionCompletionListener,
) : TimerRepository {

    // Before the first write there is no row; until then the UI shows a fresh timer. Its run id is
    // random, which is harmless: the first update() creates and stores the real initial state.
    override val state: Flow<TimerState> = timerStateDao.observe()
        .map { entity -> entity?.toDomainOrNull() ?: engine.initial(configRepository.config.first()) }
        .distinctUntilChanged()

    override suspend fun refresh() = update { state, _, _ -> TimerUpdate(state) }

    override suspend fun start() = update { state, now, _ -> engine.start(state, now) }

    override suspend fun pause() = update { state, now, _ -> engine.pause(state, now) }

    override suspend fun reset() = update { state, now, config -> engine.reset(state, now, config) }

    override suspend fun skip() = update { state, now, config -> engine.skip(state, now, config) }

    override suspend fun select(type: SessionType) = update { state, _, config -> engine.select(state, type, config) }

    /**
     * Reads, settles, applies [action] and writes back in one transaction. Room runs write
     * transactions one at a time, so a second caller always sees the first caller's result: a
     * completed session is written once, and the state it leaves behind is no longer due.
     */
    private suspend fun update(action: (TimerState, TimerInstant, TimerConfig) -> TimerUpdate) {
        val config = configRepository.config.first()
        val (completed, next) = database.withTransaction {
            val now = timeSource.now()
            val stored = timerStateDao.get()?.toDomainOrNull()
            val current = stored ?: engine.initial(config)
            val settled = engine.settle(current, now, config)
            val result = action(settled.state, now, config)
            val recorded = mutableListOf<FinishedSession>()
            (settled.finishedSessions + result.finishedSessions).forEach { session ->
                // -1: a row with this run id exists already, so this session was handled before.
                if (focusSessionDao.insert(session.toEntity()) != -1L) recorded += session
            }
            if (result.state != stored) timerStateDao.upsert(result.state.toEntity(now.epochMillis))
            recorded.filter(FinishedSession::completed) to result.state
        }
        if (completed.isNotEmpty()) completionListener.onSessionsCompleted(completed, next)
    }
}
