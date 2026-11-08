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
package com.ignitrondev.focusflow.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.screenContentInsets
import com.ignitrondev.focusflow.core.designsystem.component.withoutBottom
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.domain.timer.TimerConfig
import com.ignitrondev.focusflow.platform.AlertSettings
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.duration_minutes
import com.ignitrondev.focusflow.resources.focus_sessions_count
import com.ignitrondev.focusflow.resources.settings_alert_more
import com.ignitrondev.focusflow.resources.settings_alert_more_summary
import com.ignitrondev.focusflow.resources.settings_alert_sound
import com.ignitrondev.focusflow.resources.settings_auto_start_breaks
import com.ignitrondev.focusflow.resources.settings_auto_start_focus
import com.ignitrondev.focusflow.resources.settings_check_updates
import com.ignitrondev.focusflow.resources.settings_check_updates_summary
import com.ignitrondev.focusflow.resources.settings_dynamic_color
import com.ignitrondev.focusflow.resources.settings_dynamic_color_summary
import com.ignitrondev.focusflow.resources.settings_exact_alarms
import com.ignitrondev.focusflow.resources.settings_exact_alarms_off
import com.ignitrondev.focusflow.resources.settings_exact_alarms_on
import com.ignitrondev.focusflow.resources.settings_focus_duration
import com.ignitrondev.focusflow.resources.settings_license
import com.ignitrondev.focusflow.resources.settings_license_summary
import com.ignitrondev.focusflow.resources.settings_long_break_duration
import com.ignitrondev.focusflow.resources.settings_long_break_interval
import com.ignitrondev.focusflow.resources.settings_made_by
import com.ignitrondev.focusflow.resources.settings_made_by_summary
import com.ignitrondev.focusflow.resources.settings_notifications
import com.ignitrondev.focusflow.resources.settings_notifications_off
import com.ignitrondev.focusflow.resources.settings_notifications_on
import com.ignitrondev.focusflow.resources.settings_section_about
import com.ignitrondev.focusflow.resources.settings_section_appearance
import com.ignitrondev.focusflow.resources.settings_section_notifications
import com.ignitrondev.focusflow.resources.settings_section_timer
import com.ignitrondev.focusflow.resources.settings_short_break_duration
import com.ignitrondev.focusflow.resources.settings_source_code
import com.ignitrondev.focusflow.resources.settings_source_code_summary
import com.ignitrondev.focusflow.resources.settings_theme_dark
import com.ignitrondev.focusflow.resources.settings_theme_light
import com.ignitrondev.focusflow.resources.settings_theme_system
import com.ignitrondev.focusflow.resources.settings_timer_note
import com.ignitrondev.focusflow.resources.settings_title
import com.ignitrondev.focusflow.resources.settings_version
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Serializable
data object SettingsRoute

private const val SOURCE_CODE_URL = "https://github.com/ignitrondev/FocusFlow"

