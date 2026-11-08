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
package com.ignitrondev.focusflow.feature.statistics

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartAxisTest {
    @Test
    fun emptyWeekUsesTheFixedAxis() {
        val axis = chartAxis(List(7) { 0 })

        assertEquals(15, axis.stepMinutes)
        assertEquals(60, axis.axisMaxMinutes)
        assertEquals(listOf(0, 15, 30, 45, 60), axis.ticksMinutes)
    }

    @Test
    fun fiveMinutePeakUsesTenMinuteAxis() {
        // max 5 → niceStep(1.25) = 5 → max(2, ceil(1)) × 5 = 10.
        val axis = chartAxis(listOf(5, 0, 0, 0, 0, 0, 0))

        assertEquals(5, niceStep(5 / 4.0))
        assertEquals(5, axis.stepMinutes)
        assertEquals(10, axis.axisMaxMinutes)
        assertEquals(listOf(0, 5, 10), axis.ticksMinutes)
    }

    @Test
    fun fortyMinutePeakLandsOnTheSample() {
        // max 40 → niceStep(10) = 10 → ceil(4) × 10 = 40.
        val axis = chartAxis(listOf(25, 40, 0, 15, 0, 0, 5))

        assertEquals(10, niceStep(40 / 4.0))
        assertEquals(10, axis.stepMinutes)
        assertEquals(40, axis.axisMaxMinutes)
        assertEquals(listOf(0, 10, 20, 30, 40), axis.ticksMinutes)
    }

    @Test
    fun niceStepDoublesPastTheLastListedValue() {
        assertEquals(1440, niceStep(1440.0))
        assertEquals(2880, niceStep(1440.1))
        assertEquals(2880, niceStep(2880.0))
        assertEquals(5760, niceStep(2881.0))
    }

    @Test
    fun axisHasBetweenThreeAndFiveTicks() {
        listOf(1, 7, 25, 90, 100, 200).forEach { max ->
            val axis = chartAxis(listOf(max))
            assertEquals(niceStep(max / 4.0), axis.stepMinutes)
            val bands = maxOf(2, kotlin.math.ceil(max.toDouble() / axis.stepMinutes).toInt())
            assertEquals(bands * axis.stepMinutes, axis.axisMaxMinutes)
            assertEquals(bands + 1, axis.ticksMinutes.size)
            assertEquals(0, axis.ticksMinutes.first())
            assertEquals(axis.axisMaxMinutes, axis.ticksMinutes.last())
        }
    }

    @Test
    fun tickLabelsSwitchToHoursAtSixtyMinutes() {
        assertEquals("0 min", chartTickLabel(0))
        assertEquals("45 min", chartTickLabel(45))
        assertEquals("1h", chartTickLabel(60))
        assertEquals("1h 30m", chartTickLabel(90))
        assertEquals("2h", chartTickLabel(120))
    }
}
