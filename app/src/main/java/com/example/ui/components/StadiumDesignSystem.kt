package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.StadiumDisplayFont
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object StadiumMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val CrispOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 720f,
    )
    val TactileBounce = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 560f,
    )
    val ExpressivePop = spring<Float>(
        dampingRatio = 0.56f,
        stiffness = 460f,
    )
    val PillSlideSpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = 580f,
    )
}

/**
 * High-speed, crisp single-Animatable staggered entrance choreography.
 * Uses GPU-only graphicsLayer transforms with tight 22ms stagger (capped at index 4)
 * so menus feel immediate, airy, and flagship-grade instead of sluggish.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    offsetDp: Dp = 12.dp,
    delayStepMs: Long = 22L,
): Modifier = composed {
    val density = LocalDensity.current
    val initialPx = remember(density, offsetDp) { with(density) { offsetDp.toPx() } }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(index) {
        val cappedIndex = index.coerceIn(0, 4)
        if (cappedIndex > 0) delay(cappedIndex * delayStepMs)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.86f,
                stiffness = 620f,
            ),
        )
    }

    this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = initialPx * (1f - p)
        val sc = 0.985f + 0.015f * p
        scaleX = sc
        scaleY = sc
    }
}

/**
 * Zero-recomposition ambient floodlit stadium atmosphere canvas.
 * Radial floodlight shaders are cached once via [drawWithCache] and shifted purely
 * in the GPU draw phase — zero per-frame allocations and zero recompositions.
 */
@Composable
fun StadiumAtmosphereBackground(
    modifier: Modifier = Modifier,
    accentColor: Color = LocalStadiumColors.current.sixGold,
    secondaryColor: Color = LocalStadiumColors.current.fourMint,
    content: @Composable BoxScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val animatedPrimary by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 360, easing = StadiumMotion.CrispOut),
        label = "atmo_primary",
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 360, easing = StadiumMotion.CrispOut),
        label = "atmo_secondary",
    )

    val infinite = rememberInfiniteTransition(label = "stadium_atmosphere")
    // Kept as State<Float> (NOT delegated with `by`) so reading occurs strictly in draw phase!
    val beamDriftState = infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floodlight_drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(cs.background)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val leftBase = Offset(w * 0.14f, h * 0.02f)
                val rightBase = Offset(w * 0.88f, h * 0.12f)
                val leftRadius = w * 0.82f
                val rightRadius = w * 0.74f

                val leftBrush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.13f),
                        animatedPrimary.copy(alpha = 0.035f),
                        Color.Transparent,
                    ),
                    center = leftBase,
                    radius = leftRadius,
                )
                val rightBrush = Brush.radialGradient(
                    colors = listOf(
                        animatedSecondary.copy(alpha = 0.11f),
                        animatedSecondary.copy(alpha = 0.025f),
                        Color.Transparent,
                    ),
                    center = rightBase,
                    radius = rightRadius,
                )
                val arcColor = Color.White.copy(alpha = 0.032f)
                val arcCenter = Offset(w * 0.5f, h * 0.96f)
                val outerArcR = w * 0.72f
                val innerArcR = w * 0.44f
                val driftAmplitude = w * 0.045f

                onDrawBehind {
                    val dx = beamDriftState.value * driftAmplitude
                    translate(left = dx) {
                        drawCircle(
                            brush = leftBrush,
                            radius = leftRadius,
                            center = leftBase,
                        )
                    }
                    translate(left = -dx) {
                        drawCircle(
                            brush = rightBrush,
                            radius = rightRadius,
                            center = rightBase,
                        )
                    }
                    drawCircle(
                        color = arcColor,
                        radius = outerArcR,
                        center = arcCenter,
                        style = Stroke(width = 1.4f),
                    )
                    drawCircle(
                        color = arcColor,
                        radius = innerArcR,
                        center = arcCenter,
                        style = Stroke(width = 1f),
                    )
                }
            },
        content = content,
    )
}

/**
 * Signature Broadcast Pavilion Card optimized for ultra-fast menu rendering:
 * - Zero press-animation allocations on non-clickable display cards.
 * - Single selection interpolation fraction instead of 4 separate color animations.
 * - Crisp top-lit specular border rim with smooth tactile press feedback.
 */
