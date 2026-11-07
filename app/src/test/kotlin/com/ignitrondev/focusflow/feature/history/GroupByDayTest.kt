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
package com.ignitrondev.focusflow.feature.history

import com.ignitrondev.focusflow.domain.timer.FinishedSession
import com.ignitrondev.focusflow.domain.timer.SessionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupByDayTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 7)
    private val minute = 60_000L

    private fun session(
        id: String,
        date: LocalDate,
        end: LocalTime,
        minutes: Long,
        type: SessionType = SessionType.Focus,
        completed: Boolean = true,
    ) = date.atTime(end).toInstant(zone).toEpochMilli().let { endMillis ->
        FinishedSession(id, type, endMillis - minutes * minute, endMillis, minutes * minute, minutes * minute, completed)
    }

    @Test
    fun groupsNewestDayAndSessionFirstWithDailyFocusTotal() {
        val days = groupByDay(
            listOf(
                session("a", today.minusDays(1), LocalTime.of(9, 0), 25),
                session("b", today, LocalTime.of(9, 0), 25),
                session("c", today, LocalTime.of(11, 0), 5, type = SessionType.ShortBreak),
                session("d", today, LocalTime.of(12, 0), 10, completed = false),
            ),
            today,
            zone,
        )

        assertEquals(listOf(today, today.minusDays(1)), days.map { it.date })
        assertTrue(days[0].isToday)
        assertTrue(days[1].isYesterday)
        assertEquals(listOf("d", "c", "b"), days[0].items.map { it.id })
        assertEquals("only completed focus counts", 25 * minute, days[0].focusMillis)
        assertEquals(LocalTime.of(8, 35), days[0].items.last().startTime)
    }
}
