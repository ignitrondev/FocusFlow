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
package com.ignitrondev.focusflow.domain.timer

/**
 * Durations and cycle rules used by [TimerEngine]. Defaults follow the classic Pomodoro method:
 * 25 minutes of focus, 5 minute short breaks and a 15 minute long break after every 4 focus
 * sessions.
 */
data class TimerConfig(
    val focusMillis: Long = 25 * MINUTE,
    val shortBreakMillis: Long = 5 * MINUTE,
    val longBreakMillis: Long = 15 * MINUTE,
    val longBreakInterval: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
) {
    init {
        require(focusMillis > 0 && shortBreakMillis > 0 && longBreakMillis > 0) { "Durations must be positive" }
        require(longBreakInterval >= 1) { "longBreakInterval must be at least 1" }
    }

    fun durationOf(type: SessionType): Long = when (type) {
        SessionType.Focus -> focusMillis
        SessionType.ShortBreak -> shortBreakMillis
        SessionType.LongBreak -> longBreakMillis
    }

    fun autoStarts(type: SessionType): Boolean = if (type == SessionType.Focus) autoStartFocus else autoStartBreaks

    private companion object {
        const val MINUTE = 60_000L
    }
}
