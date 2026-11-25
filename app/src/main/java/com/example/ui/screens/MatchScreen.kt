package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.R
import com.example.game.CricketUiState
import com.example.online.ReactionType
import com.example.ui.components.AnimatedScoreText
import com.example.ui.components.BroadcastCard
import com.example.ui.components.FloatingReactionsOverlay
import com.example.ui.components.NumberPadGrid
import com.example.ui.components.PitchDuelArena
import com.example.ui.components.ReactionBubbleEvent
import com.example.ui.components.ReactionPickerBar
import com.example.ui.components.RecentBallsTicker
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MatchScreen(
    state: CricketUiState,
    onPickNumber: (Int) -> Unit,
    onRequestExit: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenRules: () -> Unit,
) {
    BackHandler(onBack = onRequestExit)

    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val localReactions = remember { mutableStateListOf<ReactionBubbleEvent>() }
    var reactionCounter by remember { mutableLongStateOf(0L) }
    var showMatchStats by rememberSaveable { mutableStateOf(false) }

    fun pushLocalReaction(sender: String, type: ReactionType, isMe: Boolean) {
        reactionCounter += 1L
        localReactions.add(
            ReactionBubbleEvent(
                id = "solo_${reactionCounter}_${type.wireValue}",
                senderName = sender,
                type = type,
                isMe = isMe,
            )
        )
        if (localReactions.size > 12) {
            localReactions.removeAt(0)
        }
    }

    // Tactile feedback burst + playful CPU emoji reaction on wickets & boundaries
    LaunchedEffect(state.deliverySeq) {
        val last = state.lastDelivery ?: return@LaunchedEffect
        if (state.hapticsEnabled && (last.isWicket || last.isSix || last.isFour)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (last.isWicket) {
            delay(280L)
            val cpuReaction = if (state.isPlayerBatting) ReactionType.WICKET else ReactionType.SHOCK
            pushLocalReaction("CPU", cpuReaction, isMe = false)
        } else if (last.isSix) {
            delay(280L)
            val cpuReaction = if (state.isPlayerBatting) ReactionType.SHOCK else ReactionType.FIRE
            pushLocalReaction("CPU", cpuReaction, isMe = false)
        }
    }

    val promptText = if (state.isPlayerBatting) {
        stringResource(R.string.prompt_pick_shot)
    } else {
        stringResource(R.string.prompt_pick_delivery)
    }

    StadiumAtmosphereBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val isWide = maxWidth >= 680.dp

            if (isWide) {
                // Adaptive Expanded / Landscape Two-Pane Stadium Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MatchTopBar(
                            state = state,
                            onRequestExit = onRequestExit,
                            onToggleSound = onToggleSound,
                            onOpenRules = onOpenRules,
                        )
                        ScoreboardCard(
                            state = state,
                            onOpenMatchStats = { showMatchStats = true },
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            PitchDuelArena(
                                playerPick = state.lastDelivery?.playerPick,
                                cpuPick = state.lastDelivery?.cpuPick,
                                deliverySeq = state.deliverySeq,
                                lastDelivery = state.lastDelivery,
                                isPlayerBatting = state.isPlayerBatting,
                                promptText = promptText,
                                modifier = Modifier.fillMaxSize(),
                            )
                            FloatingReactionsOverlay(
                                reactions = localReactions.toList(),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        RecentBallsTicker(deliveries = state.currentInnings.deliveries)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReactionPickerBar(
                                onSendReaction = { pushLocalReaction("You", it, isMe = true) },
                                hapticsEnabled = state.hapticsEnabled,
                                modifier = Modifier.testTag("solo_reaction_picker"),
                            )
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant,
                            )
                            NumberPadGrid(
                                enabled = !state.isDeliveryAnimating,
                                hapticsEnabled = state.hapticsEnabled,
                                onPickNumber = onPickNumber,
                                accentIsBatting = state.isPlayerBatting,
                            )
                        }
                    }
                }
            } else {
                // Compact Portrait Phone Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MatchTopBar(
                        state = state,
                        onRequestExit = onRequestExit,
                        onToggleSound = onToggleSound,
                        onOpenRules = onOpenRules,
                    )

                    ScoreboardCard(
                        state = state,
                        onOpenMatchStats = { showMatchStats = true },
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        PitchDuelArena(
                            playerPick = state.lastDelivery?.playerPick,
                            cpuPick = state.lastDelivery?.cpuPick,
                            deliverySeq = state.deliverySeq,
                            lastDelivery = state.lastDelivery,
                            isPlayerBatting = state.isPlayerBatting,
                            promptText = promptText,
                            modifier = Modifier.fillMaxSize(),
                        )
                        FloatingReactionsOverlay(
                            reactions = localReactions.toList(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    RecentBallsTicker(deliveries = state.currentInnings.deliveries)

                    ReactionPickerBar(
                        onSendReaction = { pushLocalReaction("You", it, isMe = true) },
                        hapticsEnabled = state.hapticsEnabled,
                        modifier = Modifier.testTag("solo_reaction_picker"),
                    )

                    NumberPadGrid(
                        enabled = !state.isDeliveryAnimating,
                        hapticsEnabled = state.hapticsEnabled,
                        onPickNumber = onPickNumber,
                        accentIsBatting = state.isPlayerBatting,
                    )
                }
            }
        }
    }

    if (showMatchStats) {
        val snapshot = remember(state) { buildSoloMatchStatisticsSnapshot(state) }
        MatchStatisticsModalSheet(
            snapshot = snapshot,
            onDismiss = { showMatchStats = false },
        )
    }
}

