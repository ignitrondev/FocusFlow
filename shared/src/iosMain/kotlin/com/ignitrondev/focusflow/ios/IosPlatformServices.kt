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
package com.ignitrondev.focusflow.ios

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ignitrondev.focusflow.core.time.dayOfWeekFromSundayBased
import com.ignitrondev.focusflow.platform.AlertSettings
import com.ignitrondev.focusflow.platform.CardPhoto
import com.ignitrondev.focusflow.platform.DateFormats
import com.ignitrondev.focusflow.platform.ImageSharer
import com.ignitrondev.focusflow.platform.PlatformServices
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.share_photo_failed
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSBundle
import platform.Foundation.NSCalendar
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

/** The iOS side of the shared UI: notification permission, photos, sharing, formats. */
class IosPlatformServices : PlatformServices {
    private val mainScope = MainScope()

    override val appVersion: String = run {
        val info = NSBundle.mainBundle
        // The full name ("1.0.0-beta.1"); CFBundleShortVersionString only allows digits and dots.
        (info.objectForInfoDictionaryKey("FocusFlowVersionName") ?: info.objectForInfoDictionaryKey("CFBundleShortVersionString"))
            as? String ?: ""
    }

    // iOS has no wallpaper-based colors.
    override val supportsDynamicColor: Boolean = false

    @Composable
    override fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

    /** iOS shows its permission dialog once; later calls do nothing, so no bookkeeping is needed. */
    @Composable
    override fun rememberNotificationPermissionRequest(): () -> Unit = remember { { requestNotificationPermission {} } }

    @Composable
    override fun rememberAlertSettings(): AlertSettings {
        var status by remember { mutableStateOf<UNAuthorizationStatus?>(null) }
        val scope = rememberCoroutineScope()
        // The permission changes in the Settings app, outside FocusFlow: re-read it on every return.
        LifecycleResumeEffect(Unit) {
            val job = scope.launch { status = notificationStatus() }
            onPauseOrDispose { job.cancel() }
        }
        return AlertSettings(
            notificationsAllowed = status in ALLOWED,
            // iOS delivers scheduled notifications on time; there is nothing to turn on.
            exactAlarmsAllowed = null,
            alertSoundTitle = "",
            openNotificationSettings = {
                if (status == UNAuthorizationStatusNotDetermined) {
                    requestNotificationPermission { scope.launch { status = notificationStatus() } }
                } else {
                    openAppSettings()
                }
            },
            // Sounds and banners are set per app in the Settings app.
            pickAlertSound = null,
            openAlertOptions = null,
            openExactAlarmSettings = {},
        )
    }

    @Composable
    override fun rememberCardPhoto(): CardPhoto {
        var photo by remember { mutableStateOf<ImageBitmap?>(null) }
        val failed = stringResource(Res.string.share_photo_failed)
        val scope = rememberCoroutineScope()
        val picker = remember {
            PhotoPicker { image ->
                scope.launch {
                    val bitmap = image?.let { withContext(Dispatchers.Default) { it.toUprightImageBitmap() } }
                    if (bitmap == null) showMessage(failed) else photo = bitmap
                }
            }
        }
        return CardPhoto(
            photo = photo,
            canTakePhoto = PhotoPicker.cameraAvailable,
            takePhoto = {
                if (!picker.takePhoto()) showMessage(failed)
            },
            choosePhoto = {
                if (!picker.choosePhoto()) showMessage(failed)
            },
            remove = { photo = null },
        )
    }

    @Composable
    override fun rememberImageSharer(): ImageSharer = remember { ImageSharer { image, text, _ -> shareImage(image, text) } }

    /** iOS has no toast: a short alert that closes itself. */
    override fun showMessage(message: String) {
        mainScope.launch {
            val host = topViewController() ?: return@launch
            val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
            host.presentViewController(alert, animated = true, completion = null)
            delay(MESSAGE_MILLIS)
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }

    override val dates: DateFormats = IosDateFormats

    override fun firstDayOfWeek(): DayOfWeek = dayOfWeekFromSundayBased(NSCalendar.currentCalendar.firstWeekday.toInt())

    override fun logError(tag: String, message: String, error: Throwable?) {
        NSLog("%@", "E/$tag: $message${error?.let { "\n" + it.stackTraceToString() }.orEmpty()}")
    }

    private fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    private companion object {
        val ALLOWED = setOf(UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral)
        const val MESSAGE_MILLIS = 1_500L
    }
}

internal fun requestNotificationPermission(onDone: () -> Unit) {
    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> onDone() }
}

private suspend fun notificationStatus(): UNAuthorizationStatus = suspendCancellableCoroutine { continuation ->
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
    }
}
