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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.platform.notificationsAllowed
import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute

fun NavGraphBuilder.timerScreen() {
    composable<TimerRoute> {
        val viewModel: TimerViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val requestNotificationPermission = rememberNotificationPermissionRequest()
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

/**
 * Asks once per screen visit for the notification permission (Android 13+). Android itself stops
 * showing the dialog after the user declined twice; the timer works either way.
 */
@Composable
private fun rememberNotificationPermissionRequest(): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return {}
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (!asked && !context.notificationsAllowed()) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
