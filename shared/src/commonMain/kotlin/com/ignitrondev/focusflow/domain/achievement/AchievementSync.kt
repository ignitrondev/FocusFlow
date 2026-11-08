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
 * Writes newly earned achievements, then asks [notify] once per achievement whose notification
 * has not been handled. [notify] is [com.ignitrondev.focusflow.platform.PlatformServices.notifyAchievementUnlocked]:
 * it posts one local notification or no-ops when notifications are not allowed. Either way the
 * unlock is then marked notified, so a later launch does not ask again.
 */
class AchievementSync(
    private val repository: AchievementRepository,
    private val notify: (achievementId: String, criteriaLabel: String) -> Unit,
    private val criteriaLabel: suspend (AchievementDefinition) -> String,
    private val nowEpochMillis: () -> Long,
) {
    suspend fun apply(statistics: FocusStatistics) {
        val plan = AchievementEvaluator.plan(statistics, repository.getUnlocks())
        if (plan.toPersist.isNotEmpty()) {
            val unlockedAt = nowEpochMillis()
            repository.insertUnlocks(
                plan.toPersist.map { definition ->
                    AchievementUnlockRecord(
                        id = definition.id,
                        unlockedAtEpochMillis = unlockedAt,
                        notified = false,
                    )
                },
            )
        }
        val pending = AchievementEvaluator.plan(statistics, repository.getUnlocks()).toNotify
        for (definition in pending) {
            notify(definition.id, criteriaLabel(definition))
            repository.markNotified(listOf(definition.id))
        }
    }
}
