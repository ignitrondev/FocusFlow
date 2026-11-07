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
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
    fun calculate(sessions: List<FinishedSession>, today: LocalDate, zone: TimeZone, firstDayOfWeek: DayOfWeek): FocusStatistics {
        val focus = sessions.filter { it.completed && it.sessionType == SessionType.Focus }
        val byDay: Map<LocalDate, List<FinishedSession>> =
            focus.groupBy { Instant.fromEpochMilliseconds(it.endedAtEpochMillis).toLocalDateTime(zone).date }

        fun day(date: LocalDate): DayFocus {
            val items = byDay[date].orEmpty()
            return DayFocus(date, items.sumOf { it.actualDurationMillis }, items.size)
        }

        val daysSinceWeekStart = (today.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
        val weekStart = today.minus(daysSinceWeekStart, DateTimeUnit.DAY)
        val weekDays = generateSequence(weekStart) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= today }.map(::day).toList()
        val weekFocus = weekDays.sumOf { it.focusMillis }
        val totalFocus = focus.sumOf { it.actualDurationMillis }

        return FocusStatistics(
            today = day(today),
            weekFocusMillis = weekFocus,
            weekSessions = weekDays.sumOf { it.sessions },
            weekDailyAverageMillis = weekFocus / (weekStart.daysUntil(today) + 1),
            averageSessionMillis = if (focus.isEmpty()) 0 else totalFocus / focus.size,
            totalSessions = focus.size,
            totalFocusMillis = totalFocus,
            lastSevenDays = (6 downTo 0).map { day(today.minus(it, DateTimeUnit.DAY)) },
            weekStart = weekStart,
            currentStreakDays = streak(byDay.keys, today),
        )
    }

    private fun streak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in activeDays) today else today.minus(1, DateTimeUnit.DAY)
        var count = 0
        while (day in activeDays) {
            count++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }
}
