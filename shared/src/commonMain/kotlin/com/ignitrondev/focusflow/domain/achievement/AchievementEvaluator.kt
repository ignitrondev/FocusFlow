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

import com.ignitrondev.focusflow.domain.statistics.FocusStatistics

/**
 * One stored unlock. [notified] means the local notification was handled: posted, or skipped
 * because notifications are not allowed. It does not mean the user saw an alert.
 * An earned achievement is never removed when a streak later breaks.
 */
data class AchievementUnlockRecord(val id: String, val unlockedAtEpochMillis: Long, val notified: Boolean)

/**
 * One achievement as of this evaluation.
 *
 * [currentWholeUnits] is completed sessions, streak days, or whole focus hours floored.
 * [remainderMinutes] is the leftover whole minutes after those hours, also floored, and is 0 for
 * the session and streak metrics. Neither value is rounded up to the next unit.
 */
data class AchievementStatus(
    val definition: AchievementDefinition,
    val earned: Boolean,
    val currentWholeUnits: Long,
    val remainderMinutes: Long,
    val notified: Boolean,
)

/**
 * [toPersist] are thresholds crossed now that are not stored yet.
 * [toNotify] are earned achievements whose notification has not been handled, including ones
 * stored earlier if the process stopped before the flag was saved. One entry per achievement.
 */
data class AchievementPlan(
    val statuses: List<AchievementStatus>,
    val toPersist: List<AchievementDefinition>,
    val toNotify: List<AchievementDefinition>,
)

object AchievementEvaluator {
    fun plan(statistics: FocusStatistics, stored: List<AchievementUnlockRecord>): AchievementPlan {
        val storedById = stored.associateBy { it.id }
        val statuses = Achievements.all.map { definition ->
            val record = storedById[definition.id]
            val met = meets(statistics, definition)
            AchievementStatus(
                definition = definition,
                earned = record != null || met,
                currentWholeUnits = wholeUnits(statistics, definition.metric),
                remainderMinutes = if (definition.metric == AchievementMetric.FocusHours) remainderMinutes(statistics) else 0L,
                notified = record?.notified == true,
            )
        }
        return AchievementPlan(
            statuses = statuses,
            toPersist = statuses.filter { it.earned && storedById[it.definition.id] == null }.map { it.definition },
            toNotify = statuses.filter { it.earned && !it.notified }.map { it.definition },
        )
    }

    private fun meets(statistics: FocusStatistics, definition: AchievementDefinition): Boolean = when (definition.metric) {
        AchievementMetric.CompletedSessions -> statistics.totalSessions >= definition.threshold
        AchievementMetric.StreakDays -> statistics.currentStreakDays >= definition.threshold
        AchievementMetric.FocusHours -> statistics.totalFocusMillis >= definition.threshold * Achievements.HOUR_MILLIS
    }

    private fun wholeUnits(statistics: FocusStatistics, metric: AchievementMetric): Long = when (metric) {
        AchievementMetric.CompletedSessions -> statistics.totalSessions.toLong()
        AchievementMetric.StreakDays -> statistics.currentStreakDays.toLong()
        AchievementMetric.FocusHours -> statistics.totalFocusMillis / Achievements.HOUR_MILLIS
    }

    private fun remainderMinutes(statistics: FocusStatistics): Long = (statistics.totalFocusMillis % Achievements.HOUR_MILLIS) / 60_000L
}
