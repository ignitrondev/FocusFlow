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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.domain.achievement.AchievementEvaluator
import com.ignitrondev.focusflow.domain.achievement.AchievementStatus
import com.ignitrondev.focusflow.domain.achievement.AchievementUnlockRecord
import com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.TimeSource
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toLocalDateTime

sealed interface AchievementsUiState {
    data object Loading : AchievementsUiState

    data class Ready(val noCompletedSessions: Boolean, val items: List<AchievementStatus>) : AchievementsUiState
}

class AchievementsViewModel(
    sessions: Flow<List<FinishedSession>>,
    unlocks: Flow<List<AchievementUnlockRecord>>,
    private val timeSource: TimeSource,
    private val firstDayOfWeek: () -> DayOfWeek,
) : ViewModel() {
    val uiState: StateFlow<AchievementsUiState> = combine(sessions, unlocks) { finished, stored ->
        val zone = timeSource.zone()
        val today = Instant.fromEpochMilliseconds(timeSource.now().epochMillis).toLocalDateTime(zone).date
        val statistics = FocusStatisticsCalculator.calculate(finished, today, zone, firstDayOfWeek())
        val plan = AchievementEvaluator.plan(statistics, stored)
        AchievementsUiState.Ready(
            noCompletedSessions = statistics.totalSessions == 0,
            items = plan.statuses,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState.Loading)
}
