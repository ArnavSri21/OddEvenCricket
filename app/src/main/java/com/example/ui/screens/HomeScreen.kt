package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.AiDifficulty
import com.example.game.CricketUiState
import com.example.game.MatchFormat
import com.example.online.Friendship
import com.example.online.GameRoom
import com.example.online.OnlineUiState
import com.example.online.PlayerProfile
import com.example.online.RoomInvite
import com.example.online.RoomMode
import com.example.online.RoomStage
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.SegmentedBarOption
import com.example.ui.components.StadiumMotion
import com.example.ui.components.StadiumSegmentedBar
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

private enum class DashboardPlayTab { ONLINE, SOLO }

@Composable
fun HomeScreen(
    currentUserId: String,
    state: CricketUiState,
    profileState: OnlineUiState<PlayerProfile?>,
    friendshipsState: OnlineUiState<List<Friendship>>,
    invitesState: OnlineUiState<List<RoomInvite>>,
    openRoomsState: OnlineUiState<List<GameRoom>>,
    myRoomsState: OnlineUiState<List<GameRoom>>,
    isBusy: Boolean,
    onSelectFormat: (MatchFormat) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenRules: () -> Unit,
    onStartToss: () -> Unit,
    onCreateRoom: (RoomMode, MatchFormat, Boolean) -> Unit,
    onJoinRoomByCode: (String) -> Unit,
    onOpenExistingRoom: (String) -> Unit,
    onSpectateRoom: (String) -> Unit,
    onAcceptInvite: (RoomInvite) -> Unit,
    onDismissInvite: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onChallengeFriend: (String, String, MatchFormat) -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val clipboard = LocalClipboardManager.current

    var playTab by rememberSaveable { mutableStateOf(DashboardPlayTab.ONLINE) }
    var onlineMode by rememberSaveable { mutableStateOf(RoomMode.DUEL_1V1) }
    var openLobby by rememberSaveable { mutableStateOf(true) }
    var joinCodeInput by rememberSaveable { mutableStateOf("") }
    var showAddFriendDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var friendToRemove by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    val profile = (profileState as? OnlineUiState.Success)?.data
    val friendships = (friendshipsState as? OnlineUiState.Success)?.data.orEmpty()
    val pendingFriendRequests = remember(friendships, currentUserId) {
        friendships.filter { it.status == "pending" && it.recipientUid == currentUserId }
    }
    val acceptedFriends = remember(friendships) {
        friendships.filter { it.status == "accepted" }
    }
    val invites = (invitesState as? OnlineUiState.Success)?.data.orEmpty()
    val myRooms = (myRoomsState as? OnlineUiState.Success)?.data.orEmpty()
    val openRooms = remember(openRoomsState, myRooms) {
        (openRoomsState as? OnlineUiState.Success)?.data.orEmpty()
            .filter { room -> myRooms.none { it.roomCode == room.roomCode } }
    }

    // Solo matches now persist directly to Firestore player_profiles + match_history; avoid double-counting!
    val totalWon = maxOf(state.sessionStats.matchesWon, profile?.matchesWon?.toInt() ?: 0)
    val bestScore = maxOf(state.sessionStats.bestPlayerScore, profile?.bestScore?.toInt() ?: 0)
    val winStreak = state.sessionStats.currentWinStreak
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(scrollState),
        ) {
            // Compact Floodlit Stadium Hero Header with GPU scroll parallax
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clipToBounds(),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_stadium_hero),
                    contentDescription = stringResource(R.string.cd_stadium_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scroll = scrollState.value.toFloat()
                            translationY = scroll * 0.30f
                            alpha = (1f - scroll / 460f).coerceIn(0.35f, 1f)
                        },
                )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        cs.background.copy(alpha = 0.56f),
                                        cs.background.copy(alpha = 0.30f),
                                        cs.background.copy(alpha = 0.92f),
                                        cs.background,
                                    )
                                )
                            ),
                    )

                    // Top Utility Bar: Player Friend Code Pill + Compact Utility Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val friendCode = profile?.friendCode.orEmpty()
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = cs.background.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.5f)),
                            onClick = {
                                if (friendCode.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(friendCode))
                                    onShowMessage("Copied your Friend Code $friendCode!")
                                }
                            },
                            modifier = Modifier.testTag("dashboard_copy_friend_code"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    text = (profile?.displayName ?: "Player").take(12),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (friendCode.isNotBlank()) {
                                    Text(
                                        text = friendCode,
                                        fontFamily = ScoreboardMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = stadium.sixGoldBright,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy friend code",
                                        tint = stadium.sixGold,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CompactGlassIconButton(
                                icon = if (state.soundEnabled) {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                },
                                contentDescription = stringResource(
                                    if (state.soundEnabled) R.string.cd_sound_on else R.string.cd_sound_off
                                ),
                                tint = if (state.soundEnabled) stadium.sixGoldBright else cs.onSurfaceVariant.copy(alpha = 0.45f),
                                onClick = onToggleSound,
                                testTag = "toggle_sound_button",
                            )
                            CompactGlassIconButton(
                                icon = Icons.Rounded.Vibration,
                                contentDescription = stringResource(
                                    if (state.hapticsEnabled) R.string.cd_haptics_on else R.string.cd_haptics_off
                                ),
                                tint = if (state.hapticsEnabled) stadium.fourMint else cs.onSurfaceVariant.copy(alpha = 0.45f),
                                onClick = onToggleHaptics,
                                testTag = "toggle_haptics_button",
                            )
                            CompactGlassIconButton(
                                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(R.string.cd_rules),
                                tint = cs.onSurface,
                                onClick = onOpenRules,
                                testTag = "open_rules_button",
                            )
                            CompactGlassIconButton(
                                icon = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign out",
                                tint = cs.onSurfaceVariant,
                                onClick = { showSignOutDialog = true },
                                testTag = "dashboard_sign_out_button",
                            )
                        }
                    }

                    // Title + Slim Inline Stats Row
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .staggeredEntrance(0),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "ODD-EVEN CRICKET",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            lineHeight = 40.sp,
                            letterSpacing = 1.sp,
                            color = cs.onBackground,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = cs.background.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, stadium.rimTop),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InlineStatBadge("WON", totalWon.toString(), stadium.fourMintBright)
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(cs.outlineVariant, CircleShape),
                                )
                                InlineStatBadge("BEST", bestScore.toString(), stadium.sixGoldBright)
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(cs.outlineVariant, CircleShape),
                                )
                                InlineStatBadge("STREAK", winStreak.toString(), if (winStreak > 0) stadium.sixGold else cs.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Decluttered Main Dashboard Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Live Alerts Strip (Only shown when invites or friend requests are pending!)
                    if (invites.isNotEmpty() || pendingFriendRequests.isNotEmpty()) {
                        Column(
                            modifier = Modifier.staggeredEntrance(1),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BroadcastSectionHeader(
                                title = "LIVE ALERTS",
                                accentColor = stadium.sixGold,
                            )
                            invites.forEach { invite ->
                                val modeLabel = RoomMode.fromWire(invite.roomMode).label
                                BroadcastCard(
                                    shape = RoundedCornerShape(16.dp),
                                    accentColor = stadium.sixGold,
                                    selected = true,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${invite.fromName} challenged you!",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = cs.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "$modeLabel • Room ${invite.roomCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = stadium.sixGoldBright,
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { onDismissInvite(invite.id) },
                                                modifier = Modifier.size(48.dp),
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Decline invite", tint = cs.onSurfaceVariant)
                                            }
                                            Button(
                                                onClick = { onAcceptInvite(invite) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = stadium.sixGold,
                                                    contentColor = cs.onPrimary,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("accept_invite_${invite.roomCode}"),
                                            ) {
                                                Text("JOIN", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            pendingFriendRequests.forEach { req ->
                                BroadcastCard(
                                    shape = RoundedCornerShape(16.dp),
                                    accentColor = stadium.fourMint,
                                    selected = true,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${req.requesterName} wants to be friends",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = cs.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "Code ${req.requesterCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = stadium.fourMintBright,
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { onRemoveFriend(req.id) },
                                                modifier = Modifier.size(48.dp),
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Decline friend", tint = cs.onSurfaceVariant)
                                            }
                                            Button(
                                                onClick = { onAcceptFriend(req.id) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = stadium.fourMint,
                                                    contentColor = cs.onSecondary,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("accept_friend_${req.requesterUid}"),
                                            ) {
                                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ACCEPT", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Unified Match Control Deck (Online Arena + Solo AI in one decluttered card!)
                    BroadcastCard(
                        shape = RoundedCornerShape(24.dp),
                        accentColor = if (playTab == DashboardPlayTab.ONLINE) stadium.sixGold else stadium.fourMint,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Top Segmented Switch: ONLINE vs SOLO AI (Spring-Sliding Glass Pill)
                            val playModeOptions = remember(stadium) {
                                listOf(
                                    SegmentedBarOption(
                                        label = "ONLINE",
                                        icon = Icons.Rounded.Public,
                                        accent = stadium.sixGold,
                                        brightAccent = stadium.sixGoldBright,
                                        testTag = "dashboard_mode_online",
                                    ),
                                    SegmentedBarOption(
                                        label = "SOLO AI",
                                        icon = Icons.Rounded.SmartToy,
                                        accent = stadium.fourMint,
                                        brightAccent = stadium.fourMintBright,
                                        testTag = "dashboard_mode_solo",
                                    ),
                                )
                            }
                            StadiumSegmentedBar(
                                options = playModeOptions,
                                selectedIndex = playTab.ordinal,
                                onSelectIndex = { idx ->
                                    playTab = DashboardPlayTab.entries.getOrElse(idx) { DashboardPlayTab.ONLINE }
                                },
                                height = 46.dp,
                            )

                            // Shared Compact Format Strip (replaces bulky 2x2 grid!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MatchFormat.entries.forEach { fmt ->
                                    val selected = state.selectedFormat == fmt
                                    val chipLabel = when (fmt) {
                                        MatchFormat.CLASSIC_1W -> "Classic"
                                        MatchFormat.SUPER_OVER -> "1 Ov"
                                        MatchFormat.T3_BLITZ -> "3 Ov"
                                        MatchFormat.T5_CLASH -> "5 Ov"
                                    }
                                    val accent = if (playTab == DashboardPlayTab.ONLINE) stadium.sixGold else stadium.fourMint
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) accent.copy(alpha = 0.22f) else cs.surfaceContainer,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) accent else cs.outlineVariant,
                                        ),
                                        onClick = { onSelectFormat(fmt) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .testTag("format_${fmt.id}"),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = chipLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) accent else cs.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }

                            // Mode-Specific Controls
                            AnimatedContent(
                                targetState = playTab,
                                transitionSpec = {
                                    val forward = targetState.ordinal >= initialState.ordinal
                                    val enterX: (Int) -> Int = { if (forward) it / 14 else -it / 14 }
                                    val exitX: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
                                    (slideInHorizontally(
                                        initialOffsetX = enterX,
                                        animationSpec = tween(150, easing = StadiumMotion.CrispOut),
                                    ) + fadeIn(tween(130))) togetherWith
                                        (slideOutHorizontally(
                                            targetOffsetX = exitX,
                                            animationSpec = tween(95),
                                        ) + fadeOut(tween(90)))
                                },
                                label = "dashboard_play_deck",
                            ) { tab ->
                                if (tab == DashboardPlayTab.ONLINE) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // 1v1 vs Team Play + Public toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RoomMode.entries.forEach { mode ->
                                                val isSel = onlineMode == mode
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSel) stadium.sixGold.copy(alpha = 0.2f) else cs.surfaceContainer,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSel) stadium.sixGold else cs.outlineVariant,
                                                    ),
                                                    onClick = { onlineMode = mode },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("online_mode_${mode.wireValue.lowercase()}"),
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Icon(
                                                            imageVector = if (mode == RoomMode.DUEL_1V1) Icons.Rounded.SportsCricket else Icons.Rounded.Groups,
                                                            contentDescription = null,
                                                            tint = if (isSel) stadium.sixGoldBright else cs.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = mode.label,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (isSel) stadium.sixGoldBright else cs.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (openLobby) stadium.fourMint.copy(alpha = 0.16f) else cs.surfaceContainer,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (openLobby) stadium.fourMint else cs.outlineVariant,
                                                ),
                                                onClick = { openLobby = !openLobby },
                                                modifier = Modifier
                                                    .height(44.dp)
                                                    .testTag("toggle_open_lobby"),
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = if (openLobby) "Public" else "Private",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (openLobby) stadium.fourMintBright else cs.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        }

                                        PremiumActionButton(
                                            text = "CREATE ${onlineMode.label.uppercase()} ROOM",
                                            icon = Icons.Rounded.Add,
                                            enabled = !isBusy,
                                            loading = isBusy,
                                            height = 56.dp,
                                            fontSize = 21.sp,
                                            onClick = { onCreateRoom(onlineMode, state.selectedFormat, openLobby) },
                                            modifier = Modifier.testTag("create_room_button"),
                                        )

                                        // Compact Inline Join Room by Code
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OutlinedTextField(
                                                value = joinCodeInput,
                                                onValueChange = { joinCodeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) },
                                                placeholder = { Text("Enter 6-char Room Code", style = MaterialTheme.typography.bodySmall) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = stadium.fourMint,
                                                    unfocusedBorderColor = cs.outlineVariant,
                                                    focusedContainerColor = cs.background.copy(alpha = 0.45f),
                                                    unfocusedContainerColor = cs.background.copy(alpha = 0.3f),
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("join_room_code_input"),
                                            )
                                            Button(
                                                onClick = {
                                                    val code = joinCodeInput
                                                    joinCodeInput = ""
                                                    onJoinRoomByCode(code)
                                                },
                                                enabled = joinCodeInput.length == 6 && !isBusy,
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = stadium.fourMint,
                                                    contentColor = cs.onSecondary,
                                                ),
                                                modifier = Modifier
                                                    .height(54.dp)
                                                    .testTag("join_room_button"),
                                            ) {
                                                Text("JOIN", style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                } else {
                                    // Clean Decluttered Solo vs AI Deck
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            AiDifficulty.entries.forEach { diff ->
                                                val sel = state.selectedDifficulty == diff
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (sel) stadium.fourMint.copy(alpha = 0.22f) else cs.surfaceContainer,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (sel) stadium.fourMint else cs.outlineVariant,
                                                    ),
                                                    onClick = { onSelectDifficulty(diff) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("difficulty_${diff.id}"),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = stringResource(diff.nameRes),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = if (sel) stadium.fourMintBright else cs.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        PremiumActionButton(
                                            text = "PLAY SOLO VS AI",
                                            icon = Icons.Rounded.PlayArrow,
                                            useMintPalette = true,
                                            height = 56.dp,
                                            fontSize = 22.sp,
                                            onClick = onStartToss,
                                            modifier = Modifier.testTag("start_toss_button"),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Integrated Friends Squad Strip (Quick Add + 1-Tap Direct Duel!)
                    Column(
                        modifier = Modifier.staggeredEntrance(3),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(14.dp)
                                        .background(stadium.fourMint, RoundedCornerShape(999.dp)),
                                )
                                Text(
                                    text = "FRIENDS (${acceptedFriends.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurfaceVariant,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = stadium.fourMintContainer,
                                border = BorderStroke(1.dp, stadium.fourMint.copy(alpha = 0.65f)),
                                onClick = { showAddFriendDialog = true },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("open_add_friend_dialog"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = "Add friend",
                                        tint = stadium.fourMintBright,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "ADD FRIEND",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = stadium.fourMintBright,
                                    )
                                }
                            }
                        }

                        if (acceptedFriends.isEmpty()) {
                            BroadcastCard(
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showAddFriendDialog = true },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Play with your squad",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = cs.onSurface,
                                        )
                                        Text(
                                            text = "Add friends by 6-char code to challenge 1v1 or build a Group.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = null,
                                        tint = stadium.fourMintBright,
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(acceptedFriends, key = { it.id }) { friendship ->
                                    val fUid = friendship.friendUidFor(currentUserId)
                                    val fName = friendship.friendNameFor(currentUserId)
                                    BroadcastCard(
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(12.dp),
                                        onClick = { friendToRemove = friendship.id to fName },
                                        modifier = Modifier.width(136.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = cs.primaryContainer,
                                                border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.6f)),
                                                modifier = Modifier.size(40.dp),
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = fName.take(1).uppercase(),
                                                        fontFamily = StadiumDisplayFont,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = stadium.sixGoldBright,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = fName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = cs.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Button(
                                                onClick = { onChallengeFriend(fUid, fName, state.selectedFormat) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = stadium.sixGoldContainer,
                                                    contentColor = stadium.sixGoldBright,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                                    .testTag("duel_friend_$fUid"),
                                            ) {
                                                Text("DUEL 1V1", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Active & Open Online Rooms (Compact Live Feed on Dashboard)
                    if (myRooms.isNotEmpty() || openRooms.isNotEmpty()) {
                        Column(
                            modifier = Modifier.staggeredEntrance(4),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BroadcastSectionHeader(
                                title = "LIVE & OPEN ROOMS",
                                accentColor = stadium.sixGold,
                            )

                            myRooms.take(3).forEach { room ->
                                DashboardRoomRow(
                                    room = room,
                                    isMyRoom = true,
                                    onPrimary = { onOpenExistingRoom(room.roomCode) },
                                    onSpectate = null,
                                )
                            }

                            openRooms.take(4).forEach { room ->
                                DashboardRoomRow(
                                    room = room,
                                    isMyRoom = false,
                                    onPrimary = { onJoinRoomByCode(room.roomCode) },
                                    onSpectate = { onSpectateRoom(room.roomCode) },
                                )
                            }
                        }
                    }
                    // Clearance for the floating Liquid Glass Pill Taskbar
                    Spacer(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(104.dp),
                    )
                }
            }
        }

    if (showAddFriendDialog) {
        AddFriendCodeDialog(
            myFriendCode = profile?.friendCode.orEmpty(),
            isBusy = isBusy,
            onDismiss = { showAddFriendDialog = false },
            onCopyMyCode = { code ->
                clipboard.setText(AnnotatedString(code))
                onShowMessage("Copied your Friend Code $code!")
            },
            onSendRequest = { code ->
                showAddFriendDialog = false
                onSendFriendRequest(code)
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = cs.surfaceContainer,
            title = { Text("Sign Out?", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface) },
            text = { Text("You can sign back in anytime with Google.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel", color = cs.onSurface) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
                ) { Text("Sign Out") }
            },
        )
    }

    friendToRemove?.let { (friendshipId, friendName) ->
        AlertDialog(
            onDismissRequest = { friendToRemove = null },
            containerColor = cs.surfaceContainer,
            title = { Text(friendName, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface) },
            text = { Text("Remove $friendName from your friends list?", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) },
            dismissButton = {
                TextButton(onClick = { friendToRemove = null }) { Text("Keep", color = cs.onSurface) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        friendToRemove = null
                        onRemoveFriend(friendshipId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError),
                ) { Text("Remove") }
            },
        )
    }
}

@Composable
private fun CompactGlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    Surface(
        shape = CircleShape,
        color = cs.background.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, stadium.rimTop),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(testTag),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun InlineStatBadge(
    label: String,
    value: String,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = ScoreboardMonoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = accent,
        )
    }
}


@Composable
private fun DashboardRoomRow(
    room: GameRoom,
    isMyRoom: Boolean,
    onPrimary: () -> Unit,
    onSpectate: (() -> Unit)?,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val maxCap = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 10
    val format = MatchFormat.fromId(room.formatId)

    BroadcastCard(
        shape = RoundedCornerShape(16.dp),
        accentColor = if (isMyRoom) stadium.fourMint else null,
        selected = isMyRoom,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = room.roomCode,
                        fontFamily = ScoreboardMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isMyRoom) stadium.fourMintBright else stadium.sixGoldBright,
                    )
                    TagPill(
                        text = if (isMyRoom) room.stage().wireValue.uppercase() else room.roomMode().label.uppercase(),
                        accent = if (isMyRoom) stadium.fourMint else stadium.sixGold,
                    )
                }
                Text(
                    text = "${room.hostName} • ${stringResource(format.nameRes)} • ${room.rosterUids.size}/$maxCap",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onSpectate != null) {
                    OutlinedButton(
                        onClick = onSpectate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, stadium.fourMint.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Spectate room",
                            tint = stadium.fourMintBright,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Button(
                    onClick = onPrimary,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMyRoom) stadium.fourMint else stadium.sixGold,
                        contentColor = if (isMyRoom) cs.onSecondary else cs.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Text(
                        text = if (isMyRoom) "REJOIN" else "JOIN",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFriendCodeDialog(
    myFriendCode: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCopyMyCode: (String) -> Unit,
    onSendRequest: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var codeInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainer,
        title = {
            Text(
                text = "Add Friend",
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (myFriendCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cs.background.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.5f)),
                        onClick = { onCopyMyCode(myFriendCode) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Your Code", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = myFriendCode,
                                    fontFamily = ScoreboardMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = stadium.sixGoldBright,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy your friend code",
                                    tint = stadium.sixGold,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) },
                    placeholder = { Text("Enter Friend's 6-char Code") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_friend_code_input"),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = cs.onSurface) }
        },
        confirmButton = {
            Button(
                onClick = { onSendRequest(codeInput) },
                enabled = codeInput.length == 6 && !isBusy,
                modifier = Modifier.testTag("confirm_add_friend_button"),
            ) {
                Text("Send Request")
            }
        },
    )
}
