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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import com.ignitrondev.focusflow.R

/**
 * The "session finished" notification channel and its sound.
 *
 * Android does not let an app change a channel's sound after creating it (only the user can, in
 * system settings). To apply a sound picked in FocusFlow, a new channel with the next version
 * number is created with that sound, the user's other choices (importance, vibration) are carried
 * over, and the old channel is deleted. There is always exactly one such channel, so its current
 * id and sound can be read back from the system, including changes made in Android settings.
 */
object AlertChannels {
    private const val PREFIX = "session_end"
    private const val TAG = "AlertChannels"

    /** The id of the current session-end channel, creating the default one on first use. */
    fun sessionEndChannelId(context: Context): String = current(context)?.id ?: createDefault(context)

    /** The sound of the current channel; null means silent. */
    fun sound(context: Context): Uri? {
        sessionEndChannelId(context)
        return current(context)?.sound
    }

    /** Replaces the alert sound. [sound] null means silent. */
    fun setSound(context: Context, sound: Uri?) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val old = current(context)
        if (old != null && old.sound == sound) return
        val name = context.getString(R.string.notification_channel_session_end)
        val next = NotificationChannel(nextId(old), name, old?.importance ?: NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.notification_channel_session_end_description)
            enableVibration(old?.shouldVibrate() ?: true)
            old?.vibrationPattern?.let { vibrationPattern = it }
            setSound(sound, alertAudioAttributes())
        }
        manager.createNotificationChannel(next)
        old?.let { manager.deleteNotificationChannel(it.id) }
    }

    /** A human-readable name for [sound]: "Silent", the ringtone title, or a generic fallback. */
    fun soundTitle(context: Context, sound: Uri?): String {
        if (sound == null) return context.getString(R.string.settings_alert_sound_silent)
        val title = try {
            RingtoneManager.getRingtone(context, sound)?.getTitle(context)
        } catch (e: SecurityException) {
            // A custom file the app may not read; the system can still play it.
            Log.w(TAG, "Cannot read the title of $sound", e)
            null
        }
        val name = title ?: context.getString(R.string.settings_alert_sound_custom)
        return if (sound == Settings.System.DEFAULT_NOTIFICATION_URI) {
            context.getString(R.string.settings_alert_sound_default, name)
        } else {
            name
        }
    }

    internal fun channels(context: Context): List<NotificationChannel> =
        context.getSystemService<NotificationManager>()?.notificationChannels.orEmpty().filter { it.id.startsWith(PREFIX) }

    private fun current(context: Context): NotificationChannel? = channels(context).maxByOrNull { version(it.id) }

    private fun createDefault(context: Context): String {
        val name = context.getString(R.string.notification_channel_session_end)
        val channel = NotificationChannel(PREFIX, name, NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.notification_channel_session_end_description)
            enableVibration(true)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        return PREFIX
    }

    private fun version(id: String): Int = id.removePrefix(PREFIX).removePrefix("_v").toIntOrNull() ?: 1

    private fun nextId(old: NotificationChannel?): String = "${PREFIX}_v${(old?.let { version(it.id) } ?: 1) + 1}"

    private fun alertAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}
