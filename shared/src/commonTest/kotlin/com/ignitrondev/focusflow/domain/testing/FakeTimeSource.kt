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
package com.ignitrondev.focusflow.domain.testing

import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerInstant
import kotlinx.datetime.TimeZone

/** A clock that only moves when a test moves it. Runs on every platform. */
class FakeTimeSource : TimeSource {
    var elapsedRealtimeMillis: Long = 1_000_000L
    var epochMillis: Long = START_EPOCH_MILLIS
    var bootCount: Int? = 7
    var zone: TimeZone = TimeZone.UTC

    override fun now() = TimerInstant(elapsedRealtimeMillis, epochMillis, bootCount)

    override fun zone(): TimeZone = zone

    /** Real time passing: both clocks move together. */
    fun advanceBy(millis: Long) {
        elapsedRealtimeMillis += millis
        epochMillis += millis
    }

    /** The user or the network changes the wall clock; the monotonic clock is unaffected. */
    fun changeWallClockBy(millis: Long) {
        epochMillis += millis
    }

    /** The device restarts after [downtimeMillis]: uptime starts again and the boot count moves on. */
    fun reboot(downtimeMillis: Long, uptimeAfterBootMillis: Long = 30_000L, newBootCount: Int? = bootCount?.plus(1)) {
        epochMillis += downtimeMillis + uptimeAfterBootMillis
        elapsedRealtimeMillis = uptimeAfterBootMillis
        bootCount = newBootCount
    }

    companion object {
        /** 2026-11-07T09:00:00Z, a fixed time so tests do not depend on the real date. */
        const val START_EPOCH_MILLIS = 1_794_042_000_000L
    }
}
