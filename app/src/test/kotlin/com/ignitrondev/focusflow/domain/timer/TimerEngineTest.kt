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

import com.ignitrondev.focusflow.testing.FakeTimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    private var nextId = 0
    private val engine = TimerEngine(newRunId = { "run-${++nextId}" })
    private val config = TimerConfig()
    private val clock = FakeTimeSource()

    private val minute = 60_000L

    private fun startedFocus(): TimerState = engine.start(engine.initial(config), clock.now()).state

    /** Settles like the repository does before every action. */
    private fun settle(state: TimerState) = engine.settle(state, clock.now(), config)

    // Initialization

    @Test
    fun initialStateIsAnIdleFullLengthFocusSession() {
        val state = engine.initial(config)

        assertEquals(SessionType.Focus, state.sessionType)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(25 * minute, state.remainingAt(clock.now()))
        assertEquals(0, state.completedFocusInCycle)
        assertFalse(state.isStarted)
    }

    // Start, pause, resume

    @Test
    fun runningTimerCountsDownFromTheDeadline() {
        val state = startedFocus()

        clock.advanceBy(10 * minute)

        assertEquals(TimerStatus.Running, state.status)
        assertEquals(15 * minute, state.remainingAt(clock.now()))
        assertEquals(FakeTimeSource.START_EPOCH_MILLIS, state.startedAtEpochMillis)
    }

    @Test
    fun startingARunningTimerChangesNothing() {
        val state = startedFocus()
        clock.advanceBy(minute)

        assertSame(state, engine.start(state, clock.now()).state)
    }

    @Test
    fun pausedTimeIsNotCounted() {
        var state = startedFocus()
        clock.advanceBy(10 * minute)
        state = engine.pause(state, clock.now()).state

        clock.advanceBy(60 * minute)
        assertEquals(TimerStatus.Paused, state.status)
        assertEquals(15 * minute, state.remainingAt(clock.now()))

        state = engine.start(state, clock.now()).state
        clock.advanceBy(5 * minute)
        assertEquals(TimerStatus.Running, state.status)
        assertEquals(10 * minute, state.remainingAt(clock.now()))
        assertEquals("resume keeps the original start time", FakeTimeSource.START_EPOCH_MILLIS, state.startedAtEpochMillis)
    }

    @Test
    fun wallClockChangesDoNotAffectARunningTimer() {
        val state = startedFocus()
        clock.advanceBy(5 * minute)

        clock.changeWallClockBy(-3 * 60 * minute)
        assertEquals(20 * minute, state.remainingAt(clock.now()))

        clock.changeWallClockBy(+6 * 60 * minute)
        assertEquals(20 * minute, state.remainingAt(clock.now()))
    }

    // Completion and session transitions

    @Test
    fun finishedFocusSessionIsRecordedAndShortBreakIsNext() {
        val focus = startedFocus()
        clock.advanceBy(25 * minute)

        val update = settle(focus)

        val session = update.finishedSessions.single()
        assertEquals(focus.runId, session.id)
        assertEquals(SessionType.Focus, session.sessionType)
        assertTrue(session.completed)
        assertEquals(25 * minute, session.actualDurationMillis)
        assertEquals(FakeTimeSource.START_EPOCH_MILLIS + 25 * minute, session.endedAtEpochMillis)

        assertEquals(SessionType.ShortBreak, update.state.sessionType)
        assertEquals(TimerStatus.Idle, update.state.status)
        assertEquals(1, update.state.completedFocusInCycle)
    }

    @Test
    fun completionNoticedLateIsRecordedAtTheRealDeadline() {
        val focus = startedFocus()
        clock.advanceBy(3 * 60 * minute)

        val session = settle(focus).finishedSessions.single()

        assertEquals(FakeTimeSource.START_EPOCH_MILLIS + 25 * minute, session.endedAtEpochMillis)
    }

    @Test
    fun longBreakFollowsEveryFourthFocusSessionAndRestartsTheCycle() {
        var state = engine.initial(config)
        val types = mutableListOf<SessionType>()
        repeat(8) {
            types += state.sessionType
            state = engine.start(state, clock.now()).state
            clock.advanceBy(config.durationOf(state.sessionType))
            state = settle(state).state
        }

        assertEquals(
            listOf(
                SessionType.Focus, SessionType.ShortBreak,
                SessionType.Focus, SessionType.ShortBreak,
                SessionType.Focus, SessionType.ShortBreak,
                SessionType.Focus, SessionType.LongBreak,
            ),
            types,
        )
        assertEquals("long break completed: new cycle", SessionType.Focus, state.sessionType)
        assertEquals(0, state.completedFocusInCycle)
    }

    @Test
    fun customLongBreakIntervalIsRespected() {
        val everySecond = config.copy(longBreakInterval = 2)
        var state = engine.initial(everySecond)
        repeat(3) {
            state = engine.start(state, clock.now()).state
            clock.advanceBy(everySecond.durationOf(state.sessionType))
            state = engine.settle(state, clock.now(), everySecond).state
        }
        // focus -> short break -> focus -> long break
        assertEquals(SessionType.LongBreak, state.sessionType)
    }

    // Duplicate completion prevention

    @Test
    fun settlingTwiceRecordsTheSessionOnce() {
        val focus = startedFocus()
        clock.advanceBy(30 * minute)

        val first = settle(focus)
        val second = settle(first.state)

        assertEquals(1, first.finishedSessions.size)
        assertTrue(second.finishedSessions.isEmpty())
        assertSame(first.state, second.state)
    }

    @Test
    fun settlingBeforeTheDeadlineChangesNothing() {
        val focus = startedFocus()
        clock.advanceBy(24 * minute)

        val update = settle(focus)

        assertSame(focus, update.state)
        assertTrue(update.finishedSessions.isEmpty())
    }

    @Test
    fun theNextSessionGetsANewRunId() {
        val focus = startedFocus()
        clock.advanceBy(25 * minute)

        val next = settle(focus).state

        assertTrue(next.runId != focus.runId)
    }

    // Reset

    @Test
    fun resetAfterAWhileKeepsAnInterruptedSession() {
        val focus = startedFocus()
        clock.advanceBy(10 * minute)

        val update = engine.reset(focus, clock.now(), config)

        assertEquals(TimerStatus.Idle, update.state.status)
        assertEquals(SessionType.Focus, update.state.sessionType)
        assertEquals(25 * minute, update.state.remainingMillis)
        val session = update.finishedSessions.single()
        assertFalse(session.completed)
        assertEquals(10 * minute, session.actualDurationMillis)
    }

    @Test
    fun resetRightAfterStartingRecordsNothing() {
        val focus = startedFocus()
        clock.advanceBy(30_000)

        val update = engine.reset(focus, clock.now(), config)

        assertTrue(update.finishedSessions.isEmpty())
        assertEquals(TimerStatus.Idle, update.state.status)
    }

    @Test
    fun resetKeepsTheCyclePosition() {
        val state = engine.initial(config).copy(completedFocusInCycle = 2)
        val running = engine.start(state, clock.now()).state

        assertEquals(2, engine.reset(running, clock.now(), config).state.completedFocusInCycle)
    }

    // Skip

    @Test
    fun skippedFocusDoesNotCountTowardsTheLongBreak() {
        val focus = startedFocus()
        clock.advanceBy(5 * minute)

        val update = engine.skip(focus, clock.now(), config)

        assertEquals(SessionType.ShortBreak, update.state.sessionType)
        assertEquals(0, update.state.completedFocusInCycle)
        assertFalse(update.finishedSessions.single().completed)
    }

    @Test
    fun skippingABreakGoesBackToFocus() {
        val shortBreak = engine.select(engine.initial(config), SessionType.ShortBreak, config).state

        val update = engine.skip(shortBreak, clock.now(), config)

        assertEquals(SessionType.Focus, update.state.sessionType)
        assertTrue("never started: nothing to record", update.finishedSessions.isEmpty())
    }

    @Test
    fun skippingALongBreakStartsANewCycle() {
        val longBreak = engine.initial(config).copy(sessionType = SessionType.LongBreak, completedFocusInCycle = 4)

        val update = engine.skip(longBreak, clock.now(), config)

        assertEquals(SessionType.Focus, update.state.sessionType)
        assertEquals(0, update.state.completedFocusInCycle)
    }

    // Mode selection

    @Test
    fun sessionTypeCanBeChangedBeforeStarting() {
        val state = engine.select(engine.initial(config), SessionType.LongBreak, config).state

        assertEquals(SessionType.LongBreak, state.sessionType)
        assertEquals(15 * minute, state.remainingMillis)
    }

    @Test
    fun sessionTypeCannotBeChangedWhileStarted() {
        val running = startedFocus()
        assertSame(running, engine.select(running, SessionType.ShortBreak, config).state)

        val paused = engine.pause(running, clock.now()).state
        assertSame(paused, engine.select(paused, SessionType.ShortBreak, config).state)
    }

    // Recovery after reboot

    @Test
    fun timerSurvivesARebootUsingTheWallClockDeadline() {
        val focus = startedFocus()
        clock.advanceBy(5 * minute)
        clock.reboot(downtimeMillis = 3 * minute, uptimeAfterBootMillis = 2 * minute)

        val update = settle(focus)

        assertTrue(update.finishedSessions.isEmpty())
        assertEquals(TimerStatus.Running, update.state.status)
        assertEquals(15 * minute, update.state.remainingAt(clock.now()))

        // After re-anchoring, the monotonic clock of the new boot drives the countdown again.
        clock.changeWallClockBy(60 * minute)
        clock.advanceBy(minute)
        assertEquals(14 * minute, update.state.remainingAt(clock.now()))
    }

    @Test
    fun sessionThatEndedWhileTheDeviceWasOffCompletesAtItsDeadline() {
        val focus = startedFocus()
        clock.advanceBy(5 * minute)
        clock.reboot(downtimeMillis = 2 * 60 * minute)

        val update = settle(focus)

        val session = update.finishedSessions.single()
        assertTrue(session.completed)
        assertEquals(FakeTimeSource.START_EPOCH_MILLIS + 25 * minute, session.endedAtEpochMillis)
        assertEquals(SessionType.ShortBreak, update.state.sessionType)
    }

    @Test
    fun rebootIsDetectedWithoutABootCount() {
        clock.bootCount = null
        val focus = startedFocus()
        clock.advanceBy(5 * minute)
        clock.reboot(downtimeMillis = 3 * minute, uptimeAfterBootMillis = 2 * minute, newBootCount = null)

        assertEquals(15 * minute, settle(focus).state.remainingAt(clock.now()))
    }

    @Test
    fun wallClockSetBackAfterARebootNeverAddsTime() {
        val focus = startedFocus()
        clock.advanceBy(5 * minute)
        clock.reboot(downtimeMillis = minute)
        clock.changeWallClockBy(-24 * 60 * minute)

        assertEquals(25 * minute, settle(focus).state.remainingAt(clock.now()))
    }

    // Auto-start

    @Test
    fun autoStartRunsTheNextSessionFromThePreviousDeadline() {
        val autoConfig = config.copy(autoStartBreaks = true)
        val focus = engine.start(engine.initial(autoConfig), clock.now()).state
        clock.advanceBy(27 * minute)

        val update = engine.settle(focus, clock.now(), autoConfig)

        assertEquals(SessionType.ShortBreak, update.state.sessionType)
        assertEquals(TimerStatus.Running, update.state.status)
        assertEquals(3 * minute, update.state.remainingAt(clock.now()))
    }

    @Test
    fun autoStartChainStopsAfterOneFullCycle() {
        val autoConfig = config.copy(autoStartBreaks = true, autoStartFocus = true)
        val focus = engine.start(engine.initial(autoConfig), clock.now()).state
        clock.advanceBy(24 * 60 * minute)

        val update = engine.settle(focus, clock.now(), autoConfig)

        assertEquals(autoConfig.longBreakInterval * 2, update.finishedSessions.size)
        assertEquals(TimerStatus.Idle, update.state.status)
        assertNull(update.state.anchor)
    }
}
