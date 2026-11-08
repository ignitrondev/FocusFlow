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

import kotlin.math.ceil

/**
 * Even y-axis for the week chart. Visual scale only: it does not change the stored statistics.
 *
 * [stepMinutes] is [niceStep] of `max / 4`, and [axisMaxMinutes] is `max(2, ceil(max / step)) * step`.
 * A week of zeros uses the fixed empty axis 0, 15, 30, 45, 60.
 */
data class ChartAxis(val stepMinutes: Int, val axisMaxMinutes: Int, val ticksMinutes: List<Int>)

private val niceSteps = intArrayOf(5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440)

/** First nice step that is at least [x]. Past 1440 the last step keeps doubling. */
fun niceStep(x: Double): Int {
    for (step in niceSteps) {
        if (step.toDouble() >= x) return step
    }
    var step = niceSteps.last()
    while (step.toDouble() < x) {
        if (step > Int.MAX_VALUE / 2) return step
        step *= 2
    }
    return step
}

/** Whole minutes, matching the on-screen duration text, which drops seconds. */
fun focusMinutes(millis: Long): Int = (millis / 60_000L).toInt().coerceAtLeast(0)

fun chartAxis(dailyMinutes: List<Int>): ChartAxis {
    val maxMinutes = dailyMinutes.maxOrNull() ?: 0
    if (maxMinutes <= 0) return ChartAxis(stepMinutes = 15, axisMaxMinutes = 60, ticksMinutes = listOf(0, 15, 30, 45, 60))
    val step = niceStep(maxMinutes / 4.0)
    val bands = maxOf(2, ceil(maxMinutes.toDouble() / step).toInt())
    val axisMax = bands * step
    return ChartAxis(step, axisMax, List(bands + 1) { it * step })
}

/** "5 min" under an hour, then "1h" and "1h 30m". */
fun chartTickLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
