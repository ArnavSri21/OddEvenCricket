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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.online.Friendship
import com.example.online.OnlineUiState
import com.example.online.PlayerProfile
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.ShimmerSkeletonCard
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun FriendsScreen(
    currentUserId: String,
    profileState: OnlineUiState<PlayerProfile?>,
    friendshipsState: OnlineUiState<List<Friendship>>,
    isBusy: Boolean,
    onSendFriendRequest: (String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onSignOut: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val clipboard = LocalClipboardManager.current
    var friendCodeInput by rememberSaveable { mutableStateOf("") }

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
                    .widthIn(max = 640.dp)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        BroadcastLivePill(
                            label = "SQUAD NETWORK",
                            accentColor = stadium.sixGold,
                        )
                        Text(
                            text = "FRIENDS & SQUAD",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            letterSpacing = 1.sp,
                            color = cs.onBackground,
                        )
                        Text(
                            text = "ADD BY FRIEND CODE • INVITE TO ROOMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = stadium.glassTop,
                        border = BorderStroke(1.dp, stadium.rimTop),
                    ) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign out",
                                tint = cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Your Profile & Shareable Friend Code Card
                Box(modifier = Modifier.staggeredEntrance(1)) {
                    when (profileState) {
                        is OnlineUiState.Loading -> {
                            ShimmerSkeletonCard(height = 104.dp)
                        }
                        is OnlineUiState.Error -> {
                            ErrorNoticeCard(message = profileState.message)
                        }
                        is OnlineUiState.Success -> {
                            val profile = profileState.data
                            BroadcastCard(
                                shape = RoundedCornerShape(22.dp),
                                accentColor = stadium.sixGold,
                                selected = true,
                                contentPadding = PaddingValues(16.dp),
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
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            stadium.sixGold.copy(alpha = 0.35f),
                                                            stadium.sixGold.copy(alpha = 0.1f),
                                                        ),
                                                    ),
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = stadium.sixGoldBright,
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = profile?.displayName ?: "Player",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = cs.onSurface,
                                            )
                                            Text(
                                                text = "W ${profile?.matchesWon ?: 0} • P ${profile?.matchesPlayed ?: 0} • Best ${profile?.bestScore ?: 0}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cs.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    val myCode = profile?.friendCode ?: "------"
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = cs.background.copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.75f)),
                                        onClick = {
                                            clipboard.setText(AnnotatedString(myCode))
                                            onShowMessage("Copied Friend Code $myCode!")
                                        },
                                        modifier = Modifier.testTag("copy_friend_code_button"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text = myCode,
                                                fontFamily = ScoreboardMonoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = stadium.sixGoldBright,
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy friend code",
                                                tint = stadium.sixGold,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Friend By Code Card
                BroadcastCard(
                    shape = RoundedCornerShape(20.dp),
                    accentColor = stadium.fourMint,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BroadcastSectionHeader(
                            title = "ADD A FRIEND",
                            accentColor = stadium.fourMint,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { next ->
                                    friendCodeInput = next.uppercase().filter { it.isLetterOrDigit() }.take(6)
                                },
                                placeholder = { Text("6-CHAR CODE") },
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
                                    .testTag("friend_code_input"),
                            )

                            Button(
                                onClick = {
                                    onSendFriendRequest(friendCodeInput)
                                    friendCodeInput = ""
                                },
                                enabled = friendCodeInput.length == 6 && !isBusy,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("send_friend_request_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = stadium.fourMint,
                                    contentColor = cs.onSecondary,
                                ),
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ADD", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Friendships Sections
                Column(
                    modifier = Modifier.staggeredEntrance(3),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (friendshipsState) {
                        is OnlineUiState.Loading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ShimmerSkeletonCard(height = 70.dp)
                                ShimmerSkeletonCard(height = 70.dp)
                            }
                        }
                        is OnlineUiState.Error -> {
                            ErrorNoticeCard(message = friendshipsState.message)
                        }
                        is OnlineUiState.Success -> {
                            val all = friendshipsState.data
                            val incoming = all.filter { it.status == "pending" && it.recipientUid == currentUserId }
                            val sent = all.filter { it.status == "pending" && it.requesterUid == currentUserId }
                            val accepted = all.filter { it.status == "accepted" }

                            if (incoming.isNotEmpty()) {
                                BroadcastSectionHeader(
                                    title = "INCOMING REQUESTS (${incoming.size})",
                                    accentColor = stadium.sixGold,
                                )
                                incoming.forEach { req ->
                                    BroadcastCard(
                                        shape = RoundedCornerShape(18.dp),
                                        accentColor = stadium.sixGold,
                                        selected = true,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column {
                                                Text(
                                                    text = req.friendNameFor(currentUserId),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = cs.onSurface,
                                                )
                                                Text(
                                                    text = "Code: ${req.friendCodeFor(currentUserId)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cs.onSurfaceVariant,
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = stadium.fourMint,
                                                    onClick = { onAcceptFriend(req.id) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("accept_friend_${req.id}"),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Accept friend request",
                                                            tint = cs.onSecondary,
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = cs.errorContainer,
                                                    border = BorderStroke(1.dp, cs.error.copy(alpha = 0.5f)),
                                                    onClick = { onRemoveFriend(req.id) },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("decline_friend_${req.id}"),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Decline friend request",
                                                            tint = cs.error,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            BroadcastSectionHeader(
                                title = "YOUR FRIENDS (${accepted.size})",
                                accentColor = stadium.fourMint,
                            )

                            if (accepted.isEmpty()) {
                                BroadcastCard(
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(22.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = stadium.fourMint.copy(alpha = 0.14f),
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PersonAdd,
                                                    contentDescription = null,
                                                    tint = stadium.fourMint,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }
                                        Text(
                                            text = "No friends added yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = cs.onSurface,
                                        )
                                        Text(
                                            text = "Share your 6-char Friend Code above or enter a friend's code to team up online!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                accepted.forEach { friend ->
                                    BroadcastCard(
                                        shape = RoundedCornerShape(18.dp),
                                        accentColor = stadium.fourMint,
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
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
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = stadium.fourMint.copy(alpha = 0.18f),
                                                    border = BorderStroke(1.dp, stadium.fourMint.copy(alpha = 0.5f)),
                                                    modifier = Modifier.size(42.dp),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = friend.friendNameFor(currentUserId).take(1).uppercase(),
                                                            fontFamily = StadiumDisplayFont,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 22.sp,
                                                            color = stadium.fourMintBright,
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = friend.friendNameFor(currentUserId),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = cs.onSurface,
                                                    )
                                                    Text(
                                                        text = "Code: ${friend.friendCodeFor(currentUserId)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = cs.onSurfaceVariant,
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { onRemoveFriend(friend.id) },
                                                modifier = Modifier.testTag("remove_friend_${friend.id}"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PersonRemove,
                                                    contentDescription = "Remove friend",
                                                    tint = cs.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (sent.isNotEmpty()) {
                                BroadcastSectionHeader(
                                    title = "SENT REQUESTS (${sent.size})",
                                    accentColor = cs.onSurfaceVariant,
                                )
                                sent.forEach { req ->
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
                                                text = "${req.friendNameFor(currentUserId)} • Pending",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = cs.onSurfaceVariant,
                                            )
                                            IconButton(onClick = { onRemoveFriend(req.id) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Cancel request",
                                                    tint = cs.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
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
fun ErrorNoticeCard(message: String) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        shape = RoundedCornerShape(16.dp),
        accentColor = cs.error,
        selected = true,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onErrorContainer,
        )
    }
}
