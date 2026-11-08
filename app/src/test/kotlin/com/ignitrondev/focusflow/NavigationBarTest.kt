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
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The phone bar is a floating glass pill: every tab is an icon above its label, the selected tab is
 * tinted with the theme's primary color, every tab keeps a 48dp target, and screens scroll under
 * the bar but end clear of it. Wider, short windows keep the side rail.
 */
@OptIn(ExperimentalTestApi::class)
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
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        hiltRule.inject()
    }

    @Test
    fun selectedTabIsPrimaryAndNavigationIsUnchanged() {
        assertBarInsetFromScreenEdges()
        assertSelected("nav_Timer", LIGHT_PRIMARY)
        saveShot("light-411-timer")

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.onNodeWithTag("timerDial").assertDoesNotExist()
        assertSelected("nav_Statistics", LIGHT_PRIMARY)
        saveShot("light-411-statistics")

        composeRule.onNodeWithTag("nav_History").performClick()
        assertSelected("nav_History", LIGHT_PRIMARY)
        saveShot("light-411-history")

        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(Res.string.settings_section_appearance)).assertExists()
        assertSelected("nav_Settings", LIGHT_PRIMARY)
        saveShot("light-411-settings")

        composeRule.onNodeWithTag("nav_Timer").performClick()
        composeRule.onNodeWithTag("timerDial").assertExists()
        composeRule.onNodeWithTag("nav_Timer").assertIsSelected()
    }

    @Test
    fun darkThemeTintsTheSelectedTabWithItsPrimary() {
        runBlocking { preferences.setThemeMode(ThemeMode.Dark) }
        composeRule.waitForIdle()
        assertSelected("nav_Timer", DARK_PRIMARY)
        saveShot("dark-411-timer")

        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertSelected("nav_Statistics", DARK_PRIMARY)
        saveShot("dark-411-statistics")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun narrowPhoneKeepsHitTargets() {
        assertSelected("nav_Timer", LIGHT_PRIMARY)
        saveShot("light-320-timer")
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertSelected("nav_Statistics", LIGHT_PRIMARY)
        assertTargetsDoNotOverlap()
        saveShot("light-320-statistics")
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-xxhdpi")
    fun commonPhoneWidthKeepsHitTargets() {
        assertSelected("nav_Timer", LIGHT_PRIMARY)
        assertTargetsDoNotOverlap()
        saveShot("light-360-timer")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi", fontScale = 2f)
    fun largeFontKeepsLabelsAndHitTargets() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        assertSelected("nav_Statistics", LIGHT_PRIMARY)
        assertTargetsDoNotOverlap()
        saveShot("light-320-font2-statistics")
    }

    @Test
    fun selectionMovesBeforeTheColorFadeEnds() {
        allowMotion()
        withPausedClock {
            composeRule.onNodeWithTag("nav_Statistics").performClick()
            composeRule.mainClock.advanceTimeBy(48)
            // Navigation does not wait for the fade.
            composeRule.onNodeWithTag("nav_Statistics").assertIsSelected()
            composeRule.onNodeWithTag("nav_Timer").assertIsNotSelected()
            assertTabs(selected = "nav_Statistics")
            assertOneNodePerTab()
            composeRule.mainClock.advanceTimeBy(400)
            assertSelected("nav_Statistics", LIGHT_PRIMARY)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timerDial").assertDoesNotExist()
    }

    @Test
    fun animatorScaleZeroTintsTheTabImmediately() = assertInstantTint(animator = 0f, transition = 1f)

    @Test
    fun transitionScaleZeroTintsTheTabImmediately() = assertInstantTint(animator = 1f, transition = 0f)

    @Test
    fun rapidTabSwitchEndsOnTheLastTab() {
        allowMotion()
        withPausedClock {
            composeRule.onNodeWithTag("nav_Statistics").performClick()
            composeRule.mainClock.advanceTimeBy(48)
            composeRule.onNodeWithTag("nav_History").performClick()
            composeRule.mainClock.advanceTimeBy(48)
            composeRule.onNodeWithTag("nav_Settings").performClick()
            composeRule.mainClock.advanceTimeBy(400)
        }
        composeRule.waitForIdle()
        assertSelected("nav_Settings", LIGHT_PRIMARY)
    }

    @Test
    fun focusStaysOnTheActivatedTab() {
        allowMotion()
        // Compose 1.12 keeps AndroidComposeView unfocusable until the window gives it focus.
        // Keyboard focus inside the bar only sticks after that view is the focused Android view.
        focusComposeView()
        composeRule.onNodeWithTag("nav_Statistics").requestFocus()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_Statistics").assertIsFocused()
        composeRule.onNodeWithTag("nav_Timer").assertIsSelected()
        withPausedClock {
            composeRule.onNodeWithTag("nav_Statistics").performKeyInput { pressKey(Key.Enter) }
            composeRule.mainClock.advanceTimeBy(100)
            composeRule.onNodeWithTag("nav_Statistics").assertIsSelected().assertIsFocused()
            composeRule.onNodeWithTag("nav_Timer").assertIsNotFocused()
            assertOneNodePerTab()
        }
    }

    @Test
    fun screensScrollUnderTheBarAndEndClearOfIt() {
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.onNodeWithText(string(Res.string.settings_section_appearance)).assertExists()
        val bar = tabBounds("nav_bar")
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        // The screen reaches the bottom of the window: the bar floats over it instead of below it.
        val scroller = composeRule.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("screen ends at ${scroller.bottom}, above the bar at ${bar.top}", scroller.bottom > bar.bottom)
        assertTrue(scroller.bottom >= root.bottom - with(composeRule.density) { 1.dp.toPx() })
        // Scrolled to the end, the last row sits above the glass.
        repeat(6) { composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() } }
        composeRule.waitForIdle()
        val credit = composeRule.onNodeWithText(AUTHOR_CREDIT).fetchSemanticsNode().boundsInRoot
        assertTrue("last row ends at ${credit.bottom}, under the bar at ${bar.top}", credit.bottom <= bar.top)
        saveShot("light-411-settings-end")
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

    private fun assertInstantTint(animator: Float, transition: Float) {
        applyAnimationScales(animator, transition)
        withPausedClock {
            composeRule.onNodeWithTag("nav_History").performClick()
            composeRule.mainClock.advanceTimeBy(64)
            assertSelected("nav_History", LIGHT_PRIMARY)
            assertOneNodePerTab()
        }
    }

    private fun focusComposeView() {
        composeRule.runOnUiThread { markComposeViewFocused() }
        composeRule.waitForIdle()
    }

    /**
     * Robolectric leaves Compose in touch mode, where a tab cannot take keyboard focus, and never
     * marks AndroidComposeView focused, which makes Compose 1.12 reject the request.
     */
    private fun markComposeViewFocused() {
        val composeView = composeView()
        val flags = View::class.java.getDeclaredField("mPrivateFlags")
        flags.isAccessible = true
        flags.setInt(composeView, flags.getInt(composeView) or 0x2)
        val manager = composeView.javaClass.getMethod("getInputModeManager").invoke(composeView)
        val companion = Class.forName("androidx.compose.ui.input.InputMode").getField("Companion").get(null)
        val keyboard = companion.javaClass.getMethod("getKeyboard-aOaMEAU").invoke(companion) as Int
        manager.javaClass.getMethod("setInputMode-iuPiT84", Int::class.javaPrimitiveType).invoke(manager, keyboard)
        check(composeView.isFocused) { "AndroidComposeView did not take focus" }
    }

    private fun composeView(): View = composeRule.activity.window.decorView.descendants().first {
        it.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView"
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) {
                yieldAll(getChildAt(index).descendants())
            }
        }
    }

    private fun allowMotion() = applyAnimationScales(animator = 1f, transition = 1f)

    private fun applyAnimationScales(animator: Float, transition: Float) {
        composeRule.runOnUiThread {
            val resolver = composeRule.activity.contentResolver
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, animator)
            Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, transition)
        }
        val scenario = composeRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()
        scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    private inline fun withPausedClock(block: () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        try {
            block()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    private fun tabBounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertTargetsDoNotOverlap() {
        val boxes = tabs.map { (tag, _) -> composeRule.onNodeWithTag(tag).fetchSemanticsNode().touchBoundsInRoot }
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val overlapW = minOf(boxes[i].right, boxes[j].right) - maxOf(boxes[i].left, boxes[j].left)
                val overlapH = minOf(boxes[i].bottom, boxes[j].bottom) - maxOf(boxes[i].top, boxes[j].top)
                assertTrue("tabs $i and $j overlap by ${overlapW}x$overlapH", overlapW <= 1f || overlapH <= 1f)
            }
        }
    }

    private fun assertOneNodePerTab() {
        tabs.forEach { (tag, _) ->
            assertEquals(tag, 1, composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
        }
    }

    private fun assertBarInsetFromScreenEdges() {
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val bar = composeRule.onNodeWithTag("nav_bar").fetchSemanticsNode().boundsInRoot
        val margin = with(composeRule.density) { 12.dp.toPx() } - 0.5f
        assertTrue(bar.left - root.left >= margin)
        assertTrue(root.right - bar.right >= margin)
        assertTrue(root.bottom - bar.bottom >= with(composeRule.density) { 8.dp.toPx() } - 0.5f)
        assertTrue(bar.right <= root.right + 0.5f)
    }

    /** [selected] is the only selected tab and the only one painted in [primary]. */
    private fun assertSelected(selected: String, primary: Int) {
        assertTabs(selected)
        val shot = screenshot()
        tabs.forEach { (tag, _) ->
            val hits = pixelsNear(shot, tag, primary)
            if (tag == selected) {
                assertTrue("$tag has $hits pixels of ${hex(primary)}, expected a primary icon and label", hits >= 20)
            } else {
                assertTrue("$tag has $hits pixels of ${hex(primary)}, expected none on an inactive tab", hits <= 2)
            }
        }
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

    private fun pixelsNear(bitmap: Bitmap, tag: String, color: Int): Int {
        val bounds = tabBounds(tag)
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
     * The window as the GPU draws it. The glass bar is a hardware render effect, so a software
     * `View.draw` shows it as an empty black box; PixelCopy in Robolectric's hardware mode does not.
     * (Compose's captureToImage needs a DisplayMetrics overload that Robolectric's SDK 36 shadow
     * returns as null, so PixelCopy is called directly.)
     */
    private fun screenshot(): Bitmap {
        val window = composeRule.activity.window
        val view = window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var result = -1
        composeRule.runOnUiThread {
            PixelCopy.request(window, bitmap, { result = it }, Handler(Looper.getMainLooper()))
        }
        shadowOf(Looper.getMainLooper()).idle()
        check(result == PixelCopy.SUCCESS) { "PixelCopy failed with $result" }
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
        // Tests run on the fixed FocusFlow palette (see FakeUserPreferencesRepository).
        val LIGHT_PRIMARY = androidx.compose.ui.graphics.Color(0xFF4A55C8).toArgb()
        val DARK_PRIMARY = androidx.compose.ui.graphics.Color(0xFFBFC2FF).toArgb()
        const val AUTHOR_CREDIT = "Bima Adam Nugraha · IgnitronDev"
    }
}
