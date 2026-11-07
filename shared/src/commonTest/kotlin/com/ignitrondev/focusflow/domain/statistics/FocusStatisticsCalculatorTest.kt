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
package com.ignitrondev.focusflow.domain.statistics

import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

class FocusStatisticsCalculatorTest {
    private val zone = TimeZone.of("Asia/Jakarta")
    private val today = LocalDate(2026, 11, 7) // a Saturday
    private val minute = 60_000L
    private var nextId = 0

    private fun session(
        date: LocalDate,
        minutes: Long,
        type: SessionType = SessionType.Focus,
        completed: Boolean = true,
        endTime: LocalTime = LocalTime(10, 0),
    ): FinishedSession {
        val end = LocalDateTime(date, endTime).toInstant(zone).toEpochMilliseconds()
        return FinishedSession("s${nextId++}", type, end - minutes * minute, end, minutes * minute, minutes * minute, completed)
    }

    private fun calculate(sessions: List<FinishedSession>, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY) =
        FocusStatisticsCalculator.calculate(sessions, today, zone, firstDayOfWeek)

    @Test
    fun noSessionsMeansEmpty() {
        val stats = calculate(emptyList())

        assertTrue(stats.isEmpty)
        assertEquals(7, stats.lastSevenDays.size)
        assertEquals(0L, stats.averageSessionMillis)
    }

    @Test
    fun onlyCompletedFocusSessionsCount() {
        val stats = calculate(
            listOf(
                session(today, 25),
                session(today, 5, type = SessionType.ShortBreak),
                session(today, 12, completed = false),
            ),
        )

        assertEquals(25 * minute, stats.today.focusMillis)
        assertEquals(1, stats.today.sessions)
        assertEquals(1, stats.totalSessions)
    }

    @Test
    fun weekStartsOnTheLocalesFirstDay() {
        val sessions = listOf(
            session(today, 25), // Saturday
            session(today.minusDays(5), 50), // Monday
            session(today.minusDays(6), 25), // Sunday
        )

        val mondayWeek = calculate(sessions, DayOfWeek.MONDAY)
        val sundayWeek = calculate(sessions, DayOfWeek.SUNDAY)

        assertEquals(75 * minute, mondayWeek.weekFocusMillis)
        assertEquals(2, mondayWeek.weekSessions)
        assertEquals(75 * minute / 6, mondayWeek.weekDailyAverageMillis, "Mon..Sat is 6 days")
        assertEquals(100 * minute, sundayWeek.weekFocusMillis)
        assertEquals(3, sundayWeek.weekSessions)
    }

    @Test
    fun lastSevenDaysEndTodayOldestFirst() {
        val stats = calculate(listOf(session(today, 25), session(today.minusDays(2), 50), session(today.minusDays(9), 25)))

        assertEquals(today.minusDays(6), stats.lastSevenDays.first().date)
        assertEquals(today, stats.lastSevenDays.last().date)
        assertEquals(listOf(0L, 0L, 0L, 0L, 50L, 0L, 25L), stats.lastSevenDays.map { it.focusMillis / minute })
    }

    @Test
    fun sessionsBelongToTheLocalDayTheyEnded() {
        // Ends at 00:10 local time: counts for today even though it started yesterday.
        val stats = calculate(listOf(session(today, 25, endTime = LocalTime(0, 10))))

        assertEquals(25 * minute, stats.today.focusMillis)
    }

    @Test
    fun streakCountsConsecutiveDaysUpToToday() {
        val stats = calculate(
            listOf(
                session(today, 25),
                session(today.minusDays(1), 25),
                session(today.minusDays(2), 25),
                session(today.minusDays(4), 25), // gap on day 3 ends the streak
            ),
        )

        assertEquals(3, stats.currentStreakDays)
    }

    @Test
    fun streakFromYesterdayIsStillAliveToday() {
        val stats = calculate(listOf(session(today.minusDays(1), 25), session(today.minusDays(2), 25)))

        assertEquals(2, stats.currentStreakDays)
    }

    @Test
    fun streakIsBrokenByAMissedDayAndIgnoresBreaksAndStoppedSessions() {
        val stats = calculate(
            listOf(
                session(today.minusDays(2), 25),
                session(today, 5, type = SessionType.ShortBreak),
                session(today.minusDays(1), 10, completed = false),
            ),
        )

        assertEquals(0, stats.currentStreakDays)
    }

    @Test
    fun weekStartFollowsTheLocale() {
        assertEquals(today.minusDays(5), calculate(emptyList(), DayOfWeek.MONDAY).weekStart)
        assertEquals(today.minusDays(6), calculate(emptyList(), DayOfWeek.SUNDAY).weekStart)
    }

    @Test
    fun averageAndTotalsCoverAllTime() {
        val stats = calculate(listOf(session(today, 25), session(today.minusDays(30), 45)))

        assertEquals(70 * minute, stats.totalFocusMillis)
        assertEquals(2, stats.totalSessions)
        assertEquals(35 * minute, stats.averageSessionMillis)
    }
}

private fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
