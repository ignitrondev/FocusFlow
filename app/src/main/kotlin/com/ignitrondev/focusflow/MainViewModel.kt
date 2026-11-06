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
package com.ignitrondev.focusflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val preferences: UserPreferences) : MainUiState
}

@HiltViewModel
class MainViewModel
@Inject
constructor(userPreferencesRepository: UserPreferencesRepository) : ViewModel() {
    // Eagerly: the splash screen waits on this before Compose has subscribed.
    val uiState: StateFlow<MainUiState> =
        userPreferencesRepository.userPreferences
            .map<UserPreferences, MainUiState> { MainUiState.Ready(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
}
