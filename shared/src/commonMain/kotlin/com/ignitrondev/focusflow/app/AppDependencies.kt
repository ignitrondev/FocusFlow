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
package com.ignitrondev.focusflow.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.platform.PlatformServices

/**
 * What the shared UI needs, assembled by each app: by Hilt on Android, by hand on iOS. The shared
 * code depends on no DI framework.
 */
interface AppDependencies {
    val timerRepository: TimerRepository
    val focusSessionRepository: FocusSessionRepository
    val timerConfigRepository: TimerConfigRepository
    val userPreferencesRepository: UserPreferencesRepository
    val timeSource: TimeSource
    val platform: PlatformServices
}

val LocalAppDependencies = staticCompositionLocalOf<AppDependencies> { error("No AppDependencies provided") }
