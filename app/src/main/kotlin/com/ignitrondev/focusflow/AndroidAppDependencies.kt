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

import com.ignitrondev.focusflow.app.AppDependencies
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.platform.AndroidPlatformServices
import javax.inject.Inject
import javax.inject.Singleton

/** The shared UI's dependencies, assembled by Hilt (tests swap individual bindings as before). */
@Singleton
class AndroidAppDependencies @Inject constructor(
    override val timerRepository: TimerRepository,
    override val focusSessionRepository: FocusSessionRepository,
    override val timerConfigRepository: TimerConfigRepository,
    override val userPreferencesRepository: UserPreferencesRepository,
    override val timeSource: TimeSource,
    override val platform: AndroidPlatformServices,
    override val achievementRepository: AchievementRepository,
) : AppDependencies
