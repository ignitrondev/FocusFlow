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

import com.ignitrondev.focusflow.platform.PreviewDateFormats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class WeekRangeTest {
    private val sunday = LocalDate(2026, 11, 8)

    @Test
    fun sundayWithAMondayStartIs2Through8Nov() {
        val week = weekContaining(sunday, DayOfWeek.MONDAY)

        assertEquals(LocalDate(2026, 11, 2), week.start)
        assertEquals(LocalDate(2026, 11, 8), week.end)
        assertEquals("2 Nov", PreviewDateFormats.dayMonth(week.start))
        assertEquals("8 Nov", PreviewDateFormats.dayMonth(week.end))
        assertEquals("Nov", PreviewDateFormats.monthShort(week.start))
    }

    @Test
    fun everyFirstDayProducesAWeekThatContainsTheDay() {
        DayOfWeek.entries.forEach { first ->
            val week = weekContaining(sunday, first)

            assertEquals(first, week.start.dayOfWeek, first.name)
            assertEquals(week.start.plus(6, DateTimeUnit.DAY), week.end)
            assertEquals(week.dates, (0..6).map { week.start.plus(it, DateTimeUnit.DAY) })
            assertTrue(sunday in week.dates)
        }
    }

    @Test
    fun sundayStartOnSundayIsThatDayThroughTheNextSaturday() {
        val week = weekContaining(sunday, DayOfWeek.SUNDAY)

        assertEquals(sunday, week.start)
        assertEquals(LocalDate(2026, 11, 14), week.end)
    }

    @Test
    fun platformWeekdayNumbersMatchIosAndAndroidCalendars() {
        val expected = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
        )
        expected.forEachIndexed { index, day ->
            assertEquals(day, dayOfWeekFromSundayBased(index + 1))
        }
        assertFailsWith<IllegalArgumentException> { dayOfWeekFromSundayBased(0) }
        assertFailsWith<IllegalArgumentException> { dayOfWeekFromSundayBased(8) }
    }
}
