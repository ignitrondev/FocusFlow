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

import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.FocusSummary
import com.ignitrondev.focusflow.domain.timer.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFocusSessionRepository(private val focusSessionDao: FocusSessionDao) : FocusSessionRepository {
    override fun observeSessions(): Flow<List<FinishedSession>> =
        focusSessionDao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeFocusSummary(fromEpochMillis: Long, toEpochMillis: Long): Flow<FocusSummary> =
        focusSessionDao.observeCompletedSummary(SessionType.Focus.name, fromEpochMillis, toEpochMillis)
            .map { FocusSummary(completedSessions = it.sessionCount, focusMillis = it.totalSeconds * 1000) }
}
