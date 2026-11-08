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
import com.ignitrondev.focusflow.domain.achievement.Achievements

/**
 * One local notification per call. Ids are stable per achievement id so a burst of up to 13
 * stays as 13 notifications. Nothing is posted when notifications are not allowed, and this
 * path does not ask for permission.
 */
internal object AchievementNotifications {
    const val CHANNEL_ID = "achievements"

    /** Above the timer notification ids (1 and 2). */
    private const val ID_BASE = 100

    fun post(context: Context, achievementId: String, criteriaLabel: String) {
        if (!context.notificationsAllowed()) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_achievements))
                .setDescription(context.getString(R.string.notification_channel_achievements_description))
                .build(),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(criteriaLabel)
            .setContentText(criteriaLabel)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        notify(manager, idFor(achievementId), notification)
    }

    fun idFor(achievementId: String): Int {
        val index = Achievements.all.indexOfFirst { it.id == achievementId }
        return ID_BASE + if (index >= 0) index else Achievements.all.size
    }

    @SuppressLint("MissingPermission")
    private fun notify(manager: NotificationManagerCompat, id: Int, notification: android.app.Notification) {
        manager.notify(id, notification)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_REQUEST,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private const val OPEN_REQUEST = 20
}
