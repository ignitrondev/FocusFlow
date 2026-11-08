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
import com.ignitrondev.focusflow.domain.achievement.AchievementMetric
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.achievement_focus_hours
import com.ignitrondev.focusflow.resources.achievement_sessions
import com.ignitrondev.focusflow.resources.achievement_streak_days
import org.jetbrains.compose.resources.getPluralString

/** Criteria label shared by the achievements screen and the unlock notification. */
suspend fun achievementCriteriaLabel(definition: AchievementDefinition): String = when (definition.metric) {
    AchievementMetric.CompletedSessions -> getPluralString(Res.plurals.achievement_sessions, definition.threshold, definition.threshold)
    AchievementMetric.StreakDays -> getPluralString(Res.plurals.achievement_streak_days, definition.threshold, definition.threshold)
    AchievementMetric.FocusHours -> getPluralString(Res.plurals.achievement_focus_hours, definition.threshold, definition.threshold)
}
