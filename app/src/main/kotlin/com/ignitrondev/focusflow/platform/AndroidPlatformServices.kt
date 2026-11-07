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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ignitrondev.focusflow.BuildConfig
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.settings_alert_sound
import com.ignitrondev.focusflow.resources.share_photo_failed
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinDayOfWeek
import org.jetbrains.compose.resources.stringResource

/** The Android side of the shared UI: permissions, pickers, sharing, formats and colors. */
@Singleton
class AndroidPlatformServices @Inject constructor(@ApplicationContext private val context: Context) : PlatformServices {
    override val appVersion: String = BuildConfig.VERSION_NAME

    override val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @Composable
    override fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    /**
     * Asks once per screen visit for the notification permission (Android 13+). Android itself
     * stops showing the dialog after the user declined twice; the timer works either way.
     */
    @Composable
    override fun rememberNotificationPermissionRequest(): () -> Unit {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return {}
        val context = LocalContext.current
        var asked by rememberSaveable { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        return {
            if (!asked && !context.notificationsAllowed()) {
                asked = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Composable
    override fun rememberAlertSettings(): AlertSettings {
        val context = LocalContext.current
        // Permissions change in Android settings, outside the app: re-read them on every return.
        var capabilities by remember { mutableStateOf(context.alertCapabilities()) }
        LifecycleResumeEffect(Unit) {
            capabilities = context.alertCapabilities()
            onPauseOrDispose {}
        }
        val pickSound = rememberLauncherForActivityResult(PickAlertSound(stringResource(Res.string.settings_alert_sound))) { pick ->
            if (pick is AlertSoundPick.Picked) {
                AlertChannels.setSound(context, pick.sound)
                capabilities = context.alertCapabilities()
            }
        }
        return AlertSettings(
            notificationsAllowed = capabilities.notificationsAllowed,
            exactAlarmsAllowed = capabilities.exactAlarmsAllowed,
            alertSoundTitle = capabilities.alertSoundTitle,
            openNotificationSettings = context::openNotificationSettings,
            pickAlertSound = {
                try {
                    pickSound.launch(AlertChannels.sound(context))
                } catch (e: ActivityNotFoundException) {
                    // No sound picker on this device: Android's channel settings have one.
                    Log.w(TAG, "No ringtone picker available", e)
                    context.openAlertSoundSettings()
                }
            },
            openAlertOptions = context::openAlertSoundSettings,
            openExactAlarmSettings = context::openExactAlarmSettings,
        )
    }

    @Composable
    override fun rememberCardPhoto(): CardPhoto {
        val context = LocalContext.current
        var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
        var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
        var photo by remember { mutableStateOf<ImageBitmap?>(null) }
        val failed = stringResource(Res.string.share_photo_failed)

        // Decoded off the main thread, again after a configuration change (the URI is saved).
        LaunchedEffect(photoUri) {
            photo = photoUri?.let { uri ->
                withContext(Dispatchers.IO) { SharePhoto.load(context, uri) }?.asImageBitmap().also {
                    if (it == null) showMessage(failed)
                }
            }
        }
        val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            if (saved) photoUri = pendingCameraUri
        }
        val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) photoUri = uri
        }
        val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
        return CardPhoto(
            photo = photo,
            canTakePhoto = hasCamera,
            takePhoto = {
                val uri = SharePhoto.cameraOutput(context)
                pendingCameraUri = uri
                try {
                    takePhoto.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    Log.w(TAG, "No camera app", e)
                    showMessage(failed)
                }
            },
            choosePhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            remove = { photoUri = null },
        )
    }

    @Composable
    override fun rememberImageSharer(): ImageSharer {
        val context = LocalContext.current
        return remember(context) {
            ImageSharer { image, text, chooserTitle ->
                val uri = withContext(Dispatchers.IO) { ShareImage.export(context, image.asAndroidBitmap()) }
                context.startActivity(ShareImage.chooser(uri, text, chooserTitle))
            }
        }
    }

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override val dates: DateFormats = AndroidDateFormats

    override fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.toKotlinDayOfWeek()

    override fun logError(tag: String, message: String, error: Throwable?) {
        Log.e(tag, message, error)
    }

    private companion object {
        const val TAG = "PlatformServices"
    }
}

/** java.time formats in the current locale (read on every call, so a language change applies). */
private object AndroidDateFormats : DateFormats {
    private val locale: Locale get() = Locale.getDefault()

    override fun fullDate(date: LocalDate): String =
        date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

    override fun mediumDate(date: LocalDate): String =
        date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    override fun dayMonth(date: LocalDate): String = date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("d MMM", locale))

    override fun shortTime(time: LocalTime): String =
        time.toJavaLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

    override fun weekdayShort(day: DayOfWeek): String = java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.SHORT, locale)

    override fun weekdayNarrow(day: DayOfWeek): String = java.time.DayOfWeek.of(day.isoDayNumber).getDisplayName(TextStyle.NARROW, locale)
}
