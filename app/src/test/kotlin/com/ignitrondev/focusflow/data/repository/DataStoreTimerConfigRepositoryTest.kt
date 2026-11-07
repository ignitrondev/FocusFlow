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
package com.ignitrondev.focusflow.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreTimerConfigRepositoryTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val minute = TimerConfig.MINUTE

    private fun TestScope.create(): Pair<DataStoreTimerConfigRepository, DataStore<Preferences>> {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { tmpFolder.newFile("t.preferences_pb") })
        return DataStoreTimerConfigRepository(dataStore) to dataStore
    }

    @Test
    fun defaultsAreTheClassicPomodoro() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(TimerConfig(), create().first.config.first())
    }

    @Test
    fun changesArePersisted() = runTest(UnconfinedTestDispatcher()) {
        val (repository, _) = create()

        repository.update { it.copy(focusMillis = 50 * minute, longBreakInterval = 3, autoStartBreaks = true) }

        val config = repository.config.first()
        assertEquals(50 * minute, config.focusMillis)
        assertEquals(3, config.longBreakInterval)
        assertEquals(true, config.autoStartBreaks)
        assertEquals("untouched values keep their default", 5 * minute, config.shortBreakMillis)
    }

    @Test
    fun valuesOutsideTheAllowedRangesAreClamped() = runTest(UnconfinedTestDispatcher()) {
        val (repository, dataStore) = create()
        dataStore.edit {
            it[intPreferencesKey("focus_minutes")] = 600
            it[intPreferencesKey("short_break_minutes")] = 0
            it[intPreferencesKey("long_break_interval")] = -2
        }

        val config = repository.config.first()

        assertEquals(TimerConfig.FOCUS_MINUTES.last * minute, config.focusMillis)
        assertEquals(TimerConfig.SHORT_BREAK_MINUTES.first * minute, config.shortBreakMillis)
        assertEquals(TimerConfig.LONG_BREAK_INTERVAL.first, config.longBreakInterval)
    }
}
