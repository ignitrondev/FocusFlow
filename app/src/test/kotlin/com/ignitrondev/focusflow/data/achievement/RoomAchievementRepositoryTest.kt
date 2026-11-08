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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.domain.achievement.AchievementUnlockRecord
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAchievementRepositoryTest {
    private lateinit var database: FocusFlowDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun notifiedFlagAndTimestampSurviveANewRepository() = runTest {
        val first = RoomAchievementRepository(database.achievementUnlockDao())
        first.insertUnlocks(listOf(AchievementUnlockRecord("streak_3", 10L, notified = false)))
        first.markNotified(listOf("streak_3"))

        val reopened = RoomAchievementRepository(database.achievementUnlockDao())
        reopened.insertUnlocks(listOf(AchievementUnlockRecord("streak_3", 99L, notified = false)))
        val kept = reopened.getUnlocks().single()

        assertEquals("streak_3", kept.id)
        assertEquals(10L, kept.unlockedAtEpochMillis)
        assertTrue(kept.notified)
    }
}
