package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.MatchFormat
import com.example.online.CricketGroup
import com.example.online.Friendship
import com.example.online.GameRoom
import com.example.online.GroupChatMessage
import com.example.online.OnlineUiState
import com.example.online.RoomMode
import com.example.online.RoomStage
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.SegmentedBarOption
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.components.StadiumMotion
import com.example.ui.components.StadiumSegmentedBar
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

private enum class GroupDetailTab { SQUAD, CHAT }

@Composable
fun GroupsScreen(
    currentUserId: String,
    groupsState: OnlineUiState<List<CricketGroup>>,
    groupRoomsState: OnlineUiState<List<GameRoom>>,
    groupMessagesState: OnlineUiState<List<GroupChatMessage>>,
    selectedGroupCode: String?,
    acceptedFriends: List<Friendship>,
    isBusy: Boolean,
    onSelectGroup: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onJoinGroupByCode: (String) -> Unit,
    onAddFriendToGroup: (CricketGroup, String, String) -> Unit,
    onLeaveOrDeleteGroup: (CricketGroup) -> Unit,
    onChallengeMember: (CricketGroup, String, String, MatchFormat) -> Unit,
    onChallengeEveryone: (CricketGroup, MatchFormat) -> Unit,
    onJoinRoomByCode: (String) -> Unit,
    onOpenExistingRoom: (String) -> Unit,
    onSpectateRoom: (String) -> Unit,
    onSendGroupMessage: (CricketGroup, String) -> Unit,
    onDeleteGroupMessage: (String, String) -> Unit,
    onKickMember: (CricketGroup, String, String) -> Unit,
    onToggleMuteMember: (CricketGroup, String, String) -> Unit,
    onUpdateGroupSettings: (CricketGroup, String, String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val groups = (groupsState as? OnlineUiState.Success)?.data.orEmpty()
    val allGroupRooms = (groupRoomsState as? OnlineUiState.Success)?.data.orEmpty()
    val activeGroupCode = remember(selectedGroupCode, groups) {
        selectedGroupCode?.takeIf { code -> groups.any { it.groupCode == code } }
    }

    AnimatedContent(
        targetState = activeGroupCode,
        transitionSpec = {
            val enteringDetail = targetState != null && initialState == null
            val enterX: (Int) -> Int = { if (enteringDetail) it / 10 else -it / 10 }
            val exitX: (Int) -> Int = { if (enteringDetail) -it / 10 else it / 10 }
            (slideInHorizontally(
                initialOffsetX = enterX,
                animationSpec = tween(190, easing = StadiumMotion.CrispOut),
            ) + fadeIn(tween(150))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = exitX,
                    animationSpec = tween(120),
                ) + fadeOut(tween(100)))
        },
        label = "groups_hub_detail_transition",
    ) { code ->
        val selectedGroup = code?.let { c -> groups.firstOrNull { it.groupCode == c } }
        if (selectedGroup != null) {
            val groupRooms = remember(allGroupRooms, selectedGroup.groupCode) {
                allGroupRooms.filter { it.groupCode == selectedGroup.groupCode }
            }
            GroupDetailScreen(
                group = selectedGroup,
                groupRooms = groupRooms,
                messagesState = groupMessagesState,
                currentUserId = currentUserId,
                acceptedFriends = acceptedFriends,
                isBusy = isBusy,
                onBack = { onSelectGroup(null) },
                onAddFriendToGroup = { fUid, fName -> onAddFriendToGroup(selectedGroup, fUid, fName) },
                onLeaveOrDelete = { onLeaveOrDeleteGroup(selectedGroup) },
                onChallengeMember = { uid, name, fmt -> onChallengeMember(selectedGroup, uid, name, fmt) },
                onChallengeEveryone = { fmt -> onChallengeEveryone(selectedGroup, fmt) },
                onJoinRoomByCode = onJoinRoomByCode,
                onOpenExistingRoom = onOpenExistingRoom,
                onSpectateRoom = onSpectateRoom,
                onSendMessage = { text -> onSendGroupMessage(selectedGroup, text) },
                onDeleteMessage = { msgId -> onDeleteGroupMessage(selectedGroup.groupCode, msgId) },
                onKickMember = { uid, name -> onKickMember(selectedGroup, uid, name) },
                onToggleMuteMember = { uid, name -> onToggleMuteMember(selectedGroup, uid, name) },
                onUpdateSettings = { name, ann -> onUpdateGroupSettings(selectedGroup, name, ann) },
                onShowMessage = onShowMessage,
            )
        } else {
            GroupsHubListView(
                groupsState = groupsState,
                allGroupRooms = allGroupRooms,
                isBusy = isBusy,
                onSelectGroup = onSelectGroup,
                onCreateGroup = onCreateGroup,
                onJoinGroupByCode = onJoinGroupByCode,
            )
        }
    }
}

