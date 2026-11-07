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

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.domain.timer.TimerConfig.Companion.MINUTE
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Timer settings, stored in the same preferences file as the appearance settings. Durations are
 * stored in whole minutes; values outside the allowed ranges (an old or edited file) are clamped.
 */
class DataStoreTimerConfigRepository @Inject constructor(private val dataStore: DataStore<Preferences>) : TimerConfigRepository {

    override val config: Flow<TimerConfig> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                Log.w(TAG, "Could not read timer settings, using defaults", error)
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { it.toTimerConfig() }
        .distinctUntilChanged()

    override suspend fun update(transform: (TimerConfig) -> TimerConfig) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toTimerConfig()).normalized()
            preferences.write(updated)
        }
    }

    private fun Preferences.toTimerConfig(): TimerConfig {
        val defaults = TimerConfig()
        return TimerConfig(
            focusMillis = minutes(Keys.FOCUS, defaults.focusMillis, TimerConfig.FOCUS_MINUTES),
            shortBreakMillis = minutes(Keys.SHORT_BREAK, defaults.shortBreakMillis, TimerConfig.SHORT_BREAK_MINUTES),
            longBreakMillis = minutes(Keys.LONG_BREAK, defaults.longBreakMillis, TimerConfig.LONG_BREAK_MINUTES),
            longBreakInterval = (this[Keys.INTERVAL] ?: defaults.longBreakInterval).coerceIn(TimerConfig.LONG_BREAK_INTERVAL),
            autoStartBreaks = this[Keys.AUTO_START_BREAKS] ?: defaults.autoStartBreaks,
            autoStartFocus = this[Keys.AUTO_START_FOCUS] ?: defaults.autoStartFocus,
        )
    }

    private fun Preferences.minutes(key: Preferences.Key<Int>, default: Long, range: IntRange): Long =
        (this[key] ?: (default / MINUTE).toInt()).coerceIn(range) * MINUTE

    private fun TimerConfig.normalized() = copy(
        focusMillis = focusMillis.wholeMinutesIn(TimerConfig.FOCUS_MINUTES),
        shortBreakMillis = shortBreakMillis.wholeMinutesIn(TimerConfig.SHORT_BREAK_MINUTES),
        longBreakMillis = longBreakMillis.wholeMinutesIn(TimerConfig.LONG_BREAK_MINUTES),
        longBreakInterval = longBreakInterval.coerceIn(TimerConfig.LONG_BREAK_INTERVAL),
    )

    private fun Long.wholeMinutesIn(range: IntRange): Long = (this / MINUTE).toInt().coerceIn(range) * MINUTE

    private fun MutablePreferences.write(config: TimerConfig) {
        this[Keys.FOCUS] = (config.focusMillis / MINUTE).toInt()
        this[Keys.SHORT_BREAK] = (config.shortBreakMillis / MINUTE).toInt()
        this[Keys.LONG_BREAK] = (config.longBreakMillis / MINUTE).toInt()
        this[Keys.INTERVAL] = config.longBreakInterval
        this[Keys.AUTO_START_BREAKS] = config.autoStartBreaks
        this[Keys.AUTO_START_FOCUS] = config.autoStartFocus
    }

    private object Keys {
        val FOCUS = intPreferencesKey("focus_minutes")
        val SHORT_BREAK = intPreferencesKey("short_break_minutes")
        val LONG_BREAK = intPreferencesKey("long_break_minutes")
        val INTERVAL = intPreferencesKey("long_break_interval")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
    }

    private companion object {
        const val TAG = "TimerSettings"
    }
}
