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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusStatisticsCalculatorTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 7) // a Saturday
    private val minute = 60_000L
    private var nextId = 0

    private fun session(
        date: LocalDate,
        minutes: Long,
        type: SessionType = SessionType.Focus,
        completed: Boolean = true,
        endTime: LocalTime = LocalTime.of(10, 0),
    ): FinishedSession {
        val end = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli()
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
        assertEquals("Mon..Sat is 6 days", 75 * minute / 6, mondayWeek.weekDailyAverageMillis)
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
        val stats = calculate(listOf(session(today, 25, endTime = LocalTime.of(0, 10))))

        assertEquals(25 * minute, stats.today.focusMillis)
    }

    @Test
    fun averageAndTotalsCoverAllTime() {
        val stats = calculate(listOf(session(today, 25), session(today.minusDays(30), 45)))

        assertEquals(70 * minute, stats.totalFocusMillis)
        assertEquals(2, stats.totalSessions)
        assertEquals(35 * minute, stats.averageSessionMillis)
    }
}
