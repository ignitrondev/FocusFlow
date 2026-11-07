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

import android.app.Application
import android.app.NotificationManager
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlertChannelsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val chime = Uri.parse("content://media/internal/audio/media/42")

    @Test
    fun defaultChannelIsCreatedOnceWithTheDefaultSound() {
        val first = AlertChannels.sessionEndChannelId(app)
        val second = AlertChannels.sessionEndChannelId(app)

        assertEquals("session_end", first)
        assertEquals(first, second)
        assertEquals(1, AlertChannels.channels(app).size)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(first).importance)
        assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI, AlertChannels.sound(app))
    }

    @Test
    fun pickingASoundReplacesTheChannelAndKeepsTheUsersOtherChoices() {
        val old = manager.getNotificationChannel(AlertChannels.sessionEndChannelId(app))
        // The user turned vibration off in Android settings.
        old.enableVibration(false)
        manager.createNotificationChannel(old)

        AlertChannels.setSound(app, chime)

        val channels = AlertChannels.channels(app)
        assertEquals("the old channel is deleted", listOf("session_end_v2"), channels.map { it.id })
        assertEquals(chime, channels.single().sound)
        assertFalse(channels.single().shouldVibrate())
        assertEquals("session_end_v2", AlertChannels.sessionEndChannelId(app))
    }

    @Test
    fun silentAndRepeatedPicks() {
        AlertChannels.setSound(app, chime)
        AlertChannels.setSound(app, chime) // same sound: nothing to do
        assertEquals("session_end_v2", AlertChannels.sessionEndChannelId(app))

        AlertChannels.setSound(app, null)

        assertEquals("session_end_v3", AlertChannels.sessionEndChannelId(app))
        assertNull(AlertChannels.sound(app))
        assertEquals("Silent", AlertChannels.soundTitle(app, null))
    }
}
