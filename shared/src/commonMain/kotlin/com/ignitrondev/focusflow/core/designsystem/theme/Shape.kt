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
package com.ignitrondev.focusflow.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Corner radii for the statistics screen and the floating navbar. Screens must use these, not raw dp. */
internal object FocusFlowShapes {
    val card = RoundedCornerShape(24.dp)
    val chip = RoundedCornerShape(12.dp)
    val bar = RoundedCornerShape(999.dp)

    /** The floating glass navbar is a full pill, and so is the press and focus shape of each tab. */
    val navContainer = RoundedCornerShape(percent = 50)
    val navItem = RoundedCornerShape(percent = 50)
    val iconButton = CircleShape
}
