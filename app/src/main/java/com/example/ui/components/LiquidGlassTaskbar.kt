package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalStadiumColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class LiquidGlassTabItem(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
    val badgeCount: Int = 0,
    val badgeColor: Color? = null,
    val testTag: String,
)

/**
 * High-performance Apple-inspired Floating Liquid Glass Pill Taskbar.
 *
 * Performance architecture:
 * - Zero recomposition during horizontal finger sliding: [pillPosition], [dragStretch],
 *   and [lensLift] are read strictly inside layout offset, graphicsLayer, and Canvas draw phases.
 * - Highlighted slot derives cleanly via [derivedStateOf] so icons/labels smoothly crossfade
 *   without redrawing the entire capsule tree on every drag pixel.
 */
@Composable
fun LiquidGlassTaskbar(
    items: List<LiquidGlassTabItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val maxIndex = (items.size - 1).coerceAtLeast(0).toFloat()
    val pillPosition = remember { Animatable(selectedIndex.toFloat()) }
    val dragStretch = remember { Animatable(0f) }
    val lensLift = remember { Animatable(0f) }

    var isDragging by remember { mutableStateOf(false) }
    var lastHapticSlot by remember { mutableIntStateOf(selectedIndex) }

    // Only triggers recomposition when the finger crosses into a new tab slot!
    val highlightedSlot by remember(items.size) {
        derivedStateOf { pillPosition.value.roundToInt().coerceIn(0, items.lastIndex) }
    }

    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            lastHapticSlot = selectedIndex
            launch {
                pillPosition.animateTo(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.70f,
                        stiffness = 540f,
                    ),
                )
            }
            launch {
                dragStretch.snapTo(0.32f)
                dragStretch.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.56f,
                        stiffness = 460f,
                    ),
                )
            }
        }
    }

    val capsuleShape = RoundedCornerShape(999.dp)
    val outerHorizontalPadding = 22.dp
    val innerTrackPadding = 5.dp
    val barHeight = 66.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerHorizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 408.dp)
                .height(barHeight)
                .shadow(
                    elevation = 20.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.78f),
                    spotColor = stadium.sixGold.copy(alpha = 0.22f),
                )
                .clip(capsuleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE0183A29),
                            Color(0xEB0A1D14),
                            Color(0xF505110B),
                        ),
                    ),
                )
                .border(
                    width = 1.15.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.14f),
                            stadium.fourMint.copy(alpha = 0.20f),
                        ),
                    ),
                    shape = capsuleShape,
                )
                .drawWithCache {
                    val topSheenBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.50f,
                    )
                    val corner = CornerRadius(size.height / 2f, size.height / 2f)
                    onDrawBehind {
                        drawRoundRect(
                            brush = topSheenBrush,
                            topLeft = Offset(2f, 1.5f),
                            size = Size(size.width - 4f, size.height * 0.48f),
                            cornerRadius = corner,
                        )
                    }
                }
                .testTag("liquid_glass_taskbar"),
        ) {
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val innerPadPx = with(density) { innerTrackPadding.toPx() }
            val trackWidthPx = (totalWidthPx - innerPadPx * 2f).coerceAtLeast(1f)
            val slotWidthPx = trackWidthPx / items.size
            val slotWidthDp = with(density) { slotWidthPx.toDp() }
            val pillHeightDp = barHeight - innerTrackPadding * 2

            // Sliding Liquid Glass Lens Pill — all animated values read strictly in layout/draw phases!
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset((innerPadPx + pillPosition.value * slotWidthPx).roundToInt(), 0)
                    }
                    .width(slotWidthDp)
                    .height(pillHeightDp)
                    .graphicsLayer {
                        val stretch = dragStretch.value.coerceIn(0f, 1f)
                        val lift = lensLift.value.coerceIn(0f, 1f)
                        scaleX = 1f + stretch * 0.12f + lift * 0.04f
                        scaleY = 1f - stretch * 0.07f + lift * 0.05f
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lift = lensLift.value.coerceIn(0f, 1f)
                    val corner = CornerRadius(size.height / 2f, size.height / 2f)

                    // Warm golden-emerald ambient refraction inside lens
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                stadium.sixGoldBright.copy(alpha = 0.34f + lift * 0.16f),
                                stadium.sixGold.copy(alpha = 0.16f + lift * 0.10f),
                                stadium.fourMint.copy(alpha = 0.09f),
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.24f),
                            radius = size.width * 0.76f,
                        ),
                        cornerRadius = corner,
                    )

                    // Frosted translucent crystal fill
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.30f + lift * 0.12f),
                                stadium.sixGold.copy(alpha = 0.18f + lift * 0.08f),
                                Color.White.copy(alpha = 0.06f),
                            ),
                        ),
                        cornerRadius = corner,
                    )

                    // Crisp top specular caustic reflection
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.46f + lift * 0.14f),
                                Color.Transparent,
                            ),
                            startY = 2f,
                            endY = size.height * 0.44f,
                        ),
                        topLeft = Offset(size.width * 0.09f, 2f),
                        size = Size(size.width * 0.82f, size.height * 0.34f),
                        cornerRadius = corner,
                    )

                    // Refractive rim stroke
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.76f + lift * 0.20f),
                                stadium.sixGoldBright.copy(alpha = 0.54f),
                                Color.White.copy(alpha = 0.16f),
                            ),
                        ),
                        cornerRadius = corner,
                        style = Stroke(width = 1.25.dp.toPx()),
                    )
                }
            }

            // Interactive Touch & Horizontal Slide Surface
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = innerTrackPadding)
                    .pointerInput(items.size, hapticsEnabled, slotWidthPx, selectedIndex) {
                        detectTapGestures { offset ->
                            val tappedIndex = (offset.x / slotWidthPx)
                                .toInt()
                                .coerceIn(0, items.lastIndex)
                            if (tappedIndex != selectedIndex) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelectIndex(tappedIndex)
                            } else {
                                scope.launch {
                                    lensLift.animateTo(0.55f, spring(stiffness = Spring.StiffnessHigh))
                                    lensLift.animateTo(0f, spring(dampingRatio = 0.60f, stiffness = 460f))
                                }
                            }
                        }
                    }
                    .pointerInput(items.size, hapticsEnabled, slotWidthPx, selectedIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = { startOffset ->
                                isDragging = true
                                val startSlot = ((startOffset.x - slotWidthPx * 0.5f) / slotWidthPx)
                                    .coerceIn(0f, maxIndex)
                                scope.launch {
                                    launch {
                                        lensLift.animateTo(
                                            targetValue = 1f,
                                            animationSpec = spring(dampingRatio = 0.70f, stiffness = 620f),
                                        )
                                    }
                                    launch {
                                        pillPosition.animateTo(
                                            targetValue = startSlot,
                                            animationSpec = spring(dampingRatio = 0.88f, stiffness = 980f),
                                        )
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaSlots = dragAmount / slotWidthPx
                                val rawNext = pillPosition.value + deltaSlots
                                val clampedNext = when {
                                    rawNext < 0f -> (rawNext * 0.24f).coerceAtLeast(-0.16f)
                                    rawNext > maxIndex -> maxIndex + ((rawNext - maxIndex) * 0.24f).coerceAtMost(0.16f)
                                    else -> rawNext
                                }
                                val velBoost = (abs(dragAmount) / 28f).coerceIn(0.14f, 1f)
                                val nearestSlot = clampedNext.roundToInt().coerceIn(0, items.lastIndex)
                                if (nearestSlot != lastHapticSlot) {
                                    lastHapticSlot = nearestSlot
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                scope.launch {
                                    pillPosition.snapTo(clampedNext)
                                    dragStretch.snapTo(velBoost)
                                }
                            },
                            onDragEnd = {
                                val targetSlot = pillPosition.value.roundToInt().coerceIn(0, items.lastIndex)
                                isDragging = false
                                if (targetSlot != selectedIndex) {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onSelectIndex(targetSlot)
                                }
                                scope.launch {
                                    launch {
                                        pillPosition.animateTo(
                                            targetValue = targetSlot.toFloat(),
                                            animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
                                        )
                                    }
                                    launch {
                                        dragStretch.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.54f, stiffness = 420f),
                                        )
                                    }
                                    launch {
                                        lensLift.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.64f, stiffness = 440f),
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                val targetSlot = selectedIndex.coerceIn(0, items.lastIndex)
                                isDragging = false
                                scope.launch {
                                    launch { pillPosition.animateTo(targetSlot.toFloat(), spring(dampingRatio = 0.70f, stiffness = 520f)) }
                                    launch { dragStretch.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = 420f)) }
                                    launch { lensLift.animateTo(0f, spring(dampingRatio = 0.66f, stiffness = 440f)) }
                                }
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val isActive = index == highlightedSlot
                    val tintColor by animateColorAsState(
                        targetValue = if (isActive) stadium.sixGoldBright else cs.onSurfaceVariant.copy(alpha = 0.78f),
                        animationSpec = tween(durationMillis = 150),
                        label = "tab_tint_$index",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else cs.onSurfaceVariant.copy(alpha = 0.70f),
                        animationSpec = tween(durationMillis = 150),
                        label = "tab_lbl_$index",
                    )
                    val iconScale = animateFloatAsState(
                        targetValue = if (isActive) 1.13f else 1f,
                        animationSpec = StadiumMotion.TactileBounce,
                        label = "tab_scale_$index",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                role = Role.Tab
                                selected = index == selectedIndex
                                contentDescription = item.contentDescription
                            }
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = tintColor,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            val s = iconScale.value
                                            scaleX = s
                                            scaleY = s
                                        },
                                )
                                if (item.badgeCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = item.badgeColor ?: stadium.wicketRed,
                                        modifier = Modifier
                                            .offset(x = 8.dp, y = (-4).dp)
                                            .size(16.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (item.badgeCount > 9) "9+" else "${item.badgeCount}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = labelColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
