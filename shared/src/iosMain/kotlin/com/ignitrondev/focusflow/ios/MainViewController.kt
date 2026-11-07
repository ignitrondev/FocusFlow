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
package com.ignitrondev.focusflow.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.navigation.FocusFlowRoot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

/** The whole app as a UIKit view controller; the Swift wrapper in iosApp/ only shows this. */
@Suppress("FunctionName", "unused") // Called from Swift.
fun MainViewController(): UIViewController = ComposeUIViewController { IosApp(IosAppDependencies.shared) }

@Composable
private fun IosApp(dependencies: IosAppDependencies) {
    // Null until the preferences are read, so the app never flashes the wrong theme.
    val preferences: UserPreferences? by dependencies.userPreferencesRepository.userPreferences
        .map<UserPreferences, UserPreferences?> { it }
        .collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    // Back from the background: record sessions that ended meanwhile (their alerts already fired).
    LifecycleResumeEffect(Unit) {
        scope.launch {
            runCatching { dependencies.timerRepository.refresh() }
                .onFailure { dependencies.platform.logError("FocusFlow", "Could not refresh the timer", it) }
        }
        onPauseOrDispose {}
    }

    val loaded = preferences ?: return
    val darkTheme = when (loaded.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    FocusFlowRoot(dependencies = dependencies, preferences = loaded, darkTheme = darkTheme)
}
