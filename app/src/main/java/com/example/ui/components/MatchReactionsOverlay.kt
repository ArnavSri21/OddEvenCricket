package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.online.ReactionType
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.StadiumDisplayFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

data class ReactionBubbleEvent(
    val id: String,
    val senderName: String,
    val type: ReactionType,
    val isMe: Boolean,
)

@Composable
fun ReactionPickerBar(
    onSendReaction: (ReactionType) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val haptic = LocalHapticFeedback.current
    var lastTapMs by remember { mutableLongStateOf(0L) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = stadium.glassBottom.copy(alpha = 0.88f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    stadium.rimTop,
                    stadium.sixGold.copy(alpha = 0.35f),
                    stadium.fourMint.copy(alpha = 0.35f),
                    stadium.rimTop,
                )
            ),
        ),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(ReactionType.entries, key = { it.wireValue }) { type ->
                ReactionChipButton(
                    type = type,
                    enabled = enabled,
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastTapMs < 350L) return@ReactionChipButton
                        lastTapMs = now
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onSendReaction(type)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReactionChipButton(
    type: ReactionType,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var popTrigger by remember { mutableLongStateOf(0L) }
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(popTrigger) {
        if (popTrigger > 0L) {
            popScale.snapTo(1.34f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.45f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = StadiumMotion.SnappySpring,
        label = "reaction_press_${type.wireValue}",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) cs.primary.copy(alpha = 0.18f) else Color.Transparent
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = cs.primary),
                onClick = {
                    popTrigger = System.currentTimeMillis()
                    onClick()
                },
            )
            .semantics {
                contentDescription = "Send ${type.callout} reaction"
            }
            .testTag("reaction_btn_${type.wireValue.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.emoji,
            fontSize = 22.sp,
            modifier = Modifier.graphicsLayer {
                val combined = pressScale * popScale.value
                scaleX = combined
                scaleY = combined
            },
        )
    }
}

/**
 * Broadcast floating reactions layer rendered on top of the Pitch Duel Arena.
 * Ignores historical reactions already present at mount time and animates each new
 * live reaction smoothly upward with a spring pop-in, sinusoidal drift, and fade-out.
 */
@Composable
fun FloatingReactionsOverlay(
    reactions: List<ReactionBubbleEvent>,
    modifier: Modifier = Modifier,
) {
    val seenIds = remember { mutableSetOf<String>() }
    var initialized by remember { mutableStateOf(false) }
    val activeBubbles = remember { mutableStateListOf<ReactionBubbleEvent>() }

    LaunchedEffect(reactions) {
        if (!initialized) {
            // Seed existing history so joining mid-match doesn't replay old reactions
            reactions.forEach { seenIds.add(it.id) }
            initialized = true
            return@LaunchedEffect
        }
        val fresh = reactions.filter { it.id !in seenIds }
        fresh.forEach { event ->
            seenIds.add(event.id)
            activeBubbles.add(event)
            if (activeBubbles.size > 10) {
                activeBubbles.removeAt(0)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        activeBubbles.forEach { bubble ->
            androidx.compose.runtime.key(bubble.id) {
                FloatingReactionPill(
                    event = bubble,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    onFinished = {
                        activeBubbles.removeAll { it.id == bubble.id }
                    },
                )
            }
        }
    }
}

@Composable
private fun FloatingReactionPill(
    event: ReactionBubbleEvent,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onFinished: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val accent = when (event.type) {
        ReactionType.SIX, ReactionType.CROWN, ReactionType.FIRE -> stadium.sixGold
        ReactionType.WICKET, ReactionType.SHOCK -> stadium.wicketRed
        ReactionType.CLAP, ReactionType.GG, ReactionType.LAUGH -> stadium.fourMint
    }

    val hash = remember(event.id) { abs(event.id.hashCode()) }
    // Anchor horizontally in a pleasing 18%..72% lane so pills stay well within bounds
    val laneBias = remember(hash, event.isMe) {
        val slot = (hash % 5) / 4f // 0f..1f
        if (event.isMe) 0.55f + (slot - 0.5f) * 0.25f else 0.25f + slot * 0.45f
    }
    val swaySign = remember(hash) { if (hash % 2 == 0) 1f else -1f }

    val progress = remember { Animatable(0f) }
    val popScale = remember { Animatable(0.35f) }

    LaunchedEffect(event.id) {
        launch {
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = StadiumMotion.ExpressivePop,
            )
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = LinearEasing),
        )
        delay(50)
        onFinished()
    }

    val p = progress.value
    val travelPx = (containerHeightPx * 0.72f).coerceAtLeast(160f)
    val yOffsetPx = -p * travelPx
    val swayPx = sin((p * 2.2f * PI).toDouble()).toFloat() * 22f * swaySign
    val alpha = when {
        p < 0.12f -> (p / 0.12f).coerceIn(0f, 1f)
        p > 0.72f -> ((1f - p) / 0.28f).coerceIn(0f, 1f)
        else -> 1f
    }
    val horizontalBias = ((laneBias - 0.5f) * (containerWidthPx * 0.58f)).coerceIn(
        -containerWidthPx * 0.30f,
        containerWidthPx * 0.30f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = cs.background.copy(alpha = 0.88f),
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.85f)),
            modifier = Modifier.graphicsLayer {
                translationX = horizontalBias + swayPx
                translationY = yOffsetPx
                scaleX = popScale.value
                scaleY = popScale.value
                this.alpha = alpha
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = event.type.emoji,
                    fontSize = 20.sp,
                )
                Text(
                    text = event.senderName.take(14),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.type.callout,
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accent,
                )
            }
        }
    }
}
