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

/**
 * The timer state machine. Every function is pure: it takes the current [TimerState], a clock
 * reading and the [TimerConfig], and returns the next state plus the sessions that ended. Nothing
 * here ticks, sleeps or touches Android, so every transition is unit-testable with fake clocks.
 *
 * Callers [settle] the state before applying a user action, so an action never operates on a
 * session whose time already ran out.
 */
class TimerEngine(private val newRunId: () -> String) {

    fun initial(config: TimerConfig): TimerState = idle(SessionType.Focus, config, completedFocusInCycle = 0)

    /**
     * Brings a persisted state up to date with the clocks: re-anchors a running timer after a
     * reboot and completes every session whose deadline has passed. With auto-start enabled, the
     * next session starts at the previous deadline, so a timer that kept "running" while the app
     * was closed ends up where it would have been. The chain stops after one full cycle. Finally,
     * an idle session that has not started takes the current durations from [config].
     */
    fun settle(state: TimerState, now: TimerInstant, config: TimerConfig): TimerUpdate {
        var current = reanchorAfterReboot(state, now)
        val finished = mutableListOf<FinishedSession>()
        val maxChained = config.longBreakInterval * 2
        while (current.status == TimerStatus.Running && current.remainingAt(now) <= 0) {
            val allowAutoStart = finished.size + 1 < maxChained
            val (next, session) = complete(current, now, config, allowAutoStart)
            finished += session
            current = next
        }
        // New durations from Settings apply to a session that has not started yet.
        val duration = config.durationOf(current.sessionType)
        if (!current.isStarted && current.plannedDurationMillis != duration) {
            current = current.copy(plannedDurationMillis = duration, remainingMillis = duration)
        }
        return TimerUpdate(current, finished)
    }

    /**
     * The session ends that will happen if nobody touches the timer: the current session's and,
     * with auto-start, the following ones (at most one full cycle, like [settle]). Platforms that
     * cannot wake the app at a deadline (iOS) schedule an alert for each of them up front.
     */
    fun upcomingSessionEnds(state: TimerState, now: TimerInstant, config: TimerConfig): List<UpcomingSessionEnd> {
        val ends = mutableListOf<UpcomingSessionEnd>()
        var current = reanchorAfterReboot(state, now)
        var at = now
        while (current.status == TimerStatus.Running && ends.size < config.longBreakInterval * 2) {
            val remaining = current.remainingAt(at).coerceAtLeast(0)
            val end = TimerInstant(at.elapsedRealtimeMillis + remaining, at.epochMillis + remaining, at.bootCount)
            val allowAutoStart = ends.size + 1 < config.longBreakInterval * 2
            val (next, _) = complete(current, end, config, allowAutoStart)
            ends += UpcomingSessionEnd(
                sessionType = current.sessionType,
                atEpochMillis = end.epochMillis,
                nextSessionType = next.sessionType,
                nextStartsAutomatically = next.status == TimerStatus.Running,
            )
            current = next
            at = end
        }
        return ends
    }

    /** Starts an idle session or resumes a paused one. */
    fun start(state: TimerState, now: TimerInstant): TimerUpdate = when (state.status) {
        TimerStatus.Running -> TimerUpdate(state)

        TimerStatus.Idle -> TimerUpdate(
            state.copy(status = TimerStatus.Running, anchor = now, startedAtEpochMillis = now.epochMillis),
        )

        TimerStatus.Paused -> TimerUpdate(state.copy(status = TimerStatus.Running, anchor = now))
    }

    fun pause(state: TimerState, now: TimerInstant): TimerUpdate {
        if (state.status != TimerStatus.Running) return TimerUpdate(state)
        val remaining = state.remainingAt(now).coerceIn(0, state.plannedDurationMillis)
        return TimerUpdate(state.copy(status = TimerStatus.Paused, remainingMillis = remaining, anchor = null))
    }

    /** Stops the current session and puts the same session type back to its full duration. */
    fun reset(state: TimerState, now: TimerInstant, config: TimerConfig): TimerUpdate {
        if (!state.isStarted && state.plannedDurationMillis == config.durationOf(state.sessionType)) {
            return TimerUpdate(state)
        }
        return TimerUpdate(
            state = idle(state.sessionType, config, state.completedFocusInCycle),
            finishedSessions = listOfNotNull(interrupted(state, now)),
        )
    }

