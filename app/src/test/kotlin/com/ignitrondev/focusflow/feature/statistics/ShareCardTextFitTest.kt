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

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.data.database.FocusSessionEntity
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.testing.FakeTimeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A busy week on the share card: "9 h 35 min" of focus, a "2 h 23 min" daily average and a
 * streak, side by side. Every label and value must stay whole on one line. The focus time used
 * to lose its last word ("9 h 35") and the third column broke its label mid-word ("STRE / AK").
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ShareCardTextFitTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var clock: FakeTimeSource

    @Inject lateinit var sessions: FocusSessionDao

    private val displayLocale = Locale.getDefault(Locale.Category.DISPLAY)
    private val formatLocale = Locale.getDefault(Locale.Category.FORMAT)

    @Before
    fun setUp() {
        hiltRule.inject()
        Locale.setDefault(Locale.Category.DISPLAY, Locale.US)
        Locale.setDefault(Locale.Category.FORMAT, Locale.UK)
        clock.epochMillis = NOW
        // Monday to Thursday: 6, 5, 8 and 4 sessions of 25 minutes = 23 sessions, 575 minutes.
        runBlocking {
            val today = NOW - NOW % DAY
            listOf(4, 8, 5, 6).forEachIndexed { daysAgo, count ->
                repeat(count) { i ->
                    val start = today - daysAgo * DAY + (8 * 60 + i * 30) * MINUTE
                    sessions.insert(
                        FocusSessionEntity("d$daysAgo-$i", SessionType.Focus.name, start, start + 25 * MINUTE, 25 * 60, 25 * 60, true),
                    )
                }
            }
        }
    }

    @After
    fun restoreLocales() {
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale)
        Locale.setDefault(Locale.Category.FORMAT, formatLocale)
    }

    @Test
    fun weekCardKeepsEveryLabelAndValueWholeOnOneLine() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareStatistics"), 5_000)
        composeRule.onNodeWithTag("shareStatistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareCard"), 5_000)

        listOf("9 h 35 min", "SESSIONS", "23", "DAILY AVG", "2 h 23 min", "STREAK", "4 days").forEach { text ->
            val layout = layoutOnCard(text)
            assertEquals("\"$text\" lines", 1, layout.lineCount)
            assertFalse("\"$text\" is cut off", layout.hasVisualOverflow)
        }
    }

    private fun layoutOnCard(text: String): TextLayoutResult {
        val node = composeRule
            .onNode(hasText(text) and hasAnyAncestor(hasTestTag("shareCard")), useUnmergedTree = true)
            .fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        checkNotNull(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action) { "no text layout for \"$text\"" }(results)
        return results.first()
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE

        /** Thursday 12 November 2026, 16:10 UTC. */
        const val NOW = FakeTimeSource.START_EPOCH_MILLIS + 5 * DAY + (7 * 60 + 10) * MINUTE
    }
}
