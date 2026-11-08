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
package com.ignitrondev.focusflow.feature.achievement

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignitrondev.focusflow.domain.achievement.AchievementDefinition
import com.ignitrondev.focusflow.resources.Res
import com.ignitrondev.focusflow.resources.achievement_tier_bronze
import com.ignitrondev.focusflow.resources.achievement_tier_gold
import com.ignitrondev.focusflow.resources.achievement_tier_legendary
import com.ignitrondev.focusflow.resources.achievement_tier_silver
import com.ignitrondev.focusflow.resources.achievement_title_focus_hours_1
import com.ignitrondev.focusflow.resources.achievement_title_focus_hours_10
import com.ignitrondev.focusflow.resources.achievement_title_focus_hours_100
import com.ignitrondev.focusflow.resources.achievement_title_focus_hours_25
import com.ignitrondev.focusflow.resources.achievement_title_sessions_1
import com.ignitrondev.focusflow.resources.achievement_title_sessions_10
import com.ignitrondev.focusflow.resources.achievement_title_sessions_100
import com.ignitrondev.focusflow.resources.achievement_title_sessions_25
import com.ignitrondev.focusflow.resources.achievement_title_sessions_50
import com.ignitrondev.focusflow.resources.achievement_title_streak_14
import com.ignitrondev.focusflow.resources.achievement_title_streak_3
import com.ignitrondev.focusflow.resources.achievement_title_streak_30
import com.ignitrondev.focusflow.resources.achievement_title_streak_7
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource

/**
 * The metal of a badge, from the first step of a goal to its rarest one. The colors are fixed,
 * like real medals, so a badge looks the same in every theme; only the [ink] used for text and
 * icons on the card switches between a deep and a light shade to stay readable.
 */
internal enum class AchievementTier(
    val label: StringResource,
    private val highlight: Color,
    private val shade: Color,
    private val deep: Color,
    val accent: Color,
) {
    Bronze(Res.string.achievement_tier_bronze, Color(0xFFE6B48C), Color(0xFF9A6440), Color(0xFF8A5530), Color(0xFFC98A5E)),
    Silver(Res.string.achievement_tier_silver, Color(0xFFE3E7EE), Color(0xFF8A94A6), Color(0xFF545E70), Color(0xFFA9B3C2)),
    Gold(Res.string.achievement_tier_gold, Color(0xFFF7D774), Color(0xFFB98314), Color(0xFF735200), Color(0xFFE8B931)),
    Legendary(Res.string.achievement_tier_legendary, Color(0xFFFFA27A), Color(0xFFC2410C), Color(0xFFA8370A), Color(0xFFFF6A3D)),
    ;

    /** The rim, lit from the top. */
    val rim: List<Color> get() = listOf(highlight, shade)

    /** Text and icon color on a card: the light metal on dark cards, a deep shade on light ones. */
    fun ink(darkTheme: Boolean): Color = if (darkTheme) highlight else deep
}

/** Name, metal and symbol of one achievement. Names stay in English in every language. */
internal class AchievementLook(val title: StringResource, val tier: AchievementTier, val icon: ImageVector)

internal fun AchievementDefinition.look(): AchievementLook = looks.getValue(id)

internal val looks: Map<String, AchievementLook> = mapOf(
    "sessions_1" to AchievementLook(Res.string.achievement_title_sessions_1, AchievementTier.Bronze, Icons.Rounded.Timer),
    "sessions_10" to AchievementLook(Res.string.achievement_title_sessions_10, AchievementTier.Silver, Icons.Rounded.DoneAll),
    "sessions_25" to AchievementLook(Res.string.achievement_title_sessions_25, AchievementTier.Silver, Icons.Rounded.Layers),
    "sessions_50" to AchievementLook(Res.string.achievement_title_sessions_50, AchievementTier.Gold, Icons.Rounded.Stars),
    "sessions_100" to AchievementLook(Res.string.achievement_title_sessions_100, AchievementTier.Legendary, Icons.Rounded.MilitaryTech),
    "streak_3" to AchievementLook(Res.string.achievement_title_streak_3, AchievementTier.Bronze, Icons.Rounded.LocalFireDepartment),
    "streak_7" to AchievementLook(Res.string.achievement_title_streak_7, AchievementTier.Silver, Icons.Rounded.DateRange),
    "streak_14" to AchievementLook(Res.string.achievement_title_streak_14, AchievementTier.Gold, Icons.Rounded.Whatshot),
    "streak_30" to AchievementLook(Res.string.achievement_title_streak_30, AchievementTier.Legendary, Icons.Rounded.WorkspacePremium),
    "focus_hours_1" to AchievementLook(Res.string.achievement_title_focus_hours_1, AchievementTier.Bronze, Icons.Rounded.HourglassTop),
    "focus_hours_10" to AchievementLook(Res.string.achievement_title_focus_hours_10, AchievementTier.Silver, Icons.Rounded.TrackChanges),
    "focus_hours_25" to AchievementLook(Res.string.achievement_title_focus_hours_25, AchievementTier.Gold, Icons.Rounded.Psychology),
    "focus_hours_100" to AchievementLook(Res.string.achievement_title_focus_hours_100, AchievementTier.Legendary, Icons.Rounded.Landscape),
)

/**
 * A hexagonal badge. Earned: a metal rim around a dark core with the symbol in the metal's color.
 * Not earned yet: a muted rim, a dashed inner line and a small lock. Decorative only; the card
 * around it carries the text for screen readers.
 */
@Composable
internal fun HexBadge(icon: ImageVector, tier: AchievementTier, earned: Boolean, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    val scheme = MaterialTheme.colorScheme
    val darkTheme = scheme.surface.luminance() < 0.5f
    val lockedRim = if (tier == AchievementTier.Legendary) {
        lerp(scheme.surfaceContainerHighest, tier.accent, 0.3f)
    } else {
        scheme.surfaceContainerHighest
    }
    val core = scheme.surfaceContainerLowest
    val symbol = if (earned) tier.ink(darkTheme) else scheme.onSurfaceVariant
    val line = symbol.copy(alpha = 0.45f)
    Box(modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val rim = hexagon(center, radius)
            if (earned) {
                drawPath(rim, Brush.verticalGradient(tier.rim, startY = center.y - radius, endY = center.y + radius))
            } else {
                drawPath(rim, lockedRim)
            }
            drawPath(hexagon(center, radius * CORE_RATIO), core)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            drawPath(
                hexagon(center, radius * INNER_LINE_RATIO),
                line,
                style = Stroke(width = 1.dp.toPx(), pathEffect = if (earned) null else dash),
            )
        }
        Icon(icon, contentDescription = null, tint = symbol, modifier = Modifier.align(Alignment.Center).size(size * 0.34f))
        if (!earned) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = -(size * 0.04f))
                    .size(size * 0.32f)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh)
                    .border(1.dp, scheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(size * 0.16f))
            }
        }
    }
}

/** A regular hexagon with a point at the top. */
private fun hexagon(center: Offset, radius: Float): Path = Path().apply {
    for (corner in 0 until 6) {
        val angle = (-90.0 + 60.0 * corner) * PI / 180.0
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (corner == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private const val CORE_RATIO = 0.78f
private const val INNER_LINE_RATIO = 0.64f
