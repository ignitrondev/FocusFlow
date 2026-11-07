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
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.createRepository(): Pair<DataStoreUserPreferencesRepository, DataStore<Preferences>> {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmpFolder.newFile("test.preferences_pb") },
        )
        return DataStoreUserPreferencesRepository(dataStore) to dataStore
    }

    @Test
    fun defaultsWhenNothingStored() = runTest(UnconfinedTestDispatcher()) {
        val (repository, _) = createRepository()
        assertEquals(UserPreferences(), repository.userPreferences.first())
    }

    @Test
    fun themeModeIsPersisted() = runTest(UnconfinedTestDispatcher()) {
        val (repository, _) = createRepository()
        repository.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, repository.userPreferences.first().themeMode)
    }

    @Test
    fun dynamicColorIsPersisted() = runTest(UnconfinedTestDispatcher()) {
        val (repository, _) = createRepository()
        repository.setUseDynamicColor(true)
        assertEquals(true, repository.userPreferences.first().useDynamicColor)
    }

    @Test
    fun shareRoleIsTrimmedAndPersisted() = runTest(UnconfinedTestDispatcher()) {
        val (repository, _) = createRepository()
        repository.setShareRole("  Software Engineer ")
        assertEquals("Software Engineer", repository.userPreferences.first().shareRole)
    }

    @Test
    fun unknownThemeValueFallsBackToSystem() = runTest(UnconfinedTestDispatcher()) {
        val (repository, dataStore) = createRepository()
        dataStore.edit { it[stringPreferencesKey("theme_mode")] = "Sepia" }
        assertEquals(ThemeMode.System, repository.userPreferences.first().themeMode)
    }
}
