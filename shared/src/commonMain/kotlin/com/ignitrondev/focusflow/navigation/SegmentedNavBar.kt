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
package com.ignitrondev.focusflow.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.lerp as lerpStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavDestination
import com.ignitrondev.focusflow.app.LocalAppDependencies
import com.ignitrondev.focusflow.core.designsystem.component.FittedLabel
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowShapes
import com.ignitrondev.focusflow.core.designsystem.theme.FocusFlowSpacing
import com.ignitrondev.focusflow.core.designsystem.theme.FocusTokens
import com.ignitrondev.focusflow.core.designsystem.theme.NavigationCapsule
import com.ignitrondev.focusflow.core.designsystem.theme.OnNavigationCapsule
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Phone navbar. The selected tab is still a horizontal row inside the orange capsule and inactive
 * tabs are still vertical columns; hit targets use that layout immediately. A single capsule and
 * the icon/label arrangement ease from the previous geometry to the new one (200ms, ease-out).
 * Navigation is not delayed. Wide-screen rails do not use this bar.
 */
@Composable
internal fun SegmentedNavBar(currentDestination: NavDestination?, onNavigate: (TopLevelDestination) -> Unit) {
    val animationsDisabled = LocalAppDependencies.current.platform.rememberSystemAnimationsDisabled()
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val shape = FocusFlowShapes.navContainer
    val selected = currentDestination.selectedTopLevel()
    val anchors = remember { AnchorCollector() }
    val motion = remember { NavMotion() }
    var visualsReady by remember { mutableStateOf(false) }

    LaunchedEffect(anchors.published, animationsDisabled) {
        val set = anchors.published ?: return@LaunchedEffect
        val selectionChanged = motion.placedSelection != null && motion.placedSelection != set.selected
        if (selectionChanged && !animationsDisabled) {
            motion.placedSelection = set.selected
            visualsReady = true
            motion.animateTo(set)
        } else {
            motion.snapTo(set)
            motion.placedSelection = set.selected
            visualsReady = true
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(start = FocusFlowSpacing.sm, end = FocusFlowSpacing.sm, top = FocusFlowSpacing.xs, bottom = FocusFlowSpacing.sm),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val inactiveSlots = TopLevelDestination.entries.size - 1
            val activeMaxWidth = (maxWidth - InnerHorizontal * 2 - MinTabSlot * inactiveSlots).coerceAtLeast(MinTabSlot)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(
                        elevation = if (dark) 8.dp else 16.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = if (dark) 0.40f else 0.10f),
                        spotColor = Color.Black.copy(alpha = if (dark) 0.50f else 0.14f),
                    )
                    .clip(shape)
                    .background(scheme.surfaceContainerHigh)
                    .testTag("nav_bar")
                    .padding(horizontal = InnerHorizontal, vertical = FocusFlowSpacing.xs),
            ) {
                if (visualsReady) {
                    MovingCapsule(motion)
                    TopLevelDestination.entries.forEach { destination ->
                        val tab = motion.tabs.getValue(destination)
                        MovingFace(tab, destination)
                    }
                }
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    TopLevelDestination.entries.forEach { destination ->
                        val itemSelected = destination == selected
                        SegmentedNavItem(
                            destination = destination,
                            selected = itemSelected,
                            activeMaxWidth = activeMaxWidth,
                            visualsReady = visualsReady,
                            onPositioned = { anchors.report(selected, destination, it) },
                            onClick = { onNavigate(destination) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovingCapsule(motion: NavMotion) {
    val density = LocalDensity.current
    val x = motion.capsuleX.value
    val width = motion.capsuleWidth.value
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), 0) }
            .width(with(density) { width.toDp() })
            .fillMaxHeight()
            .clip(FocusFlowShapes.navCapsule)
            .background(NavigationCapsule)
            .testTag(NavCapsuleTag),
    )
}

@Composable
private fun MovingFace(tab: TabMotion, destination: TopLevelDestination) {
    val density = LocalDensity.current
    val x = tab.x.value
    val width = tab.width.value
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), 0) }
            .width(with(density) { width.toDp() })
            .fillMaxHeight()
            .clearAndSetSemantics {},
    ) {
        TabFace(destination, tab.emphasis.value)
    }
}

/**
 * One icon and one label. Emphasis 0 is the inactive column; emphasis 1 is the selected row.
 * Positions are interpolated so the swap is not a one-frame jump, and screen readers never see
 * this copy — the hit target owns the semantics.
 */
