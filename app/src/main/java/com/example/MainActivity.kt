package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.CricketAudioEffectsBridge
import com.example.audio.CricketSoundEffect
import com.example.audio.CricketSoundManager
import com.example.data.PreferencesRepository
import com.example.game.CricketViewModel
import com.example.game.GameScreen
import com.example.online.OnlineCricketRepository
import com.example.online.OnlineCricketViewModel
import com.example.online.OnlineUiState
import com.example.online.authStateFlow
import com.example.online.signOutUser
import com.example.ui.components.LiquidGlassTabItem
import com.example.ui.components.LiquidGlassTaskbar
import com.example.ui.components.RulesBottomSheet
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.StadiumMotion
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExitMatchDialog
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InningsBreakScreen
import com.example.ui.screens.MatchResultScreen
import com.example.ui.screens.MatchScreen
import com.example.ui.screens.OfflineSoloCricketApp
import com.example.ui.screens.OnlineRoomHostScreen
import com.example.ui.screens.PlayerStatsScreen
import com.example.ui.screens.TossScreen
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.OddEvenCricketTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class AppTab(val label: String) {
    DASHBOARD("Home"),
    GROUPS("Groups"),
    STATS("Stats"),
}

private fun soloScreenRank(screen: GameScreen): Int = when (screen) {
    GameScreen.Home -> 0
    GameScreen.Toss -> 1
    GameScreen.Match -> 2
    GameScreen.InningsBreak -> 3
    GameScreen.Result -> 4
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            OddEvenCricketTheme {
                AppNavigationRoot()
            }
        }
    }
}

