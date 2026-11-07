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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The system font is used on purpose: it is always available offline, needs no Google Play
// services (downloadable fonts do) and renders well in every script the device supports.

private val Base = Typography()

internal val FocusFlowTypography =
    Typography(
        displayLarge = Base.displayLarge,
        displayMedium = Base.displayMedium,
        displaySmall = Base.displaySmall,
        headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = Base.titleSmall,
        bodyLarge = Base.bodyLarge,
        bodyMedium = Base.bodyMedium,
        bodySmall = Base.bodySmall,
        labelLarge = Base.labelLarge,
        labelMedium = Base.labelMedium,
        labelSmall = Base.labelSmall,
    )

/**
 * Large countdown digits. `tnum` (tabular figures) gives every digit the same width, so the text
 * does not jitter while the seconds change.
 */
val TimerTextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum",
    )
