package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.MatchFormat
import com.example.online.Friendship
import com.example.online.GameRoom
import com.example.online.MatchReaction
import com.example.online.OnlineUiState
import com.example.online.ReactionType
import com.example.online.RoomMode
import com.example.online.RoomStage
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.HandGestureIllustration
import com.example.ui.components.NumberPadGrid
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.StadiumMotion
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun OnlineRoomHostScreen(
    currentUserId: String,
    roomState: OnlineUiState<GameRoom?>,
    reactions: List<MatchReaction>,
    acceptedFriends: List<Friendship>,
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
    isBusy: Boolean,
    onToggleSound: () -> Unit,
    onLeaveRoom: () -> Unit,
    onCloseRoomView: () -> Unit,
    onCopyCode: (String) -> Unit,
    onInviteFriend: (friendUid: String, friendName: String) -> Unit,
    onAddBot: () -> Unit,
    onRemoveBot: () -> Unit,
    onStartMatch: () -> Unit,
    onProceedFromCaptainReveal: () -> Unit,
    onSubmitTossCall: (String) -> Unit,
    onSubmitTossNumber: (Int) -> Unit,
    onAdvanceToTeamDraft: () -> Unit,
    onDraftPlayer: (String) -> Unit,
    onChooseBatOrBowl: (Boolean) -> Unit,
    onSubmitDeliveryPick: (Int) -> Unit,
    onStartSecondInnings: () -> Unit,
    onSendReaction: (ReactionType) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    when (roomState) {
        is OnlineUiState.Loading -> {
            BackHandler(onBack = onCloseRoomView)
            StadiumAtmosphereBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerSkeletonCard(height = 130.dp)
                    ShimmerSkeletonCard(height = 84.dp)
                    ShimmerSkeletonCard(height = 84.dp)
                }
            }
        }
        is OnlineUiState.Error -> {
            BackHandler(onBack = onCloseRoomView)
            StadiumAtmosphereBackground {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 420.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ErrorNoticeCard(message = roomState.message)
                        PremiumActionButton(
                            text = "BACK TO DASHBOARD",
                            onClick = onCloseRoomView,
                            height = 54.dp,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }
        is OnlineUiState.Success -> {
            val room = roomState.data
            if (room == null) {
                BackHandler(onBack = onCloseRoomView)
                StadiumAtmosphereBackground {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BroadcastCard(
                            modifier = Modifier.widthIn(max = 420.dp),
                            contentPadding = PaddingValues(22.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text(
                                    text = "This room is no longer active.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                PremiumActionButton(
                                    text = "BACK TO DASHBOARD",
                                    onClick = onCloseRoomView,
                                    height = 52.dp,
                                    fontSize = 20.sp,
                                )
                            }
                        }
                    }
                }
                return
            }

            AnimatedContent(
                targetState = room.stage(),
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val enterX: (Int) -> Int = { if (forward) it / 6 else -it / 6 }
                    val exitX: (Int) -> Int = { if (forward) -it / 6 else it / 6 }
                    (slideInHorizontally(
                        initialOffsetX = enterX,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    ) + fadeIn(tween(240)) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                    )) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = exitX,
                            animationSpec = tween(200),
                        ) + fadeOut(tween(180)))
                },
                label = "online_room_stage_transition",
            ) { stage ->
                when (stage) {
                    RoomStage.WAITING -> WaitingLobbyView(
                        room = room,
                        currentUserId = currentUserId,
                        acceptedFriends = acceptedFriends,
                        isBusy = isBusy,
                        onLeave = onLeaveRoom,
                        onCopyCode = onCopyCode,
                        onInviteFriend = onInviteFriend,
                        onAddBot = onAddBot,
                        onRemoveBot = onRemoveBot,
                        onStartMatch = onStartMatch,
                    )

                    RoomStage.CAPTAIN_REVEAL -> CaptainRevealView(
                        room = room,
                        currentUserId = currentUserId,
                        onBack = onCloseRoomView,
                        onProceed = onProceedFromCaptainReveal,
                    )

                    RoomStage.DRAFT_TOSS -> OnlineTossView(
                        room = room,
                        currentUserId = currentUserId,
                        isDraftToss = true,
                        hapticsEnabled = hapticsEnabled,
                        onBack = onCloseRoomView,
                        onSubmitTossCall = onSubmitTossCall,
                        onSubmitTossNumber = onSubmitTossNumber,
                        onAdvanceToTeamDraft = onAdvanceToTeamDraft,
                        onChooseBatOrBowl = onChooseBatOrBowl,
                    )

                    RoomStage.TEAM_DRAFT -> TeamDraftView(
                        room = room,
                        currentUserId = currentUserId,
                        onBack = onCloseRoomView,
                        onDraftPlayer = onDraftPlayer,
                    )

                    RoomStage.MATCH_TOSS -> OnlineTossView(
                        room = room,
                        currentUserId = currentUserId,
                        isDraftToss = false,
                        hapticsEnabled = hapticsEnabled,
                        onBack = onCloseRoomView,
                        onSubmitTossCall = onSubmitTossCall,
                        onSubmitTossNumber = onSubmitTossNumber,
                        onAdvanceToTeamDraft = onAdvanceToTeamDraft,
                        onChooseBatOrBowl = onChooseBatOrBowl,
                    )

                    RoomStage.PLAYING -> OnlineMatchPlayView(
                        room = room,
                        currentUserId = currentUserId,
                        hapticsEnabled = hapticsEnabled,
                        soundEnabled = soundEnabled,
                        reactions = reactions,
                        onExit = onCloseRoomView,
                        onToggleSound = onToggleSound,
                        onSubmitPick = onSubmitDeliveryPick,
                        onSendReaction = onSendReaction,
                    )

                    RoomStage.INNINGS_BREAK -> OnlineInningsBreakView(
                        room = room,
                        currentUserId = currentUserId,
                        onExit = onCloseRoomView,
                        onStartSecondInnings = onStartSecondInnings,
                    )

                    RoomStage.FINISHED -> OnlineMatchResultView(
                        room = room,
                        currentUserId = currentUserId,
                        onBackToHub = onCloseRoomView,
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingLobbyView(
    room: GameRoom,
    currentUserId: String,
    acceptedFriends: List<Friendship>,
    isBusy: Boolean,
    onLeave: () -> Unit,
    onCopyCode: (String) -> Unit,
    onInviteFriend: (String, String) -> Unit,
    onAddBot: () -> Unit,
    onRemoveBot: () -> Unit,
    onStartMatch: () -> Unit,
) {
    BackHandler(onBack = onLeave)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val clipboard = LocalClipboardManager.current
    val isHost = room.hostUid == currentUserId
    val minPlayers = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 3
    val maxCap = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 10
    val format = MatchFormat.fromId(room.formatId)

    StadiumAtmosphereBackground(
        accentColor = stadium.sixGold,
        secondaryColor = stadium.fourMint,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoomStageTopBar(
                    title = "ROOM LOBBY",
                    subtitle = "${room.roomMode().label.uppercase()} • ${stringResource(format.nameRes).uppercase()}",
                    onBack = onLeave,
                )

                // Giant Shareable Room Code Card
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = stadium.sixGold,
                    selected = true,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BroadcastLivePill(
                            label = "LOBBY OPEN • SHARE CODE",
                            accentColor = stadium.sixGold,
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cs.background.copy(alpha = 0.85f),
                            border = BorderStroke(1.5.dp, stadium.sixGold.copy(alpha = 0.75f)),
                            onClick = {
                                clipboard.setText(AnnotatedString(room.roomCode))
                                onCopyCode(room.roomCode)
                            },
                            modifier = Modifier.testTag("copy_room_code_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = room.roomCode,
                                    fontFamily = ScoreboardMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    letterSpacing = 5.sp,
                                    color = stadium.sixGoldBright,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy room code",
                                    tint = stadium.sixGold,
                                )
                            }
                        }

                        if (room.roomMode() == RoomMode.TEAM_PLAY) {
                            val oddHint = if (room.rosterUids.size >= 3 && room.rosterUids.size % 2 != 0) {
                                "Odd count (${room.rosterUids.size}): Last player left after draft becomes COMMON!"
                            } else {
                                "2 random captains will toss twice: 1st for team draft, 2nd for bat/bowl."
                            }
                            Text(
                                text = oddHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = stadium.fourMint,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Joined Players Roster Header + Bot Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
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
                                .background(stadium.fourMint, RoundedCornerShape(999.dp)),
                        )
                        Text(
                            text = "PLAYERS IN ROOM (${room.rosterUids.size}/$maxCap)",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    if (isHost) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (room.botUids.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onRemoveBot,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("remove_bot_button"),
                                ) {
                                    Text("- BOT", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (room.rosterUids.size < maxCap) {
                                OutlinedButton(
                                    onClick = onAddBot,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, stadium.fourMint),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("add_bot_button"),
                                ) {
                                    Icon(
                                        Icons.Rounded.SmartToy,
                                        contentDescription = null,
                                        tint = stadium.fourMint,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ BOT", style = MaterialTheme.typography.labelSmall, color = stadium.fourMint)
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.staggeredEntrance(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    room.rosterUids.forEachIndexed { idx, pid ->
                        val isBot = room.isBot(pid)
                        val isMe = pid == currentUserId
                        BroadcastCard(
                            shape = RoundedCornerShape(16.dp),
                            accentColor = if (isMe) stadium.sixGold else if (isBot) cs.outline else stadium.fourMint,
                            selected = isMe,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isBot) cs.surfaceContainerHigh else cs.primaryContainer,
                                        border = BorderStroke(1.dp, if (isBot) cs.outlineVariant else stadium.sixGold.copy(alpha = 0.5f)),
                                        modifier = Modifier.size(34.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isBot) cs.onSurfaceVariant else cs.primary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = room.nameOf(pid) + if (isMe) " (You)" else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface,
                                    )
                                }

                                if (pid == room.hostUid) {
                                    TagPill("HOST", stadium.sixGold)
                                } else if (isBot) {
                                    TagPill("BOT", cs.onSurfaceVariant)
                                } else {
                                    TagPill("READY", stadium.fourMint)
                                }
                            }
                        }
                    }
                }

                // Invite Friends Quick List
                val invitableFriends = acceptedFriends.filter { it.friendUidFor(currentUserId) !in room.memberUids }
                if (invitableFriends.isNotEmpty()) {
                    Column(
                        modifier = Modifier.staggeredEntrance(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BroadcastSectionHeader(
                            title = "INVITE FRIENDS",
                            accentColor = stadium.fourMint,
                        )
                        invitableFriends.forEach { f ->
                            val fUid = f.friendUidFor(currentUserId)
                            val fName = f.friendNameFor(currentUserId)
                            BroadcastCard(
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(text = fName, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                                    Button(
                                        onClick = { onInviteFriend(fUid, fName) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = stadium.fourMintContainer,
                                            contentColor = stadium.fourMint,
                                        ),
                                        modifier = Modifier.testTag("invite_friend_$fUid"),
                                    ) {
                                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("INVITE", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isHost) {
                    val canStart = room.rosterUids.size >= minPlayers && !isBusy
                    PremiumActionButton(
                        text = if (canStart) {
                            if (room.roomMode() == RoomMode.TEAM_PLAY) "PICK RANDOM CAPTAINS" else "START MATCH TOSS"
                        } else {
                            "NEED AT LEAST $minPlayers PLAYERS"
                        },
                        icon = Icons.Rounded.PlayArrow,
                        enabled = canStart,
                        loading = isBusy,
                        height = 60.dp,
                        fontSize = 22.sp,
                        onClick = onStartMatch,
                        modifier = Modifier
                            .staggeredEntrance(4)
                            .testTag("start_online_match_button"),
                    )
                } else {
                    val isSpectator = room.isSpectator(currentUserId)
                    BroadcastCard(
                        shape = RoundedCornerShape(18.dp),
                        accentColor = stadium.fourMint,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(4),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BroadcastLivePill(
                                label = if (isSpectator) "SPECTATING LOBBY" else "STANDBY",
                                accentColor = stadium.fourMint,
                            )
                            Text(
                                text = if (isSpectator) {
                                    "You are spectating! The match will stream live once ${room.hostName} starts it."
                                } else {
                                    "Waiting for ${room.hostName} to start the match…"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainRevealView(
    room: GameRoom,
    currentUserId: String,
    onBack: () -> Unit,
    onProceed: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val isHostOrCap = currentUserId == room.hostUid ||
        currentUserId == room.captainAUid ||
        currentUserId == room.captainBUid

    StadiumAtmosphereBackground(
        accentColor = stadium.sixGold,
        secondaryColor = stadium.fourMint,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                RoomStageTopBar(
                    title = "CAPTAINS CHOSEN!",
                    subtitle = "RANDOMLY SELECTED FOR TEAM PLAY",
                    onBack = onBack,
                )

                BroadcastLivePill(
                    label = "HEAD-TO-HEAD CAPTAIN DRAFT",
                    accentColor = stadium.sixGold,
                    modifier = Modifier.staggeredEntrance(0),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CaptainSpotlightCard(
                        teamTitle = "TEAM GOLD",
                        captainName = room.nameOf(room.captainAUid),
                        isYou = room.captainAUid == currentUserId,
                        accent = stadium.sixGold,
                        modifier = Modifier.weight(1f),
                    )
                    CaptainSpotlightCard(
                        teamTitle = "TEAM MINT",
                        captainName = room.nameOf(room.captainBUid),
                        isYou = room.captainBUid == currentUserId,
                        accent = stadium.fourMint,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (room.commonPlayerUid.isNotBlank()) {
                    BroadcastCard(
                        shape = RoundedCornerShape(20.dp),
                        accentColor = stadium.sixGold,
                        selected = true,
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TagPill("COMMON PLAYER • PLAYS BOTH TEAMS", stadium.sixGold)
                            Text(
                                text = room.nameOf(room.commonPlayerUid),
                                fontFamily = StadiumDisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = cs.onSurface,
                            )
                            Text(
                                text = "Odd player count (3): assigned automatically as Common Player!",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    BroadcastCard(
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BroadcastSectionHeader(
                                title = "DRAFT POOL (${room.draftPoolUids.size} PLAYERS)",
                                accentColor = stadium.fourMint,
                            )
                            room.draftPoolUids.forEach { uid ->
                                Text(
                                    text = "• ${room.nameOf(uid)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurface,
                                )
                            }
                            if (room.rosterUids.size % 2 != 0) {
                                Text(
                                    text = "Odd total (${room.rosterUids.size}): The last player remaining after drafting becomes the COMMON player!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stadium.sixGold,
                                )
                            }
                        }
                    }
                }

                if (isHostOrCap) {
                    PremiumActionButton(
                        text = if (room.draftPoolUids.isEmpty()) {
                            "PROCEED TO BAT/BOWL TOSS"
                        } else {
                            "ODD-EVEN TOSS #1 (DRAFT ORDER)"
                        },
                        icon = Icons.Rounded.PlayArrow,
                        height = 60.dp,
                        fontSize = 21.sp,
                        onClick = onProceed,
                        modifier = Modifier
                            .staggeredEntrance(3)
                            .testTag("proceed_from_captains_button"),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineTossView(
    room: GameRoom,
    currentUserId: String,
    isDraftToss: Boolean,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onSubmitTossCall: (String) -> Unit,
    onSubmitTossNumber: (Int) -> Unit,
    onAdvanceToTeamDraft: () -> Unit,
    onChooseBatOrBowl: (Boolean) -> Unit,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val isCapA = currentUserId == room.captainAUid
    val isCapB = currentUserId == room.captainBUid
    val isCaller = currentUserId == room.tossCallerUid
    val isWinner = currentUserId == room.tossWinnerUid
    val bothThrown = room.tossPickA > 0L && room.tossPickB > 0L

    val myPickSubmitted = (isCapA && room.tossPickA > 0L) || (isCapB && room.tossPickB > 0L)
    val canThrow = (isCapA || isCapB) && room.tossCall.isNotBlank() && !myPickSubmitted && room.tossWinnerUid.isBlank()

    val title = when {
        isDraftToss -> "TOSS 1 OF 2 • DRAFT"
        room.roomMode() == RoomMode.TEAM_PLAY -> "TOSS 2 OF 2 • BAT / BOWL"
        else -> "ODD-EVEN MATCH TOSS"
    }
    val subtitle = if (isDraftToss) {
        "WINNER PICKS FIRST PLAYER IN TEAM DRAFT"
    } else {
        "WINNER CHOOSES TO BAT OR BOWL FIRST"
    }

    StadiumAtmosphereBackground(
        accentColor = if (isDraftToss) stadium.fourMint else stadium.sixGold,
        secondaryColor = if (isDraftToss) stadium.sixGold else stadium.fourMint,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoomStageTopBar(title = title, subtitle = subtitle, onBack = onBack)

                // Show final Teams + Common Player recap banner before Toss #2
                if (!isDraftToss && room.roomMode() == RoomMode.TEAM_PLAY) {
                    TeamRostersCompactStrip(room = room)
                }

                // Step 1: Call Odd / Even
                BroadcastCard(
                    shape = RoundedCornerShape(20.dp),
                    accentColor = stadium.sixGold,
                    selected = room.tossCall.isBlank() && isCaller,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (room.tossCall.isBlank()) {
                                "${room.nameOf(room.tossCallerUid)} is calling ODD or EVEN…"
                            } else {
                                "${room.nameOf(room.tossCallerUid)} called ${room.tossCall}!"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.primary,
                        )

                        if (room.tossCall.isBlank() && isCaller) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                PremiumActionButton(
                                    text = "ODD",
                                    height = 54.dp,
                                    fontSize = 26.sp,
                                    useMintPalette = false,
                                    onClick = { onSubmitTossCall("ODD") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_toss_odd"),
                                )
                                PremiumActionButton(
                                    text = "EVEN",
                                    height = 54.dp,
                                    fontSize = 26.sp,
                                    useMintPalette = true,
                                    onClick = { onSubmitTossCall("EVEN") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_toss_even"),
                                )
                            }
                        }
                    }
                }

                // Toss Duel Stage
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = if (bothThrown) stadium.sixGold else cs.outline,
                    selected = bothThrown,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OnlineTossHandSlot(
                                name = room.nameOf(room.captainAUid),
                                shownNumber = if (bothThrown) room.tossPickA.toInt() else null,
                                lockedIn = room.tossPickA > 0L,
                                accent = stadium.sixGold,
                                isMirrored = false,
                            )
                            Surface(
                                shape = CircleShape,
                                color = cs.background.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, stadium.rimTop),
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "VS",
                                        fontFamily = StadiumDisplayFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = stadium.sixGold,
                                    )
                                }
                            }
                            OnlineTossHandSlot(
                                name = room.nameOf(room.captainBUid),
                                shownNumber = if (bothThrown) room.tossPickB.toInt() else null,
                                lockedIn = room.tossPickB > 0L,
                                accent = stadium.fourMint,
                                isMirrored = true,
                            )
                        }

                        if (room.lastTossSummary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = stadium.sixGold.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = room.lastTossSummary,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = stadium.sixGoldBright,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }

                // Step 2: Captain Throw Pad OR Post-Toss Winner Action
                if (room.tossWinnerUid.isBlank()) {
                    if (canThrow) {
                        BroadcastLivePill(
                            label = "YOUR TURN (CAPTAIN) — THROW 1–6",
                            accentColor = stadium.sixGold,
                        )
                        NumberPadGrid(
                            enabled = true,
                            hapticsEnabled = hapticsEnabled,
                            onPickNumber = onSubmitTossNumber,
                        )
                    } else if (myPickSubmitted) {
                        BroadcastCard(
                            shape = RoundedCornerShape(16.dp),
                            accentColor = stadium.fourMint,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Your toss throw is locked! Waiting for opposing captain…",
                                style = MaterialTheme.typography.titleSmall,
                                color = stadium.fourMint,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        BroadcastCard(
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Spectating captains' Odd-Even toss live…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    if (isDraftToss) {
                        if (isWinner || currentUserId == room.hostUid) {
                            PremiumActionButton(
                                text = "BEGIN TEAM DRAFT",
                                icon = Icons.Rounded.Groups,
                                height = 60.dp,
                                fontSize = 24.sp,
                                onClick = onAdvanceToTeamDraft,
                                modifier = Modifier.testTag("start_team_draft_button"),
                            )
                        }
                    } else {
                        if (isWinner) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                PremiumActionButton(
                                    text = "BAT FIRST",
                                    icon = Icons.Rounded.SportsCricket,
                                    useMintPalette = false,
                                    height = 58.dp,
                                    fontSize = 21.sp,
                                    onClick = { onChooseBatOrBowl(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_choose_bat"),
                                )
                                PremiumActionButton(
                                    text = "BOWL FIRST",
                                    icon = Icons.Rounded.Shield,
                                    useMintPalette = true,
                                    height = 58.dp,
                                    fontSize = 21.sp,
                                    onClick = { onChooseBatOrBowl(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("online_choose_bowl"),
                                )
                            }
                        } else {
                            BroadcastCard(
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Waiting for ${room.nameOf(room.tossWinnerUid)} to choose Bat or Bowl…",
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
        }
    }
}

@Composable
private fun TeamDraftView(
    room: GameRoom,
    currentUserId: String,
    onBack: () -> Unit,
    onDraftPlayer: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val isMyTurn = room.draftTurnUid == currentUserId

    StadiumAtmosphereBackground(
        accentColor = if (isMyTurn) stadium.sixGold else stadium.fourMint,
        secondaryColor = stadium.fourMint,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RoomStageTopBar(
                    title = "TEAM DRAFT",
                    subtitle = "CAPTAINS PICK PLAYERS ONE BY ONE",
                    onBack = onBack,
                )

                // Active Turn Banner
                BroadcastCard(
                    shape = RoundedCornerShape(18.dp),
                    accentColor = if (isMyTurn) stadium.sixGold else stadium.fourMint,
                    selected = isMyTurn,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isMyTurn) {
                            BroadcastLivePill(label = "YOUR PICK", accentColor = stadium.sixGold)
                        }
                        Text(
                            text = if (isMyTurn) {
                                "Tap a player below to draft to your team!"
                            } else {
                                "${room.nameOf(room.draftTurnUid)} is choosing a teammate…"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isMyTurn) stadium.sixGoldBright else cs.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Live Team Gold vs Team Mint Rosters
                Box(modifier = Modifier.staggeredEntrance(1)) {
                    TeamRostersCompactStrip(room = room)
                }

                if (room.rosterUids.size % 2 != 0) {
                    Text(
                        text = "Odd players rule: The final player left after teams balance becomes COMMON (plays for both teams!).",
                        style = MaterialTheme.typography.bodySmall,
                        color = stadium.sixGold,
                    )
                }

                // Available Draft Pool
                BroadcastSectionHeader(
                    title = "AVAILABLE PLAYERS (${room.draftPoolUids.size})",
                    trailingText = if (isMyTurn) "SELECT ONE" else "DRAFTING",
                    accentColor = stadium.fourMint,
                    modifier = Modifier.staggeredEntrance(2),
                )

                Column(
                    modifier = Modifier.staggeredEntrance(3),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    room.draftPoolUids.forEach { candidateUid ->
                        BroadcastCard(
                            shape = RoundedCornerShape(16.dp),
                            accentColor = if (isMyTurn) stadium.fourMint else null,
                            selected = isMyTurn,
                            enabled = isMyTurn,
                            onClick = if (isMyTurn) ({ onDraftPlayer(candidateUid) }) else null,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("draft_candidate_$candidateUid"),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = room.nameOf(candidateUid) + if (candidateUid == currentUserId) " (You)" else "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                )
                                if (isMyTurn) {
                                    TagPill("PICK", stadium.fourMint)
                                } else {
                                    TagPill("POOL", cs.onSurfaceVariant)
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
internal fun RoomStageTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            shape = CircleShape,
            color = stadium.glassTop,
            border = BorderStroke(1.dp, stadium.rimTop),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("room_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = cs.onSurface,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 1.sp,
                color = cs.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
internal fun TeamRostersCompactStrip(room: GameRoom) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RosterColumnCard(
                title = "TEAM GOLD",
                members = room.teamAUids,
                captainUid = room.captainAUid,
                room = room,
                accent = stadium.sixGold,
                modifier = Modifier.weight(1f),
            )
            RosterColumnCard(
                title = "TEAM MINT",
                members = room.teamBUids,
                captainUid = room.captainBUid,
                room = room,
                accent = stadium.fourMint,
                modifier = Modifier.weight(1f),
            )
        }

        if (room.commonPlayerUid.isNotBlank()) {
            BroadcastCard(
                shape = RoundedCornerShape(16.dp),
                accentColor = stadium.sixGold,
                selected = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = stadium.sixGoldBright,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = room.nameOf(room.commonPlayerUid),
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface,
                        )
                    }
                    TagPill("COMMON • BOTH TEAMS", stadium.sixGold)
                }
            }
        }
    }
}

@Composable
private fun RosterColumnCard(
    title: String,
    members: List<String>,
    captainUid: String,
    room: GameRoom,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        accentColor = accent,
        contentPadding = PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = accent)
            members.forEach { uid ->
                val suffix = if (uid == captainUid) " (C)" else ""
                Text(
                    text = "• ${room.nameOf(uid)}$suffix",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface,
                )
            }
        }
    }
}

@Composable
private fun CaptainSpotlightCard(
    teamTitle: String,
    captainName: String,
    isYou: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "captain_spotlight_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "captain_halo_scale",
    )

    BroadcastCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        accentColor = accent,
        selected = true,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagPill(teamTitle, accent)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .shadow(10.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.12f)),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = captainName.take(1).uppercase(),
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = accent,
                )
            }
            Text(
                text = captainName,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isYou) "CAPTAIN (YOU)" else "CAPTAIN",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

@Composable
private fun OnlineTossHandSlot(
    name: String,
    shownNumber: Int?,
    lockedIn: Boolean,
    accent: Color,
    isMirrored: Boolean,
) {
    val cs = MaterialTheme.colorScheme
    val popScale by animateFloatAsState(
        targetValue = if (shownNumber != null) 1.08f else 1f,
        animationSpec = StadiumMotion.ExpressivePop,
        label = "toss_hand_pop",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = popScale
                scaleY = popScale
            },
        ) {
            HandGestureIllustration(
                number = shownNumber,
                accentColor = accent,
                isMirrored = isMirrored,
                modifier = Modifier.size(72.dp),
            )
        }
        Text(
            text = when {
                shownNumber != null -> shownNumber.toString()
                lockedIn -> "READY"
                else -> "…"
            },
            fontFamily = StadiumDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = if (shownNumber != null || lockedIn) accent else cs.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TagPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}
