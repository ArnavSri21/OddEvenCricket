package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.MatchFormat
import com.example.online.GameRoom
import com.example.online.OnlineUiState
import com.example.online.RoomInvite
import com.example.online.RoomMode
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.StadiumMotion
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun OnlineHubScreen(
    openRoomsState: OnlineUiState<List<GameRoom>>,
    myRoomsState: OnlineUiState<List<GameRoom>>,
    invitesState: OnlineUiState<List<RoomInvite>>,
    isBusy: Boolean,
    onCreateRoom: (RoomMode, MatchFormat, Boolean) -> Unit,
    onJoinRoomByCode: (String) -> Unit,
    onOpenExistingRoom: (String) -> Unit,
    onAcceptInvite: (RoomInvite) -> Unit,
    onDismissInvite: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    var selectedMode by rememberSaveable { mutableStateOf(RoomMode.TEAM_PLAY) }
    var selectedFormat by rememberSaveable { mutableStateOf(MatchFormat.T3_BLITZ) }
    var openPublicLobby by rememberSaveable { mutableStateOf(true) }
    var joinCodeInput by rememberSaveable { mutableStateOf("") }

    StadiumAtmosphereBackground(
        accentColor = stadium.fourMint,
        secondaryColor = stadium.sixGold,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header
                Column(
                    modifier = Modifier.staggeredEntrance(0),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BroadcastLivePill(
                        label = "LIVE MULTIPLAYER",
                        accentColor = stadium.fourMint,
                    )
                    Text(
                        text = "ONLINE STADIUM",
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        letterSpacing = 1.sp,
                        color = cs.onBackground,
                    )
                    Text(
                        text = "CREATE ROOMS • TEAM DRAFT • JOIN LIVE MATCHES",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                    )
                }

                // Incoming Friend Room Invites
                val invites = (invitesState as? OnlineUiState.Success)?.data.orEmpty()
                if (invites.isNotEmpty()) {
                    Column(
                        modifier = Modifier.staggeredEntrance(1),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BroadcastSectionHeader(
                            title = "ROOM INVITES (${invites.size})",
                            trailingText = "TAP TO JOIN",
                            accentColor = stadium.sixGold,
                        )
                        invites.forEach { invite ->
                            BroadcastCard(
                                shape = RoundedCornerShape(18.dp),
                                accentColor = stadium.sixGold,
                                selected = true,
                                contentPadding = PaddingValues(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = stadium.sixGold.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.55f)),
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Mail,
                                                    contentDescription = null,
                                                    tint = stadium.sixGold,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "${invite.fromName} invited you!",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = cs.onSurface,
                                            )
                                            Text(
                                                text = "${RoomMode.fromWire(invite.roomMode).label} • Room ${invite.roomCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cs.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { onDismissInvite(invite.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, cs.outline),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text("NO", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { onAcceptInvite(invite) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = stadium.sixGold,
                                                contentColor = cs.onPrimary,
                                            ),
                                            modifier = Modifier.testTag("accept_invite_${invite.roomCode}"),
                                        ) {
                                            Text("JOIN", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Create Game Room Card
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = cs.primary,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        BroadcastSectionHeader(
                            title = "CREATE GAME ROOM",
                            trailingText = if (selectedMode == RoomMode.TEAM_PLAY) "3–10 PLAYERS" else "2 PLAYERS",
                            accentColor = cs.primary,
                        )

                        // Mode Toggle: Team Play vs 1v1 Duel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ModeChoiceCard(
                                title = "Team Play",
                                subtitle = "3–10 players • 2 Captains • Double Toss",
                                icon = Icons.Rounded.Groups,
                                selected = selectedMode == RoomMode.TEAM_PLAY,
                                onClick = { selectedMode = RoomMode.TEAM_PLAY },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_team_play"),
                            )
                            ModeChoiceCard(
                                title = "1v1 Duel",
                                subtitle = "2 players • Head-to-head online",
                                icon = Icons.Rounded.Person,
                                selected = selectedMode == RoomMode.DUEL_1V1,
                                onClick = { selectedMode = RoomMode.DUEL_1V1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_duel_1v1"),
                            )
                        }

                        // Format Selector Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MatchFormat.entries.forEach { format ->
                                val selected = selectedFormat == format
                                val shortLabel = when (format) {
                                    MatchFormat.CLASSIC_1W -> "Classic"
                                    MatchFormat.SUPER_OVER -> "1 Ov"
                                    MatchFormat.T3_BLITZ -> "3 Ov"
                                    MatchFormat.T5_CLASH -> "5 Ov"
                                }
                                BroadcastCard(
                                    shape = RoundedCornerShape(12.dp),
                                    accentColor = stadium.fourMint,
                                    selected = selected,
                                    onClick = { selectedFormat = format },
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("online_format_${format.id}"),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) stadium.fourMintBright else cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // Public Lobby Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                val iconTint by animateColorAsState(
                                    targetValue = if (openPublicLobby) stadium.fourMint else cs.onSurfaceVariant,
                                    animationSpec = tween(220),
                                    label = "public_icon_tint",
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = iconTint.copy(alpha = 0.16f),
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (openPublicLobby) Icons.Rounded.Public else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (openPublicLobby) "Open Public Room" else "Private Room (Code / Invite)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface,
                                    )
                                    Text(
                                        text = if (openPublicLobby) "Visible in Open Rooms below" else "Only joinable with 6-char room code",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                            Switch(
                                checked = openPublicLobby,
                                onCheckedChange = { openPublicLobby = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = cs.onSecondary,
                                    checkedTrackColor = stadium.fourMint,
                                ),
                                modifier = Modifier.testTag("public_room_switch"),
                            )
                        }

                        PremiumActionButton(
                            text = "CREATE ${selectedMode.label.uppercase()} ROOM",
                            icon = Icons.Rounded.Add,
                            enabled = !isBusy,
                            loading = isBusy,
                            height = 58.dp,
                            fontSize = 22.sp,
                            onClick = { onCreateRoom(selectedMode, selectedFormat, openPublicLobby) },
                            modifier = Modifier.testTag("create_room_button"),
                        )
                    }
                }

                // Join By Room Code Card
                BroadcastCard(
                    shape = RoundedCornerShape(20.dp),
                    accentColor = stadium.fourMint,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(3),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BroadcastSectionHeader(
                            title = "JOIN BY ROOM CODE",
                            accentColor = stadium.fourMint,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { next ->
                                    joinCodeInput = next.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                },
                                placeholder = { Text("ENTER CODE") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = stadium.fourMint,
                                    unfocusedBorderColor = cs.outline,
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface,
                                    focusedContainerColor = cs.background.copy(alpha = 0.55f),
                                    unfocusedContainerColor = cs.background.copy(alpha = 0.35f),
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_room_code_input"),
                            )
                            Button(
                                onClick = {
                                    onJoinRoomByCode(joinCodeInput)
                                    joinCodeInput = ""
                                },
                                enabled = joinCodeInput.length == 6 && !isBusy,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_room_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = stadium.fourMint,
                                    contentColor = cs.onSecondary,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("JOIN", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // My Ongoing Rooms
                val myRooms = (myRoomsState as? OnlineUiState.Success)?.data.orEmpty()
                if (myRooms.isNotEmpty()) {
                    Column(
                        modifier = Modifier.staggeredEntrance(4),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BroadcastSectionHeader(
                            title = "YOUR ACTIVE ROOMS (${myRooms.size})",
                            accentColor = stadium.fourMint,
                        )
                        myRooms.forEach { room ->
                            RoomListItemCard(
                                room = room,
                                ctaLabel = "REJOIN",
                                accentHighlight = stadium.fourMint,
                                onClick = { onOpenExistingRoom(room.roomCode) },
                            )
                        }
                    }
                }

                // Open Public Rooms
                Column(
                    modifier = Modifier.staggeredEntrance(5),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BroadcastSectionHeader(
                        title = "OPEN PUBLIC ROOMS",
                        trailingText = (openRoomsState as? OnlineUiState.Success)?.data?.size?.let { "$it LIVE" },
                        accentColor = cs.primary,
                    )
                    when (openRoomsState) {
                        is OnlineUiState.Loading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ShimmerSkeletonCard(height = 76.dp)
                                ShimmerSkeletonCard(height = 76.dp)
                            }
                        }
                        is OnlineUiState.Error -> {
                            ErrorNoticeCard(message = openRoomsState.message)
                        }
                        is OnlineUiState.Success -> {
                            if (openRoomsState.data.isEmpty()) {
                                BroadcastCard(
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(18.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "No public rooms waiting right now — create one above and invite friends!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            } else {
                                openRoomsState.data.forEach { room ->
                                    RoomListItemCard(
                                        room = room,
                                        ctaLabel = "JOIN",
                                        accentHighlight = cs.primary,
                                        onClick = { onJoinRoomByCode(room.roomCode) },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ModeChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = StadiumMotion.TactileBounce,
        label = "mode_icon_scale",
    )

    BroadcastCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        accentColor = cs.primary,
        selected = selected,
        onClick = onClick,
        contentPadding = PaddingValues(13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) cs.primary else cs.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoomListItemCard(
    room: GameRoom,
    ctaLabel: String,
    accentHighlight: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val maxCap = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 10
    val format = MatchFormat.fromId(room.formatId)

    BroadcastCard(
        shape = RoundedCornerShape(18.dp),
        accentColor = accentHighlight,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_item_${room.roomCode}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = room.roomCode,
                        fontFamily = ScoreboardMonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = cs.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (room.roomMode() == RoomMode.TEAM_PLAY) {
                            stadium.fourMint.copy(alpha = 0.16f)
                        } else {
                            cs.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (room.roomMode() == RoomMode.TEAM_PLAY) {
                                stadium.fourMint.copy(alpha = 0.45f)
                            } else {
                                cs.outlineVariant
                            },
                        ),
                    ) {
                        Text(
                            text = room.roomMode().label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (room.roomMode() == RoomMode.TEAM_PLAY) stadium.fourMint else cs.onSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "Host: ${room.hostName} • ${stringResource(format.nameRes)} • ${room.rosterUids.size}/$maxCap players",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(stadium.sixGoldBright, stadium.sixGold),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = ctaLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onPrimary,
                )
            }
        }
    }
}
