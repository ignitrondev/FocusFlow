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
package com.ignitrondev.focusflow.data.achievement

import com.ignitrondev.focusflow.data.database.AchievementUnlockDao
import com.ignitrondev.focusflow.data.database.AchievementUnlockEntity
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.achievement.AchievementUnlockRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAchievementRepository(private val dao: AchievementUnlockDao) : AchievementRepository {
    override fun observeUnlocks(): Flow<List<AchievementUnlockRecord>> = dao.observeAll().map { rows -> rows.map { it.toRecord() } }

    override suspend fun getUnlocks(): List<AchievementUnlockRecord> = dao.getAll().map { it.toRecord() }

    override suspend fun insertUnlocks(unlocks: List<AchievementUnlockRecord>) {
        if (unlocks.isEmpty()) return
        dao.insertAll(
            unlocks.map { record ->
                AchievementUnlockEntity(
                    id = record.id,
                    unlockedAt = record.unlockedAtEpochMillis,
                    notified = record.notified,
                )
            },
        )
    }

    override suspend fun markNotified(ids: List<String>) {
        if (ids.isEmpty()) return
        dao.markNotified(ids)
    }
}

private fun AchievementUnlockEntity.toRecord() = AchievementUnlockRecord(
    id = id,
    unlockedAtEpochMillis = unlockedAt,
    notified = notified,
)
