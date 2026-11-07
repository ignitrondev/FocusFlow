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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignitrondev.focusflow.core.time.dayBounds
import com.ignitrondev.focusflow.domain.timer.FocusSessionRepository
import com.ignitrondev.focusflow.domain.timer.FocusSummary
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.domain.timer.TimerState
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Presents the persisted timer. It holds no timer state of its own: the remaining time is always
 * derived from the stored deadline and the clock, so leaving the screen, rotating or process death
 * cannot lose or skew the countdown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModel(
    private val timerRepository: TimerRepository,
    focusSessionRepository: FocusSessionRepository,
    configRepository: TimerConfigRepository,
    private val timeSource: TimeSource,
    private val logError: (tag: String, message: String, error: Throwable?) -> Unit,
) : ViewModel() {

    init {
        // Recovers the timer and records sessions that ran out while the app was not running.
        launchSafely { timerRepository.refresh() }
    }

    private val ticks: Flow<Pair<TimerState, Long>> = timerRepository.state.flatMapLatest { state ->
        if (state.status == TimerStatus.Running) countdown(state) else flowOf(state to state.remainingMillis)
    }

    private val todaySummary: Flow<FocusSummary> = timerRepository.state
        .map { dayBounds(timeSource.now().epochMillis, timeSource.zone()) }
        .distinctUntilChanged()
        .flatMapLatest { (from, to) -> focusSessionRepository.observeFocusSummary(from, to) }

    val uiState: StateFlow<TimerUiState> = combine(ticks, configRepository.config, todaySummary) { (state, remaining), config, today ->
        state.toUiState(remaining, config, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    fun onAction(action: TimerAction) {
        launchSafely {
            when (action) {
                is TimerAction.SelectMode -> timerRepository.select(action.mode)

                TimerAction.StartOrPause ->
                    // Decided on the stored state, not on what the screen last showed.
                    if (timerRepository.state.first().status == TimerStatus.Running) {
                        timerRepository.pause()
                    } else {
                        timerRepository.start()
                    }

                TimerAction.Reset -> timerRepository.reset()

                TimerAction.Skip -> timerRepository.skip()
            }
        }
    }

    /** Emits the remaining time whenever the displayed second changes, and completes at zero. */
    private fun countdown(state: TimerState): Flow<Pair<TimerState, Long>> = flow {
        while (true) {
            val remaining = state.remainingAt(timeSource.now())
            emit(state to remaining.coerceAtLeast(0))
            if (remaining <= 0) {
                // The repository records the completion; the new state then replaces this flow.
                launchSafely { timerRepository.refresh() }
                return@flow
            }
            delay((remaining - 1) % 1_000 + 1)
        }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(TAG, "Timer update failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "TimerViewModel"
    }
}

internal fun TimerState.toUiState(remainingMillis: Long, config: TimerConfig, today: FocusSummary): TimerUiState {
    val interval = config.longBreakInterval
    val position = if (sessionType == SessionType.Focus) completedFocusInCycle + 1 else completedFocusInCycle
    return TimerUiState(
        mode = sessionType,
        // Rounded up, so the display shows 25:00 at the start and 00:01 during the last second.
        remainingSeconds = (remainingMillis + 999) / 1_000,
        totalSeconds = (plannedDurationMillis + 999) / 1_000,
        isRunning = status == TimerStatus.Running,
        isPaused = status == TimerStatus.Paused,
        canSelectMode = !isStarted,
        cyclePosition = position.coerceIn(1, interval),
        cycleLength = interval,
        todayFocusMinutes = today.focusMillis / 60_000,
        todayCompletedSessions = today.completedSessions,
    )
}
