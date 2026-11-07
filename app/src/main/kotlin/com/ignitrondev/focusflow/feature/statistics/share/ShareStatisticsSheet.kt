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

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ignitrondev.focusflow.R
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.ui.durationText
import com.ignitrondev.focusflow.domain.statistics.FocusStatistics
import com.ignitrondev.focusflow.platform.ShareImage
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Width of the shared image in pixels: sharp on social feeds whatever the screen density. */
private const val EXPORT_WIDTH_PX = 1080f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStatisticsSheet(statistics: FocusStatistics, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(SharePeriod.Week) }
    var sharing by remember { mutableStateOf(false) }
    val exportLayer = rememberGraphicsLayer()

    val focusTime = durationText(if (period == SharePeriod.Today) statistics.today.focusMillis else statistics.weekFocusMillis)
    val periodName = stringResource(if (period == SharePeriod.Today) R.string.share_period_today else R.string.share_period_week)
    val shareText = stringResource(if (period == SharePeriod.Today) R.string.share_text_today else R.string.share_text_week, focusTime)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val cardDescription = stringResource(R.string.share_card_description, periodName, focusTime, statistics.currentStreakDays)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
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
                modifier = Modifier
                    .widthIn(max = 360.dp)
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
                    scope.launch {
                        try {
                            val image = exportLayer.toImageBitmap().asAndroidBitmap()
                            val uri = withContext(Dispatchers.IO) { ShareImage.export(context, image) }
                            context.startActivity(ShareImage.chooser(uri, shareText, chooserTitle))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("ShareStatistics", "Could not share the statistics image", e)
                            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                        } finally {
                            sharing = false
                        }
                    }
                },
                enabled = !sharing,
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().testTag("shareButton"),
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
