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

import com.ignitrondev.focusflow.core.time.weekContaining
import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
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
    /** The seven days of the current locale week, [weekStart] through [weekEnd], oldest first. */
    val weekDays: List<DayFocus>,
    /** First day of the current week. Same value the chart and the week label use. */
    val weekStart: LocalDate,
    /** Last day of that week (six days after [weekStart]), including days still ahead of today. */
    val weekEnd: LocalDate,
    /**
     * Consecutive days that each contain at least one completed focus session, ending today, or
     * yesterday when today has none yet. No completed focus session means 0, including when today
     * is empty and yesterday is empty. A run that crosses a week or month boundary stays intact.
     */
    val currentStreakDays: Int = 0,
) {
    val isEmpty: Boolean get() = totalSessions == 0
}

object FocusStatisticsCalculator {
    /**
     * Sessions belong to the local day on which they ended. The week is [weekContaining] of
     * [today] starting on [firstDayOfWeek] (the device locale). That one range feeds the weekly
     * totals, [FocusStatistics.weekDays] and the start/end the label prints.
     */
    fun calculate(sessions: List<FinishedSession>, today: LocalDate, zone: TimeZone, firstDayOfWeek: DayOfWeek): FocusStatistics {
        val focus = sessions.filter { it.completed && it.sessionType == SessionType.Focus }
        val byDay: Map<LocalDate, List<FinishedSession>> =
            focus.groupBy { Instant.fromEpochMilliseconds(it.endedAtEpochMillis).toLocalDateTime(zone).date }

        fun day(date: LocalDate): DayFocus {
            val items = byDay[date].orEmpty()
            return DayFocus(date, items.sumOf { it.actualDurationMillis }, items.size)
        }

        val week = weekContaining(today, firstDayOfWeek)
        val weekDays = week.dates.map(::day)
        val elapsedDays = week.dates.count { it <= today }
        val weekFocus = weekDays.sumOf { it.focusMillis }
        val totalFocus = focus.sumOf { it.actualDurationMillis }

        return FocusStatistics(
            today = day(today),
            weekFocusMillis = weekFocus,
            weekSessions = weekDays.sumOf { it.sessions },
            weekDailyAverageMillis = weekFocus / elapsedDays,
            averageSessionMillis = if (focus.isEmpty()) 0 else totalFocus / focus.size,
            totalSessions = focus.size,
            totalFocusMillis = totalFocus,
            weekDays = weekDays,
            weekStart = week.start,
            weekEnd = week.end,
            currentStreakDays = streak(byDay.keys, today),
        )
    }

    /**
     * Days in [activeDays] already have a completed focus session. Today counts only when it is
     * one of them; otherwise the run must reach yesterday or it is 0.
     */
    private fun streak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        if (activeDays.isEmpty()) return 0
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        var day = when {
            today in activeDays -> today
            yesterday in activeDays -> yesterday
            else -> return 0
        }
        var count = 0
        while (day in activeDays) {
            count++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }
}
