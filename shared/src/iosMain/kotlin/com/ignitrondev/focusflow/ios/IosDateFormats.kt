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

import com.ignitrondev.focusflow.platform.DateFormats
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

/** Dates in the user's locale and region settings, formatted by Foundation. */
internal object IosDateFormats : DateFormats {
    // Formatters are costly to create and safe to share between threads.
    private val full by lazy { formatter { dateStyle = NSDateFormatterFullStyle } }
    private val medium by lazy { formatter { dateStyle = NSDateFormatterMediumStyle } }
    private val dayMonth by lazy { formatter { setLocalizedDateFormatFromTemplate("dMMM") } }
    private val time by lazy { formatter { timeStyle = NSDateFormatterShortStyle } }
    private val weekdays by lazy { formatter {} }

    override fun fullDate(date: LocalDate): String = full.stringFromDate(date.toNSDate())

    override fun mediumDate(date: LocalDate): String = medium.stringFromDate(date.toNSDate())

    override fun dayMonth(date: LocalDate): String = dayMonth.stringFromDate(date.toNSDate())

    override fun shortTime(time: LocalTime): String = this.time.stringFromDate(
        nsDate {
            hour = time.hour.toLong()
            minute = time.minute.toLong()
        },
    )

    // Foundation's weekday lists start on Sunday.
    override fun weekdayShort(day: DayOfWeek): String = weekdays.shortWeekdaySymbols[day.isoDayNumber % 7] as String

    override fun weekdayNarrow(day: DayOfWeek): String = weekdays.veryShortWeekdaySymbols[day.isoDayNumber % 7] as String

    private fun formatter(configure: NSDateFormatter.() -> Unit) = NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        dateStyle = NSDateFormatterNoStyle
        timeStyle = NSDateFormatterNoStyle
        configure()
    }

    // Noon, so a time zone that skips midnight for daylight saving still has the date.
    private fun LocalDate.toNSDate(): NSDate = nsDate {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.month.number.toLong()
        day = this@toNSDate.day.toLong()
        hour = 12
    }

    private fun nsDate(components: NSDateComponents.() -> Unit): NSDate =
        NSCalendar.currentCalendar.dateFromComponents(NSDateComponents().apply(components)) ?: NSDate()
}
