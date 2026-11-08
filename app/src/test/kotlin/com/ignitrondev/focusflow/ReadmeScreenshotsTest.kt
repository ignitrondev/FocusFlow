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

import android.Manifest
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.data.database.FocusSessionDao
import com.ignitrondev.focusflow.data.database.FocusSessionEntity
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.timer.SessionType
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.history_today
import com.ignitrondev.focusflow.resources.settings_section_appearance
import com.ignitrondev.focusflow.resources.statistics_this_week
import com.ignitrondev.focusflow.resources.timer_action_start
import com.ignitrondev.focusflow.testing.FakeTimeSource
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.io.File
import java.time.Duration
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screenshots in docs/screenshots from two weeks of sample sessions. It only runs when
 * README_SCREENSHOTS names the output folder:
 *
 *     README_SCREENSHOTS=$PWD/docs/screenshots ./gradlew :app:testDebugUnitTest --tests '*ReadmeScreenshotsTest*'
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadmeScreenshotsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var preferences: FakeUserPreferencesRepository

    @Inject lateinit var clock: FakeTimeSource

    @Inject lateinit var sessions: FocusSessionDao

    private val outputDir = System.getenv("README_SCREENSHOTS")?.let(::File)
    private val displayLocale = Locale.getDefault(Locale.Category.DISPLAY)
    private val formatLocale = Locale.getDefault(Locale.Category.FORMAT)

    @Before
    fun setUp() {
        assumeTrue("Set README_SCREENSHOTS to render the screenshots", outputDir != null)
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        hiltRule.inject()
        shadowOf(composeRule.activity.application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // Weeks start on Monday, as in most of the world.
        Locale.setDefault(Locale.Category.DISPLAY, Locale.US)
        Locale.setDefault(Locale.Category.FORMAT, Locale.UK)
        clock.epochMillis = NOW
        seedSessions()
    }

    @After
    fun restoreLocales() {
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale)
        Locale.setDefault(Locale.Category.FORMAT, formatLocale)
    }

    @Test
    fun renderScreenshots() {
        composeRule.onNodeWithTag("nav_History").performClick()
        waitForText(string(Res.string.history_today))
        saveShot("history_light")

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        waitForText(string(Res.string.statistics_this_week))
        saveShot("statistics_light")

        composeRule.onNodeWithTag("nav_Settings").performClick()
        waitForText(string(Res.string.settings_section_appearance))
        saveShot("settings_light")

        // Achievements open from the end of Statistics; scroll there so the button is above the bar.
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        waitForText(string(Res.string.statistics_this_week))
        repeat(4) { composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        composeRule.onNodeWithTag("openAchievements").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("achievement_sessions_1"), TIMEOUT_MILLIS)
        saveShot("achievements_light")

        // A focus session a few minutes in.
        composeRule.onNodeWithTag("nav_Timer").performClick()
        composeRule.onNodeWithContentDescription(string(Res.string.timer_action_start)).performClick()
        val pause = composeRule.activity.getString(R.string.timer_action_pause)
        composeRule.waitUntilAtLeastOneExists(hasContentDescription(pause), TIMEOUT_MILLIS)
        clock.advanceBy(7 * MINUTE + 18_000L)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        waitForText("17:42")
        saveShot("timer_light")

        runBlocking { preferences.setThemeMode(ThemeMode.Dark) }
        composeRule.waitForIdle()
        saveShot("app_dark")

        // The Statistics tab comes back on Achievements; the last row holds the locked legendary badges.
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("achievement_sessions_1"), TIMEOUT_MILLIS)
        saveShot("achievements_dark")
        repeat(4) { composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        saveShot("achievements_dark_end")
    }

    /** Fourteen days in a row of focus sessions with their breaks, and one session stopped early today. */
    private fun seedSessions() = runBlocking {
        val todayMidnight = NOW - NOW % DAY
        PLAN.forEachIndexed { daysAgo, (focusSessions, firstStartMinute) ->
            var start = todayMidnight - daysAgo * DAY + firstStartMinute * MINUTE
            repeat(focusSessions) { i ->
                insert("d$daysAgo-f$i", SessionType.Focus, start, minutes = 25)
                start += 25 * MINUTE
                if (i < focusSessions - 1) {
                    val long = (i + 1) % 4 == 0
                    val minutes = if (long) 15L else 5L
                    insert("d$daysAgo-b$i", if (long) SessionType.LongBreak else SessionType.ShortBreak, start, minutes)
                    start += minutes * MINUTE
                }
            }
        }
        insert("today-stopped", SessionType.Focus, todayMidnight + (13 * 60 + 40) * MINUTE, minutes = 25, actualMinutes = 11)
    }

    private suspend fun insert(id: String, type: SessionType, start: Long, minutes: Long, actualMinutes: Long = minutes) {
        sessions.insert(
            FocusSessionEntity(
                id = id,
                sessionType = type.name,
                startedAt = start,
                endedAt = start + actualMinutes * MINUTE,
                plannedDurationSeconds = minutes * 60,
                actualDurationSeconds = actualMinutes * 60,
                completed = actualMinutes == minutes,
            ),
        )
    }

    private fun waitForText(text: String) = composeRule.waitUntilAtLeastOneExists(hasText(text), TIMEOUT_MILLIS)

    private fun string(res: StringResource) = runBlocking { getString(res) }

    /** Captured as the GPU draws it: a software `View.draw` paints the glass navbar as a black box. */
    private fun saveShot(name: String) {
        composeRule.waitForIdle()
        val window = composeRule.activity.window
        val bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        var result = -1
        composeRule.runOnUiThread { PixelCopy.request(window, bitmap, { result = it }, Handler(Looper.getMainLooper())) }
        shadowOf(Looper.getMainLooper()).idle()
        check(result == PixelCopy.SUCCESS) { "PixelCopy failed with $result" }
        val file = File(outputDir, "$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val MINUTE = 60_000L
        const val DAY = 24 * 60 * MINUTE

        /** Thursday 12 November 2026, 16:10 UTC: five days after [FakeTimeSource]'s anchor. */
        const val NOW = FakeTimeSource.START_EPOCH_MILLIS + 5 * DAY + (7 * 60 + 10) * MINUTE

        /** Per day, today first: completed focus sessions and the minute of the day the first one starts. */
        val PLAN = listOf(
            4 to 9 * 60 + 15,
            8 to 8 * 60 + 30,
            5 to 13 * 60,
            6 to 9 * 60,
            3 to 10 * 60,
            2 to 14 * 60,
            5 to 9 * 60 + 30,
            7 to 8 * 60,
            3 to 11 * 60,
            6 to 9 * 60,
            4 to 13 * 60 + 30,
            2 to 10 * 60,
            4 to 9 * 60,
            3 to 15 * 60,
        )
    }
}
