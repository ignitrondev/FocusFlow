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

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.testing.FakeTimeSource
import com.ignitrondev.focusflow.testing.FakeTimerConfigRepository
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
// A typical phone screen. Robolectric's default (320x470 dp) is so small that the timer controls
// sit below the fold, where clicks land outside the window.
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class FocusFlowAppTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var preferences: FakeUserPreferencesRepository

    @Inject
    lateinit var clock: FakeTimeSource

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var timerConfig: FakeTimerConfigRepository

    private fun string(id: Int) = composeRule.activity.getString(id)

    // Timer actions are written to Room on a background thread; wait for the UI to reflect them.
    private fun waitForText(text: String) = composeRule.waitUntilAtLeastOneExists(hasText(text), TIMEOUT_MILLIS)

    private fun waitForButton(labelId: Int) = composeRule.waitUntilAtLeastOneExists(hasContentDescription(string(labelId)), TIMEOUT_MILLIS)

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
        waitForText("05:00")
    }

    @Test
    fun startPauseResumeAndReset() {
        composeRule.onNodeWithContentDescription(string(R.string.timer_action_start)).performClick()
        waitForButton(R.string.timer_action_pause)

        composeRule.onNodeWithContentDescription(string(R.string.timer_action_pause)).performClick()
        waitForButton(R.string.timer_action_resume)

        composeRule.onNodeWithContentDescription(string(R.string.timer_action_resume)).performClick()
        waitForButton(R.string.timer_action_pause)

        composeRule.onNodeWithContentDescription(string(R.string.timer_action_reset)).performClick()
        waitForButton(R.string.timer_action_start)
        waitForText("25:00")
    }

    @Test
    fun countdownFollowsTheClock() {
        composeRule.onNodeWithContentDescription(string(R.string.timer_action_start)).performClick()
        waitForButton(R.string.timer_action_pause)

        clock.advanceBy(10 * 60_000L)
        // Let the screen's once-per-second refresh run.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))

        waitForText("15:00")
    }

    @Test
    fun sessionTypeIsLockedWhileASessionIsInProgress() {
        val shortBreak = composeRule.onNodeWithText(string(R.string.timer_mode_short_break))
        shortBreak.assertIsEnabled()

        composeRule.onNodeWithContentDescription(string(R.string.timer_action_start)).performClick()
        waitForButton(R.string.timer_action_pause)

        shortBreak.assertIsNotEnabled()
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

    @Test
    fun completedSessionAppearsInHistoryAndStatistics() {
        runBlocking {
            timerRepository.start()
            clock.advanceBy(25 * 60_000L)
            timerRepository.refresh()
        }

        composeRule.onNodeWithTag("nav_History").performClick()
        waitForText(string(R.string.history_today))
        // Rows expose one combined description to TalkBack, so their texts are in the unmerged tree.
        composeRule.onNodeWithText("25 min", useUnmergedTree = true).assertExists()

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        waitForText(string(R.string.statistics_last_seven_days))
        composeRule.onAllNodesWithText("25 min").onFirst().assertExists()
    }

    @Test
    fun focusDurationFromSettingsIsUsedByTheTimer() {
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithTag("setting_focus").performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        composeRule.waitUntil(TIMEOUT_MILLIS) { timerConfig.config.value.focusMillis == 30 * 60_000L }

        composeRule.onNodeWithTag("nav_Timer").performClick()
        waitForText("30:00")
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
