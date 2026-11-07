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
package com.ignitrondev.focusflow.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A one-line label that shrinks to fit its slot instead of being cut off ("Statisti…") or broken
 * mid-word, which happens in navigation items and segmented buttons at large font sizes. At the
 * default font size it renders exactly like `Text`.
 */
@Composable
fun FittedLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center, minFontSize: TextUnit = 8.sp) {
    val style = LocalTextStyle.current
    val maxSize = if (style.fontSize.isSpecified) style.fontSize else 14.sp
    // StepBased requires min <= max. A caller can ask for a lower floor than the current style.
    val minSize = if (minFontSize < maxSize) minFontSize else maxSize
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = LocalContentColor.current, textAlign = textAlign)),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = maxSize),
    )
}
