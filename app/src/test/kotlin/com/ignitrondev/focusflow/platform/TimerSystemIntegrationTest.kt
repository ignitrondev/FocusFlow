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
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import com.ignitrondev.focusflow.testing.FakeTimeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The deadline alarm firing while no screen is open: the path a closed app relies on. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class TimerSystemIntegrationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var integration: TimerSystemIntegration

    @Inject lateinit var repository: TimerRepository

    @Inject lateinit var sessions: FocusSessionDao

    @Inject lateinit var clock: FakeTimeSource

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val alarmManager = app.getSystemService(AlarmManager::class.java)
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        integration.start()
    }

    /** Work runs on background threads (Room, application scope); poll until it is visible. */
    private fun eventually(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Condition not met within 5 s" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    private fun nextAlarm() = shadowOf(alarmManager).peekNextScheduledAlarm()

    private fun notificationsOn(channel: String) = shadowOf(notificationManager).allNotifications.filter { it.channelId == channel }

    @Test
    fun runningTimerSchedulesTheDeadlineAndShowsTheCountdown() {
        runBlocking { repository.start() }

        eventually { nextAlarm() != null }
        assertEquals(clock.elapsedRealtimeMillis + 25 * 60_000L, nextAlarm()!!.triggerAtMs)
        eventually { notificationsOn(TimerNotifier.CHANNEL_TIMER).isNotEmpty() }
    }

    @Test
    fun pausingCancelsTheAlarm() {
        runBlocking { repository.start() }
        eventually { nextAlarm() != null }

        runBlocking { repository.pause() }

        eventually { nextAlarm() == null }
    }

    @Test
    fun deadlineAlarmRecordsTheSessionAndAlertsOnce() {
        runBlocking { repository.start() }
        eventually { nextAlarm() != null }
        clock.advanceBy(25 * 60_000L)

        val deadline = Intent(app, TimerActionReceiver::class.java).setAction(TimerActionReceiver.ACTION_DEADLINE)
        app.sendBroadcast(deadline)
        app.sendBroadcast(deadline) // a late duplicate delivery must not record twice

        eventually {
            runBlocking { sessions.getAll().isNotEmpty() } && notificationsOn(AlertChannels.sessionEndChannelId(app)).isNotEmpty()
        }
        eventually { notificationsOn(TimerNotifier.CHANNEL_TIMER).isEmpty() }
        Thread.sleep(200)

        assertEquals(1, runBlocking { sessions.getAll() }.size)
        assertEquals(1, notificationsOn(AlertChannels.sessionEndChannelId(app)).size)
        assertNull("next session waits for the user", nextAlarm())
        val state = runBlocking { repository.state.first() }
        assertEquals(SessionType.ShortBreak, state.sessionType)
        assertEquals(TimerStatus.Idle, state.status)
    }

    @Test
    fun notificationButtonsControlTheTimer() {
        runBlocking { repository.start() }
        eventually { nextAlarm() != null }

        app.sendBroadcast(Intent(app, TimerActionReceiver::class.java).setAction(TimerActionReceiver.ACTION_PAUSE))
        eventually { runBlocking { repository.state.first().status } == TimerStatus.Paused }

        app.sendBroadcast(Intent(app, TimerActionReceiver::class.java).setAction(TimerActionReceiver.ACTION_RESUME))
        eventually { runBlocking { repository.state.first().status } == TimerStatus.Running }
        eventually { nextAlarm() != null }
    }

    @Test
    fun bootBroadcastRecordsSessionsThatEndedWhileTheDeviceWasOff() {
        runBlocking { repository.start() }
        eventually { nextAlarm() != null }
        clock.reboot(downtimeMillis = 60 * 60_000L)

        app.sendBroadcast(Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(app.packageName))

        eventually { runBlocking { sessions.getAll().size } == 1 }
        assertTrue(runBlocking { sessions.getAll() }.single().completed)
    }
}
