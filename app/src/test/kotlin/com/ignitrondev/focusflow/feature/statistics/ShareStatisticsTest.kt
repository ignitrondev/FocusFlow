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

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.MainActivity
import com.ignitrondev.focusflow.domain.timer.TimerRepository
import com.ignitrondev.focusflow.platform.ShareImage
import com.ignitrondev.focusflow.testing.FakeTimeSource
import com.ignitrondev.focusflow.testing.FakeUserPreferencesRepository
import com.ignitrondev.focusflow.testing.resetFileProviderCache
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
// Real rendering: the card is drawn into a bitmap.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ShareStatisticsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var timerRepository: TimerRepository

    @Inject lateinit var clock: FakeTimeSource

    @Inject lateinit var preferences: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        resetFileProviderCache()
        hiltRule.inject()
        runBlocking {
            timerRepository.start()
            clock.advanceBy(25 * 60_000L)
            timerRepository.refresh()
        }
    }

    private fun openShareSheet() {
        composeRule.onNodeWithTag("nav_Statistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareStatistics"), 5_000)
        composeRule.onNodeWithTag("shareStatistics").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("shareCard"), 5_000)
    }

    @Test
    fun roleIsRequiredBeforeSharingAndIsRemembered() {
        openShareSheet()
        composeRule.onNodeWithTag("shareButton").assertIsNotEnabled()

        composeRule.onNodeWithTag("shareRole").performTextInput("Software Engineer")

        composeRule.onNodeWithTag("shareButton").assertIsEnabled()
        composeRule.onNodeWithTag("shareCard").captureToImage()
        composeRule.onNodeWithTag("shareButton").performClick()
        composeRule.waitUntil(5_000) { preferences.userPreferences.value.shareRole == "Software Engineer" }
    }

    @Test
    fun takePhotoOpensTheCameraAppWithAPrivateOutputFile() {
        shadowOf(composeRule.activity.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_ANY, true)
        openShareSheet()

        composeRule.onNodeWithTag("sharePhotoCamera").performClick()
        composeRule.waitForIdle()

        val started = shadowOf(composeRule.activity).nextStartedActivityForResult.intent
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, started.action)
        val output = IntentCompat.getParcelableExtra(started, MediaStore.EXTRA_OUTPUT, Uri::class.java)!!
        assertEquals("${composeRule.activity.packageName}.fileprovider", output.authority)
    }

    @Test
    fun storyFormatExportsAFullScreenStoryImage() {
        openShareSheet()
        composeRule.onNodeWithTag("shareRole").performTextInput("Software Engineer")
        composeRule.onNodeWithTag("shareFormatStory").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("shareCard").captureToImage()
        // The story preview is tall: the button is below the fold of the sheet.
        composeRule.onNodeWithTag("shareButton").performScrollTo().performClick()
        // Encoding a 1080x1920 image is slower on Robolectric than on a phone.
        composeRule.waitUntil(20_000) { shadowOf(composeRule.activity).nextStartedActivity != null }

        val image = BitmapFactory.decodeFile(File(File(composeRule.activity.cacheDir, "shared"), ShareImage.FILE_NAME).path)
        assertEquals(1080, image.width)
        assertEquals(1920, image.height)
    }

    @Test
    fun sharingSendsAHighResolutionImageThroughTheShareSheet() {
        openShareSheet()
        composeRule.onNodeWithTag("shareRole").performTextInput("Software Engineer")

        // On a device the visible card has been drawn by now. Robolectric does not draw the bottom
        // sheet's window on its own, so draw it once, as the screen would.
        composeRule.onNodeWithTag("shareCard").captureToImage()
        composeRule.onNodeWithTag("shareButton").performClick()
        var chooser: Intent? = null
        composeRule.waitUntil(5_000) {
            chooser = shadowOf(composeRule.activity).nextStartedActivity
            chooser != null
        }

        val started = checkNotNull(chooser)
        assertEquals(Intent.ACTION_CHOOSER, started.action)
        val send = IntentCompat.getParcelableExtra(started, Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        assertTrue("the receiving app may read the image", send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java)!!
        assertEquals("${composeRule.activity.packageName}.fileprovider", uri.authority)
        assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains("25 min"))

        val file = File(File(composeRule.activity.cacheDir, "shared"), ShareImage.FILE_NAME)
        val image = BitmapFactory.decodeFile(file.path)
        assertNotNull("a valid JPEG was written", image)
        assertEquals(1080, image.width)
        assertEquals(1350, image.height)
    }
}
