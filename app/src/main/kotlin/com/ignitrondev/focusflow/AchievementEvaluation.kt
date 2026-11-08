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

import com.ignitrondev.focusflow.core.common.ApplicationScope
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.feature.achievement.AchievementMonitor
import com.ignitrondev.focusflow.feature.achievement.achievementCriteriaLabel
import com.ignitrondev.focusflow.platform.AndroidPlatformServices
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/** Starts on-device achievement evaluation for the process lifetime. */
@Singleton
class AchievementEvaluation @Inject constructor(
    focusSessionRepository: FocusSessionRepository,
    achievementRepository: AchievementRepository,
    timeSource: TimeSource,
    platform: AndroidPlatformServices,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val monitor = AchievementMonitor(
        sessions = focusSessionRepository.observeSessions(),
        repository = achievementRepository,
        timeSource = timeSource,
        platform = platform,
        firstDayOfWeek = platform::firstDayOfWeek,
        criteriaLabel = ::achievementCriteriaLabel,
    )

    fun start() {
        monitor.start(scope)
    }
}
