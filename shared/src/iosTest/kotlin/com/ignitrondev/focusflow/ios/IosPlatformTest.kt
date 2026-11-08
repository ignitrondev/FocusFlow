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
package com.ignitrondev.focusflow.ios

import com.ignitrondev.focusflow.core.time.dayOfWeekFromSundayBased
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.domain.timer.UpcomingSessionEnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar

class IosPlatformTest {
    private val end = UpcomingSessionEnd(SessionType.Focus, 1_794_043_500_000L, SessionType.ShortBreak, nextStartsAutomatically = false)

    @Test
    fun alertIdentifierCarriesItsDeadline() {
        val id = IosSessionAlerts.identifier(end)

        assertTrue(id.startsWith(IosSessionAlerts.PREFIX))
        assertEquals(end.atEpochMillis, IosSessionAlerts.deadlineOf(id))
    }

    @Test
    fun aChangedAlertTextGetsANewIdentifier() {
        assertNotEquals(IosSessionAlerts.identifier(end), IosSessionAlerts.identifier(end.copy(nextStartsAutomatically = true)))
    }

    @Test
    fun clockIsMonotonicAndKnowsTheBoot() {
        val clock = IosTimeSource()
        val first = clock.now()
        val second = clock.now()

        assertTrue(second.elapsedRealtimeMillis >= first.elapsedRealtimeMillis)
        assertTrue(first.elapsedRealtimeMillis > 0)
        assertTrue((first.bootCount ?: 0) > 0, "boot time read from sysctl")
        assertEquals(first.bootCount, second.bootCount)
    }

    @Test
    fun datesAreFormattedByFoundation() {
        val date = LocalDate(2026, 11, 7)

        assertTrue(IosDateFormats.fullDate(date).contains("2026"))
        assertTrue(IosDateFormats.dayMonth(date).contains("7"))
        assertTrue(IosDateFormats.shortTime(LocalTime(9, 30)).contains("30"))
        assertEquals(DayOfWeek.entries.size, DayOfWeek.entries.map(IosDateFormats::weekdayShort).toSet().size)
    }

    @Test
    fun firstDayOfWeekIsAValidDay() {
        assertTrue(IosPlatformServices().firstDayOfWeek() in DayOfWeek.entries)
    }

    @Test
    fun firstDayOfWeekUsesTheDeviceCalendar() {
        val platformDay = NSCalendar.currentCalendar.firstWeekday.toInt()

        assertEquals(dayOfWeekFromSundayBased(platformDay), IosPlatformServices().firstDayOfWeek())
    }
}
