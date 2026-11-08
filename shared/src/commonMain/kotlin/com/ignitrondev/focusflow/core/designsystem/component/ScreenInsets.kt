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
package com.ignitrondev.focusflow.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The bottom of the screen covered by the floating phone navigation bar, including the system
 * navigation inset under it. Zero next to a navigation rail.
 */
val LocalFloatingBarInsets = staticCompositionLocalOf { WindowInsets(0, 0, 0, 0) }

/** Scaffold content insets: the system bars, and the floating navigation bar when there is one. */
@Composable
fun screenContentInsets(): WindowInsets = ScaffoldDefaults.contentWindowInsets.union(LocalFloatingBarInsets.current)

/**
 * These paddings without the bottom side. A scrolling screen applies this outside its scroll and the
 * bottom padding inside it, so content can pass under the floating bar yet still scroll clear of it.
 */
fun PaddingValues.withoutBottom(): PaddingValues = WithoutBottom(this)

@Immutable
private class WithoutBottom(private val padding: PaddingValues) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = padding.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = padding.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = padding.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = 0.dp

    override fun equals(other: Any?): Boolean = other is WithoutBottom && other.padding == padding

    override fun hashCode(): Int = padding.hashCode()
}
