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

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationFormatTest {
    @Test
    fun formatsMinutesAndSecondsWithLeadingZeros() {
        assertEquals("25:00", formatMinutesSeconds(25 * 60))
        assertEquals("04:07", formatMinutesSeconds(4 * 60 + 7))
        assertEquals("00:00", formatMinutesSeconds(0))
    }

    @Test
    fun minutesAreNotCappedAtOneHour() {
        assertEquals("90:00", formatMinutesSeconds(90 * 60))
    }

    @Test
    fun negativeDurationIsShownAsZero() {
        assertEquals("00:00", formatMinutesSeconds(-5))
    }
}
