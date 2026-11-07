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
package com.ignitrondev.focusflow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(val preferences: UserPreferences, val timerConfig: TimerConfig = TimerConfig()) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timerConfigRepository: TimerConfigRepository,
    private val timerRepository: TimerRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(userPreferencesRepository.userPreferences, timerConfigRepository.config) { preferences, config ->
            SettingsUiState.Success(preferences, config)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.setThemeMode(themeMode) }
    }

    fun setUseDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setUseDynamicColor(useDynamicColor) }
    }

    fun updateTimerConfig(transform: (TimerConfig) -> TimerConfig) {
        viewModelScope.launch {
            timerConfigRepository.update(transform)
            // Shows the new duration right away if the current session has not started.
            timerRepository.refresh()
        }
    }
}
