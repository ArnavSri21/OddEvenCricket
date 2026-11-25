package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.remember
import com.example.R
import com.example.game.DeliveryEvent
import com.example.game.TeamSide
import com.example.online.GameRoom
import com.example.online.MatchReaction
import com.example.online.ReactionType
import com.example.online.RoomMode
import com.example.ui.components.AnimatedScoreText
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.FloatingReactionsOverlay
import com.example.ui.components.NumberPadGrid
import com.example.ui.components.PitchDuelArena
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.ReactionBubbleEvent
import com.example.ui.components.ReactionPickerBar
import com.example.ui.components.RecentBallsTicker
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.VictoryConfettiOverlay
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun OnlineMatchPlayView(
    room: GameRoom,
    currentUserId: String,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    reactions: List<MatchReaction>,
    onExit: () -> Unit,
    onToggleSound: () -> Unit,
    onSubmitPick: (Int) -> Unit,
    onSendReaction: (ReactionType) -> Unit,
) {
    BackHandler(onBack = onExit)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var showMatchStats by rememberSaveable { mutableStateOf(false) }

    val isSpectator = room.isSpectator(currentUserId)
    val isActiveBatter = room.activeBatterUid == currentUserId
    val isActiveBowler = room.activeBowlerUid == currentUserId
    val alreadySubmitted = (isActiveBatter && room.batterPick > 0L) || (isActiveBowler && room.bowlerPick > 0L)
    val canSubmit = (isActiveBatter || isActiveBowler) && !alreadySubmitted

    val bubbleEvents = remember(reactions, currentUserId) {
        reactions.map { rx ->
            ReactionBubbleEvent(
                id = rx.id,
                senderName = rx.senderName,
                type = rx.type(),
                isMe = rx.senderUid == currentUserId,
            )
        }
    }

    val battingTeamName = if (room.roomMode() == RoomMode.DUEL_1V1) {
        room.nameOf(room.activeBatterUid)
    } else {
        if (room.battingTeam == "A") "TEAM GOLD" else "TEAM MINT"
    }
    val teamAccent = if (room.battingTeam == "A") stadium.sixGold else stadium.fourMint

    val lastEvent: DeliveryEvent? = if (room.lastDeliverySeq > 0L && room.lastBatterPick > 0L && room.lastBowlerPick > 0L) {
        val isWicket = room.lastBatterPick == room.lastBowlerPick
        val runs = if (isWicket) 0 else room.lastBatterPick.toInt()
        DeliveryEvent(
            ballNumber = room.balls.toInt().coerceAtLeast(1),
            playerPick = room.lastBatterPick.toInt(),
            cpuPick = room.lastBowlerPick.toInt(),
            batterSide = TeamSide.PLAYER,
            runsScored = runs,
            isWicket = isWicket,
            commentary = room.lastBallSummary,
        )
    } else {
        null
    }

    val recentDeliveryEvents = room.recentBalls.mapIndexed { idx, badge ->
        val isW = badge == "W"
        val r = badge.toIntOrNull() ?: 0
        DeliveryEvent(
            ballNumber = idx + 1,
            playerPick = r,
            cpuPick = if (isW) r else 0,
            batterSide = TeamSide.PLAYER,
            runsScored = r,
            isWicket = isW,
            commentary = "",
        )
    }

    StadiumAtmosphereBackground(accentColor = teamAccent) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RoomStageTopBar(
                    title = "ROOM ${room.roomCode}",
                    subtitle = "INNINGS ${room.inningsNumber} • $battingTeamName BATTING",
                    onBack = onExit,
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = stadium.glassTop,
                                border = BorderStroke(1.dp, stadium.fourMint.copy(alpha = 0.55f)),
                            ) {
                                IconButton(
                                    onClick = { showMatchStats = true },
                                    modifier = Modifier.testTag("online_match_stats_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BarChart,
                                        contentDescription = stringResource(R.string.cd_match_stats),
                                        tint = stadium.fourMintBright,
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = stadium.glassTop,
                                border = BorderStroke(
                                    1.dp,
                                    if (soundEnabled) stadium.sixGold.copy(alpha = 0.55f) else stadium.rimTop,
                                ),
                            ) {
                                IconButton(
                                    onClick = onToggleSound,
                                    modifier = Modifier.testTag("online_sound_button"),
                                ) {
                                    Icon(
                                        imageVector = if (soundEnabled) {
                                            Icons.AutoMirrored.Rounded.VolumeUp
                                        } else {
                                            Icons.AutoMirrored.Rounded.VolumeOff
                                        },
                                        contentDescription = stringResource(
                                            if (soundEnabled) R.string.cd_sound_on else R.string.cd_sound_off
                                        ),
                                        tint = if (soundEnabled) stadium.sixGoldBright else cs.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    },
                )

                if (isSpectator || room.spectatorUids.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSpectator) {
                            BroadcastLivePill(
                                label = "SPECTATING LIVE",
                                accentColor = stadium.fourMint,
                                modifier = Modifier.testTag("spectator_mode_pill"),
                            )
                        } else {
                            BroadcastLivePill(
                                label = "LIVE MATCH",
                                accentColor = teamAccent,
                            )
                        }
                        if (room.spectatorUids.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = "Spectators watching",
                                    tint = stadium.sixGoldBright,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "${room.spectatorUids.size} watching",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = stadium.sixGoldBright,
                                )
                            }
                        }
                    }
                }

                // Live Broadcast Scoreboard Card
                BroadcastCard(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = teamAccent,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = battingTeamName,
                                    fontFamily = StadiumDisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = teamAccent,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                AnimatedScoreText(
                                    text = "${room.runs}/${room.wickets}",
                                    color = cs.onSurface,
                                    fontSize = 46.sp,
                                    modifier = Modifier.testTag("online_score_display"),
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val oversText = if (room.maxBalls > 0L) {
                                    "${room.oversText()} / ${room.maxBalls / 6}.0 OV"
                                } else {
                                    "${room.balls} BALLS"
                                }
                                Text(
                                    text = oversText,
                                    fontFamily = ScoreboardMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = cs.onSurface,
                                )
                                Text(
                                    text = "4s:${room.fours} • 6s:${room.sixes} • Wkt left:${(room.maxWickets - room.wickets).coerceAtLeast(0)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }

                        // Active Batter vs Active Bowler Bar (with COMMON tag!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val batterCommon = if (room.activeBatterUid == room.commonPlayerUid && room.commonPlayerUid.isNotBlank()) " [COMMON]" else ""
                            val bowlerCommon = if (room.activeBowlerUid == room.commonPlayerUid && room.commonPlayerUid.isNotBlank()) " [COMMON]" else ""
                            Text(
                                text = "BAT: ${room.nameOf(room.activeBatterUid)}$batterCommon",
                                style = MaterialTheme.typography.labelSmall,
                                color = stadium.sixGoldBright,
                            )
                            Text(
                                text = "BOWL: ${room.nameOf(room.activeBowlerUid)}$bowlerCommon",
                                style = MaterialTheme.typography.labelSmall,
                                color = stadium.fourMintBright,
                            )
                        }

                        if (room.inningsNumber == 2L && room.targetRuns > 0L) {
                            val needed = (room.targetRuns - room.runs).coerceAtLeast(0L)
                            val targetProgress = (room.runs.toFloat() / room.targetRuns.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = targetProgress,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                                label = "online_chase_progress",
                            )
                            val ballsLeft = if (room.maxBalls > 0L) " off ${(room.maxBalls - room.balls).coerceAtLeast(0L)} balls" else ""
                            Text(
                                text = "TARGET ${room.targetRuns} • NEED $needed$ballsLeft",
                                style = MaterialTheme.typography.labelSmall,
                                color = stadium.sixGoldBright,
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = stadium.sixGold,
                                trackColor = cs.background.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                // Live Pitch Duel Arena + Floating Broadcast Reactions Overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    PitchDuelArena(
                        playerPick = lastEvent?.playerPick,
                        cpuPick = lastEvent?.cpuPick,
                        deliverySeq = room.lastDeliverySeq.toInt(),
                        lastDelivery = lastEvent,
                        isPlayerBatting = true,
                        promptText = room.lastBallSummary.ifBlank { "Waiting for batter and bowler…" },
                        modifier = Modifier.fillMaxSize(),
                    )
                    FloatingReactionsOverlay(
                        reactions = bubbleEvents,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                RecentBallsTicker(deliveries = recentDeliveryEvents)

                // Live In-Match Reactions Strip (usable by players & spectators!)
                ReactionPickerBar(
                    onSendReaction = onSendReaction,
                    hapticsEnabled = hapticsEnabled,
                    modifier = Modifier.testTag("online_reaction_picker"),
                )

                // Turn-gated Input Pad
                if (canSubmit) {
                    val roleAction = if (isActiveBatter) "YOU ARE BATTING — PICK 1–6" else "YOU ARE BOWLING — PICK 1–6"
                    BroadcastLivePill(
                        label = roleAction,
                        accentColor = if (isActiveBatter) stadium.sixGold else stadium.fourMint,
                    )
                    NumberPadGrid(
                        enabled = true,
                        hapticsEnabled = hapticsEnabled,
                        onPickNumber = onSubmitPick,
                        accentIsBatting = isActiveBatter,
                    )
                } else {
                    BroadcastCard(
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = when {
                                alreadySubmitted -> "Your pick is locked! Waiting for opposing player…"
                                isSpectator -> "Spectating ${room.nameOf(room.activeBatterUid)} vs ${room.nameOf(room.activeBowlerUid)} • Send live reactions above!"
                                else -> "Watching ${room.nameOf(room.activeBatterUid)} vs ${room.nameOf(room.activeBowlerUid)}…"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showMatchStats) {
        val snapshot = remember(room, currentUserId) {
            buildOnlineMatchStatisticsSnapshot(room, currentUserId)
        }
        MatchStatisticsModalSheet(
            snapshot = snapshot,
            onDismiss = { showMatchStats = false },
        )
    }
}

@Composable
fun OnlineInningsBreakView(
    room: GameRoom,
    currentUserId: String,
    onExit: () -> Unit,
    onStartSecondInnings: () -> Unit,
) {
    BackHandler(onBack = onExit)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var showMatchStats by rememberSaveable { mutableStateOf(false) }
    val firstTeamLabel = if (room.roomMode() == RoomMode.DUEL_1V1) {
        val uid = if (room.firstInningsTeam == "A") room.captainAUid else room.captainBUid
        room.nameOf(uid)
    } else {
        if (room.firstInningsTeam == "A") "TEAM GOLD" else "TEAM MINT"
    }

    StadiumAtmosphereBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                BroadcastLivePill(
                    label = "HALF-TIME • ROOM ${room.roomCode}",
                    accentColor = stadium.sixGold,
                    modifier = Modifier.staggeredEntrance(0),
                )
                Text(
                    text = "INNINGS BREAK",
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    color = cs.onBackground,
                    modifier = Modifier.staggeredEntrance(1),
                )

                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = stadium.sixGold,
                    contentPadding = PaddingValues(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "$firstTeamLabel SCORED", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Text(
                            text = "${room.firstInningsRuns}/${room.firstInningsWickets}",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 60.sp,
                            color = cs.onSurface,
                        )
                        Text(
                            text = "TARGET: ${room.targetRuns} RUNS",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            color = stadium.sixGoldBright,
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showMatchStats = true },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .staggeredEntrance(3)
                        .testTag("online_break_match_stats_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = stadium.sixGoldBright,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.match_stats_title),
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = stadium.sixGoldBright,
                    )
                }

                if (currentUserId == room.hostUid || currentUserId == room.captainAUid || currentUserId == room.captainBUid) {
                    PremiumActionButton(
                        text = "START 2ND INNINGS",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onStartSecondInnings,
                        height = 58.dp,
                        modifier = Modifier
                            .staggeredEntrance(4)
                            .testTag("online_start_second_innings"),
                    )
                } else {
                    Text(
                        text = "Waiting for Host or Captain to start 2nd Innings…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showMatchStats) {
        val snapshot = remember(room, currentUserId) {
            buildOnlineMatchStatisticsSnapshot(room, currentUserId)
        }
        MatchStatisticsModalSheet(
            snapshot = snapshot,
            onDismiss = { showMatchStats = false },
        )
    }
}

@Composable
fun OnlineMatchResultView(
    room: GameRoom,
    currentUserId: String,
    onBackToHub: () -> Unit,
) {
    BackHandler(onBack = onBackToHub)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var showMatchStats by rememberSaveable { mutableStateOf(false) }

    val playerInWinningTeam = when (room.winnerTeam) {
        "A" -> currentUserId in room.teamAUids || currentUserId == room.commonPlayerUid
        "B" -> currentUserId in room.teamBUids || currentUserId == room.commonPlayerUid
        else -> false
    }

    StadiumAtmosphereBackground(
        accentColor = if (playerInWinningTeam) stadium.sixGold else stadium.fourMint,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp),
                ) {
                    Image(
                        painter = painterResource(
                            id = if (playerInWinningTeam) R.drawable.img_trophy_banner else R.drawable.img_stadium_hero
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        cs.background.copy(alpha = 0.34f),
                                        cs.background.copy(alpha = 0.62f),
                                        cs.background,
                                    )
                                )
                            ),
                    )
                    if (playerInWinningTeam) {
                        VictoryConfettiOverlay()
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .staggeredEntrance(0),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = when {
                                room.winnerTeam == "TIE" -> "MATCH TIED!"
                                playerInWinningTeam -> "VICTORY!"
                                else -> "FULL TIME"
                            },
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = if (playerInWinningTeam) stadium.sixGoldBright else cs.onBackground,
                        )
                        Text(
                            text = room.resultHeadline.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val teamAScore = if (room.firstInningsTeam == "A") {
                        "${room.firstInningsRuns}/${room.firstInningsWickets}"
                    } else {
                        "${room.runs}/${room.wickets}"
                    }
                    val teamBScore = if (room.firstInningsTeam == "B") {
                        "${room.firstInningsRuns}/${room.firstInningsWickets}"
                    } else {
                        "${room.runs}/${room.wickets}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(1),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BroadcastCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            accentColor = stadium.sixGold,
                            selected = room.winnerTeam == "A",
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (room.roomMode() == RoomMode.DUEL_1V1) room.nameOf(room.captainAUid) else "TEAM GOLD",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = stadium.sixGoldBright,
                                )
                                Text(
                                    text = teamAScore,
                                    fontFamily = StadiumDisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    color = cs.onSurface,
                                )
                            }
                        }
                        BroadcastCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            accentColor = stadium.fourMint,
                            selected = room.winnerTeam == "B",
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (room.roomMode() == RoomMode.DUEL_1V1) room.nameOf(room.captainBUid) else "TEAM MINT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = stadium.fourMintBright,
                                )
                                Text(
                                    text = teamBScore,
                                    fontFamily = StadiumDisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 40.sp,
                                    color = cs.onSurface,
                                )
                            }
                        }
                    }

                    if (room.roomMode() == RoomMode.TEAM_PLAY) {
                        Box(modifier = Modifier.staggeredEntrance(2)) {
                            TeamRostersCompactStrip(room = room)
                        }
                    }

                    OutlinedButton(
                        onClick = { showMatchStats = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .staggeredEntrance(3)
                            .testTag("online_result_match_stats_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = stadium.sixGoldBright,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.match_stats_title),
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = stadium.sixGoldBright,
                        )
                    }

                    PremiumActionButton(
                        text = "BACK TO DASHBOARD",
                        icon = Icons.Rounded.Home,
                        onClick = onBackToHub,
                        height = 58.dp,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .staggeredEntrance(4)
                            .testTag("online_result_back_hub"),
                    )
                }
            }
        }
    }

    if (showMatchStats) {
        val snapshot = remember(room, currentUserId) {
            buildOnlineMatchStatisticsSnapshot(room, currentUserId)
        }
        MatchStatisticsModalSheet(
            snapshot = snapshot,
            onDismiss = { showMatchStats = false },
        )
    }
}
