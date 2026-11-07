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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri

/** What Android currently allows FocusFlow to do for its alerts. */
data class AlertCapabilities(
    val notificationsAllowed: Boolean,
    /**
     * Null where the user cannot turn exact alarms off: before Android 12 (no permission) and from
     * Android 13 (USE_EXACT_ALARM, granted at install). Only Android 12/12L have a user setting.
     */
    val exactAlarmsAllowed: Boolean?,
    /** Name of the sound played when a session ends ("Silent", a ringtone title, ...). */
    val alertSoundTitle: String = "",
)

fun Context.notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled() &&
    (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )

fun Context.exactAlarmsAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true

fun Context.alertCapabilities() = AlertCapabilities(
    notificationsAllowed = notificationsAllowed(),
    exactAlarmsAllowed = if (Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2) exactAlarmsAllowed() else null,
    alertSoundTitle = AlertChannels.soundTitle(this, AlertChannels.sound(this)),
)

fun Context.openNotificationSettings() = startSettings(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
)

fun Context.openAlertSoundSettings() = startSettings(
    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, AlertChannels.sessionEndChannelId(this)),
)

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        startSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()))
    }
}

private fun Context.startSettings(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Some OEM builds lack these screens; fall back to the app's details page.
        Log.w("SystemSettings", "Settings screen not available", e)
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
