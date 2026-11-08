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
package com.ignitrondev.focusflow.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowShapes
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import org.jetbrains.compose.resources.stringResource

/**
 * Phone navbar: a floating Liquid Glass pill over the screen. Every tab is an icon above its label.
 * The selected tab takes the theme's primary color, which follows the wallpaper (Material You) when
 * wallpaper colors are on, and its filled icon. Screens scroll underneath the bar: the glass
 * refracts them on Android 13+ and iOS, blurs them on Android 12, and older Android shows a
 * translucent surface. Wide-screen rails do not use this bar.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun FloatingNavBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val animationsDisabled = LocalAppDependencies.current.platform.rememberSystemAnimationsDisabled()
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    val selected = currentDestination.selectedTopLevel()
    // The tint keeps labels readable over busy content; the glass still shows what is underneath.
    val glass = remember(surface, dark) {
        GlassStyle.regular.then {
            shape(FocusFlowShapes.navContainer)
            tint(surface.copy(alpha = if (dark) DARK_TINT_ALPHA else LIGHT_TINT_ALPHA))
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(start = BarSideMargin, end = BarSideMargin, bottom = BarBottomMargin),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .widthIn(max = BarMaxWidth)
                .fillMaxWidth()
                .height(BarHeight)
                .hazeGlass(input = HazeInput.Sources(hazeState), style = glass)
                .testTag("nav_bar")
                .padding(BarInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                GlassNavItem(
                    destination = destination,
                    selected = destination == selected,
                    animationsDisabled = animationsDisabled,
                    onClick = { onNavigate(destination) },
                )
            }
        }
    }
}

/**
 * What [FloatingNavBar] covers at the bottom of the window: the system navigation inset, the bar,
 * its margin, and a small gap so the last row of a screen does not touch the glass.
 */
@Composable
internal fun rememberFloatingBarInsets(): WindowInsets {
    val system = WindowInsets.safeDrawing
    return remember(system) {
        system.only(WindowInsetsSides.Bottom).add(WindowInsets(bottom = BarHeight + BarBottomMargin + ContentGap))
    }
}

@Composable
private fun RowScope.GlassNavItem(destination: TopLevelDestination, selected: Boolean, animationsDisabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(destination.labelRes)
    val shape = FocusFlowShapes.navItem
    // Reduce Motion and Android's "Remove animations" switch tabs without the fade.
    val colorSpec: AnimationSpec<Color> = if (animationsDisabled) snap() else tween(TAB_SWITCH_MILLIS, easing = TabSwitchEasing)
    val iconSpec: FiniteAnimationSpec<Float> = if (animationsDisabled) snap() else tween(TAB_SWITCH_MILLIS, easing = TabSwitchEasing)
    val color by animateColorAsState(if (selected) scheme.primary else scheme.onSurface, colorSpec, label = "navTabColor")
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .widthIn(min = MinTabSlot)
            .clip(shape)
            // Only while focused: a 0dp border is still drawn, as a hairline.
            .then(if (focused) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = ripple(color = scheme.primary),
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag("nav_${destination.name}")
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Crossfade(targetState = selected, animationSpec = iconSpec, label = "navTabIcon") { filled ->
            Icon(
                imageVector = if (filled) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(IconSize),
            )
        }
        Spacer(Modifier.height(LabelGap))
        CompositionLocalProvider(
            LocalContentColor provides color,
            LocalTextStyle provides MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        ) {
            // 4sp floor: a 48dp slot still fits the whole word at 200% font size.
            FittedLabel(label, minFontSize = 4.sp)
        }
    }
}

private fun NavDestination?.selectedTopLevel(): TopLevelDestination? = TopLevelDestination.entries.firstOrNull { isInHierarchy(it) }

private val MinTabSlot = 48.dp
private val BarHeight = 64.dp
private val BarMaxWidth = 560.dp
private val BarSideMargin = 16.dp
private val BarBottomMargin = 8.dp
private val BarInnerPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
private val ContentGap = 8.dp
private val IconSize = 24.dp
private val LabelGap = 2.dp
private const val LIGHT_TINT_ALPHA = 0.5f
private const val DARK_TINT_ALPHA = 0.4f
private const val TAB_SWITCH_MILLIS = 200
private val TabSwitchEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
