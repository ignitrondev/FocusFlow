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
package com.ignitrondev.focusflow.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimeSource
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toLocalDateTime

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState

    data class Success(val statistics: FocusStatistics) : StatisticsUiState
}

class StatisticsViewModel(
    focusSessionRepository: FocusSessionRepository,
    timeSource: TimeSource,
    private val userPreferencesRepository: UserPreferencesRepository,
    /** First day of the week in the user's locale. */
    private val firstDayOfWeek: () -> DayOfWeek,
) : ViewModel() {
    /** The role last used on a shared card, so the user does not type it every time. */
    val shareRole: StateFlow<String> = userPreferencesRepository.userPreferences
        .map { it.shareRole }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveShareRole(role: String) {
        viewModelScope.launch { userPreferencesRepository.setShareRole(role) }
    }

    val uiState: StateFlow<StatisticsUiState> = focusSessionRepository.observeSessions()
        .map { sessions ->
            val zone = timeSource.zone()
            val today = Instant.fromEpochMilliseconds(timeSource.now().epochMillis).toLocalDateTime(zone).date
            StatisticsUiState.Success(FocusStatisticsCalculator.calculate(sessions, today, zone, firstDayOfWeek()))
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState.Loading)
}
