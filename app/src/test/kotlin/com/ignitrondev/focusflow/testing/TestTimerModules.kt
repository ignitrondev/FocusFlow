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
package com.ignitrondev.focusflow.testing

import android.content.Context
import androidx.room.Room
import com.ignitrondev.focusflow.core.time.TimeModule
import com.ignitrondev.focusflow.data.database.DatabaseModule
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.data.database.TimerStateDao
import com.ignitrondev.focusflow.domain.timer.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Every Hilt test gets a fresh in-memory database. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): FocusFlowDatabase =
        Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java).build()

    @Provides
    fun providesTimerStateDao(database: FocusFlowDatabase): TimerStateDao = database.timerStateDao()

    @Provides
    fun providesFocusSessionDao(database: FocusFlowDatabase): FocusSessionDao = database.focusSessionDao()
}

/** Hilt tests control time through [FakeTimeSource]. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [TimeModule::class])
interface TestTimeModule {
    @Binds
    fun bindsTimeSource(fake: FakeTimeSource): TimeSource
}
