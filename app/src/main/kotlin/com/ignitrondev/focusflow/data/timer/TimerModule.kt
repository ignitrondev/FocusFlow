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

import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {
    @Binds
    abstract fun bindsTimerRepository(repository: RoomTimerRepository): TimerRepository

    @Binds
    abstract fun bindsFocusSessionRepository(repository: RoomFocusSessionRepository): FocusSessionRepository

    @Binds
    abstract fun bindsTimerConfigRepository(repository: DefaultTimerConfigRepository): TimerConfigRepository

    companion object {
        @Provides
        fun providesTimerEngine(): TimerEngine = TimerEngine(newRunId = { UUID.randomUUID().toString() })
    }
}
