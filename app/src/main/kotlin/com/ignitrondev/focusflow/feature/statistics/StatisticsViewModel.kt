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
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState

    data class Success(val statistics: FocusStatistics) : StatisticsUiState
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(focusSessionRepository: FocusSessionRepository, timeSource: TimeSource) : ViewModel() {
    val uiState: StateFlow<StatisticsUiState> = focusSessionRepository.observeSessions()
        .map { sessions ->
            val zone = timeSource.zone()
            val today = Instant.ofEpochMilli(timeSource.now().epochMillis).atZone(zone).toLocalDate()
            val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            StatisticsUiState.Success(FocusStatisticsCalculator.calculate(sessions, today, zone, firstDayOfWeek))
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState.Loading)
}
