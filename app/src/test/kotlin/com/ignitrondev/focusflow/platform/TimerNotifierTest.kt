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
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.testing.FakeTimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class TimerNotifierTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val notificationManager = app.getSystemService(NotificationManager::class.java)
    private val notifier = TimerNotifier(app)
    private val engine = TimerEngine(newRunId = { "run" })
    private val config = TimerConfig()
    private val clock = FakeTimeSource()

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.createChannels()
    }

    private fun posted(): List<Notification> = shadowOf(notificationManager).allNotifications

    @Test
    fun channelsSeparateTheSilentTimerFromTheAlert() {
        assertEquals(NotificationManager.IMPORTANCE_LOW, notificationManager.getNotificationChannel(TimerNotifier.CHANNEL_TIMER).importance)
        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            notificationManager.getNotificationChannel(TimerNotifier.CHANNEL_SESSION_END).importance,
        )
    }

    @Test
    fun runningTimerShowsASystemCountdown() {
        val running = engine.start(engine.initial(config), clock.now()).state

        notifier.showTimer(running, remainingMillis = 10 * 60_000L)

        val notification = posted().single()
        assertEquals(TimerNotifier.CHANNEL_TIMER, notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals("Pause", notification.actions.single().title)
    }

    @Test
    fun pausedTimerShowsTheTimeLeftAndIdleRemovesIt() {
        val paused = engine.pause(engine.start(engine.initial(config), clock.now()).state, clock.now()).state

        notifier.showTimer(paused, remainingMillis = 12 * 60_000L + 30_000L)
        val notification = posted().single()
        assertEquals("Paused · 12:30 left", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(listOf("Resume", "Reset"), notification.actions.map { it.title.toString() })

        notifier.showTimer(engine.initial(config), remainingMillis = 0)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun completedFocusSessionAlertsAndSuggestsTheBreak() {
        val next = engine.select(engine.initial(config), SessionType.ShortBreak, config).state

        notifier.showSessionEnded(focusSession(), next)

        val notification = posted().single()
        assertEquals(TimerNotifier.CHANNEL_SESSION_END, notification.channelId)
        assertEquals("Focus session complete", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Time for a Short break.", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun withoutPermissionNothingIsPostedAndNothingCrashes() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.showSessionEnded(focusSession(), engine.initial(config))

        assertTrue(posted().isEmpty())
    }

    private fun focusSession() = FinishedSession(
        id = "run",
        sessionType = SessionType.Focus,
        startedAtEpochMillis = 0,
        endedAtEpochMillis = 25 * 60_000L,
        plannedDurationMillis = 25 * 60_000L,
        actualDurationMillis = 25 * 60_000L,
        completed = true,
    )
}
