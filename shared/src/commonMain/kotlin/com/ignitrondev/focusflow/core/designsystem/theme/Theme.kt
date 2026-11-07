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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * FocusFlow's theme. [dynamicScheme] is the platform's wallpaper-based scheme when the user chose
 * it and the platform has one (Android 12+); otherwise the FocusFlow palette is used.
 */
@Composable
fun FocusFlowTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicScheme: ColorScheme? = null, content: @Composable () -> Unit) {
    val colorScheme = dynamicScheme ?: if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusFlowTypography,
        content = content,
    )
}
