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
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickAlertSoundTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = PickAlertSound("Alert sound")
    private val chime = Uri.parse("content://media/internal/audio/media/42")

    @Test
    fun opensAndroidsPickerWithTheCurrentSoundSelected() {
        val intent = contract.createIntent(context, chime)

        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, intent.action)
        assertEquals(chime, IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri::class.java))
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false))
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false))
    }

    @Test
    fun readsThePickedSoundSilenceOrCancel() {
        val picked = Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, chime)

        assertEquals(AlertSoundPick.Picked(chime), contract.parseResult(Activity.RESULT_OK, picked))
        assertEquals(AlertSoundPick.Picked(null), contract.parseResult(Activity.RESULT_OK, Intent()))
        assertEquals(AlertSoundPick.Cancelled, contract.parseResult(Activity.RESULT_CANCELED, null))
    }
}
