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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimeSource
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Immutable
data class HistoryItem(
    val id: String,
    val type: SessionType,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMillis: Long,
    val completed: Boolean,
)

@Immutable
data class HistoryDay(
    val date: LocalDate,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val focusMillis: Long,
    val items: List<HistoryItem>,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Success(val days: List<HistoryDay>) : HistoryUiState
}

class HistoryViewModel(focusSessionRepository: FocusSessionRepository, timeSource: TimeSource) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = focusSessionRepository.observeSessions()
        .map { sessions ->
            val zone = timeSource.zone()
            val today = Instant.fromEpochMilliseconds(timeSource.now().epochMillis).toLocalDateTime(zone).date
            HistoryUiState.Success(groupByDay(sessions, today, zone))
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)
}

/** Groups sessions by the local day they ended on, newest day and newest session first. */
internal fun groupByDay(sessions: List<FinishedSession>, today: LocalDate, zone: TimeZone): List<HistoryDay> = sessions
    .sortedByDescending { it.endedAtEpochMillis }
    .groupBy { Instant.fromEpochMilliseconds(it.endedAtEpochMillis).toLocalDateTime(zone).date }
    .map { (date, daySessions) ->
        HistoryDay(
            date = date,
            isToday = date == today,
            isYesterday = date == today.minus(1, DateTimeUnit.DAY),
            focusMillis = daySessions
                .filter { it.completed && it.sessionType == SessionType.Focus }
                .sumOf { it.actualDurationMillis },
            items = daySessions.map { session ->
                HistoryItem(
                    id = session.id,
                    type = session.sessionType,
                    startTime = Instant.fromEpochMilliseconds(session.startedAtEpochMillis).toLocalDateTime(zone).time,
                    endTime = Instant.fromEpochMilliseconds(session.endedAtEpochMillis).toLocalDateTime(zone).time,
                    durationMillis = session.actualDurationMillis,
                    completed = session.completed,
                )
            },
        )
    }
