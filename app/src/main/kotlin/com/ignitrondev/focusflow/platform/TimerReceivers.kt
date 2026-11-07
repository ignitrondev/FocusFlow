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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ignitrondev.focusflow.core.common.ApplicationScope
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs [block] after onReceive returns, keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.runAsync(scope: CoroutineScope, tag: String, block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Timer update from broadcast failed", e)
        } finally {
            pending.finish()
        }
    }
}

/** The deadline alarm and the notification buttons. Not exported: only FocusFlow can send these. */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {
    @Inject lateinit var timerRepository: TimerRepository

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        runAsync(scope, TAG) {
            when (action) {
                // Completing the session also posts the alert, via SessionCompletionListener.
                ACTION_DEADLINE -> timerRepository.refresh()

                ACTION_PAUSE -> timerRepository.pause()

                ACTION_RESUME -> timerRepository.start()

                ACTION_RESET -> timerRepository.reset()
            }
        }
    }

    companion object {
        private const val TAG = "TimerActionReceiver"
        const val ACTION_DEADLINE = "com.ignitrondev.focusflow.action.DEADLINE"
        const val ACTION_PAUSE = "com.ignitrondev.focusflow.action.PAUSE"
        const val ACTION_RESUME = "com.ignitrondev.focusflow.action.RESUME"
        const val ACTION_RESET = "com.ignitrondev.focusflow.action.RESET"
    }
}

/**
 * Restores the timer after events that clear alarms: a reboot or an app update. Also reacts to the
 * user granting the exact-alarm permission. These broadcasts are protected (only the system can
 * send them); anything else is ignored. Starting the process is enough to resync the alarm and
 * notification (see [TimerSystemIntegration.start]); the refresh also records sessions that ended
 * while the device was off.
 */
@AndroidEntryPoint
class TimerRestoreReceiver : BroadcastReceiver() {
    @Inject lateinit var timerRepository: TimerRepository

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        runAsync(scope, TAG) { timerRepository.refresh() }
    }

    private companion object {
        const val TAG = "TimerRestoreReceiver"
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31); older
            // versions never send it.
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
