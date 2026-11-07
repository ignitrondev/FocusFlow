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
package com.ignitrondev.focusflow.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Simple English formats for @Preview, where no platform services exist. */
object PreviewDateFormats : DateFormats {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    override fun fullDate(date: LocalDate) = "${weekdayShort(date.dayOfWeek)}, ${dayMonth(date)} ${date.year}"

    override fun mediumDate(date: LocalDate) = "${months[date.month.ordinal]} ${date.day}, ${date.year}"

    override fun dayMonth(date: LocalDate) = "${date.day} ${months[date.month.ordinal]}"

    override fun shortTime(time: LocalTime) = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

    override fun weekdayShort(day: DayOfWeek) = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    override fun weekdayNarrow(day: DayOfWeek) = day.name.take(1)
}

/** The locale's date formats; the app root provides the platform's, previews get [PreviewDateFormats]. */
val LocalDateFormats = androidx.compose.runtime.staticCompositionLocalOf<DateFormats> { PreviewDateFormats }
