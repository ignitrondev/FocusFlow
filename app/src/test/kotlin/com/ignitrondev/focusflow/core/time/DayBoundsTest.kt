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

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DayBoundsTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun millis(text: String, zone: ZoneId) = ZonedDateTime.of(
        java.time.LocalDateTime.parse(text),
        zone,
    ).toInstant().toEpochMilli()

    @Test
    fun boundsAreLocalMidnights() {
        val (start, end) = dayBounds(millis("2026-11-07T23:30", jakarta), jakarta)

        assertEquals(millis("2026-11-07T00:00", jakarta), start)
        assertEquals(millis("2026-11-08T00:00", jakarta), end)
    }

    @Test
    fun dayWithADaylightSavingChangeIsNot24Hours() {
        val berlin = ZoneId.of("Europe/Berlin")

        val (start, end) = dayBounds(millis("2026-10-25T12:00", berlin), berlin)

        assertEquals(25 * 60 * 60 * 1000L, end - start)
    }
}
