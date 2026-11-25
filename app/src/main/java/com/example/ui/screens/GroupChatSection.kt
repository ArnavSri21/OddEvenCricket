package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.online.CricketGroup
import com.example.online.GroupChatMessage
import com.example.online.OnlineUiState
import com.example.ui.components.BroadcastCard
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun GroupAnnouncementBanner(
    announcement: String,
    isCaptain: Boolean,
    onEditAnnouncement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (announcement.isBlank()) return
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = stadium.sixGoldContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.60f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("group_announcement_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Campaign,
                contentDescription = "Captain announcement",
                tint = stadium.sixGoldBright,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CAPTAIN ANNOUNCEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = stadium.sixGoldBright,
                    fontSize = 10.sp,
                )
                Text(
                    text = announcement,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface,
                )
            }
            if (isCaptain) {
                IconButton(
                    onClick = onEditAnnouncement,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_announcement_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit announcement",
                        tint = stadium.sixGoldBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GroupChatPanel(
    group: CricketGroup,
    currentUserId: String,
    messagesState: OnlineUiState<List<GroupChatMessage>>,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onOpenCaptainSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val isCaptain = group.isCaptain(currentUserId)
    val isMuted = group.isMuted(currentUserId)

    var draft by rememberSaveable(group.groupCode) { mutableStateOf("") }
    val listState = rememberLazyListState()

    val messages = (messagesState as? OnlineUiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (group.announcement.isNotBlank()) {
            GroupAnnouncementBanner(
                announcement = group.announcement,
                isCaptain = isCaptain,
                onEditAnnouncement = onOpenCaptainSettings,
            )
        }

        // Messages Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (messagesState) {
                is OnlineUiState.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShimmerSkeletonCard(height = 68.dp)
                        ShimmerSkeletonCard(height = 68.dp)
                        ShimmerSkeletonCard(height = 68.dp)
                    }
                }
                is OnlineUiState.Error -> {
                    ErrorNoticeCard(message = messagesState.message)
                }
                is OnlineUiState.Success -> {
                    if (messages.isEmpty()) {
                        BroadcastCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = stadium.fourMint,
                                    modifier = Modifier.size(36.dp),
                                )
                                Text(
                                    text = "Squad dressing room is quiet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = if (isCaptain) {
                                        "As Group Captain (Admin), kick off the chat, pin announcements, or moderate messages anytime."
                                    } else {
                                        "Say hello, call out a rival for a 1v1 Duel, or rally the club for a Team Match!"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("group_chat_message_list"),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                GroupChatBubble(
                                    message = msg,
                                    isMine = msg.senderUid == currentUserId,
                                    canDelete = msg.senderUid == currentUserId || isCaptain,
                                    onDelete = { onDeleteMessage(msg.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sticky Composer or Muted Notice
        if (isMuted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = stadium.wicketRedContainer.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, stadium.wicketRed.copy(alpha = 0.60f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("chat_muted_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeOff,
                        contentDescription = "Muted in chat",
                        tint = stadium.wicketRed,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "You have been muted in group chat by the Group Captain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface,
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = stadium.glassBottom.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, stadium.rimTop),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 280) draft = it },
                        placeholder = {
                            Text(
                                text = if (isCaptain) "Message squad as Captain..." else "Message squad...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = stadium.fourMint.copy(alpha = 0.75f),
                            unfocusedBorderColor = cs.outlineVariant.copy(alpha = 0.4f),
                            focusedContainerColor = cs.background.copy(alpha = 0.45f),
                            unfocusedContainerColor = cs.background.copy(alpha = 0.30f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("group_chat_input"),
                    )

                    val canSend = draft.trim().isNotEmpty()
                    Surface(
                        shape = CircleShape,
                        color = if (canSend) stadium.sixGold else cs.surfaceContainerHigh,
                        modifier = Modifier.size(48.dp),
                    ) {
                        IconButton(
                            enabled = canSend,
                            onClick = {
                                val textToSend = draft.trim()
                                if (textToSend.isNotEmpty()) {
                                    draft = ""
                                    onSendMessage(textToSend)
                                }
                            },
                            modifier = Modifier.testTag("group_chat_send_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send chat message",
                                tint = if (canSend) cs.onPrimary else cs.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupChatBubble(
    message: GroupChatMessage,
    isMine: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val timeLabel = message.createdAt?.toDate()?.let { date ->
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } ?: "Now"

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp,
    )
    val bubbleColor = when {
        isMine -> stadium.sixGoldContainer.copy(alpha = 0.78f)
        message.isCaptain -> stadium.fourMintContainer.copy(alpha = 0.72f)
        else -> stadium.glassTop.copy(alpha = 0.92f)
    }
    val bubbleBorder = when {
        isMine -> stadium.sixGold.copy(alpha = 0.65f)
        message.isCaptain -> stadium.sixGold.copy(alpha = 0.75f)
        else -> stadium.rimTop
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = BorderStroke(1.dp, bubbleBorder),
            modifier = Modifier.widthIn(min = 120.dp, max = 310.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Sender Header + Captain Admin Badge + Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (isMine) "You" else message.senderName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isCaptain) stadium.sixGoldBright else cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (message.isCaptain) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = stadium.sixGold.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.7f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = "Group Captain Admin",
                                        tint = stadium.sixGoldBright,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = "CAPTAIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stadium.sixGoldBright,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = timeLabel,
                            fontFamily = ScoreboardMonoFont,
                            fontSize = 10.sp,
                            color = cs.onSurfaceVariant,
                        )
                        if (canDelete) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("delete_chat_msg_${message.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete message",
                                    tint = cs.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun CaptainAdminSettingsDialog(
    group: CricketGroup,
    onDismiss: () -> Unit,
    onSave: (newName: String, newAnnouncement: String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var nameInput by rememberSaveable(group.groupCode) { mutableStateOf(group.name) }
    var announcementInput by rememberSaveable(group.groupCode) { mutableStateOf(group.announcement) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainer,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = stadium.sixGoldBright,
                )
                Text(
                    text = "Captain Admin Controls",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "As Group Captain, you can rename the club, pin an announcement for all members, mute/unmute chatters, kick members, and moderate messages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 40) nameInput = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("captain_edit_group_name"),
                )
                OutlinedTextField(
                    value = announcementInput,
                    onValueChange = { if (it.length <= 140) announcementInput = it },
                    label = { Text("Pinned Announcement (Optional)") },
                    placeholder = { Text("e.g. Tournament starts at 8 PM!") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("captain_edit_announcement"),
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
                onClick = { onSave(nameInput.trim(), announcementInput.trim()) },
                enabled = nameInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("captain_save_settings_button"),
            ) {
                Text("Save Changes")
            }
        },
    )
}
