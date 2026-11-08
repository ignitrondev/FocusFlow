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
package com.ignitrondev.focusflow.feature.statistics

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.share_card_empty
import com.ignitrondev.focusflow.resources.share_card_focus_time
import com.ignitrondev.focusflow.resources.week_range_same_month
import com.ignitrondev.focusflow.testing.FakeTimeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Sunday 8 Nov 2026, with the format locale starting weeks on Monday and the display language
 * left on US English. The week label and an empty card are what a user sees on the share sheet.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ShareCardFixesTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var timerRepository: TimerRepository

    @Inject lateinit var clock: FakeTimeSource

    private val displayLocale = Locale.getDefault(Locale.Category.DISPLAY)
    private val formatLocale = Locale.getDefault(Locale.Category.FORMAT)

    private fun string(res: StringResource) = runBlocking { getString(res) }

    @Before
    fun setUp() {
        hiltRule.inject()
        Locale.setDefault(Locale.Category.DISPLAY, Locale.US)
        Locale.setDefault(Locale.Category.FORMAT, Locale.UK)
        // 2026-11-08T09:00:00Z, the Sunday after [FakeTimeSource]'s Saturday anchor.
        clock.epochMillis = FakeTimeSource.START_EPOCH_MILLIS + 24L * 60L * 60L * 1000L
    }

    @After
    fun restoreLocales() {
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale)
        Locale.setDefault(Locale.Category.FORMAT, formatLocale)
    }

    @Test
    fun zeroFocusKeepsShareAndReplacesTheCardWithTheFirstSessionPrompt() {
        val prompt = string(Res.string.share_card_empty)
        assertEquals("Start your first session", prompt)

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareStatistics"), 5_000)
        composeRule.onNodeWithTag("shareStatistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareCardEmpty"), 5_000)

        composeRule.onNodeWithTag("shareCardEmpty").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(prompt).assertIsDisplayed()
        composeRule.onNodeWithText(string(Res.string.share_card_focus_time).uppercase()).assertDoesNotExist()
        composeRule.onNodeWithText(weekLabel(), substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("shareButton").assertIsNotEnabled()
        composeRule.onNodeWithTag("shareRole").performTextInput("Software Engineer")
        composeRule.onNodeWithTag("shareButton").assertIsEnabled()
    }

    @Test
    fun mondayWeekOnSunday8Nov2026LabelsTheCard2Through8Nov() {
        runBlocking {
            timerRepository.start()
            clock.advanceBy(25 * 60_000L)
            timerRepository.refresh()
        }

        val label = weekLabel()
        assertEquals("2 – 8 Nov", label)

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareStatistics"), 5_000)
        // The statistics chart and the share card both print the one week range.
        composeRule.onNodeWithText(label, substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("shareStatistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareCard"), 5_000)

        assertTrue(composeRule.onAllNodesWithText(label, substring = true).fetchSemanticsNodes().size >= 2)
        composeRule.onNodeWithTag("shareCardEmpty").assertDoesNotExist()
        assertTrue(composeRule.onAllNodesWithText("25 min", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    private fun weekLabel() = runBlocking { getString(Res.string.week_range_same_month, 2, 8, "Nov") }
}
