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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
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
import com.ignitrondev.focusflow.core.designsystem.component.LocalFloatingBarInsets
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.feature.history.historyScreen
import com.ignitrondev.focusflow.feature.settings.settingsScreen
import com.ignitrondev.focusflow.feature.statistics.statisticsScreen
import com.ignitrondev.focusflow.feature.timer.TimerRoute
import com.ignitrondev.focusflow.feature.timer.timerScreen
import com.ignitrondev.focusflow.platform.LocalDateFormats
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource

/** App shell: floating glass bottom bar on phones, navigation rail or drawer on larger windows, one NavHost. */
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
        FloatingNavigation(currentDestination = currentDestination, onNavigate = navController::navigateToTopLevel) {
            AppContent(navController)
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
            // The rail already pads for the system bars on its side; consume those insets so
            // screen Scaffolds don't add the same padding a second time.
            Box(Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))) {
                AppContent(navController)
            }
        }
    }
}

@Composable
private fun AppContent(navController: NavHostController) {
    NavHost(navController = navController, startDestination = TimerRoute) {
        timerScreen()
        statisticsScreen()
        historyScreen()
        settingsScreen()
    }
}

/**
 * Screens fill the window and the bar floats over their bottom edge. The screens are the glass's
 * source, and [LocalFloatingBarInsets] tells their Scaffolds how much of the bottom the bar covers,
 * so scrolling content passes under the glass and still ends clear of it.
 */
@Composable
private fun FloatingNavigation(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    val hazeState = rememberHazeState()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            CompositionLocalProvider(LocalFloatingBarInsets provides rememberFloatingBarInsets()) { content() }
        }
        FloatingNavBar(currentDestination, onNavigate, hazeState, Modifier.align(Alignment.BottomCenter))
    }
}

internal fun NavDestination?.isInHierarchy(destination: TopLevelDestination): Boolean =
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
