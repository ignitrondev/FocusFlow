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
package com.ignitrondev.focusflow.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import com.ignitrondev.focusflow.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

class DataStoreUserPreferencesRepository(private val dataStore: DataStore<Preferences>) : UserPreferencesRepository {
    override val userPreferences: Flow<UserPreferences> =
        dataStore.data
            .catch { error ->
                // A corrupt or unreadable file must not crash the app: fall back to defaults.
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                UserPreferences(
                    themeMode = preferences[Keys.THEME_MODE].toThemeMode(),
                    useDynamicColor = preferences[Keys.USE_DYNAMIC_COLOR] ?: false,
                    shareRole = preferences[Keys.SHARE_ROLE].orEmpty(),
                )
            }.distinctUntilChanged()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }
    }

    override suspend fun setShareRole(role: String) {
        dataStore.edit { it[Keys.SHARE_ROLE] = role.trim().take(MAX_ROLE_LENGTH) }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SHARE_ROLE = stringPreferencesKey("share_role")
    }

    private companion object {
        const val MAX_ROLE_LENGTH = 40
    }
}

/** Unknown or missing values (e.g. written by a future version) fall back to the default. */
internal fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.System
