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
package com.ignitrondev.focusflow.platform

import android.util.Log
import com.ignitrondev.focusflow.core.common.ApplicationScope
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionCompletionListener
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.domain.timer.TimerState
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the system (alarm and notifications) in line with the stored timer. Started with the
 * process, so it also runs when an alarm or the boot broadcast starts FocusFlow in the background.
 * Syncing is idempotent: every state change simply re-applies the alarm and the notification.
 */
@Singleton
class TimerSystemIntegration @Inject constructor(
    private val timerRepository: TimerRepository,
    private val alarmScheduler: TimerAlarmScheduler,
    private val notifier: TimerNotifier,
    private val timeSource: TimeSource,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        notifier.createChannels()
        scope.launch {
            runCatchingLogged { timerRepository.refresh() }
            timerRepository.state.collect { state -> runCatchingLogged { sync(state) } }
        }
    }

    private fun sync(state: TimerState) {
        val now = timeSource.now()
        val remaining = state.remainingAt(now).coerceAtLeast(0)
        if (state.status == TimerStatus.Running) {
            alarmScheduler.schedule(now.elapsedRealtimeMillis + remaining)
        } else {
            alarmScheduler.cancel()
        }
        notifier.showTimer(state, remaining)
    }

    private suspend fun runCatchingLogged(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Timer sync failed", e)
        }
    }

    private companion object {
        const val TAG = "TimerSystem"
    }
}

/** Alerts the user when a session completes. Kept separate to avoid a dependency cycle. */
@Singleton
class SessionEndAlert @Inject constructor(private val notifier: TimerNotifier) : SessionCompletionListener {
    override fun onSessionsCompleted(sessions: List<FinishedSession>, next: TimerState) {
        // Several sessions can complete at once after a long absence: one alert is enough.
        notifier.showSessionEnded(sessions.last(), next)
    }
}
