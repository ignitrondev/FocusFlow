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

import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.timer_mode_focus
import com.ignitrondev.focusflow.resources.timer_mode_long_break
import com.ignitrondev.focusflow.resources.timer_mode_short_break
import org.jetbrains.compose.resources.StringResource

/** The user-facing name of a session type ("Focus", "Short break", ...). */
fun SessionType.labelRes(): StringResource = when (this) {
    SessionType.Focus -> Res.string.timer_mode_focus
    SessionType.ShortBreak -> Res.string.timer_mode_short_break
    SessionType.LongBreak -> Res.string.timer_mode_long_break
}
