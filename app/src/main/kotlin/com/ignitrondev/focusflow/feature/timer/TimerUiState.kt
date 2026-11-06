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
package com.ignitrondev.focusflow.feature.timer

import androidx.compose.runtime.Immutable

enum class TimerMode { Focus, ShortBreak, LongBreak }

/**
 * Everything the timer screen renders. Phase 1 only shows the idle state; the Phase 2 timer
 * engine produces this from the persisted, deadline-based timer state.
 */
@Immutable
data class TimerUiState(
    val mode: TimerMode = TimerMode.Focus,
    val remainingSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    /** 1-based position in the current long-break cycle, e.g. "2 of 4". */
    val cyclePosition: Int = 1,
    val cycleLength: Int = 4,
    val todayFocusMinutes: Long = 0,
    val todayCompletedSessions: Int = 0,
) {
    /** Fraction of the session still remaining, from 1 (just started) to 0 (finished). */
    val remainingFraction: Float
        get() = if (totalSeconds <= 0) 0f else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

sealed interface TimerAction {
    data class SelectMode(val mode: TimerMode) : TimerAction

    data object StartOrPause : TimerAction

    data object Reset : TimerAction

    data object Skip : TimerAction
}
