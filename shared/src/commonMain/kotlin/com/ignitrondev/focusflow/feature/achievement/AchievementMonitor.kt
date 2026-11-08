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
package com.ignitrondev.focusflow.feature.achievement

import com.ignitrondev.focusflow.domain.achievement.AchievementDefinition
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.achievement.AchievementSync
import com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.platform.PlatformServices
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.toLocalDateTime

/**
 * Recomputes achievements whenever session history changes, including the first launch after
 * an update that adds them. Each newly unlocked achievement is one [PlatformServices.notifyAchievementUnlocked]
 * call. Up to 13 calls can happen on that first evaluation. They are not combined.
 */
class AchievementMonitor(
    private val sessions: Flow<List<FinishedSession>>,
    private val repository: AchievementRepository,
    private val timeSource: TimeSource,
    private val platform: PlatformServices,
    private val firstDayOfWeek: () -> DayOfWeek,
    private val criteriaLabel: suspend (AchievementDefinition) -> String,
) {
    private var started = false

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            sessions.collect { finished ->
                try {
                    val zone = timeSource.zone()
                    val today = Instant.fromEpochMilliseconds(timeSource.now().epochMillis).toLocalDateTime(zone).date
                    val statistics = FocusStatisticsCalculator.calculate(finished, today, zone, firstDayOfWeek())
                    AchievementSync(
                        repository = repository,
                        notify = platform::notifyAchievementUnlocked,
                        criteriaLabel = criteriaLabel,
                        nowEpochMillis = { timeSource.now().epochMillis },
                    ).apply(statistics)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    platform.logError(TAG, "Could not update achievements", e)
                }
            }
        }
    }

    private companion object {
        const val TAG = "Achievements"
    }
}