@Composable
private fun GroupsHubListView(
    groupsState: OnlineUiState<List<CricketGroup>>,
    allGroupRooms: List<GameRoom>,
    isBusy: Boolean,
    onSelectGroup: (String?) -> Unit,
    onCreateGroup: (String) -> Unit,
    onJoinGroupByCode: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }

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
                        label = "SQUAD CLUBS • WATCH & CHALLENGE",
                        accentColor = stadium.fourMint,
                    )
                    Text(
                        text = "CRICKET GROUPS",
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        color = cs.onBackground,
                    )
                    Text(
                        text = "Challenge friends 1v1, rally the whole club for team matches, or spectate live games.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                // Dual Action Bar: Create Group & Join by Code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PremiumActionButton(
                        text = "CREATE GROUP",
                        icon = Icons.Rounded.Add,
                        enabled = !isBusy,
                        height = 54.dp,
                        fontSize = 18.sp,
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_group_button"),
                    )
                    PremiumActionButton(
                        text = "JOIN CODE",
                        icon = Icons.Rounded.Key,
                        enabled = !isBusy,
                        useMintPalette = true,
                        height = 54.dp,
                        fontSize = 18.sp,
                        onClick = { showJoinDialog = true },
                        modifier = Modifier
                            .weight(0.85f)
                            .testTag("join_group_button"),
                    )
                }

                // Groups List
                Column(
                    modifier = Modifier.staggeredEntrance(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BroadcastSectionHeader(
                        title = "YOUR GROUPS",
                        accentColor = stadium.sixGold,
                    )

                    when (groupsState) {
                        is OnlineUiState.Loading -> {
                            ShimmerSkeletonCard(height = 90.dp)
                            ShimmerSkeletonCard(height = 90.dp)
                        }
                        is OnlineUiState.Error -> {
                            ErrorNoticeCard(message = groupsState.message)
                        }
                        is OnlineUiState.Success -> {
                            val groups = groupsState.data
                            if (groups.isEmpty()) {
                                BroadcastCard(
                                    shape = RoundedCornerShape(22.dp),
                                    contentPadding = PaddingValues(22.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Groups,
                                            contentDescription = null,
                                            tint = stadium.fourMint,
                                            modifier = Modifier.size(38.dp),
                                        )
                                        Text(
                                            text = "No cricket groups yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = cs.onSurface,
                                        )
                                        Text(
                                            text = "Create a club for your squad or join with a 6-character Group Code to play and spectate together!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                groups.forEach { group ->
                                    val roomsInGroup = allGroupRooms.filter { it.groupCode == group.groupCode }
                                    val liveCount = roomsInGroup.count { it.isLiveMatch() }
                                    val waitingCount = roomsInGroup.count { it.stage() == RoomStage.WAITING }

                                    BroadcastCard(
                                        shape = RoundedCornerShape(20.dp),
                                        accentColor = if (liveCount > 0) stadium.fourMint else stadium.sixGold,
                                        selected = liveCount > 0,
                                        onClick = { onSelectGroup(group.groupCode) },
                                        contentPadding = PaddingValues(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("group_card_${group.groupCode}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = cs.primaryContainer,
                                                    border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.55f)),
                                                    modifier = Modifier.size(44.dp),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = group.name.take(2).uppercase(),
                                                            fontFamily = StadiumDisplayFont,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 18.sp,
                                                            color = stadium.sixGoldBright,
                                                        )
                                                    }
                                                }
                                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Text(
                                                        text = group.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = cs.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "${group.memberUids.size} members • Code ${group.groupCode}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = cs.onSurfaceVariant,
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                if (liveCount > 0) {
                                                    BroadcastLivePill(
                                                        label = "$liveCount LIVE",
                                                        accentColor = stadium.fourMint,
                                                    )
                                                } else if (waitingCount > 0) {
                                                    TagPill("$waitingCount OPEN", stadium.sixGold)
                                                } else {
                                                    TagPill("ENTER", cs.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreateGroup(name)
            },
        )
    }

    if (showJoinDialog) {
        JoinGroupCodeDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { code ->
                showJoinDialog = false
                onJoinGroupByCode(code)
            },
        )
    }
}

@Composable
private fun GroupDetailScreen(
    group: CricketGroup,
    groupRooms: List<GameRoom>,
    messagesState: OnlineUiState<List<GroupChatMessage>>,
    currentUserId: String,
    acceptedFriends: List<Friendship>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onAddFriendToGroup: (String, String) -> Unit,
    onLeaveOrDelete: () -> Unit,
    onChallengeMember: (String, String, MatchFormat) -> Unit,
    onChallengeEveryone: (MatchFormat) -> Unit,
    onJoinRoomByCode: (String) -> Unit,
    onOpenExistingRoom: (String) -> Unit,
    onSpectateRoom: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onKickMember: (String, String) -> Unit,
    onToggleMuteMember: (String, String) -> Unit,
    onUpdateSettings: (String, String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val clipboard = LocalClipboardManager.current
    val isCaptain = group.isCaptain(currentUserId)

    var detailTab by rememberSaveable(group.groupCode) { mutableStateOf(GroupDetailTab.SQUAD) }
    var selectedFormat by rememberSaveable { mutableStateOf(MatchFormat.T3_BLITZ) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    var showCaptainSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var memberToKick by remember { mutableStateOf<Pair<String, String>?>(null) }

    val liveMatches = remember(groupRooms) { groupRooms.filter { it.isLiveMatch() } }
    val waitingRooms = remember(groupRooms) { groupRooms.filter { it.stage() == RoomStage.WAITING } }
    val addableFriends = remember(acceptedFriends, currentUserId, group.memberUids) {
        acceptedFriends.filter { it.friendUidFor(currentUserId) !in group.memberUids }
    }
    val chatCount = (messagesState as? OnlineUiState.Success)?.data?.size ?: 0

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top Bar with Back, Name, Captain Settings (Admin), Code Copy, Leave/Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = cs.surfaceContainer,
                            border = BorderStroke(1.dp, stadium.rimTop),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("group_detail_back"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back to groups",
                                    tint = cs.onSurface,
                                )
                            }
                        }

                        Column {
                            Text(
                                text = group.name.uppercase(),
                                fontFamily = StadiumDisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 26.sp,
                                color = cs.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${group.memberUids.size} MEMBERS • CODE ${group.groupCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = stadium.sixGoldBright,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isCaptain) {
                            Surface(
                                shape = CircleShape,
                                color = stadium.sixGoldContainer,
                                border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.7f)),
                            ) {
                                IconButton(
                                    onClick = { showCaptainSettingsDialog = true },
                                    modifier = Modifier.testTag("captain_admin_settings_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Captain Admin Settings",
                                        tint = stadium.sixGoldBright,
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = cs.surfaceContainer,
                            border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.5f)),
                        ) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(group.groupCode))
                                    onShowMessage("Copied Group Code ${group.groupCode}!")
                                },
                                modifier = Modifier.testTag("copy_group_code_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy group code",
                                    tint = stadium.sixGoldBright,
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = cs.surfaceContainer,
                            border = BorderStroke(1.dp, stadium.rimTop),
                        ) {
                            IconButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.testTag("leave_or_delete_group_button"),
                            ) {
                                Icon(
                                    imageVector = if (isCaptain) Icons.Rounded.DeleteOutline else Icons.AutoMirrored.Rounded.ExitToApp,
                                    contentDescription = if (isCaptain) "Delete group" else "Leave group",
                                    tint = cs.error,
                                )
                            }
                        }
                    }
                }

                // Segmented 2-Tab Spring-Sliding Pill Switcher: SQUAD & MATCHES vs GROUP CHAT
                val detailTabOptions = remember(chatCount, stadium) {
                    listOf(
                        SegmentedBarOption(
                            label = "SQUAD & MATCHES",
                            icon = Icons.Rounded.SportsCricket,
                            accent = stadium.sixGold,
                            brightAccent = stadium.sixGoldBright,
                            testTag = "group_tab_squad",
                        ),
                        SegmentedBarOption(
                            label = if (chatCount > 0) "GROUP CHAT ($chatCount)" else "GROUP CHAT",
                            icon = Icons.Rounded.ChatBubble,
                            accent = stadium.fourMint,
                            brightAccent = stadium.fourMintBright,
                            testTag = "group_tab_chat",
                        ),
                    )
                }
                StadiumSegmentedBar(
                    options = detailTabOptions,
                    selectedIndex = detailTab.ordinal,
                    onSelectIndex = { idx ->
                        detailTab = GroupDetailTab.entries.getOrElse(idx) { GroupDetailTab.SQUAD }
                    },
                    height = 46.dp,
                )

                AnimatedContent(
                    targetState = detailTab,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enterX: (Int) -> Int = { if (forward) it / 14 else -it / 14 }
                        val exitX: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
                        (slideInHorizontally(
                            initialOffsetX = enterX,
                            animationSpec = tween(160, easing = StadiumMotion.CrispOut),
                        ) + fadeIn(tween(130))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = exitX,
                                animationSpec = tween(100),
                            ) + fadeOut(tween(90)))
                    },
                    modifier = Modifier.weight(1f),
                    label = "group_detail_tab_transition",
                ) { activeTab ->
                    if (activeTab == GroupDetailTab.CHAT) {
                        GroupChatPanel(
                            group = group,
                            currentUserId = currentUserId,
                            messagesState = messagesState,
                            onSendMessage = onSendMessage,
                            onDeleteMessage = onDeleteMessage,
                            onOpenCaptainSettings = { showCaptainSettingsDialog = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                        if (group.announcement.isNotBlank()) {
                            GroupAnnouncementBanner(
                                announcement = group.announcement,
                                isCaptain = isCaptain,
                                onEditAnnouncement = { showCaptainSettingsDialog = true },
                            )
                        }

                        // Quick Challenge Everyone Card (with compact format strip)
                BroadcastCard(
                    shape = RoundedCornerShape(22.dp),
                    accentColor = stadium.sixGold,
                    selected = true,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "MATCH FORMAT",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(selectedFormat.nameRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = stadium.sixGoldBright,
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(MatchFormat.entries) { fmt ->
                                val isSelected = selectedFormat == fmt
                                val shortLabel = when (fmt) {
                                    MatchFormat.CLASSIC_1W -> "Classic"
                                    MatchFormat.SUPER_OVER -> "1 Ov"
                                    MatchFormat.T3_BLITZ -> "3 Ov"
                                    MatchFormat.T5_CLASH -> "5 Ov"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) stadium.sixGold.copy(alpha = 0.22f) else cs.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) stadium.sixGold else cs.outlineVariant,
                                    ),
                                    onClick = { selectedFormat = fmt },
                                    modifier = Modifier.height(40.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) stadium.sixGoldBright else cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        PremiumActionButton(
                            text = "CHALLENGE EVERYONE • TEAM MATCH",
                            icon = Icons.Rounded.Groups,
                            enabled = !isBusy,
                            loading = isBusy,
                            height = 56.dp,
                            fontSize = 19.sp,
                            onClick = { onChallengeEveryone(selectedFormat) },
                            modifier = Modifier.testTag("challenge_everyone_button"),
                        )
                    }
                }

                // Live & Waiting Group Matches (Join or Spectate!)
                Column(
                    modifier = Modifier.staggeredEntrance(1),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BroadcastSectionHeader(
                        title = "GROUP MATCHES (${liveMatches.size} LIVE • ${waitingRooms.size} OPEN)",
                        accentColor = stadium.fourMint,
                    )

                    if (groupRooms.isEmpty()) {
                        BroadcastCard(
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "No active group matches right now. Challenge everyone above or pick a member below for a 1v1 Duel!",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        // Live Matches First (Spectate Live!)
                        liveMatches.forEach { room ->
                            val isMember = currentUserId in room.memberUids
                            GroupLiveMatchCard(
                                room = room,
                                isMember = isMember,
                                onPrimaryAction = {
                                    if (isMember) onOpenExistingRoom(room.roomCode) else onSpectateRoom(room.roomCode)
                                },
                            )
                        }

                        // Waiting Group Rooms (Join or Spectate!)
                        waitingRooms.forEach { room ->
                            val isMember = currentUserId in room.memberUids
                            val canJoinAsPlayer = !isMember &&
                                (room.challengedUid.isBlank() || room.challengedUid == currentUserId)
                            GroupWaitingRoomCard(
                                room = room,
                                currentUserId = currentUserId,
                                isMember = isMember,
                                canJoinAsPlayer = canJoinAsPlayer,
                                onOpenOrJoin = {
                                    if (isMember) onOpenExistingRoom(room.roomCode) else onJoinRoomByCode(room.roomCode)
                                },
                                onSpectate = { onSpectateRoom(room.roomCode) },
                            )
                        }
                    }
                }

                // Group Members (1v1 Challenge any member!)
                Column(
                    modifier = Modifier.staggeredEntrance(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BroadcastSectionHeader(
                        title = "GROUP MEMBERS (${group.memberUids.size})",
                        accentColor = stadium.sixGold,
                    )
                    group.memberUids.forEach { memberUid ->
                        val isMe = memberUid == currentUserId
                        val isMemberCaptain = memberUid == group.ownerUid
                        val isMemberMuted = group.isMuted(memberUid)
                        val memberName = group.nameOf(memberUid)
                        BroadcastCard(
                            shape = RoundedCornerShape(16.dp),
                            accentColor = if (isMemberCaptain) stadium.sixGold else if (isMe) stadium.fourMint else null,
                            selected = isMe || isMemberCaptain,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isMemberCaptain) cs.primaryContainer else cs.surfaceContainerHigh,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isMemberCaptain) stadium.sixGold else cs.outlineVariant,
                                            ),
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = memberName.take(1).uppercase(),
                                                    fontFamily = StadiumDisplayFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = if (isMemberCaptain) stadium.sixGoldBright else cs.onSurface,
                                                )
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = memberName + if (isMe) " (You)" else "",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = cs.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                if (isMemberCaptain) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Shield,
                                                        contentDescription = null,
                                                        tint = stadium.sixGold,
                                                        modifier = Modifier.size(13.dp),
                                                    )
                                                    Text(
                                                        text = "Captain • Admin",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = stadium.sixGold,
                                                    )
                                                }
                                                if (isMemberMuted) {
                                                    TagPill("MUTED", stadium.wicketRed)
                                                }
                                            }
                                        }
                                    }

                                    if (!isMe) {
                                        Button(
                                            onClick = { onChallengeMember(memberUid, memberName, selectedFormat) },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = stadium.sixGoldContainer,
                                                contentColor = stadium.sixGoldBright,
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("challenge_member_$memberUid"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.SportsCricket,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("CHALLENGE", style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        TagPill("YOU", stadium.sixGold)
                                    }
                                }

                                // Captain Admin Moderation Strip for other members
                                if (isCaptain && !isMe) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { onToggleMuteMember(memberUid, memberName) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isMemberMuted) stadium.fourMint.copy(alpha = 0.7f) else stadium.sixGold.copy(alpha = 0.55f),
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("captain_mute_$memberUid"),
                                        ) {
                                            Icon(
                                                imageVector = if (isMemberMuted) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                                                contentDescription = null,
                                                tint = if (isMemberMuted) stadium.fourMintBright else stadium.sixGoldBright,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isMemberMuted) "UNMUTE CHAT" else "MUTE CHAT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isMemberMuted) stadium.fourMintBright else stadium.sixGoldBright,
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { memberToKick = memberUid to memberName },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, stadium.wicketRed.copy(alpha = 0.65f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(0.75f)
                                                .height(36.dp)
                                                .testTag("captain_kick_$memberUid"),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PersonRemove,
                                                contentDescription = null,
                                                tint = stadium.wicketRed,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "REMOVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = stadium.wicketRed,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Captain Quick-Add Friends to Group
                if (isCaptain && addableFriends.isNotEmpty()) {
                    Column(
                        modifier = Modifier.staggeredEntrance(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BroadcastSectionHeader(
                            title = "ADD FRIENDS TO GROUP",
                            accentColor = stadium.fourMint,
                        )
                        addableFriends.forEach { friendship ->
                            val fUid = friendship.friendUidFor(currentUserId)
                            val fName = friendship.friendNameFor(currentUserId)
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
                                    Text(
                                        text = fName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = cs.onSurface,
                                    )
                                    Button(
                                        onClick = { onAddFriendToGroup(fUid, fName) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = stadium.fourMintContainer,
                                            contentColor = stadium.fourMintBright,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("add_friend_to_group_$fUid"),
                                    ) {
                                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("ADD", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(20.dp),
                )
                        }
                    }
                }
            }
        }

    if (showCaptainSettingsDialog && isCaptain) {
        CaptainAdminSettingsDialog(
            group = group,
            onDismiss = { showCaptainSettingsDialog = false },
            onSave = { newName, newAnnouncement ->
                showCaptainSettingsDialog = false
                onUpdateSettings(newName, newAnnouncement)
            },
        )
    }

    memberToKick?.let { (targetUid, targetName) ->
        AlertDialog(
            onDismissRequest = { memberToKick = null },
            containerColor = cs.surfaceContainer,
            title = {
                Text(
                    text = "Remove $targetName?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                )
            },
            text = {
                Text(
                    text = "$targetName will be removed from ${group.name} and lose access to group chat and matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            },
            dismissButton = {
                TextButton(onClick = { memberToKick = null }) {
                    Text("Cancel", color = cs.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberToKick = null
                        onKickMember(targetUid, targetName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.error,
                        contentColor = cs.onError,
                    ),
                ) {
                    Text("Remove")
                }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = cs.surfaceContainer,
            title = {
                Text(
                    text = if (isCaptain) "Delete ${group.name}?" else "Leave ${group.name}?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                )
            },
            text = {
                Text(
                    text = if (isCaptain) {
                        "Deleting this group removes it for all members."
                    } else {
                        "You can rejoin anytime with code ${group.groupCode}."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", color = cs.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveOrDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.error,
                        contentColor = cs.onError,
                    ),
                ) {
                    Text(if (isCaptain) "Delete" else "Leave")
                }
            },
        )
    }
}

@Composable
private fun GroupLiveMatchCard(
    room: GameRoom,
    isMember: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val stageLabel = when (room.stage()) {
        RoomStage.PLAYING -> "LIVE • ${room.runs}/${room.wickets} (${room.oversText()} OV)"
        RoomStage.INNINGS_BREAK -> "INNINGS BREAK • TARGET ${room.targetRuns}"
        RoomStage.TEAM_DRAFT -> "LIVE DRAFT"
        RoomStage.DRAFT_TOSS, RoomStage.MATCH_TOSS -> "LIVE TOSS"
        else -> "STARTING"
    }
    val matchupText = when {
        room.activeBatterUid.isNotBlank() && room.activeBowlerUid.isNotBlank() ->
            "${room.nameOf(room.activeBatterUid)} vs ${room.nameOf(room.activeBowlerUid)}"
        room.roomMode() == RoomMode.DUEL_1V1 && room.captainAUid.isNotBlank() ->
            "${room.nameOf(room.captainAUid)} vs ${room.nameOf(room.captainBUid)}"
        else -> "${room.hostName}'s ${room.roomMode().label}"
    }

    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        accentColor = stadium.fourMint,
        selected = true,
        contentPadding = PaddingValues(15.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BroadcastLivePill(label = stageLabel, accentColor = stadium.fourMint)
                if (room.spectatorUids.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Spectators watching",
                            tint = stadium.sixGoldBright,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "${room.spectatorUids.size}",
                            fontFamily = ScoreboardMonoFont,
                            fontSize = 12.sp,
                            color = stadium.sixGoldBright,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = matchupText,
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${room.roomMode().label} • ${room.rosterUids.size} players • Room ${room.roomCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMember) stadium.sixGold else stadium.fourMint,
                        contentColor = if (isMember) cs.onPrimary else cs.onSecondary,
                    ),
                    modifier = Modifier.testTag("spectate_room_${room.roomCode}"),
                ) {
                    Icon(
                        imageVector = if (isMember) Icons.Rounded.PlayArrow else Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isMember) "REJOIN" else "SPECTATE",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupWaitingRoomCard(
    room: GameRoom,
    currentUserId: String,
    isMember: Boolean,
    canJoinAsPlayer: Boolean,
    onOpenOrJoin: () -> Unit,
    onSpectate: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val isChallengeForMe = room.challengedUid == currentUserId
    val subtitle = when {
        isChallengeForMe -> "${room.hostName} challenged YOU to a 1v1 Duel!"
        room.challengedUid.isNotBlank() -> "1v1 Challenge by ${room.hostName} • Open for spectators"
        else -> "Group ${room.roomMode().label} by ${room.hostName} • ${room.rosterUids.size} joined"
    }

    BroadcastCard(
        shape = RoundedCornerShape(18.dp),
        accentColor = if (isChallengeForMe) stadium.sixGold else null,
        selected = isChallengeForMe,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (room.challengedUid.isNotBlank()) "1v1 CHALLENGE • ${room.roomCode}" else "GROUP MATCH • ${room.roomCode}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isChallengeForMe) stadium.sixGoldBright else cs.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isMember || canJoinAsPlayer) {
                    Button(
                        onClick = onOpenOrJoin,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = stadium.sixGold,
                            contentColor = cs.onPrimary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("join_group_room_${room.roomCode}"),
                    ) {
                        Text(
                            text = when {
                                isMember -> "OPEN LOBBY"
                                isChallengeForMe -> "ACCEPT CHALLENGE"
                                else -> "JOIN MATCH"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (!isMember) {
                    OutlinedButton(
                        onClick = onSpectate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, stadium.fourMint.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(if (canJoinAsPlayer) 0.75f else 1f)
                            .testTag("spectate_waiting_${room.roomCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = stadium.fourMintBright,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SPECTATE",
                            style = MaterialTheme.typography.labelMedium,
                            color = stadium.fourMintBright,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var groupName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainer,
        title = {
            Text(
                text = "Create Cricket Group",
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Give your squad a name. You'll get a 6-character code to share with friends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { if (it.length <= 40) groupName = it },
                    placeholder = { Text("e.g. Gully Legends XI") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_name_input"),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurface)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(groupName) },
                enabled = groupName.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_create_group_button"),
            ) {
                Text("Create")
            }
        },
    )
}

@Composable
private fun JoinGroupCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var codeInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainer,
        title = {
            Text(
                text = "Join Group by Code",
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the 6-character group code shared by your friend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) },
                    placeholder = { Text("6-CHAR CODE") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.secondary,
                        unfocusedBorderColor = cs.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_group_code_input"),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurface)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(codeInput) },
                enabled = codeInput.length == 6,
                modifier = Modifier.testTag("confirm_join_group_button"),
            ) {
                Text("Join")
            }
        },
    )
}
