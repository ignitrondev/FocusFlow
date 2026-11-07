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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.feature.history.historyScreen
import com.ignitrondev.focusflow.feature.settings.settingsScreen
import com.ignitrondev.focusflow.feature.statistics.statisticsScreen
import com.ignitrondev.focusflow.feature.timer.TimerRoute
import com.ignitrondev.focusflow.feature.timer.timerScreen
import com.ignitrondev.focusflow.platform.LocalDateFormats
import org.jetbrains.compose.resources.stringResource

/** App shell: floating glass bar on phones, navigation rail or drawer on larger windows, one NavHost. */
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
        FloatingGlassNavigation(currentDestination = currentDestination, onNavigate = navController::navigateToTopLevel) {
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
private fun FloatingGlassNavigation(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        FloatingGlassNavBar(currentDestination, onNavigate)
    }
}

@Composable
private fun FloatingGlassNavBar(currentDestination: NavDestination?, onNavigate: (TopLevelDestination) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(percent = 50)
    val glass = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                scheme.surfaceContainerHigh.copy(alpha = 0.78f),
                scheme.surfaceContainer.copy(alpha = 0.90f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                scheme.surfaceContainerLowest.copy(alpha = 0.78f),
                scheme.surfaceContainerLow.copy(alpha = 0.88f),
            ),
        )
    }
    val border = if (dark) Color.White.copy(alpha = 0.20f) else scheme.outlineVariant.copy(alpha = 0.55f)
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = if (dark) 12.dp else 20.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.12f),
                    spotColor = Color.Black.copy(alpha = if (dark) 0.55f else 0.16f),
                )
                .clip(shape)
                .background(glass)
                .border(width = 1.dp, color = border, shape = shape)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                FloatingNavItem(
                    destination = destination,
                    selected = currentDestination.isInHierarchy(destination),
                    onClick = { onNavigate(destination) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.FloatingNavItem(destination: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val color = if (selected) scheme.primary else scheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag("nav_${destination.name}")
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) scheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
        CompositionLocalProvider(LocalContentColor provides color) {
            FittedLabel(stringResource(destination.labelRes))
        }
    }
}

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
