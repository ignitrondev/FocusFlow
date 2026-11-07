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

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.history_today
import com.ignitrondev.focusflow.resources.settings_section_timer
import com.ignitrondev.focusflow.resources.settings_source_code
import com.ignitrondev.focusflow.resources.statistics_last_seven_days
import com.ignitrondev.focusflow.testing.FakeTimeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Checks every screen, with data, for two things TalkBack and motor-impaired users depend on:
 * every clickable element has a label, and its touch target is at least 48x48 dp.
 *
 * (Compose's ATF integration, `enableAccessibilityChecks()`, is skipped on Robolectric, so these
 * checks are done directly on the semantics tree. Color contrast is covered by ColorContrastTest.)
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class AccessibilityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var timerRepository: TimerRepository

    @Inject lateinit var clock: FakeTimeSource

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            repeat(2) {
                timerRepository.start()
                clock.advanceBy(25 * 60_000L)
                timerRepository.refresh()
                timerRepository.skip()
            }
        }
    }

    private fun SemanticsNode.label(): String? {
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
        val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        return (texts + description).ifBlank { null }
    }

    private fun checkScreen(tab: String?, readyText: String?, scrollTo: String? = null) {
        tab?.let { composeRule.onNodeWithTag(it).performClick() }
        readyText?.let { composeRule.waitUntilAtLeastOneExists(hasText(it), 5_000) }
        composeRule.waitForIdle()
        checkVisibleNodes()
        // Also check what is below the fold.
        scrollTo?.let {
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(it))
            composeRule.waitForIdle()
            checkVisibleNodes()
        }
    }

    private fun checkVisibleNodes() {
        val density = composeRule.density
        val minimum = with(density) { 48.dp.toPx() } - 0.5f
        val visible = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { node ->
            // Only nodes that are fully on screen: a row cut off by the screen edge reports its
            // visible part only. Scrolling (see checkScreen) brings the others into view.
            val bounds = node.boundsInRoot
            bounds.width >= node.size.width - 1 && bounds.height >= node.size.height - 1 && bounds.width > 0f
        }
        assertTrue("no clickable nodes found", visible.isNotEmpty())
        val problems = visible.mapNotNull { node ->
            val bounds = node.touchBoundsInRoot
            val name = node.label() ?: "<unlabelled node ${node.id}>"
            when {
                node.label() == null -> "$name has no text or content description"

                bounds.width < minimum || bounds.height < minimum ->
                    "$name touch target is ${with(density) { bounds.width.toDp() }} x ${with(density) { bounds.height.toDp() }}"

                else -> null
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun string(id: Int) = composeRule.activity.getString(id)

    /** A string of the shared (multiplatform) UI. */
    private fun string(res: StringResource) = runBlocking { getString(res) }

    @Test
    fun timerScreen() = checkScreen(null, null)

    @Test
    fun statisticsScreen() = checkScreen("nav_Statistics", string(Res.string.statistics_last_seven_days))

    @Test
    fun historyScreen() = checkScreen("nav_History", string(Res.string.history_today))

    @Test
    fun settingsScreen() =
        checkScreen("nav_Settings", string(Res.string.settings_section_timer), scrollTo = string(Res.string.settings_source_code))
}