@Composable
private fun MatchTopBar(
    state: CricketUiState,
    onRequestExit: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenRules: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = cs.surfaceContainer,
            border = BorderStroke(1.dp, stadium.rimTop),
        ) {
            IconButton(
                onClick = onRequestExit,
                modifier = Modifier.testTag("match_exit_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_exit_match),
                    tint = cs.onSurface,
                )
            }
        }

        // Innings + Role Pill
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val inningsLabel = when {
                state.isSuperOverDecider -> "${stringResource(R.string.innings_super_over)} • ${state.inningsNumber}/2"
                state.inningsNumber == 1 -> stringResource(R.string.innings_first)
                else -> stringResource(R.string.innings_second)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = cs.surfaceContainerHigh,
                border = BorderStroke(1.dp, stadium.rimTop),
            ) {
                Text(
                    text = inningsLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (state.isPlayerBatting) stadium.sixGoldContainer else stadium.fourMintContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.isPlayerBatting) stadium.sixGold else stadium.fourMint,
                ),
            ) {
                Text(
                    text = if (state.isPlayerBatting) {
                        stringResource(R.string.role_batting)
                    } else {
                        stringResource(R.string.role_bowling)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.isPlayerBatting) stadium.sixGoldBright else stadium.fourMintBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("role_badge"),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = cs.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (state.soundEnabled) stadium.sixGold.copy(alpha = 0.55f) else stadium.rimTop,
                ),
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier.testTag("match_sound_button"),
                ) {
                    Icon(
                        imageVector = if (state.soundEnabled) {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        },
                        contentDescription = stringResource(
                            if (state.soundEnabled) R.string.cd_sound_on else R.string.cd_sound_off
                        ),
                        tint = if (state.soundEnabled) stadium.sixGoldBright else cs.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = cs.surfaceContainer,
                border = BorderStroke(1.dp, stadium.rimTop),
            ) {
                IconButton(
                    onClick = onOpenRules,
                    modifier = Modifier.testTag("match_rules_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.cd_rules),
                        tint = cs.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreboardCard(
    state: CricketUiState,
    onOpenMatchStats: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val innings = state.currentInnings
    val maxWickets = state.activeFormat.maxWickets
    val maxBalls = state.activeFormat.maxBalls
    val sideAccent = if (state.isPlayerBatting) stadium.sixGold else stadium.fourMint

    BroadcastCard(
        shape = RoundedCornerShape(22.dp),
        accentColor = sideAccent,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scoreboard_card"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                // Batting side & giant animated broadcast score
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (state.isPlayerBatting) "YOU" else "CPU",
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = sideAccent,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    AnimatedScoreText(
                        text = "${innings.runs}/${innings.wickets}",
                        color = cs.onSurface,
                        fontSize = 48.sp,
                        modifier = Modifier.testTag("score_display"),
                    )
                    Text(
                        text = "(${maxWickets - innings.wickets} wkt left)",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }

                // Overs, Run Rate & Quick Match Stats Trigger
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val oversDisplay = if (maxBalls != null) {
                            "${innings.oversText} / ${maxBalls / 6}.0 OV"
                        } else {
                            "${innings.balls} BALLS"
                        }
                        Text(
                            text = oversDisplay,
                            fontFamily = ScoreboardMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = cs.onSurface,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = sideAccent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, sideAccent.copy(alpha = 0.55f)),
                            onClick = onOpenMatchStats,
                            modifier = Modifier.testTag("match_stats_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = stringResource(R.string.cd_match_stats),
                                    tint = sideAccent,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "STATS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = sideAccent,
                                )
                            }
                        }
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "SR %.0f  •  4s:%d  6s:%d",
                            innings.strikeRate,
                            innings.fours,
                            innings.sixes,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            // 2nd Innings Animated Target Chase Bar
            val target = state.targetRuns
            val needed = state.runsNeeded
            if (target != null && needed != null) {
                val targetProgress = (innings.runs.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.75f,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                    label = "chase_progress",
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val ballsLeftText = state.ballsRemaining?.let { " off $it balls" } ?: ""
                        Text(
                            text = "TARGET $target  •  NEED $needed$ballsLeftText",
                            style = MaterialTheme.typography.labelSmall,
                            color = stadium.sixGoldBright,
                            modifier = Modifier.testTag("target_chase_text"),
                        )
                        val rrr = state.requiredRunRate
                        if (rrr != null) {
                            Text(
                                text = String.format(Locale.US, "RRR %.1f", rrr),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rrr > 24.0) stadium.wicketRed else cs.onSurfaceVariant,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = sideAccent,
                        trackColor = cs.background.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
fun ExitMatchDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainer,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.dialog_exit_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_exit_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_stay_button"),
            ) {
                Text(stringResource(R.string.dialog_stay), color = cs.onSurface)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.error,
                    contentColor = cs.onError,
                ),
                modifier = Modifier.testTag("dialog_leave_button"),
            ) {
                Text(stringResource(R.string.dialog_leave))
            }
        },
    )
}
