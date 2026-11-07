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

import com.ignitrondev.focusflow.core.ui.labelRes
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.domain.timer.UpcomingSessionEnd
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.notification_break_done
import com.ignitrondev.focusflow.resources.notification_focus_done
import com.ignitrondev.focusflow.resources.notification_next_focus
import com.ignitrondev.focusflow.resources.notification_next_long_break
import com.ignitrondev.focusflow.resources.notification_next_short_break
import com.ignitrondev.focusflow.resources.notification_next_started
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * Session alerts on iOS. Unlike Android, iOS cannot wake the app at a deadline, so every session
 * end that will happen while the timer is left alone (with auto-start, the following sessions
 * too) is handed to the system up front as a local notification. Each change of the timer or its
 * settings re-plans them; the database transaction stays the only place sessions are recorded.
 */
class IosSessionAlerts(
    private val timerRepository: TimerRepository,
    private val configRepository: TimerConfigRepository,
    private val timeSource: TimeSource,
    private val engine: TimerEngine,
    private val scope: CoroutineScope,
    private val logError: (String, Throwable) -> Unit,
) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    // The center keeps its delegate weakly: this reference keeps it alive.
    private val foregroundPresenter = ForegroundPresenter()

    fun start() {
        center.delegate = foregroundPresenter
        scope.launch {
            combine(timerRepository.state, configRepository.config, ::Pair).collectLatest { (state, config) ->
                try {
                    plan(engine.upcomingSessionEnds(state, timeSource.now(), config))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logError("Could not schedule the session alerts", e)
                }
            }
        }
    }

    private suspend fun plan(ends: List<UpcomingSessionEnd>) {
        val nowEpoch = timeSource.now().epochMillis
        val wanted = ends.associateBy(::identifier)
        val pending = pendingIdentifiers().filter { it.startsWith(PREFIX) }
        // An alert that is due right now stays: the session it announces has just ended, which
        // is what made the timer change and brought us here.
        val obsolete = pending.filter { it !in wanted && (deadlineOf(it) ?: 0) > nowEpoch + DUE_NOW_MILLIS }
        if (obsolete.isNotEmpty()) center.removePendingNotificationRequestsWithIdentifiers(obsolete)
        for ((id, end) in wanted) {
            if (id !in pending) schedule(id, end, nowEpoch)
        }
    }

    private suspend fun schedule(id: String, end: UpcomingSessionEnd, nowEpoch: Long) {
        val content = UNMutableNotificationContent().apply {
            setTitle(
                getString(
                    if (end.sessionType ==
                        SessionType.Focus
                    ) Res.string.notification_focus_done else Res.string.notification_break_done,
                ),
            )
            setBody(body(end))
            setSound(UNNotificationSound.defaultSound)
        }
        val seconds = ((end.atEpochMillis - nowEpoch) / 1000.0).coerceAtLeast(MIN_INTERVAL_SECONDS)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger)) { error ->
            if (error != null) logError("Could not schedule a session alert", IllegalStateException(error.localizedDescription))
        }
    }

    private suspend fun body(end: UpcomingSessionEnd): String {
        val next = getString(end.nextSessionType.labelRes())
        return when {
            end.nextStartsAutomatically -> getString(Res.string.notification_next_started, next)
            end.nextSessionType == SessionType.Focus -> getString(Res.string.notification_next_focus)
            end.nextSessionType == SessionType.LongBreak -> getString(Res.string.notification_next_long_break, next)
            else -> getString(Res.string.notification_next_short_break, next)
        }
    }

    private suspend fun pendingIdentifiers(): List<String> = suspendCancellableCoroutine { continuation ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            continuation.resume(requests.orEmpty().mapNotNull { (it as? UNNotificationRequest)?.identifier })
        }
    }

    internal companion object {
        const val PREFIX = "focusflow.end."
        private const val DUE_NOW_MILLIS = 1_000L
        private const val MIN_INTERVAL_SECONDS = 1.0

        /** The deadline and the text decide the identifier, so a changed alert gets a new one. */
        fun identifier(end: UpcomingSessionEnd): String =
            "$PREFIX${end.atEpochMillis}.${end.sessionType}.${end.nextSessionType}.${end.nextStartsAutomatically}"

        fun deadlineOf(identifier: String): Long? = identifier.removePrefix(PREFIX).substringBefore('.').toLongOrNull()
    }
}

/** Shows session alerts while FocusFlow is open, too (iOS hides them by default). */
private class ForegroundPresenter :
    NSObject(),
    UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound,
        )
    }
}
