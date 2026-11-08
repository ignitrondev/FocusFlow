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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowInsets
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.domain.model.ThemeMode
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.nav_history
import com.ignitrondev.focusflow.resources.nav_settings
import com.ignitrondev.focusflow.resources.nav_statistics
import com.ignitrondev.focusflow.resources.nav_timer
import com.ignitrondev.focusflow.resources.settings_section_appearance
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.io.File
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The phone bar is a segmented container: the active tab is an orange capsule around its icon and
 * label, inactive tabs stay on the container color, and every tab keeps a 48dp target.
 * Wider, short windows keep the side rail.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationBarTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var preferences: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun activeTabIsACapsuleAndNavigationIsUnchanged() {
        assertBarInsetFromScreenEdges()
        assertTabs(selected = "nav_Timer")
        assertCapsule("nav_Timer", CAPSULE)
        assertInactive("nav_Statistics", LIGHT_CONTAINER)
        saveShot("light-411-timer")

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.onNodeWithTag("timerDial").assertDoesNotExist()
        assertTabs(selected = "nav_Statistics")
        assertCapsule("nav_Statistics", CAPSULE)
        assertInactive("nav_Timer", LIGHT_CONTAINER)
        saveShot("light-411-statistics")

        composeRule.onNodeWithTag("nav_History").performClick()
        assertTabs(selected = "nav_History")
        saveShot("light-411-history")

        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(Res.string.settings_section_appearance)).assertExists()
        assertTabs(selected = "nav_Settings")
        assertCapsule("nav_Settings", CAPSULE)
        saveShot("light-411-settings")

        composeRule.onNodeWithTag("nav_Timer").performClick()
        composeRule.onNodeWithTag("timerDial").assertExists()
        composeRule.onNodeWithTag("nav_Timer").assertIsSelected()
    }

    @Test
    fun darkThemeKeepsTheOrangeCapsule() {
        runBlocking { preferences.setThemeMode(ThemeMode.Dark) }
        composeRule.waitForIdle()
        assertTabs(selected = "nav_Timer")
        assertCapsule("nav_Timer", CAPSULE)
        assertInactive("nav_Settings", DARK_CONTAINER)
        saveShot("dark-411-timer")

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertTabs(selected = "nav_Statistics")
        assertCapsule("nav_Statistics", CAPSULE)
        assertInactive("nav_History", DARK_CONTAINER)
        saveShot("dark-411-statistics")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun narrowPhoneKeepsHitTargets() {
        assertTabs(selected = "nav_Timer")
        assertCapsule("nav_Timer", CAPSULE)
        saveShot("light-320-timer")
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertTabs(selected = "nav_Statistics")
        assertCapsule("nav_Statistics", CAPSULE)
        saveShot("light-320-statistics")
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-xxhdpi")
    fun commonPhoneWidthKeepsHitTargets() {
        assertTabs(selected = "nav_Timer")
        assertCapsule("nav_Timer", CAPSULE)
        saveShot("light-360-timer")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi", fontScale = 2f)
    fun largeFontKeepsLabelsAndHitTargets() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertTabs(selected = "nav_Statistics")
        assertCapsule("nav_Statistics", CAPSULE)
        saveShot("light-320-font2-statistics")
    }

    @Test
    @Config(qualifiers = "w840dp-h360dp-xxhdpi")
    fun landscapePhoneKeepsTheSideRail() {
        composeRule.onNodeWithTag("nav_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_Timer").assertIsSelected()
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(Res.string.settings_section_appearance)).assertExists()
        composeRule.onNodeWithTag("nav_Settings").assertIsSelected()
        composeRule.onNodeWithTag("nav_Timer").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_bar").assertDoesNotExist()
    }

    @Test
    fun barClearsSystemBarInsets() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.systemBars(), android.graphics.Insets.of(80, 60, 48, 120))
                .build()
            composeRule.activity.window.decorView.dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val bar = composeRule.onNodeWithTag("nav_bar").fetchSemanticsNode().boundsInRoot
        val bottomGap = root.bottom - bar.bottom
        val startGap = bar.left - root.left
        assertTrue("bottom gap ${bottomGap}px is inside the 120px navigation inset", bottomGap >= 120f)
        assertTrue("start gap ${startGap}px is inside the 80px side inset", startGap >= 80f)
        assertTabs(selected = "nav_Timer")
        saveShot("light-411-safe-area")
    }

    private fun assertBarInsetFromScreenEdges() {
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val bar = composeRule.onNodeWithTag("nav_bar").fetchSemanticsNode().boundsInRoot
        val margin = with(composeRule.density) { 12.dp.toPx() } - 0.5f
        assertTrue(bar.left - root.left >= margin)
        assertTrue(root.right - bar.right >= margin)
        assertTrue(root.bottom - bar.bottom >= margin)
        assertTrue(bar.right <= root.right + 0.5f)
    }

    private fun assertTabs(selected: String) {
        tabs.forEach { (tag, labelRes) ->
            val node = composeRule.onNodeWithTag(tag)
            if (tag == selected) node.assertIsSelected() else node.assertIsNotSelected()
            val semantics = node.fetchSemanticsNode()
            val text = semantics.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
            assertEquals(string(labelRes), text)
            val touch = semantics.touchBoundsInRoot
            val minimum = with(composeRule.density) { 48.dp.toPx() } - 0.5f
            assertTrue(
                "$tag touch target is ${with(composeRule.density) {
                    touch.width.toDp()
                }} x ${with(composeRule.density) { touch.height.toDp() }}",
                touch.width >= minimum && touch.height >= minimum,
            )
        }
    }

    private fun assertCapsule(tag: String, color: Int) {
        val hits = pixelsNear(tag, color)
        assertTrue("$tag capsule has $hits pixels of ${hex(color)}, expected the orange fill", hits >= 20)
    }

    private fun assertInactive(tag: String, container: Int) {
        val hits = pixelsNear(tag, container)
        assertTrue("$tag has $hits container pixels (${hex(container)})", hits >= 20)
    }

    private fun pixelsNear(tag: String, color: Int): Int {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val bitmap = screenshot()
        val left = bounds.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, bitmap.height - 1)
        val right = bounds.right.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = bounds.bottom.toInt().coerceIn(top + 1, bitmap.height)
        var hits = 0
        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                if (near(bitmap.getPixel(x, y), color)) hits++
                x += 3
            }
            y += 3
        }
        return hits
    }

    private fun saveShot(name: String) {
        val dir = System.getenv("NAVBAR_SCREENSHOTS") ?: return
        val file = File(dir, "$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { screenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * Compose's captureToImage allocates a bitmap with a DisplayMetrics overload that Robolectric's
     * SDK 36 shadow returns as null. Drawing the window into an ARGB_8888 bitmap avoids that.
     */
    private fun screenshot(): Bitmap {
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        return bitmap
    }

    private fun string(res: StringResource) = runBlocking { getString(res) }

    private fun near(actual: Int, expected: Int): Boolean {
        fun channel(color: Int, shift: Int) = (color shr shift) and 0xFF
        return abs(channel(actual, 16) - channel(expected, 16)) <= 12 &&
            abs(channel(actual, 8) - channel(expected, 8)) <= 12 &&
            abs(channel(actual, 0) - channel(expected, 0)) <= 12
    }

    private fun hex(color: Int) = "#%08X".format(color)

    private val tabs = listOf(
        "nav_Timer" to Res.string.nav_timer,
        "nav_Statistics" to Res.string.nav_statistics,
        "nav_History" to Res.string.nav_history,
        "nav_Settings" to Res.string.nav_settings,
    )

    private companion object {
        // NavigationCapsule / surfaceContainerHigh from the theme. The capsule is orange in both themes.
        val CAPSULE = android.graphics.Color.parseColor("#FF6A1A")
        val LIGHT_CONTAINER = android.graphics.Color.parseColor("#E9E7EF")
        val DARK_CONTAINER = android.graphics.Color.parseColor("#292930")
    }
}