@Composable
private fun TabFace(destination: TopLevelDestination, emphasis: Float) {
    val t = emphasis.coerceIn(0f, 1f)
    val label = stringResource(destination.labelRes)
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val inactive = if (dark) FocusTokens.textSecondary else scheme.onSurfaceVariant
    val color = if (t <= 0f) inactive else if (t >= 1f) OnNavigationCapsule else lerpColor(inactive, OnNavigationCapsule, t)
    val inactiveStyle = MaterialTheme.typography.labelMedium
    val activeStyle = MaterialTheme.typography.labelLarge
    val style = if (t <= 0f) inactiveStyle else if (t >= 1f) activeStyle else lerpStyle(inactiveStyle, activeStyle, t)
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Icon(
                imageVector = destination.unselectedIcon,
                contentDescription = null,
                modifier = Modifier.sizeHidden(1f - t),
                tint = color,
            )
            Icon(
                imageVector = destination.selectedIcon,
                contentDescription = null,
                modifier = Modifier.sizeHidden(t),
                tint = color,
            )
            CompositionLocalProvider(LocalContentColor provides color, LocalTextStyle provides style) {
                FittedLabel(label, minFontSize = 4.sp)
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val iconPx = IconSize.roundToPx()
        val gap = ActiveContentGap.roundToPx()
        val padActive = FocusFlowSpacing.sm.roundToPx()
        val padInactive = InactiveHorizontalPadding.roundToPx()
        val inactiveLabelMax = (width - padInactive * 2).coerceAtLeast(0)
        val activeLabelMax = (width - padActive * 2 - iconPx - gap).coerceAtLeast(0)
        val labelMax = lerp(inactiveLabelMax.toFloat(), activeLabelMax.toFloat(), t).roundToInt().coerceAtLeast(0)
        val labelPlaceable = measurables[2].measure(Constraints(maxWidth = labelMax))
        val iconConstraints = Constraints.fixed(iconPx, iconPx)
        val unselectedIcon = measurables[0].measure(iconConstraints)
        val selectedIcon = measurables[1].measure(iconConstraints)

        val columnHeight = iconPx + labelPlaceable.height
        val columnIconX = (width - iconPx) / 2
        val columnIconY = (height - columnHeight) / 2
        val columnLabelX = (width - labelPlaceable.width) / 2
        val columnLabelY = columnIconY + iconPx

        val rowWidth = iconPx + gap + labelPlaceable.width
        val innerWidth = (width - padActive * 2).coerceAtLeast(0)
        val rowStart = padActive + ((innerWidth - rowWidth) / 2).coerceAtLeast(0)
        val ltr = layoutDirection == LayoutDirection.Ltr
        val rowIconX = if (ltr) rowStart else width - rowStart - iconPx
        val rowLabelX = if (ltr) rowStart + iconPx + gap else rowIconX - gap - labelPlaceable.width
        val rowIconY = (height - iconPx) / 2
        val rowLabelY = (height - labelPlaceable.height) / 2

        val iconX = lerpPx(columnIconX, rowIconX, t)
        val iconY = lerpPx(columnIconY, rowIconY, t)
        val labelX = lerpPx(columnLabelX, rowLabelX, t)
        val labelY = lerpPx(columnLabelY, rowLabelY, t)
        layout(width, height) {
            if (t < 1f) unselectedIcon.place(iconX, iconY)
            if (t > 0f) selectedIcon.place(iconX, iconY)
            labelPlaceable.place(labelX, labelY)
        }
    }
}

@Composable
private fun RowScope.SegmentedNavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    activeMaxWidth: Dp,
    visualsReady: Boolean,
    onPositioned: (Rect) -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val capsule = FocusFlowShapes.navCapsule
    val label = stringResource(destination.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusBorder = if (focused) 2.dp else 0.dp
    val press = ripple(color = Color.White.copy(alpha = 0.08f))
    val inactive = if (dark) FocusTokens.textSecondary else scheme.onSurfaceVariant
    // Hide the painted glyphs once the moving layer is up. Drawing nothing (rather than alpha 0)
    // keeps this node's text in the semantics tree for TalkBack.
    val glyphs = if (visualsReady) Modifier.undrawn() else Modifier
    // Selectable stays on this box for every selection. Swapping Row and Column on the same node
    // would dispose the focus target and drop keyboard focus onto whatever is underneath.
    val slot = if (selected) {
        Modifier.fillMaxHeight().widthIn(min = MinTabSlot, max = activeMaxWidth)
    } else {
        Modifier.weight(1f).fillMaxHeight().widthIn(min = MinTabSlot)
    }
    Box(
        modifier = Modifier.onGloballyPositioned { onPositioned(it.boundsInParent()) }.then(slot),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(capsule)
                .border(focusBorder, FocusTokens.focusRing, capsule)
                .then(if (selected && !visualsReady) Modifier.background(NavigationCapsule) else Modifier)
                .selectable(
                    selected = selected,
                    interactionSource = interaction,
                    indication = press,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .testTag("nav_${destination.name}"),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Row(
                    modifier = Modifier.padding(horizontal = FocusFlowSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = null,
                        tint = OnNavigationCapsule,
                        modifier = glyphs,
                    )
                    Spacer(Modifier.width(ActiveContentGap))
                    CompositionLocalProvider(
                        LocalContentColor provides OnNavigationCapsule,
                        LocalTextStyle provides MaterialTheme.typography.labelLarge,
                    ) {
                        FittedLabel(label, modifier = glyphs, minFontSize = 4.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = InactiveHorizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = destination.unselectedIcon,
                        contentDescription = null,
                        tint = inactive,
                        modifier = glyphs,
                    )
                    CompositionLocalProvider(
                        LocalContentColor provides inactive,
                        LocalTextStyle provides MaterialTheme.typography.labelMedium,
                    ) {
                        // 4sp floor: a 48dp slot still fits the whole word at 200% font size.
                        FittedLabel(label, modifier = glyphs, minFontSize = 4.sp)
                    }
                }
            }
        }
    }
}

