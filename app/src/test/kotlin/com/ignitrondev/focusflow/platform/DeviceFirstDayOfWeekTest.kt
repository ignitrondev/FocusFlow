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

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceFirstDayOfWeekTest {
    @Test
    fun firstDayFollowsTheFormatLocaleRatherThanTheDisplayLanguage() {
        val display = Locale.getDefault(Locale.Category.DISPLAY)
        val format = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.DISPLAY, Locale.US)
            Locale.setDefault(Locale.Category.FORMAT, Locale.UK)
            assertEquals(DayOfWeek.MONDAY, deviceFirstDayOfWeek())

            Locale.setDefault(Locale.Category.DISPLAY, Locale.UK)
            Locale.setDefault(Locale.Category.FORMAT, Locale.US)
            assertEquals(DayOfWeek.SUNDAY, deviceFirstDayOfWeek())
        } finally {
            Locale.setDefault(Locale.Category.DISPLAY, display)
            Locale.setDefault(Locale.Category.FORMAT, format)
        }
    }
}
