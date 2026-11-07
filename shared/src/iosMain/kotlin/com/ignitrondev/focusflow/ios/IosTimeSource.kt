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

import com.ignitrondev.focusflow.domain.timer.TimeSource
import com.ignitrondev.focusflow.domain.timer.TimerInstant
import kotlin.time.Clock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlinx.datetime.TimeZone
import platform.darwin.sysctlbyname
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime_nsec_np
import platform.posix.size_tVar
import platform.posix.timeval

/**
 * The device clocks on iOS. On Darwin, `CLOCK_MONOTONIC` keeps counting while the device sleeps
 * (unlike `CLOCK_UPTIME_RAW` or `systemUptime`), which is what the timer needs: the same meaning
 * as Android's `elapsedRealtime`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTimeSource : TimeSource {
    override fun now(): TimerInstant = TimerInstant(
        elapsedRealtimeMillis = (clock_gettime_nsec_np(CLOCK_MONOTONIC.convert()) / NANOS_PER_MILLI).toLong(),
        epochMillis = Clock.System.now().toEpochMilliseconds(),
        bootCount = bootIdentity,
    )

    override fun zone(): TimeZone = TimeZone.currentSystemDefault()

    /**
     * iOS has no boot counter, so the boot time (whole seconds) identifies the boot. It is read
     * once per process: iOS shifts it when the wall clock is set, and a change between two app
     * launches only makes the engine fall back to the wall clock, which is then correct anyway.
     */
    private val bootIdentity: Int? by lazy {
        memScoped {
            val bootTime = alloc<timeval>()
            val size = alloc<size_tVar>().apply { value = sizeOf<timeval>().convert() }
            if (sysctlbyname("kern.boottime", bootTime.ptr, size.ptr, null, 0u) == 0) bootTime.tv_sec.toInt() else null
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000uL
    }
}
