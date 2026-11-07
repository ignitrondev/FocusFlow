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
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class TimerAlarmSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val scheduler = TimerAlarmScheduler(context)

    @Test
    fun deadlineAlarmUsesTheMonotonicClock() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.schedule(triggerAtElapsedRealtimeMillis = 1_500_000L)

        val alarm = shadowOf(alarmManager).peekNextScheduledAlarm()!!
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarm.getType())
        assertTrue("fires in Doze", alarm.isAllowWhileIdle)
        assertEquals(1_500_000L, alarm.triggerAtMs)
        assertTrue(scheduler.canScheduleExact())
    }

    @Test
    fun withoutTheExactAlarmPermissionAnInexactAlarmIsStillScheduled() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(triggerAtElapsedRealtimeMillis = 1_500_000L)

        assertFalse(scheduler.canScheduleExact())
        assertEquals(1_500_000L, shadowOf(alarmManager).peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun exactAlarmsArePermittedWithoutAskingTheUserOnAndroid13AndLater() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()

        // Granted at install for timer apps on Android 13+, so alerts are not deferred by the
        // inexact-alarm window (up to 75 % of the time until the alarm).
        assertTrue(Manifest.permission.USE_EXACT_ALARM in requested)
        // Only requested up to Android 12L (maxSdkVersion 32); there is no user toggle to ask for here.
        assertFalse(Manifest.permission.SCHEDULE_EXACT_ALARM in requested)
        assertNull("no Precise alerts setting to show on Android 13+", context.alertCapabilities().exactAlarmsAllowed)
    }

    @Test
    fun reschedulingReplacesTheAlarmAndCancelRemovesIt() {
        scheduler.schedule(1_000L)
        scheduler.schedule(2_000L)

        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
        assertEquals(2_000L, shadowOf(alarmManager).peekNextScheduledAlarm()!!.triggerAtMs)

        scheduler.cancel()
        assertNull(shadowOf(alarmManager).peekNextScheduledAlarm())
    }
}
