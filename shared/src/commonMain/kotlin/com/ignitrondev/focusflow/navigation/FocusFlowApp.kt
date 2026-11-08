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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.ignitrondev.focusflow.app.AppDependencies
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowShapes
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowSpacing
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.designsystem.theme.FocusTokens
import com.ignitrondev.focusflow.core.designsystem.theme.NavigationCapsule
import com.ignitrondev.focusflow.core.designsystem.theme.OnNavigationCapsule
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.feature.history.historyScreen
import com.ignitrondev.focusflow.feature.settings.settingsScreen
import com.ignitrondev.focusflow.feature.statistics.statisticsScreen
import com.ignitrondev.focusflow.feature.timer.TimerRoute
import com.ignitrondev.focusflow.feature.timer.timerScreen
import com.ignitrondev.focusflow.platform.LocalDateFormats
import org.jetbrains.compose.resources.stringResource

/** App shell: segmented bottom bar on phones, navigation rail or drawer on larger windows, one NavHost. */
@Composable
fun FocusFlowApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass
    // A phone in landscape is wide but short: a bottom bar would take a fifth of the height, so
    // use the rail there too.
    val layoutType = if (
        !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) &&
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    ) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    }

    if (layoutType == NavigationSuiteType.NavigationBar) {
        SegmentedNavigation(currentDestination = currentDestination, onNavigate = navController::navigateToTopLevel) {
            AppContent(navController, layoutType)
        }
    } else {
        NavigationSuiteScaffold(
            layoutType = layoutType,
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination.isInHierarchy(destination)
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = null,
                            )
                        },
                        label = { FittedLabel(stringResource(destination.labelRes)) },
                        modifier = Modifier.testTag("nav_${destination.name}"),
                    )
                }
            },
        ) {
            AppContent(navController, layoutType)
        }
    }
}

@Composable
private fun AppContent(navController: NavHostController, layoutType: NavigationSuiteType) {
    // The navigation bar/rail already pads for the system bars on its side; consume those
    // insets so screen Scaffolds don't add the same padding a second time.
    val navigationSide = if (layoutType == NavigationSuiteType.NavigationBar) {
        WindowInsetsSides.Bottom
    } else {
        WindowInsetsSides.Start
    }
    Box(Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(navigationSide))) {
        NavHost(navController = navController, startDestination = TimerRoute) {
            timerScreen()
            statisticsScreen()
            historyScreen()
            settingsScreen()
        }
    }
}

@Composable
private fun SegmentedNavigation(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        SegmentedNavBar(currentDestination, onNavigate)
    }
}

@Composable
private fun SegmentedNavBar(currentDestination: NavDestination?, onNavigate: (TopLevelDestination) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    // Orange capsule in both themes. The bar and inactive tabs stay on the theme colors.
    // Inactive slots stay at least 48dp; the capsule narrows before they do.
    // 72dp container with 8dp vertical padding leaves a 56dp capsule.
    val shape = FocusFlowShapes.navContainer
    val innerHorizontal = 6.dp
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(start = FocusFlowSpacing.sm, end = FocusFlowSpacing.sm, top = FocusFlowSpacing.xs, bottom = FocusFlowSpacing.sm),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val inactiveSlots = TopLevelDestination.entries.size - 1
            val activeMaxWidth = (maxWidth - innerHorizontal * 2 - MinTabSlot * inactiveSlots).coerceAtLeast(MinTabSlot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(
                        elevation = if (dark) 8.dp else 16.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = if (dark) 0.40f else 0.10f),
                        spotColor = Color.Black.copy(alpha = if (dark) 0.50f else 0.14f),
                    )
                    .clip(shape)
                    .background(scheme.surfaceContainerHigh)
                    .testTag("nav_bar")
                    .padding(horizontal = innerHorizontal, vertical = FocusFlowSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    SegmentedNavItem(
                        destination = destination,
                        selected = currentDestination.isInHierarchy(destination),
                        activeMaxWidth = activeMaxWidth,
                        onClick = { onNavigate(destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SegmentedNavItem(destination: TopLevelDestination, selected: Boolean, activeMaxWidth: Dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val capsule = FocusFlowShapes.navCapsule
    val label = stringResource(destination.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusBorder = if (focused) 2.dp else 0.dp
    val press = ripple(color = Color.White.copy(alpha = 0.08f))
    val inactive = if (dark) FocusTokens.textSecondary else scheme.onSurfaceVariant
    if (selected) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = MinTabSlot, max = activeMaxWidth)
                .clip(capsule)
                .border(focusBorder, FocusTokens.focusRing, capsule)
                .background(NavigationCapsule)
                .selectable(
                    selected = true,
                    interactionSource = interaction,
                    indication = press,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .testTag("nav_${destination.name}")
                .padding(horizontal = FocusFlowSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = destination.selectedIcon,
                contentDescription = null,
                tint = OnNavigationCapsule,
            )
            Spacer(Modifier.width(6.dp))
            CompositionLocalProvider(
                LocalContentColor provides OnNavigationCapsule,
                LocalTextStyle provides MaterialTheme.typography.labelLarge,
            ) {
                FittedLabel(label, minFontSize = 4.sp)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .widthIn(min = MinTabSlot)
                .clip(capsule)
                .border(focusBorder, FocusTokens.focusRing, capsule)
                .selectable(
                    selected = false,
                    interactionSource = interaction,
                    indication = press,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .testTag("nav_${destination.name}")
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = destination.unselectedIcon,
                contentDescription = null,
                tint = inactive,
            )
            CompositionLocalProvider(
                LocalContentColor provides inactive,
                LocalTextStyle provides MaterialTheme.typography.labelMedium,
            ) {
                // 4sp floor: a 48dp slot still fits the whole word at 200% font size.
                FittedLabel(label, minFontSize = 4.sp)
            }
        }
    }
}

private val MinTabSlot = 48.dp

private fun NavDestination?.isInHierarchy(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        // Keep one copy of each tab on the back stack and remember its state.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Entry point of the shared UI, used by the Android activity and the iOS view controller: provides
 * the app's dependencies and the theme, then shows [FocusFlowApp].
 */
@Composable
fun FocusFlowRoot(dependencies: AppDependencies, preferences: UserPreferences, darkTheme: Boolean) {
    val dynamicScheme = if (preferences.useDynamicColor) dependencies.platform.dynamicColorScheme(darkTheme) else null
    CompositionLocalProvider(
        LocalAppDependencies provides dependencies,
        LocalDateFormats provides dependencies.platform.dates,
    ) {
        FocusFlowTheme(darkTheme = darkTheme, dynamicScheme = dynamicScheme) {
            FocusFlowApp()
        }
    }
}
