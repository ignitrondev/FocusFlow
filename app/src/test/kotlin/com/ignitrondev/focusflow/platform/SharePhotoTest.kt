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
package com.ignitrondev.focusflow.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ignitrondev.focusflow.testing.resetFileProviderCache
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharePhotoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() = resetFileProviderCache()

    private fun jpeg(width: Int, height: Int, orientation: Int = ExifInterface.ORIENTATION_NORMAL): Uri {
        val file = File(context.cacheDir, "photo-${width}x$height-$orientation.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 120, 40)) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return Uri.fromFile(file)
    }

    @Test
    fun cameraPhotosAreShownUpright() {
        // Phone cameras usually store the pixels landscape and mark them "rotate 90°" in EXIF.
        val photo = SharePhoto.load(context, jpeg(400, 200, ExifInterface.ORIENTATION_ROTATE_90))

        assertNotNull(photo)
        assertEquals(200, photo!!.width)
        assertEquals(400, photo.height)
    }

    @Test
    fun largePhotosAreDownsampled() {
        val photo = SharePhoto.load(context, jpeg(4000, 3000))!!

        assertTrue("longest side ${photo.width} stays manageable", photo.width in 1600..2000)
    }

    @Test
    fun unreadableFilesGiveNull() {
        val notAnImage = File(context.cacheDir, "notes.txt").apply { writeText("hello") }

        assertNull(SharePhoto.load(context, Uri.fromFile(notAnImage)))
    }

    @Test
    fun cameraOutputIsAPrivateFileProviderUri() {
        val uri = SharePhoto.cameraOutput(context)

        assertEquals("${context.packageName}.fileprovider", uri.authority)
    }
}