@Composable
fun AppNavigationRoot() {
    val context = LocalContext.current
    val firebaseAvailable = remember(context) {
        try {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        } catch (_: Throwable) {
            false
        }
    }

    if (!firebaseAvailable) {
        OfflineSoloCricketApp()
        return
    }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    AnimatedContent(
        targetState = currentUser?.uid,
        transitionSpec = {
            (fadeIn(tween(220, easing = StadiumMotion.CrispOut)) +
                scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = StadiumMotion.CrispOut))) togetherWith
                (fadeOut(tween(140)) + scaleOut(targetScale = 1.02f, animationSpec = tween(140)))
        },
        label = "auth_root_transition",
    ) { uid ->
        val user = currentUser
        if (uid == null || user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(uid) {
                AuthenticatedCricketApp(
                    currentUserId = uid,
                    initialDisplayName = user.displayName,
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedCricketApp(
    currentUserId: String,
    initialDisplayName: String?,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val preferencesRepository = remember(appContext) { PreferencesRepository(appContext) }
    val soloViewModel: CricketViewModel = viewModel(
        key = "solo_$currentUserId",
        factory = CricketViewModel.Factory(preferencesRepository),
    )

    val onlineViewModel: OnlineCricketViewModel = viewModel(
        key = "online_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = runCatching { app.getString(R.string.firestore_database_id) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                val db = if (databaseId != null) {
                    FirebaseFirestore.getInstance(databaseId)
                } else {
                    FirebaseFirestore.getInstance()
                }
                OnlineCricketViewModel(
                    repository = OnlineCricketRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName,
                )
            }
        },
    )

    val soloState by soloViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by onlineViewModel.profileState.collectAsStateWithLifecycle()
    val friendshipsState by onlineViewModel.friendshipsState.collectAsStateWithLifecycle()
    val invitesState by onlineViewModel.invitesState.collectAsStateWithLifecycle()
    val openRoomsState by onlineViewModel.openRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by onlineViewModel.myRoomsState.collectAsStateWithLifecycle()
    val groupsState by onlineViewModel.groupsState.collectAsStateWithLifecycle()
    val groupRoomsState by onlineViewModel.groupRoomsState.collectAsStateWithLifecycle()
    val selectedGroupCode by onlineViewModel.selectedGroupCode.collectAsStateWithLifecycle()
    val selectedGroupMessagesState by onlineViewModel.selectedGroupMessagesState.collectAsStateWithLifecycle()
    val matchHistoryState by onlineViewModel.matchHistoryState.collectAsStateWithLifecycle()
    val activeRoomCode by onlineViewModel.activeRoomCode.collectAsStateWithLifecycle()
    val activeRoomState by onlineViewModel.activeRoomState.collectAsStateWithLifecycle()
    val activeRoomReactions by onlineViewModel.activeRoomReactions.collectAsStateWithLifecycle()
    val feedbackMessage by onlineViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isBusy by onlineViewModel.isBusy.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val soundManager = remember(appContext) { CricketSoundManager(appContext) }
    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    CricketAudioEffectsBridge(
        soundManager = soundManager,
        soundEnabled = soloState.soundEnabled,
        soloState = soloState,
        activeOnlineRoom = (activeRoomState as? OnlineUiState.Success)?.data,
        currentUserId = currentUserId,
    )

    val onToggleSound: () -> Unit = {
        val willEnable = !soloState.soundEnabled
        soloViewModel.toggleSound()
        if (willEnable) {
            soundManager.setEnabled(true)
            soundManager.play(CricketSoundEffect.BAT_HIT_SINGLE, volume = 0.65f)
        } else {
            soundManager.setEnabled(false)
        }
    }

    // Persist completed Solo AI matches into Firestore career stats & sync completed Online matches into Current Session Stats
    DisposableEffect(soloViewModel, onlineViewModel) {
        soloViewModel.onMatchCompletedListener = { match ->
            onlineViewModel.recordSoloMatchCompleted(
                formatId = match.formatId,
                outcome = match.outcome,
                runsScored = match.runsScored,
                wicketsLost = match.wicketsLost,
                ballsFaced = match.ballsFaced,
                fours = match.fours,
                sixes = match.sixes,
                opponentName = match.opponentName,
            )
        }
        onlineViewModel.onOnlineMatchCompletedListener = { match ->
            soloViewModel.recordOnlineSessionMatch(
                modeLabel = match.modeLabel,
                opponentName = match.opponentName,
                outcome = match.outcome,
                runsScored = match.runsScored,
                ballsFaced = match.ballsFaced,
                wicketsTaken = match.wicketsTaken,
                ballsBowled = match.ballsBowled,
                runsConceded = match.runsConceded,
                fours = match.fours,
                sixes = match.sixes,
            )
        }
        onDispose {
            soloViewModel.onMatchCompletedListener = null
            onlineViewModel.onOnlineMatchCompletedListener = null
        }
    }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onlineViewModel.clearFeedback()
    }

    val pendingFriendRequestsCount = (friendshipsState as? OnlineUiState.Success)
        ?.data
        ?.count { it.status == "pending" && it.recipientUid == currentUserId } ?: 0
    val pendingInvitesCount = (invitesState as? OnlineUiState.Success)?.data?.size ?: 0
    val dashboardAlertCount = pendingFriendRequestsCount + pendingInvitesCount
    val activeGroupMatchesCount = (groupRoomsState as? OnlineUiState.Success)?.data?.size ?: 0

    val acceptedFriends = (friendshipsState as? OnlineUiState.Success)
        ?.data
        ?.filter { it.status == "accepted" }
        .orEmpty()

    val inActiveOnlineRoom = !activeRoomCode.isNullOrBlank()
    val inActiveSoloMatch = currentTab == AppTab.DASHBOARD && soloState.screen != GameScreen.Home
    val inGroupDetail = currentTab == AppTab.GROUPS && !selectedGroupCode.isNullOrBlank()
    val showFloatingTaskbar = !inActiveOnlineRoom && !inActiveSoloMatch && !inGroupDetail

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val glassTabs = remember(dashboardAlertCount, activeGroupMatchesCount, stadium) {
        listOf(
            LiquidGlassTabItem(
                label = AppTab.DASHBOARD.label,
                icon = Icons.Rounded.SportsCricket,
                contentDescription = "Home Dashboard",
                badgeCount = dashboardAlertCount,
                badgeColor = stadium.wicketRed,
                testTag = "nav_tab_dashboard",
            ),
            LiquidGlassTabItem(
                label = AppTab.GROUPS.label,
                icon = Icons.Rounded.Groups,
                contentDescription = "Cricket Groups",
                badgeCount = activeGroupMatchesCount,
                badgeColor = stadium.fourMint,
                testTag = "nav_tab_groups",
            ),
            LiquidGlassTabItem(
                label = AppTab.STATS.label,
                icon = Icons.Rounded.BarChart,
                contentDescription = "Player Statistics",
                badgeCount = 0,
                testTag = "nav_tab_stats",
            ),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = if (showFloatingTaskbar) 84.dp else 8.dp),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = inActiveOnlineRoom,
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically(
                            initialOffsetY = { it / 6 },
                            animationSpec = tween(240, easing = StadiumMotion.CrispOut),
                        ) + fadeIn(tween(190))) togetherWith
                            (fadeOut(tween(140)) + scaleOut(targetScale = 0.97f, animationSpec = tween(150)))
                    } else {
                        (fadeIn(tween(190)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220, easing = StadiumMotion.CrispOut))) togetherWith
                            (slideOutVertically(
                                targetOffsetY = { it / 7 },
                                animationSpec = tween(150),
                            ) + fadeOut(tween(130)))
                    }
                },
                label = "online_room_overlay_transition",
            ) { roomOpen ->
                if (roomOpen) {
                    OnlineRoomHostScreen(
                        currentUserId = currentUserId,
                        roomState = activeRoomState,
                        reactions = activeRoomReactions,
                        acceptedFriends = acceptedFriends,
                        hapticsEnabled = soloState.hapticsEnabled,
                        soundEnabled = soloState.soundEnabled,
                        isBusy = isBusy,
                        onToggleSound = onToggleSound,
                        onLeaveRoom = onlineViewModel::leaveActiveWaitingRoom,
                        onCloseRoomView = onlineViewModel::closeRoomView,
                        onCopyCode = { code ->
                            scope.launch { snackbarHostState.showSnackbar("Copied Room Code $code!") }
                        },
                        onInviteFriend = onlineViewModel::inviteFriendToActiveRoom,
                        onAddBot = onlineViewModel::addBotPlayer,
                        onRemoveBot = onlineViewModel::removeBotPlayer,
                        onStartMatch = onlineViewModel::startMatch,
                        onProceedFromCaptainReveal = onlineViewModel::proceedFromCaptainReveal,
                        onSubmitTossCall = onlineViewModel::submitTossCall,
                        onSubmitTossNumber = onlineViewModel::submitTossNumber,
                        onAdvanceToTeamDraft = onlineViewModel::advanceToTeamDraft,
                        onDraftPlayer = onlineViewModel::draftPlayer,
                        onChooseBatOrBowl = onlineViewModel::chooseBatOrBowl,
                        onSubmitDeliveryPick = onlineViewModel::submitDeliveryPick,
                        onStartSecondInnings = onlineViewModel::startSecondInnings,
                        onSendReaction = onlineViewModel::sendMatchReaction,
                    )
                } else {
                    val ambientAccent = when (currentTab) {
                        AppTab.DASHBOARD -> stadium.sixGold
                        AppTab.GROUPS -> stadium.fourMint
                        AppTab.STATS -> stadium.sixGoldBright
                    }
                    val ambientSecondary = when (currentTab) {
                        AppTab.DASHBOARD -> stadium.fourMint
                        AppTab.GROUPS -> stadium.sixGold
                        AppTab.STATS -> stadium.fourMint
                    }
                    StadiumAtmosphereBackground(
                        accentColor = ambientAccent,
                        secondaryColor = ambientSecondary,
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val forward = targetState.ordinal >= initialState.ordinal
                                val enterOffset: (Int) -> Int = { if (forward) it / 14 else -it / 14 }
                                val exitOffset: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
                                (slideInHorizontally(
                                    initialOffsetX = enterOffset,
                                    animationSpec = tween(180, easing = StadiumMotion.CrispOut),
                                ) + fadeIn(tween(150))) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = exitOffset,
                                        animationSpec = tween(110),
                                    ) + fadeOut(tween(100)))
                            },
                            label = "main_tab_transition",
                        ) { tab ->
                            when (tab) {
                                AppTab.DASHBOARD -> {
                                    AnimatedContent(
                                        targetState = soloState.screen,
                                        transitionSpec = {
                                            val forward = soloScreenRank(targetState) >= soloScreenRank(initialState)
                                            val enterX: (Int) -> Int = { if (forward) it / 10 else -it / 10 }
                                            val exitX: (Int) -> Int = { if (forward) -it / 10 else it / 10 }
                                            (slideInHorizontally(
                                                initialOffsetX = enterX,
                                                animationSpec = tween(210, easing = StadiumMotion.CrispOut),
                                            ) + fadeIn(tween(170)) + scaleIn(
                                                initialScale = 0.98f,
                                                animationSpec = tween(210, easing = StadiumMotion.CrispOut),
                                            )) togetherWith
                                                (slideOutHorizontally(
                                                    targetOffsetX = exitX,
                                                    animationSpec = tween(140),
                                                ) + fadeOut(tween(120)))
                                        },
                                        label = "solo_screen_transition",
                                    ) { screen ->
                                    when (screen) {
                                        GameScreen.Home -> HomeScreen(
                                            currentUserId = currentUserId,
                                            state = soloState,
                                            profileState = profileState,
                                            friendshipsState = friendshipsState,
                                            invitesState = invitesState,
                                            openRoomsState = openRoomsState,
                                            myRoomsState = myRoomsState,
                                            isBusy = isBusy,
                                            onSelectFormat = soloViewModel::selectFormat,
                                            onSelectDifficulty = soloViewModel::selectDifficulty,
                                            onToggleHaptics = soloViewModel::toggleHaptics,
                                            onToggleSound = onToggleSound,
                                            onOpenRules = { soloViewModel.setRulesSheetVisible(true) },
                                            onStartToss = soloViewModel::startNewMatchToToss,
                                            onCreateRoom = onlineViewModel::createRoom,
                                            onJoinRoomByCode = onlineViewModel::joinRoom,
                                            onOpenExistingRoom = onlineViewModel::openRoom,
                                            onSpectateRoom = onlineViewModel::spectateRoom,
                                            onAcceptInvite = onlineViewModel::acceptInviteAndJoin,
                                            onDismissInvite = onlineViewModel::dismissInvite,
                                            onSendFriendRequest = onlineViewModel::sendFriendRequest,
                                            onAcceptFriend = onlineViewModel::acceptFriend,
                                            onRemoveFriend = onlineViewModel::removeFriend,
                                            onChallengeFriend = onlineViewModel::challengeFriendDirect,
                                            onSignOut = {
                                                onlineViewModel.closeRoomView()
                                                signOutUser(credentialManager = credentialManager, scope = scope)
                                            },
                                            onShowMessage = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            },
                                        )
                                        GameScreen.Toss -> TossScreen(
                                            state = soloState,
                                            onSelectCall = soloViewModel::selectTossCall,
                                            onThrowTossNumber = soloViewModel::throwTossNumber,
                                            onChooseBatOrBowl = soloViewModel::chooseBattingOrder,
                                            onStartAfterCpuToss = soloViewModel::startMatchAfterCpuToss,
                                            onBack = soloViewModel::confirmExitToHome,
                                        )
                                        GameScreen.Match -> MatchScreen(
                                            state = soloState,
                                            onPickNumber = soloViewModel::playDelivery,
                                            onRequestExit = soloViewModel::requestExitMatch,
                                            onToggleSound = onToggleSound,
                                            onOpenRules = { soloViewModel.setRulesSheetVisible(true) },
                                        )
                                        GameScreen.InningsBreak -> InningsBreakScreen(
                                            state = soloState,
                                            onStartSecondInnings = soloViewModel::startSecondInnings,
                                            onRequestExit = soloViewModel::requestExitMatch,
                                        )
                                        GameScreen.Result -> MatchResultScreen(
                                            state = soloState,
                                            onPlaySuperOver = soloViewModel::startSuperOverDecider,
                                            onRematch = soloViewModel::startNewMatchToToss,
                                            onBackToHome = soloViewModel::confirmExitToHome,
                                        )
                                    }
                                }
                            }

                            AppTab.GROUPS -> GroupsScreen(
                                currentUserId = currentUserId,
                                groupsState = groupsState,
                                groupRoomsState = groupRoomsState,
                                groupMessagesState = selectedGroupMessagesState,
                                selectedGroupCode = selectedGroupCode,
                                acceptedFriends = acceptedFriends,
                                isBusy = isBusy,
                                onSelectGroup = onlineViewModel::selectGroup,
                                onCreateGroup = onlineViewModel::createGroup,
                                onJoinGroupByCode = onlineViewModel::joinGroup,
                                onAddFriendToGroup = onlineViewModel::addFriendToGroup,
                                onLeaveOrDeleteGroup = onlineViewModel::leaveOrDeleteGroup,
                                onChallengeMember = onlineViewModel::challengeGroupMember,
                                onChallengeEveryone = onlineViewModel::challengeEveryoneInGroup,
                                onJoinRoomByCode = onlineViewModel::joinRoom,
                                onOpenExistingRoom = onlineViewModel::openRoom,
                                onSpectateRoom = onlineViewModel::spectateRoom,
                                onSendGroupMessage = onlineViewModel::sendGroupChatMessage,
                                onDeleteGroupMessage = onlineViewModel::deleteGroupChatMessage,
                                onKickMember = onlineViewModel::kickGroupMember,
                                onToggleMuteMember = onlineViewModel::toggleMuteGroupMember,
                                onUpdateGroupSettings = onlineViewModel::updateGroupCaptainSettings,
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                            )

                            AppTab.STATS -> PlayerStatsScreen(
                                soloState = soloState,
                                activeOnlineRoom = (activeRoomState as? OnlineUiState.Success)?.data,
                                currentUserId = currentUserId,
                                profileState = profileState,
                                matchHistoryState = matchHistoryState,
                                onPlayQuickMatch = {
                                    currentTab = AppTab.DASHBOARD
                                },
                            )
                        }
                    }
                    }
                }
            }

            // Floating Apple-Style Liquid Glass Pill Taskbar overlaid cleanly at bottom center
            AnimatedVisibility(
                visible = showFloatingTaskbar,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(190, easing = StadiumMotion.CrispOut),
                ) + fadeIn(tween(150)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(140),
                ) + fadeOut(tween(110)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                LiquidGlassTaskbar(
                    items = glassTabs,
                    selectedIndex = currentTab.ordinal,
                    onSelectIndex = { idx ->
                        currentTab = AppTab.entries.getOrElse(idx) { AppTab.DASHBOARD }
                    },
                    hapticsEnabled = soloState.hapticsEnabled,
                )
            }
        }
    }

    if (soloState.showExitDialog) {
        ExitMatchDialog(
            onDismiss = soloViewModel::dismissExitDialog,
            onConfirmExit = soloViewModel::confirmExitToHome,
        )
    }

    if (soloState.showRulesSheet) {
        RulesBottomSheet(onDismiss = { soloViewModel.setRulesSheetVisible(false) })
    }
}
