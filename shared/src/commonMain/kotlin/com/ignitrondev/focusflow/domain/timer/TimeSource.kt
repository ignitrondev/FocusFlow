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
package com.ignitrondev.focusflow.domain.timer

import kotlinx.datetime.TimeZone

/**
 * One reading of the device clocks, taken at the same moment.
 *
 * @property elapsedRealtimeMillis monotonic time since boot, including deep sleep. It never jumps
 *   when the user or the network changes the wall clock, so it drives the active countdown.
 * @property epochMillis wall-clock time. Only used for history timestamps and to recover a running
 *   timer after a reboot, when [elapsedRealtimeMillis] has restarted from zero.
 * @property bootCount number of device boots, when the platform exposes it. A different value
 *   means [elapsedRealtimeMillis] values are not comparable.
 */
data class TimerInstant(val elapsedRealtimeMillis: Long, val epochMillis: Long, val bootCount: Int?)

/** Injectable clock, so timer logic can be tested without waiting for real time to pass. */
interface TimeSource {
    fun now(): TimerInstant

    fun zone(): TimeZone
}
