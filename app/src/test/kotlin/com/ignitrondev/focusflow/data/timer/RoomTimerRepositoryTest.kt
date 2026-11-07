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
package com.ignitrondev.focusflow.data.timer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.core.time.dayBounds
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.data.database.TimerStateEntity
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import com.ignitrondev.focusflow.testing.FakeTimeSource
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTimerRepositoryTest {
    private lateinit var database: FocusFlowDatabase
    private val clock = FakeTimeSource()
    private val minute = 60_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusFlowDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** A new instance over the same database behaves like the app after process death. */
    private fun newRepository() = RoomTimerRepository(
        database = database,
        timerStateDao = database.timerStateDao(),
        focusSessionDao = database.focusSessionDao(),
        configRepository = DefaultTimerConfigRepository(),
        timeSource = clock,
        engine = TimerEngine(newRunId = { UUID.randomUUID().toString() }),
    )

    @Test
    fun freshInstallShowsAnIdleFocusSession() = runTest {
        val state = newRepository().state.first()

        assertEquals(SessionType.Focus, state.sessionType)
        assertEquals(TimerStatus.Idle, state.status)
    }

    @Test
    fun runningTimerSurvivesProcessRecreation() = runTest {
        newRepository().start()
        clock.advanceBy(10 * minute)

        val recreated = newRepository()
        recreated.refresh()
        val state = recreated.state.first()

        assertEquals(TimerStatus.Running, state.status)
        assertEquals(15 * minute, state.remainingAt(clock.now()))
    }

    @Test
    fun pausedTimerSurvivesProcessRecreation() = runTest {
        val repository = newRepository()
        repository.start()
        clock.advanceBy(10 * minute)
        repository.pause()
        clock.advanceBy(60 * minute)

        val state = newRepository().state.first()

        assertEquals(TimerStatus.Paused, state.status)
        assertEquals(15 * minute, state.remainingAt(clock.now()))
    }

    @Test
    fun sessionThatEndedWhileTheAppWasClosedIsRecordedOnRecovery() = runTest {
        newRepository().start()
        clock.advanceBy(2 * 60 * minute)

        val recreated = newRepository()
        recreated.refresh()

        val session = database.focusSessionDao().getAll().single()
        assertTrue(session.completed)
        assertEquals(25 * 60L, session.actualDurationSeconds)
        assertEquals(FakeTimeSource.START_EPOCH_MILLIS + 25 * minute, session.endedAt)
        assertEquals(SessionType.ShortBreak, recreated.state.first().sessionType)
    }

    @Test
    fun concurrentCompletionIsRecordedExactlyOnce() = runTest {
        newRepository().start()
        clock.advanceBy(25 * minute)

        // The UI, a second screen and (from Phase 3) an alarm may all notice the deadline at once.
        val repositories = List(5) { newRepository() }
        repositories.map { async { it.refresh() } }.awaitAll()
        repositories.forEach { it.refresh() }

        assertEquals(1, database.focusSessionDao().getAll().size)
        assertEquals(1, newRepository().state.first().completedFocusInCycle)
    }

    @Test
    fun actionsAfterTheDeadlineApplyToTheNextSession() = runTest {
        val repository = newRepository()
        repository.start()
        clock.advanceBy(26 * minute)

        // The user taps pause after the focus session already ended: it is completed first.
        repository.pause()

        assertTrue(database.focusSessionDao().getAll().single().completed)
        val state = repository.state.first()
        assertEquals(SessionType.ShortBreak, state.sessionType)
        assertEquals(TimerStatus.Idle, state.status)
    }

    @Test
    fun todaysSummaryCountsCompletedFocusSessionsOnly() = runTest {
        val repository = newRepository()
        repeat(2) {
            repository.start()
            clock.advanceBy(25 * minute)
            repository.refresh() // focus done, short break next
            repository.skip() // back to focus
        }
        repository.start()
        clock.advanceBy(10 * minute)
        repository.reset() // interrupted: kept in history, not in today's focus total

        val (from, to) = dayBounds(clock.now().epochMillis, clock.zone())
        val summary = RoomFocusSessionRepository(database.focusSessionDao()).observeFocusSummary(from, to).first()

        assertEquals(3, database.focusSessionDao().getAll().size)
        assertEquals(2, summary.completedSessions)
        assertEquals(50 * minute, summary.focusMillis)
    }

    @Test
    fun corruptedStateRowFallsBackToAFreshTimer() = runTest {
        database.timerStateDao().upsert(
            TimerStateEntity(
                runId = "broken",
                sessionType = "Meditation",
                status = TimerStatus.Running.name,
                plannedDurationMillis = 1,
                remainingMillis = 1,
                anchorElapsedRealtimeMillis = null,
                anchorEpochMillis = null,
                anchorBootCount = null,
                startedAtEpochMillis = null,
                completedFocusInCycle = 0,
                updatedAtEpochMillis = 0,
            ),
        )

        val repository = newRepository()
        repository.refresh()
        val state = repository.state.first()

        assertEquals(SessionType.Focus, state.sessionType)
        assertFalse(state.isStarted)
    }
}
