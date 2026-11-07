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

import com.ignitrondev.focusflow.data.di.DataModule
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

/** Replaces disk-backed repositories with in-memory fakes in every Hilt test. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
interface TestDataModule {
    @Binds
    fun bindsUserPreferencesRepository(fake: FakeUserPreferencesRepository): UserPreferencesRepository

    @Binds
    fun bindsTimerConfigRepository(fake: FakeTimerConfigRepository): TimerConfigRepository
}
