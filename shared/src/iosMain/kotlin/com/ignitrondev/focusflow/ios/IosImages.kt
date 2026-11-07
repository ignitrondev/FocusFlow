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
@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ignitrondev.focusflow.ios

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.max
import kotlin.math.min
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** The longest side of a card photo, as on Android: plenty for a 1080 px wide card. */
private const val MAX_PHOTO_SIDE = 1600.0
private const val JPEG_QUALITY = 95

/**
 * Takes a photo with the camera or picks one from the library. Neither needs the photo library
 * permission: the system picker only hands over what the user chose. [onResult] runs on the main
 * thread, with null when the photo could not be read.
 */
internal class PhotoPicker(private val onResult: (UIImage?) -> Unit) {
    private val cameraDelegate = CameraDelegate(onResult)
    private val libraryDelegate = LibraryDelegate(onResult)

    /** False when there was no screen to show the camera on. */
    fun takePhoto(): Boolean {
        val host = topViewController() ?: return false
        val camera = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            delegate = cameraDelegate
        }
        host.presentViewController(camera, animated = true, completion = null)
        return true
    }

    fun choosePhoto(): Boolean {
        val host = topViewController() ?: return false
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val library = PHPickerViewController(configuration).apply { delegate = libraryDelegate }
        host.presentViewController(library, animated = true, completion = null)
        return true
    }

    companion object {
        val cameraAvailable: Boolean
            get() = UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
            )
    }
}

private class CameraDelegate(private val onResult: (UIImage?) -> Unit) :
    NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private class LibraryDelegate(private val onResult: (UIImage?) -> Unit) :
    NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return // cancelled
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val image = data?.let { UIImage.imageWithData(it) }
            dispatch_async(dispatch_get_main_queue()) { onResult(image) }
        }
    }
}

/**
 * Draws the photo upright (camera photos carry their rotation as metadata) and no larger than
 * [MAX_PHOTO_SIDE], then decodes it for Compose.
 */
internal fun UIImage.toUprightImageBitmap(): ImageBitmap? {
    val (width, height) = size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = min(1.0, MAX_PHOTO_SIDE / max(width, height))
    val targetWidth = width * scale
    val targetHeight = height * scale
    val format = UIGraphicsImageRendererFormat().apply { this.scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(targetWidth, targetHeight), format = format)
    val upright = renderer.imageWithActions { drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight)) }
    val jpeg = UIImageJPEGRepresentation(upright, JPEG_QUALITY / 100.0) ?: return null
    return Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap()
}

/** Opens the share sheet with the card as a JPEG image, like on Android. */
internal suspend fun shareImage(image: ImageBitmap, text: String) {
    val jpeg = withContext(Dispatchers.Default) {
        Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes
    } ?: error("Could not encode the image")
    withContext(Dispatchers.Main) {
        val photo = UIImage.imageWithData(jpeg.toNSData()) ?: error("Could not decode the image")
        val host = topViewController() ?: error("No window to share from")
        val sheet = UIActivityViewController(activityItems = listOf(photo, text), applicationActivities = null)
        // On iPad the sheet is a popover and needs an anchor; it is centered without an arrow.
        sheet.popoverPresentationController?.apply {
            sourceView = host.view
            sourceRect = host.view.bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 1.0, 1.0) }
            permittedArrowDirections = 0u
        }
        host.presentViewController(sheet, animated = true, completion = null)
    }
}

/** The view controller currently on top, to present system screens from. */
internal fun topViewController(): UIViewController? {
    val windows = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
    var top = (windows.firstOrNull { it.isKeyWindow() } ?: windows.firstOrNull())?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

private fun ByteArray.toNSData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
