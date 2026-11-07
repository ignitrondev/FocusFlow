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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wakes FocusFlow when a running session reaches its deadline, so the session is recorded and the
 * user alerted even if the app is closed.
 *
 * The alarm uses the monotonic clock (ELAPSED_REALTIME_WAKEUP), like the timer itself, so wall
 * clock changes cannot move it. It is exact only when the system allows it: since Android 12 that
 * needs the "Alarms & reminders" permission, which Android 14+ does not grant by default. Without
 * it the alarm is inexact and, in battery-saving idle modes, may fire several minutes late.
 */
@Singleton
class TimerAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    fun canScheduleExact(): Boolean = context.exactAlarmsAllowed()

    fun schedule(triggerAtElapsedRealtimeMillis: Long) {
        val manager = alarmManager ?: return
        val intent = alarmIntent()
        try {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsedRealtimeMillis, intent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsedRealtimeMillis, intent)
            }
        } catch (e: SecurityException) {
            // The exact-alarm permission was revoked between the check and the call.
            Log.w(TAG, "Exact alarm not allowed, using an inexact one", e)
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsedRealtimeMillis, intent)
        }
    }

    fun cancel() {
        alarmManager?.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TimerActionReceiver::class.java).setAction(TimerActionReceiver.ACTION_DEADLINE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "TimerAlarm"
        const val REQUEST_CODE = 100
    }
}
