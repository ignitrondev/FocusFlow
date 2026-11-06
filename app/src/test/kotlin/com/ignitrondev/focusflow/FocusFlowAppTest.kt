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
package com.ignitrondev.focusflow

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class FocusFlowAppTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var preferences: FakeUserPreferencesRepository

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun startsOnTimerWithDefaultFocusSession() {
        composeRule.onNodeWithTag("nav_Timer").assertIsSelected()
        composeRule.onNodeWithText("25:00").assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.timer_action_start)).assertExists()
    }

    @Test
    fun selectingShortBreakShowsItsDuration() {
        composeRule.onNodeWithText(string(R.string.timer_mode_short_break)).performClick()
        composeRule.onNodeWithText("05:00").assertExists()
    }

    @Test
    fun navigatesBetweenTopLevelDestinations() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.onNodeWithText(string(R.string.statistics_empty_title)).assertExists()

        composeRule.onNodeWithTag("nav_History").performClick()
        composeRule.onNodeWithText(string(R.string.history_empty_title)).assertExists()

        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(R.string.settings_section_appearance)).assertExists()

        composeRule.onNodeWithTag("nav_Timer").performClick()
        composeRule.onNodeWithTag("timerDial").assertExists()
    }

    @Test
    fun choosingDarkThemeInSettingsPersistsIt() {
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).performClick()
        composeRule.waitForIdle()

        assertEquals(ThemeMode.Dark, preferences.userPreferences.value.themeMode)
        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).assertIsSelected()
    }
}
