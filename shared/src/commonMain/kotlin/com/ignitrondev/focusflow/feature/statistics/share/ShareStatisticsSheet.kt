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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.share_button
import com.ignitrondev.focusflow.resources.share_card_description
import com.ignitrondev.focusflow.resources.share_card_empty
import com.ignitrondev.focusflow.resources.share_choose_photo
import com.ignitrondev.focusflow.resources.share_chooser_title
import com.ignitrondev.focusflow.resources.share_failed
import com.ignitrondev.focusflow.resources.share_format_post
import com.ignitrondev.focusflow.resources.share_format_story
import com.ignitrondev.focusflow.resources.share_period_today
import com.ignitrondev.focusflow.resources.share_period_week
import com.ignitrondev.focusflow.resources.share_photo_failed
import com.ignitrondev.focusflow.resources.share_remove_photo
import com.ignitrondev.focusflow.resources.share_role_label
import com.ignitrondev.focusflow.resources.share_role_placeholder
import com.ignitrondev.focusflow.resources.share_role_required
import com.ignitrondev.focusflow.resources.share_take_photo
import com.ignitrondev.focusflow.resources.share_text_empty
import com.ignitrondev.focusflow.resources.share_text_today
import com.ignitrondev.focusflow.resources.share_text_week
import com.ignitrondev.focusflow.resources.share_title
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
    val platform = LocalAppDependencies.current.platform
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(SharePeriod.Week) }
    var format by rememberSaveable { mutableStateOf(ShareFormat.Post) }
    var role by rememberSaveable(savedRole) { mutableStateOf(savedRole) }
    var sharing by remember { mutableStateOf(false) }
    val exportLayer = rememberGraphicsLayer()
    val cardPhoto = platform.rememberCardPhoto()
    val photo = cardPhoto.photo
    val sharer = platform.rememberImageSharer()

    val focusMillis = if (period == SharePeriod.Today) statistics.today.focusMillis else statistics.weekFocusMillis
    val focusTime = durationText(focusMillis)
    val periodName = stringResource(if (period == SharePeriod.Today) Res.string.share_period_today else Res.string.share_period_week)
    val shareText = if (focusMillis == 0L) {
        stringResource(Res.string.share_text_empty)
    } else {
        stringResource(if (period == SharePeriod.Today) Res.string.share_text_today else Res.string.share_text_week, focusTime)
    }
    val chooserTitle = stringResource(Res.string.share_chooser_title)
    val cardDescription = if (focusMillis == 0L) {
        stringResource(Res.string.share_card_empty)
    } else {
        stringResource(Res.string.share_card_description, periodName, focusTime, statistics.currentStreakDays)
    }
    val shareFailed = stringResource(Res.string.share_failed)
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
            Text(stringResource(Res.string.share_title), style = MaterialTheme.typography.titleLarge)
            val controls = Modifier.widthIn(max = 360.dp).fillMaxWidth()
            OutlinedTextField(
                value = role,
                onValueChange = { role = it.take(MAX_ROLE_LENGTH) },
                label = { Text(stringResource(Res.string.share_role_label)) },
                placeholder = { Text(stringResource(Res.string.share_role_placeholder)) },
                supportingText = { if (roleMissing) Text(stringResource(Res.string.share_role_required)) },
                isError = false,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                modifier = controls.testTag("shareRole"),
            )
            Row(controls, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (cardPhoto.canTakePhoto) {
                    OutlinedButton(
                        onClick = cardPhoto.takePhoto,
                        modifier = Modifier.weight(1f).testTag("sharePhotoCamera"),
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        FittedLabel(stringResource(Res.string.share_take_photo), Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedButton(
                    onClick = cardPhoto.choosePhoto,
                    modifier = Modifier.weight(1f).testTag("sharePhotoPick"),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    FittedLabel(stringResource(Res.string.share_choose_photo), Modifier.padding(start = 8.dp))
                }
            }
            if (photo != null) {
                TextButton(onClick = cardPhoto.remove) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Text(stringResource(Res.string.share_remove_photo), modifier = Modifier.padding(start = 8.dp))
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
                                    ) Res.string.share_period_today else Res.string.share_period_week,
                                ),
                            )
                        },
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(controls) {
                ShareFormat.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = format == option,
                        onClick = { format = option },
                        shape = SegmentedButtonDefaults.itemShape(index, ShareFormat.entries.size),
                        icon = {},
                        label = {
                            val label = if (option == ShareFormat.Post) Res.string.share_format_post else Res.string.share_format_story
                            FittedLabel(stringResource(label))
                        },
                        modifier = Modifier.testTag("shareFormat${option.name}"),
                    )
                }
            }
            FocusShareCard(
                statistics = statistics,
                period = period,
                role = role,
                photo = photo,
                format = format,
                // A story preview is tall: keep it narrower so the whole card fits on screen.
                modifier = Modifier
                    .widthIn(max = if (format == ShareFormat.Story) 280.dp else 360.dp)
                    .fillMaxWidth()
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
                            sharer.share(exportLayer.toImageBitmap(), shareText, chooserTitle)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            platform.logError(TAG, "Could not share the statistics image", e)
                            platform.showMessage(shareFailed)
                        } finally {
                            sharing = false
                        }
                    }
                },
                enabled = !sharing && !roleMissing,
                modifier = controls.testTag("shareButton"),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(Res.string.share_button), modifier = Modifier.padding(start = 8.dp))
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
