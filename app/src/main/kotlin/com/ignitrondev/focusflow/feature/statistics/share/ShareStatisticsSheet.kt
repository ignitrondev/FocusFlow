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
package com.ignitrondev.focusflow.feature.statistics.share

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.platform.ShareImage
import com.ignitrondev.focusflow.platform.SharePhoto
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Width of the shared image in pixels: sharp on social feeds whatever the screen density. */
private const val EXPORT_WIDTH_PX = 1080f
private const val MAX_ROLE_LENGTH = 40
private const val TAG = "ShareStatistics"

/**
 * Builds a card to share: the user's role (required, remembered), an optional photo of where they
 * are working (camera or photo picker), and the period. The photo never leaves the device unless
 * the user shares the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStatisticsSheet(statistics: FocusStatistics, savedRole: String, onSaveRole: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(SharePeriod.Week) }
    var role by rememberSaveable(savedRole) { mutableStateOf(savedRole) }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var sharing by remember { mutableStateOf(false) }
    val exportLayer = rememberGraphicsLayer()

    // Decoded off the main thread, again after a configuration change (the URI is saved).
    LaunchedEffect(photoUri) {
        photo = photoUri?.let { uri ->
            withContext(Dispatchers.IO) { SharePhoto.load(context, uri) }?.asImageBitmap().also {
                if (it == null) Toast.makeText(context, R.string.share_photo_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) photoUri = pendingCameraUri
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    val focusTime = durationText(if (period == SharePeriod.Today) statistics.today.focusMillis else statistics.weekFocusMillis)
    val periodName = stringResource(if (period == SharePeriod.Today) R.string.share_period_today else R.string.share_period_week)
    val shareText = stringResource(if (period == SharePeriod.Today) R.string.share_text_today else R.string.share_text_week, focusTime)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val cardDescription = stringResource(R.string.share_card_description, periodName, focusTime, statistics.currentStreakDays)
    val roleMissing = role.isBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleLarge)
            val controls = Modifier.widthIn(max = 360.dp).fillMaxWidth()
            OutlinedTextField(
                value = role,
                onValueChange = { role = it.take(MAX_ROLE_LENGTH) },
                label = { Text(stringResource(R.string.share_role_label)) },
                placeholder = { Text(stringResource(R.string.share_role_placeholder)) },
                supportingText = { if (roleMissing) Text(stringResource(R.string.share_role_required)) },
                isError = false,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = controls.testTag("shareRole"),
            )
            Row(controls, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasCamera) {
                    OutlinedButton(
                        onClick = {
                            val uri = SharePhoto.cameraOutput(context)
                            pendingCameraUri = uri
                            try {
                                takePhoto.launch(uri)
                            } catch (e: ActivityNotFoundException) {
                                Log.w(TAG, "No camera app", e)
                                Toast.makeText(context, R.string.share_photo_failed, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("sharePhotoCamera"),
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        FittedLabel(stringResource(R.string.share_take_photo), Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedButton(
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f).testTag("sharePhotoPick"),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    FittedLabel(stringResource(R.string.share_choose_photo), Modifier.padding(start = 8.dp))
                }
            }
            if (photo != null) {
                TextButton(onClick = { photoUri = null }) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Text(stringResource(R.string.share_remove_photo), modifier = Modifier.padding(start = 8.dp))
                }
            }
            SingleChoiceSegmentedButtonRow(controls) {
                SharePeriod.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = period == option,
                        onClick = { period = option },
                        shape = SegmentedButtonDefaults.itemShape(index, SharePeriod.entries.size),
                        icon = {},
                        label = {
                            FittedLabel(
                                stringResource(
                                    if (option ==
                                        SharePeriod.Today
                                    ) R.string.share_period_today else R.string.share_period_week,
                                ),
                            )
                        },
                    )
                }
            }
            FocusShareCard(
                statistics = statistics,
                period = period,
                role = role,
                photo = photo,
                modifier = controls
                    .testTag("shareCard")
                    .semantics { contentDescription = cardDescription }
                    // Rounded on screen only: the recording below is taken inside this clip.
                    .clip(RoundedCornerShape(28.dp))
                    .recordForExport(exportLayer),
            )
            Button(
                onClick = {
                    sharing = true
                    onSaveRole(role)
                    scope.launch {
                        try {
                            val image = exportLayer.toImageBitmap().asAndroidBitmap()
                            val uri = withContext(Dispatchers.IO) { ShareImage.export(context, image) }
                            context.startActivity(ShareImage.chooser(uri, shareText, chooserTitle))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(TAG, "Could not share the statistics image", e)
                            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                        } finally {
                            sharing = false
                        }
                    }
                },
                enabled = !sharing && !roleMissing,
                modifier = controls.testTag("shareButton"),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(R.string.share_button), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * Draws the content normally, and also records it, scaled to [EXPORT_WIDTH_PX] wide, into
 * [layer]. Text and shapes are redrawn at that size, so the exported image stays sharp instead of
 * being an upscaled screenshot.
 */
private fun Modifier.recordForExport(layer: GraphicsLayer): Modifier = drawWithContent {
    drawContent()
    val factor = EXPORT_WIDTH_PX / size.width
    layer.record(IntSize(EXPORT_WIDTH_PX.roundToInt(), (size.height * factor).roundToInt())) {
        scale(factor, factor, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
    }
}
