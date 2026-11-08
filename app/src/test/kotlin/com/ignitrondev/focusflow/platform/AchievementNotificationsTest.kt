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

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.domain.achievement.Achievements
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AchievementNotificationsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    @Before
    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    private fun posted(): List<Notification> = shadowOf(notificationManager).allNotifications

    @Test
    fun withoutPermissionNothingIsPosted() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        Achievements.all.forEach { AchievementNotifications.post(app, it.id, it.id) }

        assertTrue(posted().isEmpty())
    }

    @Test
    fun eachAchievementIsASeparateNotification() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        Achievements.all.forEach { achievement ->
            AchievementNotifications.post(app, achievement.id, achievement.id)
        }
        AchievementNotifications.post(app, "sessions_1", "sessions_1")

        val notifications = posted()
        assertEquals(13, notifications.size)
        assertEquals(
            Achievements.all.map {
                it.id
            }.toSet(),
            notifications.map { it.extras.getCharSequence(Notification.EXTRA_TITLE).toString() }.toSet(),
        )
        assertTrue(notifications.all { it.group == null })
        assertTrue(notifications.all { it.channelId == AchievementNotifications.CHANNEL_ID })
        assertEquals(13, Achievements.all.map { AchievementNotifications.idFor(it.id) }.toSet().size)
        assertTrue(Achievements.all.map { AchievementNotifications.idFor(it.id) }.all { it >= 100 })
    }
}
