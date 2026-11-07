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

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Start (inclusive) and end (exclusive) of the local calendar day containing [epochMillis]. */
fun dayBounds(epochMillis: Long, zone: TimeZone): Pair<Long, Long> {
    val day = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val start = day.atStartOfDayIn(zone).toEpochMilliseconds()
    val end = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
    return start to end
}
