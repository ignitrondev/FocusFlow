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
package com.ignitrondev.focusflow.core.ui

import androidx.compose.runtime.Composable
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.duration_hours
import com.ignitrondev.focusflow.resources.duration_hours_minutes
import com.ignitrondev.focusflow.resources.duration_minutes_short
import org.jetbrains.compose.resources.stringResource

/** "25 min", "2 h" or "1 h 40 min"; seconds are dropped. */
@Composable
fun durationText(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(Res.string.duration_hours_minutes, hours.toInt(), minutes.toInt())
        hours > 0 -> stringResource(Res.string.duration_hours, hours.toInt())
        else -> stringResource(Res.string.duration_minutes_short, minutes.toInt())
    }
}
