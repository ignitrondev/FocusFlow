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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class DayFocus(val date: LocalDate, val focusMillis: Long, val sessions: Int)

/** Statistics over **completed focus sessions**; breaks and interrupted sessions do not count. */
data class FocusStatistics(
    val today: DayFocus,
    val weekFocusMillis: Long,
    val weekSessions: Int,
    /** Week total divided by the days of this week so far (today included). */
    val weekDailyAverageMillis: Long,
    val averageSessionMillis: Long,
    val totalSessions: Int,
    val totalFocusMillis: Long,
    /** Seven entries, oldest first, ending today. */
    val lastSevenDays: List<DayFocus>,
    /** First day of the current week, per the user's locale. */
    val weekStart: LocalDate = today.date,
    /**
     * Consecutive days with at least one completed focus session, up to today. A streak that
     * reached yesterday is still alive today until the day ends.
     */
    val currentStreakDays: Int = 0,
) {
    val isEmpty: Boolean get() = totalSessions == 0
}

object FocusStatisticsCalculator {
    /**
     * Sessions belong to the local day on which they ended. The week starts on [firstDayOfWeek]
     * (taken from the user's locale).
     */
    fun calculate(sessions: List<FinishedSession>, today: LocalDate, zone: ZoneId, firstDayOfWeek: DayOfWeek): FocusStatistics {
        val focus = sessions.filter { it.completed && it.sessionType == SessionType.Focus }
        val byDay: Map<LocalDate, List<FinishedSession>> =
            focus.groupBy { Instant.ofEpochMilli(it.endedAtEpochMillis).atZone(zone).toLocalDate() }

        fun day(date: LocalDate): DayFocus {
            val items = byDay[date].orEmpty()
            return DayFocus(date, items.sumOf { it.actualDurationMillis }, items.size)
        }

        val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val weekDays = generateSequence(weekStart) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.map(::day).toList()
        val weekFocus = weekDays.sumOf { it.focusMillis }
        val totalFocus = focus.sumOf { it.actualDurationMillis }

        return FocusStatistics(
            today = day(today),
            weekFocusMillis = weekFocus,
            weekSessions = weekDays.sumOf { it.sessions },
            weekDailyAverageMillis = weekFocus / (ChronoUnit.DAYS.between(weekStart, today) + 1),
            averageSessionMillis = if (focus.isEmpty()) 0 else totalFocus / focus.size,
            totalSessions = focus.size,
            totalFocusMillis = totalFocus,
            lastSevenDays = (6L downTo 0L).map { day(today.minusDays(it)) },
            weekStart = weekStart,
            currentStreakDays = streak(byDay.keys, today),
        )
    }

    private fun streak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in activeDays) today else today.minusDays(1)
        var count = 0
        while (day in activeDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}
