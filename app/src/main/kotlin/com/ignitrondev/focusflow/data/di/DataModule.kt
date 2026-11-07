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
package com.ignitrondev.focusflow.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ignitrondev.focusflow.data.repository.DataStoreTimerConfigRepository
import com.ignitrondev.focusflow.data.repository.DataStoreUserPreferencesRepository
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Wires the shared (multiplatform) settings repositories into Hilt for the Android app. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun providesUserPreferencesRepository(dataStore: DataStore<Preferences>): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(dataStore)

    @Provides
    @Singleton
    fun providesTimerConfigRepository(dataStore: DataStore<Preferences>): TimerConfigRepository = DataStoreTimerConfigRepository(dataStore)
}
