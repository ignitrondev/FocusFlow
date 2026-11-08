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
package com.ignitrondev.focusflow.data.timer

import com.ignitrondev.focusflow.data.achievement.RoomAchievementRepository
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.data.database.TimerStateDao
import com.ignitrondev.focusflow.domain.achievement.AchievementRepository
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.SessionCompletionListener
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

/** Wires the shared (multiplatform) timer repositories into Hilt for the Android app. */
@Module
@InstallIn(SingletonComponent::class)
object TimerModule {
    @Provides
    @Singleton
    fun providesTimerRepository(
        database: FocusFlowDatabase,
        timerStateDao: TimerStateDao,
        focusSessionDao: FocusSessionDao,
        configRepository: TimerConfigRepository,
        timeSource: TimeSource,
        engine: TimerEngine,
        completionListener: SessionCompletionListener,
    ): TimerRepository = RoomTimerRepository(
        database = database,
        timerStateDao = timerStateDao,
        focusSessionDao = focusSessionDao,
        configRepository = configRepository,
        timeSource = timeSource,
        engine = engine,
        completionListener = completionListener,
    )

    @Provides
    @Singleton
    fun providesFocusSessionRepository(focusSessionDao: FocusSessionDao): FocusSessionRepository =
        RoomFocusSessionRepository(focusSessionDao)

    @Provides
    @Singleton
    fun providesAchievementRepository(database: FocusFlowDatabase): AchievementRepository =
        RoomAchievementRepository(database.achievementUnlockDao())

    @Provides
    fun providesTimerEngine(): TimerEngine = TimerEngine(newRunId = { UUID.randomUUID().toString() })
}