@Composable
fun BroadcastCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    accentColor: Color? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val effectiveAccent = accentColor ?: cs.primary

    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160, easing = StadiumMotion.CrispOut),
        label = "card_sel",
    )

    val topFill = lerp(
        stadium.glassTop.copy(alpha = 0.92f),
        effectiveAccent.copy(alpha = 0.22f),
        selectionProgress,
    )
    val bottomFill = lerp(
        stadium.glassBottom.copy(alpha = 0.96f),
        effectiveAccent.copy(alpha = 0.08f),
        selectionProgress,
    )
    val rimTopColor = lerp(
        accentColor?.copy(alpha = 0.48f) ?: stadium.rimTop,
        effectiveAccent.copy(alpha = 0.94f),
        selectionProgress,
    )
    val rimBottomColor = lerp(
        stadium.rimBottom,
        effectiveAccent.copy(alpha = 0.32f),
        selectionProgress,
    )

    val interactiveMod = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale = animateFloatAsState(
            targetValue = if (isPressed && enabled) 0.972f else 1f,
            animationSpec = StadiumMotion.SnappySpring,
            label = "broadcast_card_scale",
        )
        Modifier
            .graphicsLayer {
                val s = pressScale.value
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topFill, bottomFill)))
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                brush = Brush.verticalGradient(listOf(rimTopColor, rimBottomColor)),
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = effectiveAccent),
                onClick = onClick,
            )
    } else {
        Modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topFill, bottomFill)))
            .border(
                width = if (selected) 1.4.dp else 1.dp,
                brush = Brush.verticalGradient(listOf(rimTopColor, rimBottomColor)),
                shape = shape,
            )
    }

    Box(
        modifier = modifier
            .then(interactiveMod)
            .padding(contentPadding),
        content = content,
    )
}

@Immutable
data class SegmentedBarOption(
    val label: String,
    val icon: ImageVector? = null,
    val accent: Color,
    val brightAccent: Color,
    val testTag: String,
)

/**
 * Hardware-accelerated Apple/Broadcast-grade Segmented Control with a spring-sliding
 * luminous glass pill indicator. Reads pill offset strictly in layout/draw phases.
 */
@Composable
fun StadiumSegmentedBar(
    options: List<SegmentedBarOption>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
) {
    if (options.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val density = LocalDensity.current

    val activeOption = options[selectedIndex.coerceIn(0, options.lastIndex)]
    val pillProgress = animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, options.lastIndex).toFloat(),
        animationSpec = StadiumMotion.PillSlideSpring,
        label = "segmented_pill_pos",
    )
    val pillAccent by animateColorAsState(
        targetValue = activeOption.accent,
        animationSpec = tween(durationMillis = 180, easing = StadiumMotion.CrispOut),
        label = "segmented_pill_accent",
    )

    val trackPadding = 4.dp
    val outerShape = RoundedCornerShape(15.dp)
    val pillShape = RoundedCornerShape(11.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(outerShape)
            .background(cs.background.copy(alpha = 0.72f))
            .border(1.dp, stadium.rimTop.copy(alpha = 0.25f), outerShape)
            .padding(trackPadding),
    ) {
        val innerWidthPx = with(density) { maxWidth.toPx() }
        val slotWidthPx = innerWidthPx / options.size
        val slotWidthDp = maxWidth / options.size

        // Spring-sliding glass pill background (GPU/layout offset only — zero recomposition)
        Box(
            modifier = Modifier
                .offset { IntOffset((pillProgress.value * slotWidthPx).roundToInt(), 0) }
                .width(slotWidthDp)
                .fillMaxHeight()
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            pillAccent.copy(alpha = 0.28f),
                            pillAccent.copy(alpha = 0.14f),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            pillAccent.copy(alpha = 0.85f),
                            pillAccent.copy(alpha = 0.35f),
                        )
                    ),
                    shape = pillShape,
                ),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { idx, opt ->
                val isSelected = idx == selectedIndex
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) opt.brightAccent else cs.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 150),
                    label = "seg_txt_$idx",
                )
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(pillShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = opt.accent),
                            onClick = { if (!isSelected) onSelectIndex(idx) },
                        )
                        .testTag(opt.testTag),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (opt.icon != null) {
                        Icon(
                            imageVector = opt.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = opt.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Flagship broadcast primary CTA with metallic gradient, GPU-deferred specular sheen
 * (zero per-frame recompositions), and snappy spring press physics.
 */
@Composable
fun PremiumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    useMintPalette: Boolean = false,
    height: Dp = 60.dp,
    fontSize: TextUnit = 24.sp,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale = animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.965f else 1f,
        animationSpec = StadiumMotion.SnappySpring,
        label = "premium_btn_scale",
    )

    // Only animate sheen when button is active; read strictly inside drawWithContent!
    val sheenState = if (enabled && !loading) {
        val infinite = rememberInfiniteTransition(label = "btn_sheen")
        infinite.animateFloat(
            initialValue = -0.45f,
            targetValue = 1.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, delayMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "btn_sheen_x",
        )
    } else {
        null
    }

    val gradColors = remember(enabled, useMintPalette, cs, stadium) {
        when {
            !enabled -> listOf(cs.surfaceContainerHigh, cs.surfaceContainer)
            useMintPalette -> listOf(stadium.fourMintBright, stadium.fourMint, stadium.fourMintDeep)
            else -> listOf(stadium.sixGoldBright, stadium.sixGold, stadium.sixGoldDeep)
        }
    }
    val contentColor = when {
        !enabled -> cs.onSurfaceVariant.copy(alpha = 0.5f)
        useMintPalette -> cs.onSecondary
        else -> cs.onPrimary
    }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                val s = pressScale.value
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(Brush.horizontalGradient(gradColors))
            .drawWithContent {
                drawContent()
                val sheenPos = sheenState?.value
                if (sheenPos != null && sheenPos > -0.35f && sheenPos < 1.45f) {
                    val sheenWidth = size.width * 0.30f
                    val startX = sheenPos * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.24f),
                                Color.Transparent,
                            ),
                            start = Offset(startX - sheenWidth, 0f),
                            end = Offset(startX, size.height),
                        ),
                        size = size,
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.52f else 0.12f),
                        Color.Transparent,
                    )
                ),
                shape = shape,
            )
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 1.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Broadcast odometer digit transition for scores, runs, and counters.
 */
