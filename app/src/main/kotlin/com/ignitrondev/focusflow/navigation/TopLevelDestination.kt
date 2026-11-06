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

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.feature.history.HistoryRoute
import com.ignitrondev.focusflow.feature.settings.SettingsRoute
import com.ignitrondev.focusflow.feature.statistics.StatisticsRoute
import com.ignitrondev.focusflow.feature.timer.TimerRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Timer(TimerRoute, TimerRoute::class, R.string.nav_timer, Icons.Rounded.Timer, Icons.Outlined.Timer),
    Statistics(StatisticsRoute, StatisticsRoute::class, R.string.nav_statistics, Icons.Rounded.Insights, Icons.Outlined.Insights),
    History(HistoryRoute, HistoryRoute::class, R.string.nav_history, Icons.Rounded.History, Icons.Outlined.History),
    Settings(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
}
