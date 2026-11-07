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

enum class TimerStatus { Idle, Running, Paused }

/**
 * The persisted state of the timer. It never stores a ticking counter: while running it stores
 * the time that was left at [anchor], and the time left now is derived from the clocks.
 *
 * @property runId unique id of this session run. A finished run is saved under this id, which is
 *   what makes completion idempotent.
 * @property remainingMillis Idle/Paused: the time left. Running: the time left at [anchor].
 * @property anchor the clock reading [remainingMillis] refers to. Set only while running.
 * @property startedAtEpochMillis wall-clock time the run was first started; null until started.
 * @property completedFocusInCycle focus sessions completed since the last long break.
 */
data class TimerState(
    val runId: String,
    val sessionType: SessionType,
    val status: TimerStatus,
    val plannedDurationMillis: Long,
    val remainingMillis: Long,
    val anchor: TimerInstant?,
    val startedAtEpochMillis: Long?,
    val completedFocusInCycle: Int,
) {
    init {
        require((status == TimerStatus.Running) == (anchor != null)) { "anchor must be set exactly when running" }
        require(remainingMillis in 0..plannedDurationMillis) { "remainingMillis out of range" }
    }

    val isStarted: Boolean get() = status != TimerStatus.Idle

    /**
     * Time left at [now]. Negative when a running session is overdue (its deadline has passed but
     * the completion has not been recorded yet).
     */
    fun remainingAt(now: TimerInstant): Long {
        val anchor = anchor ?: return remainingMillis
        return if (isSameBoot(anchor, now)) {
            remainingMillis - (now.elapsedRealtimeMillis - anchor.elapsedRealtimeMillis)
        } else {
            // elapsedRealtime restarted with the device: fall back to the wall-clock deadline. A
            // clock that moved backwards must not give back more time than was left.
            minOf(anchor.epochMillis + remainingMillis - now.epochMillis, remainingMillis)
        }
    }
}

/**
 * Whether two readings come from the same boot, so their elapsed-realtime values can be compared.
 * Without a boot count, a monotonic clock that went backwards still proves a reboot.
 */
internal fun isSameBoot(earlier: TimerInstant, now: TimerInstant): Boolean {
    val bootCountMatches = earlier.bootCount == null || now.bootCount == null || earlier.bootCount == now.bootCount
    return bootCountMatches && now.elapsedRealtimeMillis >= earlier.elapsedRealtimeMillis
}
