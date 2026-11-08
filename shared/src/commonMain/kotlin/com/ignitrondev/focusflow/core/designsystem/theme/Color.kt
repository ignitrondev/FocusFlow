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

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// FocusFlow palette: a calm indigo for focus, a soft teal for breaks, and neutral surfaces with
// a slight cool tint. Tones follow the Material 3 tonal system so contrast pairs stay accessible.

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF4A55C8),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E0FF),
        onPrimaryContainer = Color(0xFF121A7A),
        secondary = Color(0xFF5C5D72),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE1E0F9),
        onSecondaryContainer = Color(0xFF191A2C),
        tertiary = Color(0xFF006A62),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF9EF2E6),
        onTertiaryContainer = Color(0xFF00201D),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBF8FF),
        onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE3E1EC),
        onSurfaceVariant = Color(0xFF46464F),
        outline = Color(0xFF777680),
        outlineVariant = Color(0xFFC7C5D0),
        inverseSurface = Color(0xFF303036),
        inverseOnSurface = Color(0xFFF3EFF7),
        inversePrimary = Color(0xFFBFC2FF),
        surfaceDim = Color(0xFFDBD9E0),
        surfaceBright = Color(0xFFFBF8FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F2FA),
        surfaceContainer = Color(0xFFEFEDF4),
        surfaceContainerHigh = Color(0xFFE9E7EF),
        surfaceContainerHighest = Color(0xFFE4E1E9),
    )

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFBFC2FF),
        onPrimary = Color(0xFF1D2593),
        primaryContainer = Color(0xFF3239B0),
        onPrimaryContainer = Color(0xFFE0E0FF),
        secondary = Color(0xFFC5C4DD),
        onSecondary = Color(0xFF2E2F42),
        secondaryContainer = Color(0xFF444559),
        onSecondaryContainer = Color(0xFFE1E0F9),
        tertiary = Color(0xFF82D5CA),
        onTertiary = Color(0xFF003732),
        tertiaryContainer = Color(0xFF005049),
        onTertiaryContainer = Color(0xFF9EF2E6),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF46464F),
        onSurfaceVariant = Color(0xFFC7C5D0),
        outline = Color(0xFF918F9A),
        outlineVariant = Color(0xFF46464F),
        inverseSurface = Color(0xFFE4E1E9),
        inverseOnSurface = Color(0xFF303036),
        inversePrimary = Color(0xFF4A55C8),
        surfaceDim = Color(0xFF121318),
        surfaceBright = Color(0xFF39383F),
        surfaceContainerLowest = Color(0xFF0D0E13),
        surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25),
        surfaceContainerHigh = Color(0xFF292930),
        surfaceContainerHighest = Color(0xFF34343B),
    )

/**
 * Dark statistics palette from the approved phase-1 screen spec. The selected navbar capsule uses
 * the same accent and on-accent pair. #18181B on #FF6A1A is 6.18:1. The accent on the light
 * navigation surface is below 3:1, so inactive tabs stay on the theme colors in light mode.
 */
internal object FocusTokens {
    val screen = Color(0xFF0E0E10)
    val card = Color(0xFF1A1A1E)
    val cardPressed = Color(0xFF232328)
    val cardBorder = Color(0x14FFFFFF)
    val baseline = Color(0xFF2A2A30)
    val accent = Color(0xFFFF6A1A)
    val accentPressed = Color(0xFFE5560B)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFA1A1AA)
    val onAccent = Color(0xFF18181B)
    val streak = Color(0xFFF5B83D)
    val session = Color(0xFF9B6BFF)
    val focusRing = Color(0xFFFFB27A)
    val gridline = Color(0x1AFFFFFF)
}

internal val NavigationCapsule = FocusTokens.accent
internal val OnNavigationCapsule = FocusTokens.onAccent
