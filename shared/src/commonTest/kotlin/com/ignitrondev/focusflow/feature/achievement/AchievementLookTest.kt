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

import com.ignitrondev.focusflow.domain.achievement.AchievementMetric
import com.ignitrondev.focusflow.domain.achievement.Achievements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementLookTest {
    @Test
    fun everyAchievementHasItsOwnBadge() {
        assertEquals(Achievements.all.map { it.id }.toSet(), looks.keys)
        assertEquals(looks.size, looks.values.map { it.title }.toSet().size, "badge names must differ")
    }

    @Test
    fun badgesGetRarerAlongEachGoal() {
        AchievementMetric.entries.forEach { metric ->
            val tiers = Achievements.all.filter { it.metric == metric }.sortedBy { it.threshold }.map { it.look().tier }
            assertEquals(tiers.sortedBy { it.ordinal }, tiers, "$metric badges must not get less rare")
            assertEquals(AchievementTier.Legendary, tiers.last(), "the last $metric goal is the legendary badge")
            assertTrue(tiers.first() == AchievementTier.Bronze, "the first $metric goal is bronze")
        }
    }
}
