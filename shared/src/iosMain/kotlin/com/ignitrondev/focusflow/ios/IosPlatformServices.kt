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
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
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
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
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

    /**
     * Reduce Motion lives in Settings, outside FocusFlow. Re-read it on every return to the
     * foreground, and while the app is resumed if the system posts a status change. True skips
     * the navbar tab transition.
     */
    @Composable
    override fun rememberSystemAnimationsDisabled(): Boolean {
        var disabled by remember { mutableStateOf(systemReduceMotionEnabled()) }
        LifecycleResumeEffect(Unit) {
            val center = NSNotificationCenter.defaultCenter
            // Subscribe first so a change between the read and the observer is not missed.
            val observer = center.addObserverForName(
                UIAccessibilityReduceMotionStatusDidChangeNotification,
                null,
                NSOperationQueue.mainQueue,
            ) { _ ->
                disabled = systemReduceMotionEnabled()
            }
            disabled = systemReduceMotionEnabled()
            onPauseOrDispose { center.removeObserver(observer) }
        }
        return disabled
    }

    /**
     * One local notification per call, on the same [UNUserNotificationCenter] as session alerts.
     * Does not ask for permission. When authorization is not already granted, this returns without
     * posting; the unlock stays in the app, and the caller marks it handled after this returns.
     *
     * [criteriaLabel] is the title and the body. There is no other copy.
     * [achievementId] (`sessions_1`, `streak_3`, `focus_hours_1`, …) becomes the request id, so a
     * burst of up to 13 calls stays as 13 notifications instead of replacing one another.
     */
    override fun notifyAchievementUnlocked(achievementId: String, criteriaLabel: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        // Reads the current authorization. Unlike [requestNotificationPermission], this does not prompt.
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined
            if (status in ALLOWED) postAchievementNotification(center, achievementId, criteriaLabel)
        }
    }

    private fun postAchievementNotification(center: UNUserNotificationCenter, achievementId: String, criteriaLabel: String) {
        val identifier = achievementNotificationIdentifier(achievementId)
        val content = UNMutableNotificationContent().apply {
            setTitle(criteriaLabel)
            setBody(criteriaLabel)
            setSound(UNNotificationSound.defaultSound)
            // Automatic grouping stacks notifications that share a thread. One thread per achievement
            // keeps a first-launch burst from collapsing into a single group.
            setThreadIdentifier(identifier)
        }
        // A null trigger delivers immediately. The same identifier replaces a pending or delivered
        // request; distinct achievement ids therefore cannot overwrite each other.
        val request = UNNotificationRequest.requestWithIdentifier(identifier, content, null)
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                logError(
                    "Achievements",
                    "Could not post an achievement notification",
                    IllegalStateException(error.localizedDescription),
                )
            }
        }
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

/**
 * Stable per achievement id. Session alerts use [IosSessionAlerts.PREFIX], so these requests
 * are not removed when the timer re-plans its pending alerts.
 */
internal fun achievementNotificationIdentifier(achievementId: String): String = "focusflow.achievement.$achievementId"

internal fun requestNotificationPermission(onDone: () -> Unit) {
    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { _, _ -> onDone() }
}

private suspend fun notificationStatus(): UNAuthorizationStatus = suspendCancellableCoroutine { continuation ->
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
    }
}

/** Settings → Accessibility → Motion → Reduce Motion. */
private fun systemReduceMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()
