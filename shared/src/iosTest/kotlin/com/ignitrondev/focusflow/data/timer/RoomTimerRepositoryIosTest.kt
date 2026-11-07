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

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.domain.testing.FakeTimeSource
import com.ignitrondev.focusflow.domain.testing.FakeTimerConfigRepository
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.TimerEngine
import com.ignitrondev.focusflow.domain.timer.TimerStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The shared database and timer repository on iOS: Room with the bundled SQLite driver, the
 * same driver the iOS app uses. Mirrors the key Android (Robolectric) repository tests.
 */
class RoomTimerRepositoryIosTest {
    private val database = Room.inMemoryDatabaseBuilder<FocusFlowDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
    private val clock = FakeTimeSource()
    private val config = FakeTimerConfigRepository()
    private val notified = mutableListOf<FinishedSession>()
    private var nextId = 0
    private val minute = 60_000L

    @AfterTest
    fun tearDown() = database.close()

    private fun newRepository() = RoomTimerRepository(
        database = database,
        timerStateDao = database.timerStateDao(),
        focusSessionDao = database.focusSessionDao(),
        configRepository = config,
        timeSource = clock,
        engine = TimerEngine(newRunId = { "run-${++nextId}" }),
        completionListener = { sessions, _ -> notified += sessions },
    )

    @Test
    fun runningTimerSurvivesRecreation() = runTest {
        newRepository().start()
        clock.advanceBy(10 * minute)

        val recreated = newRepository()
        recreated.refresh()
        val state = recreated.state.first()

        assertEquals(TimerStatus.Running, state.status)
        assertEquals(15 * minute, state.remainingAt(clock.now()))
    }

    @Test
    fun concurrentCompletionIsRecordedExactlyOnce() = runTest {
        newRepository().start()
        clock.advanceBy(25 * minute)

        val repositories = List(5) { newRepository() }
        repositories.map { async { it.refresh() } }.awaitAll()

        val sessions = database.focusSessionDao().getAll()
        assertEquals(1, sessions.size)
        assertTrue(sessions.single().completed)
        assertEquals(1, notified.size, "the user is alerted once")
        assertEquals(SessionType.ShortBreak, newRepository().state.first().sessionType)
    }
}
