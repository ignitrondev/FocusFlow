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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.app.LocalAppDependencies
import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute

fun NavGraphBuilder.timerScreen() {
    composable<TimerRoute> {
        val dependencies = LocalAppDependencies.current
        val viewModel = viewModel {
            TimerViewModel(
                timerRepository = dependencies.timerRepository,
                focusSessionRepository = dependencies.focusSessionRepository,
                configRepository = dependencies.timerConfigRepository,
                timeSource = dependencies.timeSource,
                logError = dependencies.platform::logError,
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val requestNotificationPermission = dependencies.platform.rememberNotificationPermissionRequest()
        TimerScreen(
            uiState = uiState,
            onAction = { action ->
                // Asked in context: the first time the user starts a session, so they know why.
                if (action == TimerAction.StartOrPause && !uiState.isRunning) requestNotificationPermission()
                viewModel.onAction(action)
            },
        )
    }
}
