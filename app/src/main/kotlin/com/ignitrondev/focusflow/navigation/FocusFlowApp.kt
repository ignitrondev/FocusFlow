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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.feature.history.historyScreen
import com.ignitrondev.focusflow.feature.settings.settingsScreen
import com.ignitrondev.focusflow.feature.statistics.statisticsScreen
import com.ignitrondev.focusflow.feature.timer.TimerRoute
import com.ignitrondev.focusflow.feature.timer.timerScreen

/** App shell: bottom bar on phones, navigation rail on tablets / landscape, one NavHost. */
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
