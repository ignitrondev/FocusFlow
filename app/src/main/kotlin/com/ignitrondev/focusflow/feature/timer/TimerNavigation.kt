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
package com.ignitrondev.focusflow.feature.timer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute

fun NavGraphBuilder.timerScreen() {
    composable<TimerRoute> {
        // Phase 1: only the mode selection is interactive. Start/pause/reset/skip are handled by
        // the Phase 2 timer engine (TimerViewModel), which replaces this local state.
        var mode by rememberSaveable { mutableStateOf(TimerMode.Focus) }
        TimerScreen(
            uiState = TimerUiState(mode = mode, remainingSeconds = mode.defaultSeconds, totalSeconds = mode.defaultSeconds),
            onAction = { action -> if (action is TimerAction.SelectMode) mode = action.mode },
        )
    }
}

private val TimerMode.defaultSeconds: Long
    get() =
        when (this) {
            TimerMode.Focus -> 25 * 60L
            TimerMode.ShortBreak -> 5 * 60L
            TimerMode.LongBreak -> 15 * 60L
        }