// FocusFlow has no internet permission: the browser shows the newest release and its downloads.
private const val LATEST_RELEASE_URL = "https://github.com/ignitrondev/FocusFlow/releases/latest"

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute> {
        val dependencies = LocalAppDependencies.current
        val viewModel = viewModel {
            SettingsViewModel(
                userPreferencesRepository = dependencies.userPreferencesRepository,
                timerConfigRepository = dependencies.timerConfigRepository,
                timerRepository = dependencies.timerRepository,
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsScreen(
            uiState = uiState,
            alerts = dependencies.platform.rememberAlertSettings(),
            appVersion = dependencies.platform.appVersion,
            showDynamicColorOption = dependencies.platform.supportsDynamicColor,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setUseDynamicColor,
            onTimerConfigChange = viewModel::updateTimerConfig,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    alerts: AlertSettings,
    appVersion: String,
    showDynamicColorOption: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onTimerConfigChange: ((TimerConfig) -> TimerConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = screenContentInsets(),
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.withoutBottom())
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                when (uiState) {
                    SettingsUiState.Loading -> Unit

                    is SettingsUiState.Success -> {
                        TimerSection(uiState.timerConfig, onTimerConfigChange)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        AppearanceSection(
                            preferences = uiState.preferences,
                            onThemeModeChange = onThemeModeChange,
                            onDynamicColorChange = onDynamicColorChange,
                            showDynamicColorOption = showDynamicColorOption,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NotificationsSection(alerts)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AboutSection(appVersion)
            }
        }
    }
}

@Composable
private fun TimerSection(config: TimerConfig, onChange: ((TimerConfig) -> TimerConfig) -> Unit) {
    SectionHeader(stringResource(Res.string.settings_section_timer))
    val minutesLabel: @Composable (Int) -> String = { pluralStringResource(Res.plurals.duration_minutes, it, it) }
    SliderSetting(
        title = stringResource(Res.string.settings_focus_duration),
        value = (config.focusMillis / TimerConfig.MINUTE).toInt(),
        range = TimerConfig.FOCUS_MINUTES,
        step = 5,
        valueLabel = minutesLabel,
        testTag = "setting_focus",
        onValueChange = { minutes -> onChange { it.copy(focusMillis = minutes * TimerConfig.MINUTE) } },
    )
    SliderSetting(
        title = stringResource(Res.string.settings_short_break_duration),
        value = (config.shortBreakMillis / TimerConfig.MINUTE).toInt(),
        range = TimerConfig.SHORT_BREAK_MINUTES,
        step = 1,
        valueLabel = minutesLabel,
        testTag = "setting_short_break",
        onValueChange = { minutes -> onChange { it.copy(shortBreakMillis = minutes * TimerConfig.MINUTE) } },
    )
    SliderSetting(
        title = stringResource(Res.string.settings_long_break_duration),
        value = (config.longBreakMillis / TimerConfig.MINUTE).toInt(),
        range = TimerConfig.LONG_BREAK_MINUTES,
        step = 5,
        valueLabel = minutesLabel,
        testTag = "setting_long_break",
        onValueChange = { minutes -> onChange { it.copy(longBreakMillis = minutes * TimerConfig.MINUTE) } },
    )
    SliderSetting(
        title = stringResource(Res.string.settings_long_break_interval),
        value = config.longBreakInterval,
        range = TimerConfig.LONG_BREAK_INTERVAL,
        step = 1,
        valueLabel = { pluralStringResource(Res.plurals.focus_sessions_count, it, it) },
        testTag = "setting_interval",
        onValueChange = { sessions -> onChange { it.copy(longBreakInterval = sessions) } },
    )
    SwitchSetting(
        title = stringResource(Res.string.settings_auto_start_breaks),
        checked = config.autoStartBreaks,
        onCheckedChange = { checked -> onChange { it.copy(autoStartBreaks = checked) } },
    )
    SwitchSetting(
        title = stringResource(Res.string.settings_auto_start_focus),
        checked = config.autoStartFocus,
        onCheckedChange = { checked -> onChange { it.copy(autoStartFocus = checked) } },
    )
    Text(
        text = stringResource(Res.string.settings_timer_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * A setting picked on a slider. The value is only saved when the user lets go, so dragging does
 * not write to disk on every step.
 */
@Composable
private fun SliderSetting(
    title: String,
    value: Int,
    range: IntRange,
    step: Int,
    valueLabel: @Composable (Int) -> String,
    testTag: String,
    onValueChange: (Int) -> Unit,
) {
    var dragged by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val shown = dragged.roundToInt()
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueLabel(shown), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        val label = valueLabel(shown)
        Slider(
            value = dragged,
            onValueChange = { dragged = it },
            onValueChangeFinished = { if (dragged.roundToInt() != value) onValueChange(dragged.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            // Still snaps to every step, without a row of dots that would be noise at 30 steps.
            colors = SliderDefaults.colors(activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
            modifier = Modifier
                .testTag(testTag)
                .semantics {
                    contentDescription = title
                    stateDescription = label
                },
        )
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    )
}

@Composable
private fun AppearanceSection(
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    showDynamicColorOption: Boolean,
) {
    SectionHeader(stringResource(Res.string.settings_section_appearance))
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            val selected = preferences.themeMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = selected, onClick = { onThemeModeChange(mode) }, role = Role.RadioButton)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(mode.labelRes()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (showDynamicColorOption) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.settings_dynamic_color)) },
            supportingContent = { Text(stringResource(Res.string.settings_dynamic_color_summary)) },
            trailingContent = { Switch(checked = preferences.useDynamicColor, onCheckedChange = null) },
            modifier = Modifier.toggleable(
                value = preferences.useDynamicColor,
                onValueChange = onDynamicColorChange,
                role = Role.Switch,
            ),
        )
    }
}

@Composable
private fun NotificationsSection(alerts: AlertSettings) {
    SectionHeader(stringResource(Res.string.settings_section_notifications))
    ClickableItem(
        title = stringResource(Res.string.settings_notifications),
        summary = stringResource(
            if (alerts.notificationsAllowed) Res.string.settings_notifications_on else Res.string.settings_notifications_off,
        ),
        onClick = alerts.openNotificationSettings,
    )
    alerts.pickAlertSound?.let { pick ->
        ClickableItem(
            title = stringResource(Res.string.settings_alert_sound),
            summary = alerts.alertSoundTitle,
            onClick = pick,
            modifier = Modifier.testTag("setting_alert_sound"),
        )
    }
    val openAlertOptions = alerts.openAlertOptions
    if (alerts.notificationsAllowed && openAlertOptions != null) {
        ClickableItem(
            title = stringResource(Res.string.settings_alert_more),
            summary = stringResource(Res.string.settings_alert_more_summary),
            onClick = openAlertOptions,
        )
    }
    alerts.exactAlarmsAllowed?.let { allowed ->
        ClickableItem(
            title = stringResource(Res.string.settings_exact_alarms),
            summary = stringResource(if (allowed) Res.string.settings_exact_alarms_on else Res.string.settings_exact_alarms_off),
            onClick = alerts.openExactAlarmSettings,
        )
    }
}

@Composable
private fun ClickableItem(title: String, summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = modifier.clickable(onClick = onClick, role = Role.Button),
    )
}

@Composable
private fun AboutSection(appVersion: String) {
    val uriHandler = LocalUriHandler.current
    SectionHeader(stringResource(Res.string.settings_section_about))
    ListItem(
        headlineContent = { Text(stringResource(Res.string.settings_version)) },
        supportingContent = { Text(appVersion) },
    )
    ListItem(
        headlineContent = { Text(stringResource(Res.string.settings_check_updates)) },
        supportingContent = { Text(stringResource(Res.string.settings_check_updates_summary)) },
        modifier = Modifier
            .selectable(selected = false, onClick = { uriHandler.openUri(LATEST_RELEASE_URL) }, role = Role.Button)
            .testTag("setting_check_updates"),
    )
    ListItem(
        headlineContent = { Text(stringResource(Res.string.settings_source_code)) },
        supportingContent = { Text(stringResource(Res.string.settings_source_code_summary)) },
        modifier = Modifier.selectable(selected = false, onClick = { uriHandler.openUri(SOURCE_CODE_URL) }, role = Role.Button),
    )
    ListItem(
        headlineContent = { Text(stringResource(Res.string.settings_made_by)) },
        supportingContent = { Text(stringResource(Res.string.settings_made_by_summary)) },
    )
    ListItem(
        headlineContent = { Text(stringResource(Res.string.settings_license)) },
        supportingContent = { Text(stringResource(Res.string.settings_license_summary)) },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

private fun ThemeMode.labelRes(): StringResource = when (this) {
    ThemeMode.System -> Res.string.settings_theme_system
    ThemeMode.Light -> Res.string.settings_theme_light
    ThemeMode.Dark -> Res.string.settings_theme_dark
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    FocusFlowTheme {
        SettingsScreen(
            uiState = SettingsUiState.Success(UserPreferences()),
            alerts = AlertSettings(
                notificationsAllowed = true,
                exactAlarmsAllowed = false,
                alertSoundTitle = "Default (Chime)",
                openNotificationSettings = {},
                pickAlertSound = {},
                openAlertOptions = {},
                openExactAlarmSettings = {},
            ),
            appVersion = "1.0.0",
            showDynamicColorOption = true,
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onTimerConfigChange = {},
        )
    }
}
