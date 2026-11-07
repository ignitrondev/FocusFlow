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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ignitrondev.focusflow.BuildConfig
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowTheme
import com.ignitrondev.focusflow.core.designsystem.theme.supportsDynamicColor
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.platform.AlertCapabilities
import com.ignitrondev.focusflow.platform.alertCapabilities
import com.ignitrondev.focusflow.platform.openAlertSoundSettings
import com.ignitrondev.focusflow.platform.openExactAlarmSettings
import com.ignitrondev.focusflow.platform.openNotificationSettings
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

private const val SOURCE_CODE_URL = "https://github.com/ignitrondev/FocusFlow"

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        // Permissions change in Android settings, outside the app: re-read them on every return.
        var alerts by remember { mutableStateOf(context.alertCapabilities()) }
        LifecycleResumeEffect(Unit) {
            alerts = context.alertCapabilities()
            onPauseOrDispose {}
        }
        SettingsScreen(
            uiState = uiState,
            alerts = alerts,
            onThemeModeChange = viewModel::setThemeMode,
            onDynamicColorChange = viewModel::setUseDynamicColor,
            onOpenNotificationSettings = context::openNotificationSettings,
            onOpenAlertSoundSettings = context::openAlertSoundSettings,
            onOpenExactAlarmSettings = context::openExactAlarmSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    alerts: AlertCapabilities,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAlertSoundSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showDynamicColorOption: Boolean = supportsDynamicColor(),
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                when (uiState) {
                    SettingsUiState.Loading -> Unit

                    is SettingsUiState.Success -> AppearanceSection(
                        preferences = uiState.preferences,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        showDynamicColorOption = showDynamicColorOption,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NotificationsSection(alerts, onOpenNotificationSettings, onOpenAlertSoundSettings, onOpenExactAlarmSettings)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                AboutSection()
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    showDynamicColorOption: Boolean,
) {
    SectionHeader(stringResource(R.string.settings_section_appearance))
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
            headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
            supportingContent = { Text(stringResource(R.string.settings_dynamic_color_summary)) },
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
private fun NotificationsSection(
    alerts: AlertCapabilities,
    onOpenNotificationSettings: () -> Unit,
    onOpenAlertSoundSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    SectionHeader(stringResource(R.string.settings_section_notifications))
    ClickableItem(
        title = stringResource(R.string.settings_notifications),
        summary = stringResource(
            if (alerts.notificationsAllowed) R.string.settings_notifications_on else R.string.settings_notifications_off,
        ),
        onClick = onOpenNotificationSettings,
    )
    if (alerts.notificationsAllowed) {
        ClickableItem(
            title = stringResource(R.string.settings_alert_sound),
            summary = stringResource(R.string.settings_alert_sound_summary),
            onClick = onOpenAlertSoundSettings,
        )
    }
    alerts.exactAlarmsAllowed?.let { allowed ->
        ClickableItem(
            title = stringResource(R.string.settings_exact_alarms),
            summary = stringResource(if (allowed) R.string.settings_exact_alarms_on else R.string.settings_exact_alarms_off),
            onClick = onOpenExactAlarmSettings,
        )
    }
}

@Composable
private fun ClickableItem(title: String, summary: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = Modifier.clickable(onClick = onClick, role = Role.Button),
    )
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    SectionHeader(stringResource(R.string.settings_section_about))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_version)) },
        supportingContent = { Text(BuildConfig.VERSION_NAME) },
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_source_code)) },
        supportingContent = { Text(stringResource(R.string.settings_source_code_summary)) },
        modifier = Modifier.selectable(selected = false, onClick = { uriHandler.openUri(SOURCE_CODE_URL) }, role = Role.Button),
    )
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_license)) },
        supportingContent = { Text(stringResource(R.string.settings_license_summary)) },
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

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    FocusFlowTheme {
        SettingsScreen(
            uiState = SettingsUiState.Success(UserPreferences()),
            alerts = AlertCapabilities(notificationsAllowed = true, exactAlarmsAllowed = false),
            onThemeModeChange = {},
            onDynamicColorChange = {},
            onOpenNotificationSettings = {},
            onOpenAlertSoundSettings = {},
            onOpenExactAlarmSettings = {},
            showDynamicColorOption = true,
        )
    }
}