@Composable
fun AnimatedScoreText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontFamily = StadiumDisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize,
    ),
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 620f),
                initialOffsetY = { it / 2 },
            ) + fadeIn(tween(130))) togetherWith
                (slideOutVertically(
                    animationSpec = tween(110),
                    targetOffsetY = { -it / 2 },
                ) + fadeOut(tween(100)))
        },
        label = "odometer_score",
        modifier = modifier,
    ) { value ->
        Text(
            text = value,
            style = style,
            color = color,
        )
    }
}

/**
 * Broadcast Live indicator pill with an expanding pulsing sonar ring.
 * Pulse state is read strictly in Canvas draw phase — zero recompositions per frame.
 */
@Composable
fun BroadcastLivePill(
    label: String,
    accentColor: Color = LocalStadiumColors.current.fourMint,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "live_pill_pulse")
    val pulseState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_ring",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = cs.background.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier.size(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pulse = pulseState.value
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - pulse) * 0.55f),
                        radius = size.minDimension * (0.3f + pulse * 0.45f),
                    )
                    drawCircle(
                        color = accentColor,
                        radius = size.minDimension * 0.28f,
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Broadcast section title with a luminous vertical accent bar.
 */
@Composable
fun BroadcastSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
            )
        }
    }
}

/**
 * Zero-recomposition animated shimmer loading skeleton card for async Firestore views.
 */
@Composable
fun ShimmerSkeletonCard(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "shimmer_loading")
    val shiftState = infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_x",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceContainer)
            .drawWithContent {
                drawContent()
                val shift = shiftState.value
                val w = size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        start = Offset((shift - 0.35f) * w, 0f),
                        end = Offset(shift * w, size.height),
                    ),
                )
            }
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(20.dp)),
    )
}

private data class ConfettiPiece(
    val xFraction: Float,
    val phaseOffset: Float,
    val speed: Float,
    val widthPx: Float,
    val heightPx: Float,
    val swayAmplitude: Float,
    val spinSpeed: Float,
    val colorIndex: Int,
)

/**
 * Zero-recomposition 60-120fps stadium trophy celebration confetti overlay.
 */
@Composable
fun VictoryConfettiOverlay(
    modifier: Modifier = Modifier,
) {
    val stadium = LocalStadiumColors.current
    val palette = remember(stadium) {
        listOf(
            stadium.sixGold,
            stadium.sixGoldBright,
            stadium.fourMint,
            stadium.fourMintBright,
            Color.White,
        )
    }

    val pieces = remember {
        List(36) { idx ->
            val seed = (idx * 73 + 19) % 100
            ConfettiPiece(
                xFraction = ((idx * 37 + 11) % 96 + 2) / 100f,
                phaseOffset = (idx * 0.13f) % 1f,
                speed = 0.65f + (seed % 45) / 100f,
                widthPx = 7f + (seed % 8),
                heightPx = 12f + (seed % 10),
                swayAmplitude = 18f + (seed % 24),
                spinSpeed = if (idx % 2 == 0) 240f else -210f,
                colorIndex = idx % 5,
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "confetti_rain")
    val clockState = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti_clock",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val clock = clockState.value
        val w = size.width
        val h = size.height
        pieces.forEach { piece ->
            val progress = (clock * piece.speed + piece.phaseOffset) % 1f
            val y = -30f + progress * (h + 60f)
            val sway = sin((progress * 2f * PI + piece.phaseOffset * 6f).toDouble()).toFloat() * piece.swayAmplitude
            val x = piece.xFraction * w + sway
            val alpha = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.82f -> (1f - progress) / 0.18f
                else -> 0.88f
            }
            val color = palette[piece.colorIndex].copy(alpha = alpha.coerceIn(0f, 0.9f))
            rotate(degrees = progress * piece.spinSpeed, pivot = Offset(x, y)) {
                drawRect(
                    color = color,
                    topLeft = Offset(x - piece.widthPx / 2f, y - piece.heightPx / 2f),
                    size = Size(piece.widthPx, piece.heightPx),
                )
            }
        }
    }
}
