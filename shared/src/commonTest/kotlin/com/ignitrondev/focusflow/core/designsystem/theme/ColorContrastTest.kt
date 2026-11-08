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

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG 2.1 contrast for the text/background pairs FocusFlow actually uses, in both themes:
 * 4.5:1 for body text, 3:1 for large text and UI components. (Dynamic color schemes come from the
 * system and are not checked here.)
 */
class ColorContrastTest {
    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.luminance() + 0.05
        val b = background.luminance() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun ColorScheme.pairs(): List<Triple<String, Pair<Color, Color>, Double>> = listOf(
        Triple("onSurface on surface", onSurface to surface, 4.5),
        Triple("onSurfaceVariant on surface (secondary text)", onSurfaceVariant to surface, 4.5),
        Triple("onSurfaceVariant on surfaceContainer (cards)", onSurfaceVariant to surfaceContainer, 4.5),
        Triple("primary on surface (section headers, focus label)", primary to surface, 4.5),
        Triple("tertiary on surface (break label)", tertiary to surface, 4.5),
        Triple("onPrimary on primary (start button)", onPrimary to primary, 4.5),
        Triple("onSurface on surface (inactive tab on the glass navigation bar)", onSurface to surface, 4.5),
        Triple("primary on surfaceContainerHigh (selected tab over a busy screen)", primary to surfaceContainerHigh, 4.5),
        Triple("onSecondaryContainer on secondaryContainer (selected segment)", onSecondaryContainer to secondaryContainer, 4.5),
        Triple("primary on surfaceContainerHighest (dial arc vs track)", primary to surfaceContainerHighest, 3.0),
        Triple("tertiary on surfaceContainer (today's bar)", tertiary to surfaceContainer, 3.0),
        Triple("onSurface on surfaceContainer (statistics values)", onSurface to surfaceContainer, 4.5),
        Triple("primary on surfaceContainer (statistics icons and bars)", primary to surfaceContainer, 3.0),
        Triple("secondary on surfaceContainer (session icon)", secondary to surfaceContainer, 3.0),
        Triple("onSecondaryContainer on secondaryContainer (share and back buttons)", onSecondaryContainer to secondaryContainer, 4.5),
    )

    private fun assertContrast(name: String, scheme: ColorScheme) {
        val failures = scheme.pairs().mapNotNull { (label, colors, minimum) ->
            val ratio = contrast(colors.first, colors.second)
            if (ratio < minimum) "$label: ${(ratio * 100).roundToInt() / 100.0} < $minimum" else null
        }
        assertTrue(failures.isEmpty(), "$name theme:\n" + failures.joinToString("\n"))
    }

    @Test
    fun lightThemeMeetsWcag() = assertContrast("Light", LightColors)

    @Test
    fun darkThemeMeetsWcag() = assertContrast("Dark", DarkColors)
}
