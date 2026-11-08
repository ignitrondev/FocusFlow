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

import androidx.compose.runtime.Composable
import com.ignitrondev.focusflow.platform.DateFormats
import com.ignitrondev.focusflow.platform.LocalDateFormats
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.share_card_week_range
import com.ignitrondev.focusflow.resources.week_range_same_month
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Label for a week already computed by [com.ignitrondev.focusflow.core.time.weekContaining].
 * A shared month is written once: 2 Nov through 8 Nov is "2 – 8 Nov".
 */
@Composable
fun weekRangeLabel(start: LocalDate, end: LocalDate, dates: DateFormats = LocalDateFormats.current): String = when {
    start == end -> dates.dayMonth(start)

    start.year == end.year && start.month == end.month ->
        stringResource(Res.string.week_range_same_month, start.day, end.day, dates.monthShort(start))

    else -> stringResource(Res.string.share_card_week_range, dates.dayMonth(start), dates.dayMonth(end))
}