    /**
     * Ends the current session without completing it and moves on. A skipped focus session does
     * not count towards the long break; skipping a long break still starts a new cycle.
     */
    fun skip(state: TimerState, now: TimerInstant, config: TimerConfig): TimerUpdate {
        val (nextType, cycle) = when (state.sessionType) {
            SessionType.Focus -> SessionType.ShortBreak to state.completedFocusInCycle
            SessionType.ShortBreak -> SessionType.Focus to state.completedFocusInCycle
            SessionType.LongBreak -> SessionType.Focus to 0
        }
        return TimerUpdate(
            state = idle(nextType, config, cycle),
            finishedSessions = listOfNotNull(interrupted(state, now)),
        )
    }

    /** Switches session type. Only allowed before the session has started. */
    fun select(state: TimerState, type: SessionType, config: TimerConfig): TimerUpdate {
        if (state.isStarted || type == state.sessionType) return TimerUpdate(state)
        return TimerUpdate(idle(type, config, state.completedFocusInCycle))
    }

    private fun complete(
        state: TimerState,
        now: TimerInstant,
        config: TimerConfig,
        allowAutoStart: Boolean,
    ): Pair<TimerState, FinishedSession> {
        val overshoot = (-state.remainingAt(now)).coerceAtLeast(0)
        // Measured back from "now" with the monotonic overshoot, so a wall-clock change during the
        // session does not move the recorded end time.
        val deadline = TimerInstant(
            elapsedRealtimeMillis = now.elapsedRealtimeMillis - overshoot,
            epochMillis = now.epochMillis - overshoot,
            bootCount = now.bootCount,
        )
        val session = FinishedSession(
            id = state.runId,
            sessionType = state.sessionType,
            startedAtEpochMillis = state.startedAtEpochMillis ?: (deadline.epochMillis - state.plannedDurationMillis),
            endedAtEpochMillis = deadline.epochMillis,
            plannedDurationMillis = state.plannedDurationMillis,
            actualDurationMillis = state.plannedDurationMillis,
            completed = true,
        )
        val cycle = when (state.sessionType) {
            SessionType.Focus -> state.completedFocusInCycle + 1
            SessionType.ShortBreak -> state.completedFocusInCycle
            SessionType.LongBreak -> 0
        }
        val nextType = when (state.sessionType) {
            SessionType.Focus -> if (cycle >= config.longBreakInterval) SessionType.LongBreak else SessionType.ShortBreak
            SessionType.ShortBreak, SessionType.LongBreak -> SessionType.Focus
        }
        var next = idle(nextType, config, cycle)
        if (allowAutoStart && config.autoStarts(nextType)) {
            next = next.copy(status = TimerStatus.Running, anchor = deadline, startedAtEpochMillis = deadline.epochMillis)
        }
        return next to session
    }

    /** A session that ran for a while before being reset or skipped is kept in the history. */
    private fun interrupted(state: TimerState, now: TimerInstant): FinishedSession? {
        val startedAt = state.startedAtEpochMillis ?: return null
        val actual = state.plannedDurationMillis - state.remainingAt(now).coerceIn(0, state.plannedDurationMillis)
        if (actual < MIN_RECORDED_INTERRUPTION_MILLIS) return null
        return FinishedSession(
            id = state.runId,
            sessionType = state.sessionType,
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = now.epochMillis,
            plannedDurationMillis = state.plannedDurationMillis,
            actualDurationMillis = actual,
            completed = false,
        )
    }

    private fun reanchorAfterReboot(state: TimerState, now: TimerInstant): TimerState {
        val anchor = state.anchor ?: return state
        if (isSameBoot(anchor, now)) return state
        val wallClockRemaining = state.remainingAt(now)
        // Move the anchor back by the overdue time, so an overdue session completes at its real
        // deadline instead of at the moment the app noticed the reboot.
        val overdue = (-wallClockRemaining).coerceAtLeast(0)
        return state.copy(
            remainingMillis = wallClockRemaining.coerceAtLeast(0),
            anchor = TimerInstant(now.elapsedRealtimeMillis - overdue, now.epochMillis - overdue, now.bootCount),
        )
    }

    private fun idle(type: SessionType, config: TimerConfig, completedFocusInCycle: Int): TimerState {
        val duration = config.durationOf(type)
        return TimerState(
            runId = newRunId(),
            sessionType = type,
            status = TimerStatus.Idle,
            plannedDurationMillis = duration,
            remainingMillis = duration,
            anchor = null,
            startedAtEpochMillis = null,
            completedFocusInCycle = completedFocusInCycle,
        )
    }

    companion object {
        /** Reset or skipped sessions shorter than this are not worth a history entry. */
        const val MIN_RECORDED_INTERRUPTION_MILLIS = 60_000L
    }
}
