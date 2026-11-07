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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.time.formatMinutesSeconds
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerState
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the timer's notifications:
 * - an ongoing, silent one while a session runs, counting down with the system chronometer (it
 *   ticks without FocusFlow's process running), or showing the time left while paused;
 * - an alerting one (sound and vibration from its channel) when a session completes.
 */
@Singleton
class TimerNotifier @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_TIMER, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.notification_channel_timer))
                    .setDescription(context.getString(R.string.notification_channel_timer_description))
                    .setShowBadge(false)
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_SESSION_END, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.notification_channel_session_end))
                    .setDescription(context.getString(R.string.notification_channel_session_end_description))
                    .setVibrationEnabled(true)
                    .build(),
            ),
        )
    }

    /** Shows, updates or removes the ongoing notification to match [state]. */
    fun showTimer(state: TimerState, remainingMillis: Long) {
        when (state.status) {
            TimerStatus.Idle -> manager.cancel(ID_TIMER)
            TimerStatus.Running, TimerStatus.Paused -> notify(ID_TIMER, timerNotification(state, remainingMillis))
        }
    }

    fun showSessionEnded(session: FinishedSession, next: TimerState) {
        val title = context.getString(
            if (session.sessionType == SessionType.Focus) R.string.notification_focus_done else R.string.notification_break_done,
        )
        val text = context.getString(
            when {
                next.status == TimerStatus.Running -> R.string.notification_next_started
                next.sessionType == SessionType.Focus -> R.string.notification_next_focus
                next.sessionType == SessionType.LongBreak -> R.string.notification_next_long_break
                else -> R.string.notification_next_short_break
            },
            context.getString(next.sessionType.labelRes()),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION_END)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        notify(ID_SESSION_END, notification)
    }

    fun canPostNotifications(): Boolean = context.notificationsAllowed()

    private fun timerNotification(state: TimerState, remainingMillis: Long) = NotificationCompat.Builder(context, CHANNEL_TIMER)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(state.sessionType.labelRes()))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(openAppIntent())
        .apply {
            if (state.status == TimerStatus.Running) {
                setUsesChronometer(true)
                setChronometerCountDown(true)
                setShowWhen(true)
                setWhen(System.currentTimeMillis() + remainingMillis)
                addAction(0, context.getString(R.string.timer_action_pause), actionIntent(TimerActionReceiver.ACTION_PAUSE))
            } else {
                setShowWhen(false)
                setContentText(context.getString(R.string.notification_paused, formatMinutesSeconds((remainingMillis + 999) / 1000)))
                addAction(0, context.getString(R.string.timer_action_resume), actionIntent(TimerActionReceiver.ACTION_RESUME))
                addAction(0, context.getString(R.string.timer_action_reset), actionIntent(TimerActionReceiver.ACTION_RESET))
            }
        }
        .build()

    // The permission is checked right before posting; without it the timer works silently.
    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: android.app.Notification) {
        if (!canPostNotifications()) return
        manager.notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, TimerActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_TIMER = "timer"
        const val CHANNEL_SESSION_END = "session_end"
        private const val ID_TIMER = 1
        private const val ID_SESSION_END = 2
    }
}

private fun SessionType.labelRes(): Int = when (this) {
    SessionType.Focus -> R.string.timer_mode_focus
    SessionType.ShortBreak -> R.string.timer_mode_short_break
    SessionType.LongBreak -> R.string.timer_mode_long_break
}
