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
package com.ignitrondev.focusflow.core.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * One calendar week, [start] through [end] inclusive. Computed once and then shared by the week
 * label, the chart and the weekly totals so those cannot drift apart.
 */
data class WeekRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(end >= start)
    }

    val dates: List<LocalDate> = buildList {
        var day = start
        while (day <= end) {
            add(day)
            day = day.plus(1, DateTimeUnit.DAY)
        }
    }
}

/**
 * The week that contains [day] when weeks begin on [firstDayOfWeek].
 * [firstDayOfWeek] comes from the device locale; nothing here assumes Monday or Sunday.
 */
fun weekContaining(day: LocalDate, firstDayOfWeek: DayOfWeek): WeekRange {
    val daysSinceStart = (day.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
    val start = day.minus(daysSinceStart, DateTimeUnit.DAY)
    return WeekRange(start, start.plus(6, DateTimeUnit.DAY))
}

/**
 * Maps a platform weekday number to [DayOfWeek].
 *
 * Android (`android.icu.util.Calendar`, `java.util.Calendar`) and iOS (`NSCalendar.firstWeekday`)
 * all number Sunday as 1 through Saturday as 7. ISO numbers, which [DayOfWeek] uses, start at Monday.
 */
fun dayOfWeekFromSundayBased(platformDay: Int): DayOfWeek {
    require(platformDay in 1..7) { "Platform weekday must be 1 (Sunday) through 7 (Saturday), was $platformDay" }
    return DayOfWeek((platformDay + 5) % 7 + 1)
}
