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
package com.ignitrondev.focusflow.domain.achievement

/**
 * The 13 on-device achievements for FocusFlow 1.2.0.
 *
 * Stable ids, one per threshold. These ids are the Room primary key and the
 * `achievementId` passed to [com.ignitrondev.focusflow.platform.PlatformServices.notifyAchievementUnlocked].
 * Do not rename them:
 * - `sessions_<count>` for 1, 10, 25, 50, 100 completed focus sessions
 * - `streak_<days>` for 3, 7, 14, 30 days
 * - `focus_hours_<hours>` for 1, 10, 25, 100 hours of all-time focus
 *
 * Thresholds use [com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator] only.
 * Completed sessions are [com.ignitrondev.focusflow.domain.statistics.FocusStatistics.totalSessions].
 * Streak days are [com.ignitrondev.focusflow.domain.statistics.FocusStatistics.currentStreakDays].
 * Focus hours compare [com.ignitrondev.focusflow.domain.statistics.FocusStatistics.totalFocusMillis]
 * with exact [HOUR_MILLIS] units. Rounded minutes are not a threshold.
 */
object Achievements {
    /** One hour in milliseconds. Hour thresholds use this value, not a rounded minute count. */
    const val HOUR_MILLIS: Long = 3_600_000L

    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition("sessions_1", AchievementMetric.CompletedSessions, 1),
        AchievementDefinition("sessions_10", AchievementMetric.CompletedSessions, 10),
        AchievementDefinition("sessions_25", AchievementMetric.CompletedSessions, 25),
        AchievementDefinition("sessions_50", AchievementMetric.CompletedSessions, 50),
        AchievementDefinition("sessions_100", AchievementMetric.CompletedSessions, 100),
        AchievementDefinition("streak_3", AchievementMetric.StreakDays, 3),
        AchievementDefinition("streak_7", AchievementMetric.StreakDays, 7),
        AchievementDefinition("streak_14", AchievementMetric.StreakDays, 14),
        AchievementDefinition("streak_30", AchievementMetric.StreakDays, 30),
        AchievementDefinition("focus_hours_1", AchievementMetric.FocusHours, 1),
        AchievementDefinition("focus_hours_10", AchievementMetric.FocusHours, 10),
        AchievementDefinition("focus_hours_25", AchievementMetric.FocusHours, 25),
        AchievementDefinition("focus_hours_100", AchievementMetric.FocusHours, 100),
    )

    init {
        require(all.size == 13) { "FocusFlow 1.2.0 ships 13 achievements" }
        require(all.distinctBy { it.id }.size == all.size) { "Achievement ids must be unique" }
    }
}

enum class AchievementMetric {
    CompletedSessions,
    StreakDays,
    FocusHours,
}

data class AchievementDefinition(val id: String, val metric: AchievementMetric, val threshold: Int)
