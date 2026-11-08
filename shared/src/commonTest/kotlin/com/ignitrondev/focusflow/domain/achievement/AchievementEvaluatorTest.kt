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
package com.ignitrondev.focusflow.domain.achievement

import com.ignitrondev.focusflow.domain.statistics.FocusStatisticsCalculator
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

class AchievementEvaluatorTest {
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 11, 8)
    private var nextId = 0

    private fun focus(date: LocalDate, millis: Long, completed: Boolean = true, type: SessionType = SessionType.Focus): FinishedSession {
        val end = LocalDateTime(date, LocalTime(12, 0)).toInstant(zone).toEpochMilliseconds()
        return FinishedSession("s${nextId++}", type, end - millis, end, millis, millis, completed)
    }

    private fun calculate(sessions: List<FinishedSession>) = FocusStatisticsCalculator.calculate(sessions, today, zone, DayOfWeek.MONDAY)

    private fun plan(sessions: List<FinishedSession>, stored: List<AchievementUnlockRecord> = emptyList()) =
        AchievementEvaluator.plan(calculate(sessions), stored)

    private fun earnedIds(sessions: List<FinishedSession>, metric: AchievementMetric): Set<String> =
        plan(sessions).statuses.filter { it.definition.metric == metric && it.earned }.map { it.definition.id }.toSet()

    @Test
    fun noHistoryEarnsNothing() {
        assertTrue(plan(emptyList()).statuses.none { it.earned })
        assertTrue(plan(emptyList()).toNotify.isEmpty())
    }

    @Test
    fun resetSkipAndBreaksDoNotCount() {
        // TimerEngine records Reset and Skip as completed = false. The statistics calculator is the rule.
        val stats = calculate(
            listOf(
                focus(today, 25 * 60_000L, completed = false),
                focus(today, 5 * 60_000L, type = SessionType.ShortBreak),
                focus(today, 15 * 60_000L, type = SessionType.LongBreak),
            ),
        )

        assertEquals(0, stats.totalSessions)
        assertEquals(0L, stats.totalFocusMillis)
        assertEquals(0, stats.currentStreakDays)
        assertTrue(AchievementEvaluator.plan(stats, emptyList()).statuses.none { it.earned })
    }

    @Test
    fun everySessionThreshold() {
        assertEquals(emptySet(), earnedIds(emptyList(), AchievementMetric.CompletedSessions))
        assertEquals(setOf("sessions_1"), earnedIds(List(1) { focus(today, 1L) }, AchievementMetric.CompletedSessions))
        assertEquals(setOf("sessions_1"), earnedIds(List(9) { focus(today, 1L) }, AchievementMetric.CompletedSessions))
        assertEquals(setOf("sessions_1", "sessions_10"), earnedIds(List(10) { focus(today, 1L) }, AchievementMetric.CompletedSessions))
        assertEquals(setOf("sessions_1", "sessions_10"), earnedIds(List(24) { focus(today, 1L) }, AchievementMetric.CompletedSessions))
        assertEquals(
            setOf("sessions_1", "sessions_10", "sessions_25"),
            earnedIds(List(25) { focus(today, 1L) }, AchievementMetric.CompletedSessions),
        )
        assertEquals(
            setOf("sessions_1", "sessions_10", "sessions_25"),
            earnedIds(List(49) { focus(today, 1L) }, AchievementMetric.CompletedSessions),
        )
        assertEquals(
            setOf("sessions_1", "sessions_10", "sessions_25", "sessions_50"),
            earnedIds(List(50) { focus(today, 1L) }, AchievementMetric.CompletedSessions),
        )
        assertEquals(
            setOf("sessions_1", "sessions_10", "sessions_25", "sessions_50"),
            earnedIds(List(99) { focus(today, 1L) }, AchievementMetric.CompletedSessions),
        )
        assertEquals(
            setOf("sessions_1", "sessions_10", "sessions_25", "sessions_50", "sessions_100"),
            earnedIds(List(100) { focus(today, 1L) }, AchievementMetric.CompletedSessions),
        )
    }

    @Test
    fun everyStreakThreshold() {
        assertEquals(emptySet(), earnedIds(streak(2), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3"), earnedIds(streak(3), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3"), earnedIds(streak(6), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3", "streak_7"), earnedIds(streak(7), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3", "streak_7"), earnedIds(streak(13), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3", "streak_7", "streak_14"), earnedIds(streak(14), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3", "streak_7", "streak_14"), earnedIds(streak(29), AchievementMetric.StreakDays))
        assertEquals(setOf("streak_3", "streak_7", "streak_14", "streak_30"), earnedIds(streak(30), AchievementMetric.StreakDays))
    }

    @Test
    fun everyFocusHourThresholdUsesExactMillis() {
        val hour = Achievements.HOUR_MILLIS
        assertEquals(emptySet(), earnedIds(listOf(focus(today, hour - 1)), AchievementMetric.FocusHours))
        assertEquals(setOf("focus_hours_1"), earnedIds(listOf(focus(today, hour)), AchievementMetric.FocusHours))
        assertEquals(setOf("focus_hours_1"), earnedIds(listOf(focus(today, 10 * hour - 1)), AchievementMetric.FocusHours))
        assertEquals(setOf("focus_hours_1", "focus_hours_10"), earnedIds(listOf(focus(today, 10 * hour)), AchievementMetric.FocusHours))
        assertEquals(setOf("focus_hours_1", "focus_hours_10"), earnedIds(listOf(focus(today, 25 * hour - 1)), AchievementMetric.FocusHours))
        assertEquals(
            setOf("focus_hours_1", "focus_hours_10", "focus_hours_25"),
            earnedIds(listOf(focus(today, 25 * hour)), AchievementMetric.FocusHours),
        )
        assertEquals(
            setOf("focus_hours_1", "focus_hours_10", "focus_hours_25"),
            earnedIds(listOf(focus(today, 100 * hour - 1)), AchievementMetric.FocusHours),
        )
        assertEquals(
            setOf("focus_hours_1", "focus_hours_10", "focus_hours_25", "focus_hours_100"),
            earnedIds(listOf(focus(today, 100 * hour)), AchievementMetric.FocusHours),
        )
    }

    @Test
    fun anHourMinusOneMillisecondDoesNotDisplayAsOneHour() {
        val status = plan(listOf(focus(today, Achievements.HOUR_MILLIS - 1)))
            .statuses
            .first { it.definition.id == "focus_hours_1" }

        assertFalse(status.earned)
        assertEquals(0L, status.currentWholeUnits)
        assertEquals(59L, status.remainderMinutes)
    }

    @Test
    fun existingHistoryUnlocksEveryThresholdAtOnce() {
        val plan = plan(fullHistory())

        assertEquals(Achievements.all.map { it.id }, plan.toPersist.map { it.id })
        assertEquals(Achievements.all.map { it.id }, plan.toNotify.map { it.id })
        assertEquals(13, plan.toNotify.distinctBy { it.id }.size)
    }

    @Test
    fun earnedStreakStaysAfterTheStreakBreaks() {
        val stored = listOf(
            AchievementUnlockRecord("streak_3", 5L, notified = true),
            AchievementUnlockRecord("streak_7", 5L, notified = true),
        )
        val plan = plan(emptyList(), stored)

        assertTrue(plan.statuses.first { it.definition.id == "streak_3" }.earned)
        assertTrue(plan.statuses.first { it.definition.id == "streak_7" }.earned)
        assertFalse(plan.statuses.first { it.definition.id == "streak_14" }.earned)
        assertTrue(plan.toPersist.isEmpty())
        assertTrue(plan.toNotify.isEmpty())
    }

    @Test
    fun storedUnlockThatWasNotNotifiedIsStillPending() {
        val stored = listOf(AchievementUnlockRecord("sessions_1", 5L, notified = false))
        val plan = plan(emptyList(), stored)

        assertEquals(listOf("sessions_1"), plan.toNotify.map { it.id })
        assertTrue(plan.toPersist.isEmpty())
        assertTrue(plan.statuses.first { it.definition.id == "sessions_1" }.earned)
    }

    private fun streak(days: Int) = (0 until days).map { focus(today.minus(it, DateTimeUnit.DAY), 1L) }

    private fun fullHistory(): List<FinishedSession> {
        val hour = Achievements.HOUR_MILLIS
        val days = (0 until 30).map { focus(today.minus(it, DateTimeUnit.DAY), hour) }
        val extraToday = List(70) { focus(today, hour) }
        return days + extraToday
    }
}

