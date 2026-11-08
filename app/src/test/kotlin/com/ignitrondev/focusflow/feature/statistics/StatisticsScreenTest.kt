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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.data.database.FocusFlowDatabase
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.statistics_chart_empty
import com.ignitrondev.focusflow.resources.statistics_today
import com.ignitrondev.focusflow.resources.week_range_same_month
import com.ignitrondev.focusflow.testing.FakeTimeSource
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.io.File
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
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
 * Locale week order, the empty statistics screen, and large text. Sample chart numbers are not
 * asserted beyond the scale a single 25 minute session produces.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h1200dp-xxhdpi")
class StatisticsScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var timerRepository: TimerRepository

    @Inject lateinit var clock: FakeTimeSource

    @Inject lateinit var preferences: FakeUserPreferencesRepository

    @Inject lateinit var database: FocusFlowDatabase

    private val displayLocale = Locale.getDefault(Locale.Category.DISPLAY)
    private val formatLocale = Locale.getDefault(Locale.Category.FORMAT)

    private fun string(res: StringResource, vararg args: Any) = runBlocking {
        if (args.isEmpty()) getString(res) else getString(res, *args)
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            withContext(Dispatchers.IO) { database.clearAllTables() }
            preferences.setThemeMode(ThemeMode.Dark)
        }
        clock.zone = TimeZone.UTC
        // Sunday 8 Nov 2026, 09:00 UTC.
        clock.epochMillis = FakeTimeSource.START_EPOCH_MILLIS + 24L * 60L * 60L * 1000L
    }

    @After
    fun restoreLocales() {
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale)
        Locale.setDefault(Locale.Category.FORMAT, formatLocale)
    }

    @Test
    fun sundayStartWeekRunsSundayThroughSaturday() {
        useLocale(Locale.US)
        completeFocus()
        openStatistics()

        val range = string(Res.string.week_range_same_month, 8, 14, "Nov")
        composeRule.onNodeWithTag("weekRange").assertExists()
        composeRule.onNodeWithText(range).assertExists()
        assertDay("dayLabel_0", java.time.DayOfWeek.SUNDAY, Locale.US)
        assertDay("dayLabel_6", java.time.DayOfWeek.SATURDAY, Locale.US)
        composeRule.onNodeWithTag("tick_30").assertExists()
        composeRule.onNodeWithText("All time").assertDoesNotExist()
        composeRule.onNodeWithText("Month").assertDoesNotExist()
        composeRule.onNodeWithText("Year").assertDoesNotExist()
        assertSideBySide("summaryToday", "summaryWeek")
        saveShot("statistics_sunday_start")
    }

    @Test
    fun mondayStartWeekRunsMondayThroughSunday() {
        useLocale(Locale.UK)
        completeFocus()
        openStatistics()

        composeRule.onNodeWithText(string(Res.string.week_range_same_month, 2, 8, "Nov")).assertExists()
        assertDay("dayLabel_0", java.time.DayOfWeek.MONDAY, Locale.UK)
        assertDay("dayLabel_6", java.time.DayOfWeek.SUNDAY, Locale.UK)
        composeRule.onNodeWithTag("tick_30").assertExists()
        assertSideBySide("summaryToday", "summaryWeek")
        saveShot("statistics_monday_start")
    }

    @Test
    fun emptyStateShowsZerosAndKeepsShare() {
        useLocale(Locale.US)
        openStatistics()

        composeRule.onNodeWithText(string(Res.string.statistics_today)).assertExists()
        assertTrue(composeRule.onAllNodesWithText("0 min").fetchSemanticsNodes().size >= 4)
        assertTrue(composeRule.onAllNodesWithText("0 sessions").fetchSemanticsNodes().size >= 2)
        composeRule.onNodeWithText("0 days").assertExists()
        composeRule.onNodeWithTag("chartEmpty").assertExists()
        composeRule.onNodeWithText(string(Res.string.statistics_chart_empty)).assertExists()
        composeRule.onNodeWithTag("tick_60").assertExists()
        composeRule.onNodeWithTag("tick_15").assertExists()
        composeRule.onNodeWithTag("shareStatistics").assertIsEnabled()
        composeRule.onNodeWithText("All time").assertDoesNotExist()
        composeRule.onNodeWithText("Month").assertDoesNotExist()
        composeRule.onNodeWithText("Year").assertDoesNotExist()
        composeRule.onNodeWithText("+%").assertDoesNotExist()
        saveShot("statistics_empty")
    }

    @Test
    @Config(qualifiers = "w411dp-h1600dp-xxhdpi", fontScale = 1.3f)
    fun largeFontStacksTheCardsAndTallsTheChart() {
        useLocale(Locale.US)
        completeFocus()
        openStatistics()

        val plot = composeRule.onNodeWithTag("chartPlot").fetchSemanticsNode().size.height
        val expected = with(composeRule.density) { 200.dp.roundToPx() }
        assertEquals(expected, plot)
        assertStacked("summaryToday", "summaryWeek")
        assertStacked("metricDaily", "metricSession")
        assertTrue(composeRule.onAllNodesWithText("25 min").fetchSemanticsNodes().isNotEmpty())
        saveShot("statistics_large_font")
    }

    private fun useLocale(locale: Locale) {
        Locale.setDefault(Locale.Category.DISPLAY, Locale.US)
        Locale.setDefault(Locale.Category.FORMAT, locale)
    }

    private fun completeFocus() {
        runBlocking {
            timerRepository.start()
            clock.advanceBy(25 * 60_000L)
            timerRepository.refresh()
        }
    }

    private fun openStatistics() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("weekRange"), 5_000)
    }

    private fun assertDay(tag: String, day: java.time.DayOfWeek, locale: Locale) {
        val expected = day.getDisplayName(TextStyle.SHORT, locale)
        composeRule.onNodeWithTag(tag).assertExists()
        composeRule.onNodeWithText(expected).assertExists()
    }

    private fun assertSideBySide(first: String, second: String) {
        val a = composeRule.onNodeWithTag(first).fetchSemanticsNode().boundsInRoot
        val b = composeRule.onNodeWithTag(second).fetchSemanticsNode().boundsInRoot
        assertTrue("$second should sit to the right of $first", b.left >= a.right - 1f)
    }

    private fun assertStacked(first: String, second: String) {
        val a = composeRule.onNodeWithTag(first).fetchSemanticsNode().boundsInRoot
        val b = composeRule.onNodeWithTag(second).fetchSemanticsNode().boundsInRoot
        assertTrue("$second should sit below $first", b.top >= a.bottom - 1f)
        assertTrue(abs(a.left - b.left) < 2f)
    }

    private fun saveShot(name: String) {
        val dir = System.getenv("STATISTICS_SCREENSHOTS") ?: "build/statistics-screenshots"
        val file = File(dir, "$name.png")
        file.parentFile?.mkdirs()
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
