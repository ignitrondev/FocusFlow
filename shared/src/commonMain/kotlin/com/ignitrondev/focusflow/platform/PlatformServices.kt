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

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Everything the shared UI needs from the operating system. Each app (Android, iOS) provides one
 * implementation; the shared code never touches platform APIs directly.
 */
interface PlatformServices {
    /** Version shown in Settings → About. */
    val appVersion: String

    /** Whether the system offers wallpaper-based ("dynamic") colors. */
    val supportsDynamicColor: Boolean

    /** The dynamic color scheme, or null where it is not available. */
    @Composable
    fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

    /** Returns a function that asks for the notification permission when it is still needed. */
    @Composable
    fun rememberNotificationPermissionRequest(): () -> Unit

    /** What the system allows for session alerts, re-read when the app returns to the foreground. */
    @Composable
    fun rememberAlertSettings(): AlertSettings

    /** The optional photo behind a shared statistics card. */
    @Composable
    fun rememberCardPhoto(): CardPhoto

    /** Hands an image to the system share sheet. */
    @Composable
    fun rememberImageSharer(): ImageSharer

    /** A short, non-blocking message (a toast on Android). */
    fun showMessage(message: String)

    val dates: DateFormats

    /**
     * First day of the week in the device locale: the format locale's calendar on Android, and
     * `NSCalendar.firstWeekday` on iOS (which also honors the user's calendar setting).
     */
    fun firstDayOfWeek(): DayOfWeek

    fun logError(tag: String, message: String, error: Throwable? = null)

    /**
     * True when tab-switch motion must be skipped: Reduce Motion, or Android animation scale 0.
     * Re-read when the app returns to the foreground.
     */
    @Composable
    fun rememberSystemAnimationsDisabled(): Boolean
}

/** What the system allows for session alerts, and the actions to change it. */
data class AlertSettings(
    val notificationsAllowed: Boolean,
    /** Null where the user cannot turn precise alerts off. */
    val exactAlarmsAllowed: Boolean?,
    /** Name of the sound played when a session ends ("Silent", a ringtone title, ...). */
    val alertSoundTitle: String,
    val openNotificationSettings: () -> Unit,
    /** Null where the platform has no sound picker. */
    val pickAlertSound: (() -> Unit)?,
    /** Null where there are no further alert options to open. */
    val openAlertOptions: (() -> Unit)?,
    val openExactAlarmSettings: () -> Unit,
)

class CardPhoto(
    val photo: ImageBitmap?,
    val canTakePhoto: Boolean,
    val takePhoto: () -> Unit,
    val choosePhoto: () -> Unit,
    val remove: () -> Unit,
)

fun interface ImageSharer {
    /** Shares [image]; [text] accompanies it where the receiving app supports text. */
    suspend fun share(image: ImageBitmap, text: String, chooserTitle: String)
}

/** Dates and times formatted for the user's locale. */
interface DateFormats {
    /** E.g. "Saturday, November 7, 2026". */
    fun fullDate(date: LocalDate): String

    /** E.g. "Nov 7, 2026". */
    fun mediumDate(date: LocalDate): String

    /** E.g. "7 Nov". */
    fun dayMonth(date: LocalDate): String

    /** Short month name in the user's locale, e.g. "Nov". */
    fun monthShort(date: LocalDate): String

    /** E.g. "9:30 AM" or "09.30". */
    fun shortTime(time: LocalTime): String

    /** E.g. "Mon". */
    fun weekdayShort(day: DayOfWeek): String

    /** E.g. "M". */
    fun weekdayNarrow(day: DayOfWeek): String
}
