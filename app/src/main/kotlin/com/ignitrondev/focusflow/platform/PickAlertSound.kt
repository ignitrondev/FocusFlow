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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat

sealed interface AlertSoundPick {
    data object Cancelled : AlertSoundPick

    /** [sound] null means the user chose "Silent". */
    data class Picked(val sound: Uri?) : AlertSoundPick
}

/**
 * Android's own sound picker (the one used by alarm clocks and messaging apps), listing every
 * system sound (alarms, notifications, ringtones) and the user's own, plus "Default" and "Silent".
 * Input: the sound selected now.
 */
class PickAlertSound(private val title: String) : ActivityResultContract<Uri?, AlertSoundPick>() {
    override fun createIntent(context: Context, input: Uri?): Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        // TYPE_ALL, not TYPE_NOTIFICATION or TYPE_ALARM: Google's sound picker on Android 17 crashes
        // ("Invalid RingtoneManager type: 6") when that combination meets an existing media sound,
        // such as an alarm tone picked earlier. It accepts single types and TYPE_ALL.
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, input)

    override fun parseResult(resultCode: Int, intent: Intent?): AlertSoundPick {
        if (resultCode != Activity.RESULT_OK || intent == null) return AlertSoundPick.Cancelled
        return AlertSoundPick.Picked(IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java))
    }
}
