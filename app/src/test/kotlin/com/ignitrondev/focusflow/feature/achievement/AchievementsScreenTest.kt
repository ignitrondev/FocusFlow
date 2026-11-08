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
package com.ignitrondev.focusflow.feature.achievement

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.domain.achievement.Achievements
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.navigation.TopLevelDestination
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.achievements_earned
import com.ignitrondev.focusflow.resources.achievements_no_completed_sessions
import com.ignitrondev.focusflow.resources.achievements_not_earned
import com.ignitrondev.focusflow.testing.FakeTimeSource
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Achievements open from Statistics. The phone bar stays on the four existing destinations.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h2000dp-xxhdpi")
class AchievementsScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var timerRepository: TimerRepository

    @Inject lateinit var clock: FakeTimeSource

    @Inject lateinit var preferences: FakeUserPreferencesRepository

    @Inject lateinit var database: FocusFlowDatabase

    private fun string(res: StringResource) = runBlocking { getString(res) }

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            withContext(Dispatchers.IO) { database.clearAllTables() }
            preferences.setThemeMode(ThemeMode.Dark)
        }
        clock.zone = TimeZone.UTC
        clock.epochMillis = FakeTimeSource.START_EPOCH_MILLIS + 24L * 60L * 60L * 1000L
        assertEquals(4, TopLevelDestination.entries.size)
        assertEquals(listOf("Timer", "Statistics", "History", "Settings"), TopLevelDestination.entries.map { it.name })
    }

    @Test
    fun statisticsOpensAchievementsAndBackKeepsTheSameTab() {
        val empty = string(Res.string.achievements_no_completed_sessions)
        assertFalse(empty.contains("lost", ignoreCase = true))
        openStatistics()
        composeRule.onNodeWithTag("openAchievements").performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("achievement_sessions_1"), 5_000)

        composeRule.onNodeWithTag("achievementsTitle").assertExists()
        composeRule.onNodeWithText(empty).assertExists()
        composeRule.onNodeWithTag("achievementsEmpty").assertExists()
        Achievements.all.forEach { achievement ->
            composeRule.onNodeWithTag("achievement_${achievement.id}").performScrollTo().assertExists()
            composeRule.onNodeWithTag("achievement_${achievement.id}_status")
                .assertTextEquals(string(Res.string.achievements_not_earned))
        }
        assertFourTabs(statisticsSelected = true)
        composeRule.onNodeWithTag("nav_Achievements").assertDoesNotExist()
        saveShot("achievements-empty-dark")

        composeRule.onNodeWithTag("achievementsBack").performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("weekRange"), 5_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("openAchievements").assertExists()
        assertFourTabs(statisticsSelected = true)
        saveShot("statistics-entry-dark")
    }

    @Test
    fun aCompletedSessionIsEarnedAndTheEmptyNoteIsGone() {
        runBlocking {
            timerRepository.start()
            clock.advanceBy(25 * 60_000L)
            timerRepository.refresh()
        }
        openStatistics()
        composeRule.onNodeWithTag("openAchievements").performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("achievement_sessions_1"), 5_000)

        composeRule.onNodeWithTag("achievementsEmpty").assertDoesNotExist()
        composeRule.onNodeWithTag("achievement_sessions_1_status").assertTextEquals(string(Res.string.achievements_earned))
        composeRule.onNodeWithTag("achievement_sessions_10").performScrollTo()
        composeRule.onNodeWithTag("achievement_sessions_10_status").assertTextEquals(string(Res.string.achievements_not_earned))
        assertFourTabs(statisticsSelected = true)
    }

    @Test
    @Config(qualifiers = "w320dp-h1600dp-xxhdpi", fontScale = 2f)
    fun largeTextKeepsTheEntryAtLeast48Dp() {
        runBlocking { preferences.setThemeMode(ThemeMode.Light) }
        openStatistics()
        val node = composeRule.onNodeWithTag("openAchievements").performScrollTo()
        val height = node.fetchSemanticsNode().size.height
        val minimum = with(composeRule.density) { 48.dp.roundToPx() }
        assertTrue("entry height $height is below $minimum", height >= minimum)
        node.performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("achievementsTitle"), 5_000)
        composeRule.onNodeWithTag("achievementsTitle").assertExists()
        composeRule.onNodeWithTag("achievement_focus_hours_100").performScrollTo().assertExists()
        saveShot("achievements-large-text-light")
    }

    private fun openStatistics() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("openAchievements"), 5_000)
    }

    private fun assertFourTabs(statisticsSelected: Boolean) {
        listOf("nav_Timer", "nav_Statistics", "nav_History", "nav_Settings").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertExists()
        }
        if (statisticsSelected) composeRule.onNodeWithTag("nav_Statistics").assertIsSelected()
        composeRule.onNodeWithTag("nav_Achievements").assertDoesNotExist()
    }

    private fun saveShot(name: String) {
        val file = File("build/achievements-screenshots", "$name.png")
        file.parentFile?.mkdirs()
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