class AchievementSyncTest {
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 11, 8)
    private var nextId = 0
    private var now = 1_000L

    private fun focus(date: LocalDate, millis: Long): FinishedSession {
        val end = LocalDateTime(date, LocalTime(12, 0)).toInstant(zone).toEpochMilliseconds()
        return FinishedSession("s${nextId++}", SessionType.Focus, end - millis, end, millis, millis, completed = true)
    }

    private fun calculate(sessions: List<FinishedSession>) = FocusStatisticsCalculator.calculate(sessions, today, zone, DayOfWeek.MONDAY)

    private fun sync(repository: FakeAchievementRepository, calls: MutableList<String>) = AchievementSync(
        repository = repository,
        notify = { id, _ -> calls += id },
        criteriaLabel = { it.id },
        nowEpochMillis = { now },
    )

    @Test
    fun notifiedFlagPreventsAnotherNotification() = runBlocking {
        val repository = FakeAchievementRepository()
        val calls = mutableListOf<String>()
        val sync = sync(repository, calls)
        val stats = calculate(listOf(focus(today, 60_000L)))

        sync.apply(stats)
        sync.apply(stats)

        assertEquals(listOf("sessions_1"), calls)
        val stored = repository.getUnlocks().single()
        assertTrue(stored.notified)
        assertEquals(1_000L, stored.unlockedAtEpochMillis)
    }

    @Test
    fun recomputeDoesNotResetUnlockedAt() = runBlocking {
        val repository = FakeAchievementRepository()
        val sync = sync(repository, mutableListOf())
        sync.apply(calculate(listOf(focus(today, 60_000L))))
        now = 9_000L
        sync.apply(calculate(List(10) { focus(today, 60_000L) }))

        assertEquals(1_000L, repository.getUnlocks().first { it.id == "sessions_1" }.unlockedAtEpochMillis)
        assertEquals(9_000L, repository.getUnlocks().first { it.id == "sessions_10" }.unlockedAtEpochMillis)
    }

    @Test
    fun eachUnlockIsASeparateNotificationCall() = runBlocking {
        val repository = FakeAchievementRepository()
        val calls = mutableListOf<String>()
        val hour = Achievements.HOUR_MILLIS
        val days = (0 until 30).map { focus(today.minus(it, DateTimeUnit.DAY), hour) }
        val extraToday = List(70) { focus(today, hour) }

        sync(repository, calls).apply(calculate(days + extraToday))

        assertEquals(Achievements.all.map { it.id }, calls)
        assertEquals(13, calls.distinct().size)
        assertTrue(repository.getUnlocks().all { it.notified })
    }

    @Test
    fun earnedStreakIsNotNotifiedAgainAfterItBreaks() = runBlocking {
        val repository = FakeAchievementRepository()
        val calls = mutableListOf<String>()
        val sync = sync(repository, calls)
        val streak = (0 until 3).map { focus(today.minus(it, DateTimeUnit.DAY), 60_000L) }

        sync.apply(calculate(streak))
        sync.apply(calculate(emptyList()))

        assertEquals(listOf("sessions_1", "streak_3"), calls)
        assertTrue(repository.getUnlocks().first { it.id == "streak_3" }.notified)
    }
}

private class FakeAchievementRepository : AchievementRepository {
    private val rows = mutableListOf<AchievementUnlockRecord>()
    private val updates = MutableStateFlow<List<AchievementUnlockRecord>>(emptyList())

    override fun observeUnlocks(): Flow<List<AchievementUnlockRecord>> = updates

    override suspend fun getUnlocks(): List<AchievementUnlockRecord> = rows.toList()

    override suspend fun insertUnlocks(unlocks: List<AchievementUnlockRecord>) {
        val existing = rows.map { it.id }.toSet()
        rows += unlocks.filter { it.id !in existing }
        updates.value = rows.toList()
    }

    override suspend fun markNotified(ids: List<String>) {
        val idSet = ids.toSet()
        for (index in rows.indices) {
            val row = rows[index]
            if (row.id in idSet) rows[index] = row.copy(notified = true)
        }
        updates.value = rows.toList()
    }
}