/** Icon slot. Alpha lives on the icon so a zero-emphasis glyph does not get its own semantics node. */
private fun Modifier.sizeHidden(alpha: Float): Modifier = size(IconSize).graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f) }

private fun Modifier.undrawn(): Modifier = drawWithContent {
    // The moving layer paints the glyphs. Skipping drawContent keeps the text in semantics.
}

private fun lerpPx(start: Int, end: Int, fraction: Float): Int = lerp(start.toFloat(), end.toFloat(), fraction).roundToInt()

private fun NavDestination?.selectedTopLevel(): TopLevelDestination? = TopLevelDestination.entries.firstOrNull { isInHierarchy(it) }

private data class TabAnchor(val x: Int, val width: Int)

private data class AnchorSet(val selected: TopLevelDestination, val bounds: Map<TopLevelDestination, TabAnchor>)

/**
 * Hit-target positions for one selection. Published only after every tab has been measured for
 * that selection, so the capsule never animates toward a stale layout.
 */
private class AnchorCollector {
    var published by mutableStateOf<AnchorSet?>(null)
    private var selection: TopLevelDestination? = null
    private val pending = mutableMapOf<TopLevelDestination, TabAnchor>()

    fun report(selected: TopLevelDestination?, destination: TopLevelDestination, rect: Rect) {
        if (selected == null || rect.width <= 0f) return
        if (selection != selected) {
            selection = selected
            pending.clear()
        }
        pending[destination] = TabAnchor(rect.left.roundToInt(), rect.width.roundToInt())
        if (pending.size == TopLevelDestination.entries.size && pending.values.all { it.width > 0 }) {
            val next = AnchorSet(selected, pending.toMap())
            if (next != published) published = next
        }
    }
}

private class TabMotion {
    val x = Animatable(0f)
    val width = Animatable(0f)
    val emphasis = Animatable(0f)
}

private class NavMotion {
    var placedSelection: TopLevelDestination? = null
    val capsuleX = Animatable(0f)
    val capsuleWidth = Animatable(0f)
    val tabs: Map<TopLevelDestination, TabMotion> = TopLevelDestination.entries.associateWith { TabMotion() }

    suspend fun snapTo(set: AnchorSet) {
        apply(set) { anim, target -> anim.snapTo(target) }
    }

    /** Restarts from the current values, so a newer tap replaces a tap that is still in flight. */
    suspend fun animateTo(set: AnchorSet) {
        val spec = tween<Float>(durationMillis = TabSwitchMillis, easing = TabSwitchEasing)
        apply(set) { anim, target -> anim.animateTo(target, spec) }
    }

    private suspend fun apply(set: AnchorSet, update: suspend (Animatable<Float, AnimationVector1D>, Float) -> Unit) {
        val capsule = set.bounds.getValue(set.selected)
        coroutineScope {
            TopLevelDestination.entries.forEach { destination ->
                val anchor = set.bounds.getValue(destination)
                val tab = tabs.getValue(destination)
                launch { update(tab.x, anchor.x.toFloat()) }
                launch { update(tab.width, anchor.width.toFloat()) }
                launch { update(tab.emphasis, if (destination == set.selected) 1f else 0f) }
            }
            launch { update(capsuleX, capsule.x.toFloat()) }
            launch { update(capsuleWidth, capsule.width.toFloat()) }
        }
    }
}

private val MinTabSlot = 48.dp
private val BarHeight = 72.dp
private val InnerHorizontal = 6.dp
private val IconSize = 24.dp
private val ActiveContentGap = 6.dp
private val InactiveHorizontalPadding = 2.dp
private const val TabSwitchMillis = 200
private val TabSwitchEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
internal const val NavCapsuleTag = "nav_capsule"
