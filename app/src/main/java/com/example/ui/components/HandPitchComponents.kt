package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.DeliveryEvent
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont
import kotlinx.coroutines.launch

/**
 * Custom Canvas illustration of authentic Odd-Even Hand Cricket gestures:
 * 1..5 show raised fingers; 6 shows the classic upright THUMB gesture!
 */
@Composable
fun HandGestureIllustration(
    number: Int?,
    accentColor: Color,
    isMirrored: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palmColor = MaterialTheme.colorScheme.onSurface
    val palmShadow = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val gloveCuff = accentColor

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val mirror = if (isMirrored) -1f else 1f

        if (number == null) {
            // Ready fist silhouette waiting for throw
            val fistW = w * 0.46f
            val fistH = h * 0.38f
            drawRoundRect(
                color = palmShadow,
                topLeft = Offset(cx - fistW / 2f, h * 0.34f),
                size = Size(fistW, fistH),
                cornerRadius = CornerRadius(fistW * 0.35f, fistW * 0.35f),
            )
            // Wristband
            drawRoundRect(
                color = gloveCuff.copy(alpha = 0.65f),
                topLeft = Offset(cx - fistW * 0.38f, h * 0.72f),
                size = Size(fistW * 0.76f, h * 0.14f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            return@Canvas
        }

        val palmW = w * 0.44f
        val palmH = h * 0.32f
        val palmTop = h * 0.46f
        val palmLeft = cx - palmW / 2f

        // Wrist cuff
        drawRoundRect(
            color = gloveCuff,
            topLeft = Offset(cx - palmW * 0.36f, palmTop + palmH - 4f),
            size = Size(palmW * 0.72f, h * 0.13f),
            cornerRadius = CornerRadius(10f, 10f),
        )

        if (number == 6) {
            // Iconic Hand Cricket "6": Closed fist + proud raised Thumb!
            drawRoundRect(
                color = palmColor,
                topLeft = Offset(palmLeft, palmTop),
                size = Size(palmW, palmH),
                cornerRadius = CornerRadius(palmW * 0.32f, palmW * 0.32f),
            )
            // Knuckle dividers on closed fist
            for (i in 1..3) {
                val yLine = palmTop + palmH * (i * 0.25f)
                drawLine(
                    color = palmShadow.copy(alpha = 0.4f),
                    start = Offset(palmLeft + palmW * 0.18f, yLine),
                    end = Offset(palmLeft + palmW * 0.82f, yLine),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                )
            }
            // Raised Thumb (gold-rimmed highlight)
            val thumbW = w * 0.17f
            val thumbH = h * 0.34f
            val thumbX = if (isMirrored) cx + palmW * 0.08f else cx - palmW * 0.08f - thumbW
            rotate(degrees = if (isMirrored) 10f else -10f, pivot = Offset(cx, palmTop)) {
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(thumbX - 2f, palmTop - thumbH + h * 0.08f),
                    size = Size(thumbW + 4f, thumbH + 2f),
                    cornerRadius = CornerRadius(thumbW * 0.55f, thumbW * 0.55f),
                )
                drawRoundRect(
                    color = palmColor,
                    topLeft = Offset(thumbX, palmTop - thumbH + h * 0.09f),
                    size = Size(thumbW, thumbH - 2f),
                    cornerRadius = CornerRadius(thumbW * 0.5f, thumbW * 0.5f),
                )
            }
        } else {
            // Base palm
            drawRoundRect(
                color = palmColor,
                topLeft = Offset(palmLeft, palmTop),
                size = Size(palmW, palmH),
                cornerRadius = CornerRadius(palmW * 0.28f, palmW * 0.28f),
            )

            // Raised fingers for 1..5
            val fingerW = w * 0.105f
            val fingerGap = w * 0.025f
            val fingerCount = number.coerceAtMost(4)
            val totalFingersW = fingerCount * fingerW + (fingerCount - 1) * fingerGap
            val startX = cx - totalFingersW / 2f

            for (i in 0 until fingerCount) {
                val fHeight = when {
                    fingerCount == 1 -> h * 0.37f
                    fingerCount == 2 -> h * 0.35f
                    i == 1 -> h * 0.38f
                    i == 2 && fingerCount == 4 -> h * 0.36f
                    else -> h * 0.32f
                }
                val fx = startX + i * (fingerW + fingerGap)
                val tilt = when {
                    fingerCount == 2 && i == 0 -> -8f * mirror
                    fingerCount == 2 && i == 1 -> 8f * mirror
                    fingerCount >= 3 && i == 0 -> -6f
                    fingerCount >= 3 && i == fingerCount - 1 -> 6f
                    else -> 0f
                }
                rotate(degrees = tilt, pivot = Offset(fx + fingerW / 2f, palmTop + 8f)) {
                    drawRoundRect(
                        color = palmColor,
                        topLeft = Offset(fx, palmTop - fHeight + h * 0.07f),
                        size = Size(fingerW, fHeight),
                        cornerRadius = CornerRadius(fingerW * 0.5f, fingerW * 0.5f),
                    )
                }
            }

            // 5th finger = outstretched thumb
            if (number == 5) {
                val thumbW = w * 0.115f
                val thumbH = h * 0.24f
                val thumbPivotX = if (isMirrored) palmLeft + palmW - 4f else palmLeft + 4f
                val thumbLeft = if (isMirrored) thumbPivotX - thumbW * 0.2f else thumbPivotX - thumbW * 0.8f
                rotate(
                    degrees = if (isMirrored) 38f else -38f,
                    pivot = Offset(thumbPivotX, palmTop + palmH * 0.45f),
                ) {
                    drawRoundRect(
                        color = palmColor,
                        topLeft = Offset(thumbLeft, palmTop + palmH * 0.45f - thumbH),
                        size = Size(thumbW, thumbH),
                        cornerRadius = CornerRadius(thumbW * 0.5f, thumbW * 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
fun PitchDuelArena(
    playerPick: Int?,
    cpuPick: Int?,
    deliverySeq: Int,
    lastDelivery: DeliveryEvent?,
    isPlayerBatting: Boolean,
    promptText: String,
    modifier: Modifier = Modifier,
) {
    val stadium = LocalStadiumColors.current
    val cs = MaterialTheme.colorScheme

    val clashScale = remember { Animatable(1f) }
    val clashLunge = remember { Animatable(0f) }
    val impactProgress = remember { Animatable(1f) }
    val stumpShake = remember { Animatable(0f) }
    val arenaShakeX = remember { Animatable(0f) }

    LaunchedEffect(deliverySeq) {
        if (deliverySeq > 0) {
            // Launch simultaneous spring choreography for each delivery
            clashScale.snapTo(0.84f)
            clashLunge.snapTo(1f)
            impactProgress.snapTo(0f)

            kotlinx.coroutines.coroutineScope {
                launch {
                    clashScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
                launch {
                    clashLunge.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.56f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
                launch {
                    impactProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 640,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    )
                }
                if (lastDelivery?.isWicket == true) {
                    launch {
                        stumpShake.snapTo(-18f)
                        stumpShake.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                    launch {
                        arenaShakeX.snapTo(14f)
                        arenaShakeX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.28f,
                                stiffness = Spring.StiffnessHigh,
                            ),
                        )
                    }
                }
            }
        }
    }

    val targetHighlightAccent = when {
        lastDelivery == null -> cs.primary
        lastDelivery.isWicket -> stadium.wicketRed
        lastDelivery.isSix -> stadium.sixGold
        lastDelivery.isFour -> stadium.fourMint
        else -> cs.onSurfaceVariant
    }
    val highlightAccent by androidx.compose.animation.animateColorAsState(
        targetValue = targetHighlightAccent,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "arena_highlight_accent",
    )

    val borderStrokeColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            lastDelivery == null -> stadium.rimTop
            lastDelivery.isWicket -> stadium.wicketRed.copy(alpha = 0.9f)
            lastDelivery.isSix -> stadium.sixGold.copy(alpha = 0.9f)
            lastDelivery.isFour -> stadium.fourMint.copy(alpha = 0.9f)
            else -> cs.outline
        },
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "arena_border_color",
    )

    val arenaShape = RoundedCornerShape(26.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = arenaShakeX.value
            }
            .testTag("pitch_duel_arena"),
        shape = arenaShape,
        color = cs.surfaceContainer,
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.verticalGradient(
                listOf(borderStrokeColor, borderStrokeColor.copy(alpha = 0.25f))
            ),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Custom Mown Stadium Pitch + Dynamic Impact Particle Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripeCount = 7
                val stripeW = size.width / stripeCount
                for (i in 0 until stripeCount) {
                    drawRect(
                        color = if (i % 2 == 0) stadium.turfDark else stadium.turfLight.copy(alpha = 0.72f),
                        topLeft = Offset(i * stripeW, 0f),
                        size = Size(stripeW + 1f, size.height),
                    )
                }

                // Subtle night stadium edge vignette
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                        center = Offset(size.width / 2f, size.height * 0.46f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )

                // Center cricket pitch strip
                val stripW = size.width * 0.24f
                val stripLeft = (size.width - stripW) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            stadium.pitchSoilShadow.copy(alpha = 0.34f),
                            stadium.pitchSoil.copy(alpha = 0.48f),
                            stadium.pitchSoilShadow.copy(alpha = 0.34f),
                        )
                    ),
                    topLeft = Offset(stripLeft, size.height * 0.07f),
                    size = Size(stripW, size.height * 0.86f),
                    cornerRadius = CornerRadius(12f, 12f),
                )

                // Popping creases
                val creaseColor = Color.White.copy(alpha = 0.38f)
                drawLine(
                    color = creaseColor,
                    start = Offset(stripLeft - 14f, size.height * 0.21f),
                    end = Offset(stripLeft + stripW + 14f, size.height * 0.21f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = creaseColor,
                    start = Offset(stripLeft - 14f, size.height * 0.79f),
                    end = Offset(stripLeft + stripW + 14f, size.height * 0.79f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )

                val cx = size.width / 2f
                val cy = size.height * 0.44f
                val p = impactProgress.value

                // Dynamic Radial Glow + Expanding Shockwave & Celebration Particles
                if (lastDelivery != null && (lastDelivery.isWicket || lastDelivery.isSix || lastDelivery.isFour)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightAccent.copy(alpha = 0.30f),
                                Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = size.maxDimension * 0.54f,
                        ),
                        radius = size.maxDimension * 0.54f,
                        center = Offset(cx, cy),
                    )

                    if (p < 0.99f) {
                        val fade = (1f - p).coerceIn(0f, 1f)
                        // Expanding shockwave ring
                        drawCircle(
                            color = highlightAccent.copy(alpha = fade * 0.72f),
                            radius = size.minDimension * (0.14f + p * 0.58f),
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (10f * fade).coerceAtLeast(1.5f)),
                        )

                        // Burst rays & sparks for SIX or FOUR
                        if (lastDelivery.isSix || lastDelivery.isFour) {
                            val rayCount = if (lastDelivery.isSix) 14 else 10
                            val maxRadius = size.minDimension * (if (lastDelivery.isSix) 0.56f else 0.44f)
                            for (i in 0 until rayCount) {
                                val angleRad = (i * (360f / rayCount)) * (kotlin.math.PI / 180.0)
                                val distStart = maxRadius * (0.18f + p * 0.55f)
                                val distEnd = maxRadius * (0.28f + p * 0.85f)
                                val sx = cx + kotlin.math.cos(angleRad).toFloat() * distStart
                                val sy = cy + kotlin.math.sin(angleRad).toFloat() * distStart
                                val ex = cx + kotlin.math.cos(angleRad).toFloat() * distEnd
                                val ey = cy + kotlin.math.sin(angleRad).toFloat() * distEnd
                                drawLine(
                                    color = highlightAccent.copy(alpha = fade * 0.85f),
                                    start = Offset(sx, sy),
                                    end = Offset(ex, ey),
                                    strokeWidth = if (lastDelivery.isSix) 4.5f else 3.2f,
                                    cap = StrokeCap.Round,
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = fade * 0.9f),
                                    radius = if (lastDelivery.isSix) 4f * fade else 2.8f * fade,
                                    center = Offset(ex, ey),
                                )
                            }
                        }
                    }
                }

                // Miniature Stumps + Animated Flying Bails on Wicket
                val stumpsY = size.height * 0.34f
                val stumpH = size.height * 0.22f
                val isWicket = lastDelivery?.isWicket == true
                val stumpTint = if (isWicket) stadium.wicketRed else Color(0xFFE8D5B5)
                val offsets = listOf(-12f, 0f, 12f)
                offsets.forEachIndexed { idx, dx ->
                    val angle = if (isWicket) {
                        when (idx) {
                            0 -> -26f + stumpShake.value
                            1 -> 8f - stumpShake.value
                            else -> 30f + stumpShake.value
                        }
                    } else 0f
                    rotate(degrees = angle, pivot = Offset(cx + dx, stumpsY + stumpH)) {
                        drawLine(
                            color = stumpTint.copy(alpha = if (isWicket) 0.95f else 0.72f),
                            start = Offset(cx + dx, stumpsY),
                            end = Offset(cx + dx, stumpsY + stumpH),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                if (!isWicket) {
                    // Intact golden bails
                    drawLine(
                        color = stadium.sixGold.copy(alpha = 0.9f),
                        start = Offset(cx - 14f, stumpsY - 2f),
                        end = Offset(cx + 14f, stumpsY - 2f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round,
                    )
                } else if (p < 0.95f) {
                    // Flying spinning bails launched upward!
                    val bailFade = (1f - p).coerceIn(0f, 1f)
                    val liftY = stumpsY - 12f - (p * size.height * 0.22f)
                    val spreadX = 16f + p * 44f
                    rotate(degrees = -p * 260f, pivot = Offset(cx - spreadX, liftY)) {
                        drawLine(
                            color = stadium.sixGoldBright.copy(alpha = bailFade),
                            start = Offset(cx - spreadX - 8f, liftY),
                            end = Offset(cx - spreadX + 8f, liftY),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round,
                        )
                    }
                    rotate(degrees = p * 290f, pivot = Offset(cx + spreadX, liftY - 6f)) {
                        drawLine(
                            color = stadium.sixGoldBright.copy(alpha = bailFade),
                            start = Offset(cx + spreadX - 8f, liftY - 6f),
                            end = Offset(cx + spreadX + 8f, liftY - 6f),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            // Foreground Duel Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hands Clash Row with horizontal lunge impulse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DuelHandPanel(
                        label = stringResource(R.string.label_you),
                        roleTag = if (isPlayerBatting) "BAT" else "BOWL",
                        number = playerPick,
                        accentColor = if (isPlayerBatting) stadium.sixGold else stadium.fourMint,
                        isMirrored = false,
                        scale = clashScale.value,
                        lungeOffsetDp = clashLunge.value * 16f,
                        modifier = Modifier.weight(1f),
                    )

                    // Center VS / Outcome Stamp
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .scale(clashScale.value),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = lastDelivery,
                            transitionSpec = {
                                (fadeIn(androidx.compose.animation.core.tween(160)) +
                                    scaleIn(
                                        initialScale = 0.55f,
                                        animationSpec = StadiumMotion.ExpressivePop,
                                    )) togetherWith fadeOut(androidx.compose.animation.core.tween(120))
                            },
                            label = "outcome_stamp",
                        ) { event ->
                            if (event == null) {
                                Surface(
                                    shape = CircleShape,
                                    color = cs.background.copy(alpha = 0.84f),
                                    border = BorderStroke(1.dp, stadium.rimTop),
                                ) {
                                    Text(
                                        text = "VS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = cs.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                    )
                                }
                            } else {
                                val badgeBg = when {
                                    event.isWicket -> stadium.wicketRed
                                    event.isSix -> stadium.sixGold
                                    event.isFour -> stadium.fourMint
                                    else -> cs.background.copy(alpha = 0.92f)
                                }
                                val badgeFg = when {
                                    event.isWicket -> cs.onTertiary
                                    event.isSix -> cs.onPrimary
                                    event.isFour -> cs.onSecondary
                                    else -> cs.onSurface
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = badgeBg,
                                        shadowElevation = 8.dp,
                                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.45f)),
                                    ) {
                                        Text(
                                            text = if (event.isWicket) "OUT" else "+${event.runsScored}",
                                            fontFamily = StadiumDisplayFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (event.isWicket) 21.sp else 25.sp,
                                            color = badgeFg,
                                            modifier = Modifier
                                                .padding(horizontal = 13.dp, vertical = 10.dp)
                                                .testTag("delivery_outcome_badge"),
                                        )
                                    }
                                    if (event.isSix || event.isFour || event.isWicket) {
                                        Text(
                                            text = when {
                                                event.isWicket -> "BOWLED!"
                                                event.isSix -> "MAX SIX"
                                                else -> "FOUR!"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = badgeBg,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DuelHandPanel(
                        label = stringResource(R.string.label_cpu),
                        roleTag = if (isPlayerBatting) "BOWL" else "BAT",
                        number = cpuPick,
                        accentColor = if (isPlayerBatting) stadium.fourMint else stadium.sixGold,
                        isMirrored = true,
                        scale = clashScale.value,
                        lungeOffsetDp = -clashLunge.value * 16f,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated Broadcast Commentary Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cs.background.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, highlightAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = lastDelivery?.commentary ?: promptText,
                        transitionSpec = {
                            (androidx.compose.animation.slideInVertically { it / 2 } + fadeIn(androidx.compose.animation.core.tween(180))) togetherWith
                                (androidx.compose.animation.slideOutVertically { -it / 2 } + fadeOut(androidx.compose.animation.core.tween(120)))
                        },
                        label = "commentary_transition",
                    ) { commentaryText ->
                        Text(
                            text = commentaryText,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (lastDelivery?.isWicket == true) {
                                stadium.wicketRedBright
                            } else if (lastDelivery?.isSix == true) {
                                stadium.sixGoldBright
                            } else if (lastDelivery?.isFour == true) {
                                stadium.fourMintBright
                            } else {
                                cs.onSurface
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .testTag("commentary_banner"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuelHandPanel(
    label: String,
    roleTag: String,
    number: Int?,
    accentColor: Color,
    isMirrored: Boolean,
    scale: Float,
    lungeOffsetDp: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.offset(x = lungeOffsetDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.45f)),
            ) {
                Text(
                    text = roleTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(106.dp)
                .scale(scale),
        ) {
            // Subtle luminous halo behind active hand
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (number != null) 0.24f else 0.08f),
                                cs.background.copy(alpha = 0.72f),
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (number != null) accentColor.copy(alpha = 0.85f) else cs.outline,
                                if (number != null) accentColor.copy(alpha = 0.25f) else cs.outlineVariant,
                            )
                        ),
                        shape = CircleShape,
                    ),
            )

            HandGestureIllustration(
                number = number,
                accentColor = accentColor,
                isMirrored = isMirrored,
                modifier = Modifier.size(84.dp),
            )

            // Crisp corner numeral badge
            if (number != null) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(31.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = number.toString(),
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = cs.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPadGrid(
    enabled: Boolean,
    hapticsEnabled: Boolean,
    onPickNumber: (Int) -> Unit,
    accentIsBatting: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6))
        rows.forEach { rowNumbers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowNumbers.forEach { number ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed && enabled) 0.92f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                        label = "pad_scale_$number",
                    )

                    val isSpecialBoundary = (number == 4 || number == 6) && accentIsBatting
                    val topColor = when {
                        !enabled -> cs.surfaceContainer.copy(alpha = 0.45f)
                        number == 6 && accentIsBatting -> stadium.sixGold.copy(alpha = 0.28f)
                        number == 4 && accentIsBatting -> stadium.fourMint.copy(alpha = 0.24f)
                        else -> stadium.glassTop
                    }
                    val bottomColor = when {
                        !enabled -> cs.surfaceContainer.copy(alpha = 0.45f)
                        number == 6 && accentIsBatting -> stadium.sixGoldContainer
                        number == 4 && accentIsBatting -> stadium.fourMintContainer
                        else -> stadium.glassBottom
                    }
                    val borderTop = when {
                        !enabled -> cs.outlineVariant.copy(alpha = 0.35f)
                        number == 6 && accentIsBatting -> stadium.sixGoldBright.copy(alpha = 0.85f)
                        number == 4 && accentIsBatting -> stadium.fourMintBright.copy(alpha = 0.8f)
                        else -> stadium.rimTop
                    }
                    val borderBottom = when {
                        !enabled -> cs.outlineVariant.copy(alpha = 0.2f)
                        number == 6 && accentIsBatting -> stadium.sixGold.copy(alpha = 0.3f)
                        number == 4 && accentIsBatting -> stadium.fourMint.copy(alpha = 0.3f)
                        else -> stadium.rimBottom
                    }
                    val numberColor = when {
                        !enabled -> cs.onSurfaceVariant.copy(alpha = 0.4f)
                        number == 6 && accentIsBatting -> stadium.sixGoldBright
                        number == 4 && accentIsBatting -> stadium.fourMintBright
                        else -> cs.onSurface
                    }
                    val padShape = RoundedCornerShape(18.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .scale(pressScale)
                            .clip(padShape)
                            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
                            .border(
                                width = if (isSpecialBoundary) 1.5.dp else 1.dp,
                                brush = Brush.verticalGradient(listOf(borderTop, borderBottom)),
                                shape = padShape,
                            )
                            .clickable(
                                enabled = enabled,
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(color = numberColor),
                                onClick = {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onPickNumber(number)
                                },
                            )
                            .testTag("number_pad_$number")
                            .semantics {
                                contentDescription = "Throw number $number"
                            },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = number.toString(),
                                fontFamily = StadiumDisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 33.sp,
                                lineHeight = 35.sp,
                                color = numberColor,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (number == 6) {
                                    Text(
                                        text = "THUMB",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = numberColor.copy(alpha = 0.9f),
                                    )
                                } else {
                                    repeat(number) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(numberColor.copy(alpha = 0.7f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentBallsTicker(
    deliveries: List<DeliveryEvent>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(deliveries.size) {
        if (deliveries.isNotEmpty()) {
            listState.animateScrollToItem(deliveries.lastIndex)
        }
    }

    BroadcastCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "OVER",
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
            )

            if (deliveries.isEmpty()) {
                Text(
                    text = stringResource(R.string.recent_balls_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant.copy(alpha = 0.75f),
                )
            } else {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(deliveries, key = { it.ballNumber }) { event ->
                        val popScale = remember { Animatable(0.45f) }
                        LaunchedEffect(event.ballNumber) {
                            popScale.animateTo(
                                targetValue = 1f,
                                animationSpec = StadiumMotion.ExpressivePop,
                            )
                        }
                        val bg = when {
                            event.isWicket -> stadium.wicketRed
                            event.isSix -> stadium.sixGold
                            event.isFour -> stadium.fourMint
                            else -> cs.surfaceContainerHigh
                        }
                        val fg = when {
                            event.isWicket -> cs.onTertiary
                            event.isSix -> cs.onPrimary
                            event.isFour -> cs.onSecondary
                            else -> cs.onSurface
                        }
                        val hasSpecialRing = event.isWicket || event.isSix || event.isFour
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(29.dp)
                                    .scale(popScale.value)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (hasSpecialRing) {
                                            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                        } else {
                                            Modifier.border(1.dp, cs.outlineVariant, CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = event.badgeLabel,
                                    fontFamily = ScoreboardMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = fg,
                                )
                            }
                            if (event.ballNumber % 6 == 0 && event.ballNumber != deliveries.last().ballNumber) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(18.dp)
                                        .background(cs.outline),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesBottomSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surfaceContainer,
        contentColor = cs.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsCricket,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.rules_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onSurface,
                )
            }

            RuleCard(
                title = stringResource(R.string.rules_toss_heading),
                body = stringResource(R.string.rules_toss_body),
                accent = stadium.sixGold,
            )
            RuleCard(
                title = stringResource(R.string.rules_play_heading),
                body = stringResource(R.string.rules_play_body),
                accent = stadium.fourMint,
            )
            RuleCard(
                title = stringResource(R.string.rules_win_heading),
                body = stringResource(R.string.rules_win_body),
                accent = stadium.wicketRed,
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("rules_dismiss_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.rules_got_it),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun RuleCard(
    title: String,
    body: String,
    accent: Color,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceContainerLow,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
    }
}
