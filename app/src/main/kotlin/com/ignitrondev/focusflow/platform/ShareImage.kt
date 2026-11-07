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

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands [image] to the app the user picks in Android's share sheet. The PNG is written to the
 * app's cache (exposed only through FileProvider, read-only, to the chosen app); FocusFlow itself
 * never sends anything over the network.
 */
object ShareImage {
    const val FILE_NAME = "focusflow-stats.png"

    /** Writes the PNG; call off the main thread. Returns the content URI to share. */
    fun export(context: Context, image: Bitmap): android.net.Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun chooser(uri: android.net.Uri, text: String, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // ClipData lets the share sheet show a preview and carries the read grant.
        send.clipData = ClipData.newRawUri(null, uri)
        return Intent.createChooser(send, title)
    }
}
