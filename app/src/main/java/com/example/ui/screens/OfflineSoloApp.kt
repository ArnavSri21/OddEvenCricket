package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.audio.CricketAudioEffectsBridge
import com.example.audio.CricketSoundEffect
import com.example.audio.CricketSoundManager
import com.example.data.PreferencesRepository
import com.example.game.AiDifficulty
import com.example.game.CricketUiState
import com.example.game.CricketViewModel
import com.example.game.GameScreen
import com.example.game.MatchFormat
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.LiquidGlassTabItem
import com.example.ui.components.LiquidGlassTaskbar
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.RulesBottomSheet
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.StadiumMotion
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

private enum class OfflineTab(val label: String) {
    PLAY("Play"),
    STATS("Stats"),
}

private fun offlineSoloRank(screen: GameScreen): Int = when (screen) {
    GameScreen.Home -> 0
    GameScreen.Toss -> 1
    GameScreen.Match -> 2
    GameScreen.InningsBreak -> 3
    GameScreen.Result -> 4
}

/**
 * Resilient offline Solo AI Cricket experience used when Firebase configuration
 * (`google-services.json`) is not bundled in the APK (for example, CI builds without Firebase secrets).
 * Guarantees the app always opens cleanly with full Solo AI gameplay, sound FX, and session statistics.
 */
@Composable
fun OfflineSoloCricketApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val preferencesRepository = remember(appContext) { PreferencesRepository(appContext) }
    val soloViewModel: CricketViewModel = viewModel(
        key = "solo_offline",
        factory = CricketViewModel.Factory(preferencesRepository),
    )
    val soloState by soloViewModel.uiState.collectAsStateWithLifecycle()

    val soundManager = remember(appContext) { CricketSoundManager(appContext) }
    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    CricketAudioEffectsBridge(
        soundManager = soundManager,
        soundEnabled = soloState.soundEnabled,
        soloState = soloState,
        activeOnlineRoom = null,
        currentUserId = "offline_player",
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

    var currentTab by rememberSaveable { mutableStateOf(OfflineTab.PLAY) }
    val inActiveSoloMatch = currentTab == OfflineTab.PLAY && soloState.screen != GameScreen.Home
    val showTaskbar = !inActiveSoloMatch

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val glassTabs = remember {
        listOf(
            LiquidGlassTabItem(
                label = OfflineTab.PLAY.label,
                icon = Icons.Rounded.SportsCricket,
                contentDescription = "Solo Cricket Play",
                testTag = "offline_tab_play",
            ),
            LiquidGlassTabItem(
                label = OfflineTab.STATS.label,
                icon = Icons.Rounded.BarChart,
                contentDescription = "Match Statistics",
                testTag = "offline_tab_stats",
            ),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            StadiumAtmosphereBackground(
                accentColor = if (currentTab == OfflineTab.PLAY) stadium.fourMint else stadium.sixGold,
                secondaryColor = if (currentTab == OfflineTab.PLAY) stadium.sixGold else stadium.fourMint,
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enterX: (Int) -> Int = { if (forward) it / 14 else -it / 14 }
                        val exitX: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
                        (slideInHorizontally(
                            initialOffsetX = enterX,
                            animationSpec = tween(180, easing = StadiumMotion.CrispOut),
                        ) + fadeIn(tween(150))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = exitX,
                                animationSpec = tween(110),
                            ) + fadeOut(tween(100)))
                    },
                    label = "offline_tab_transition",
                ) { tab ->
                    when (tab) {
                        OfflineTab.PLAY -> {
                            AnimatedContent(
                                targetState = soloState.screen,
                                transitionSpec = {
                                    val forward = offlineSoloRank(targetState) >= offlineSoloRank(initialState)
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
                                label = "offline_solo_screen_transition",
                            ) { screen ->
                                when (screen) {
                                    GameScreen.Home -> OfflineSoloHomeScreen(
                                        state = soloState,
                                        onSelectFormat = soloViewModel::selectFormat,
                                        onSelectDifficulty = soloViewModel::selectDifficulty,
                                        onToggleHaptics = soloViewModel::toggleHaptics,
                                        onToggleSound = onToggleSound,
                                        onOpenRules = { soloViewModel.setRulesSheetVisible(true) },
                                        onStartToss = soloViewModel::startNewMatchToToss,
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

                        OfflineTab.STATS -> OfflineStatsTab(
                            soloState = soloState,
                            onStartMatch = { currentTab = OfflineTab.PLAY },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showTaskbar,
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
                        currentTab = OfflineTab.entries.getOrElse(idx) { OfflineTab.PLAY }
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

@Composable
private fun OfflineSoloHomeScreen(
    state: CricketUiState,
    onSelectFormat: (MatchFormat) -> Unit,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenRules: () -> Unit,
    onStartToss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BroadcastLivePill(
                        label = "SOLO STADIUM",
                        accentColor = stadium.fourMint,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OfflineGlassIconButton(
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
                            testTag = "offline_toggle_sound",
                        )
                        OfflineGlassIconButton(
                            icon = Icons.Rounded.Vibration,
                            contentDescription = stringResource(
                                if (state.hapticsEnabled) R.string.cd_haptics_on else R.string.cd_haptics_off
                            ),
                            tint = if (state.hapticsEnabled) stadium.fourMint else cs.onSurfaceVariant.copy(alpha = 0.45f),
                            onClick = onToggleHaptics,
                            testTag = "offline_toggle_haptics",
                        )
                        OfflineGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = stringResource(R.string.cd_rules),
                            tint = cs.onSurface,
                            onClick = onOpenRules,
                            testTag = "offline_open_rules",
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .staggeredEntrance(0),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                            OfflineStatChip("WON", state.sessionStats.matchesWon.toString(), stadium.fourMintBright)
                            Box(modifier = Modifier.size(3.dp).background(cs.outlineVariant, CircleShape))
                            OfflineStatChip("BEST", state.sessionStats.bestPlayerScore.toString(), stadium.sixGoldBright)
                            Box(modifier = Modifier.size(3.dp).background(cs.outlineVariant, CircleShape))
                            OfflineStatChip(
                                "STREAK",
                                state.sessionStats.currentWinStreak.toString(),
                                if (state.sessionStats.currentWinStreak > 0) stadium.sixGold else cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = stadium.fourMint,
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "MATCH FORMAT",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                        )
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
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) stadium.fourMint.copy(alpha = 0.22f) else cs.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) stadium.fourMint else cs.outlineVariant,
                                    ),
                                    onClick = { onSelectFormat(fmt) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("offline_format_${fmt.id}"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = chipLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) stadium.fourMintBright else cs.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "AI DIFFICULTY",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AiDifficulty.entries.forEach { diff ->
                                val sel = state.selectedDifficulty == diff
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (sel) stadium.sixGold.copy(alpha = 0.22f) else cs.surfaceContainer,
                                    border = BorderStroke(
                                        1.dp,
                                        if (sel) stadium.sixGold else cs.outlineVariant,
                                    ),
                                    onClick = { onSelectDifficulty(diff) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("offline_difficulty_${diff.id}"),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = stringResource(diff.nameRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (sel) stadium.sixGoldBright else cs.onSurfaceVariant,
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
                            modifier = Modifier.testTag("offline_start_toss_button"),
                        )
                    }
                }

                BroadcastCard(
                    shape = RoundedCornerShape(18.dp),
                    accentColor = stadium.sixGold,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = stadium.sixGoldBright,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Offline Solo Build",
                                style = MaterialTheme.typography.titleSmall,
                                color = cs.onSurface,
                            )
                            Text(
                                text = "Online multiplayer, friends, and groups require Firebase config (google-services.json) in the build.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(104.dp),
                )
            }
        }
    }
}

@Composable
private fun OfflineStatsTab(
    soloState: CricketUiState,
    onStartMatch: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val snapshot = remember(soloState) {
        buildSessionStatisticsSnapshot(
            soloState = soloState,
            activeOnlineRoom = null,
            currentUserId = "offline_player",
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.staggeredEntrance(0),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BroadcastLivePill(
                    label = snapshot.statusPill,
                    accentColor = if (snapshot.isLive) stadium.fourMint else stadium.sixGold,
                )
                Text(
                    text = "MATCH STATISTICS",
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    lineHeight = 40.sp,
                    color = cs.onBackground,
                )
                Text(
                    text = snapshot.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }

            MatchStatisticsContent(
                snapshot = snapshot,
                onStartMatch = onStartMatch,
            )

            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(104.dp),
            )
        }
    }
}

@Composable
private fun OfflineGlassIconButton(
    icon: ImageVector,
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
private fun OfflineStatChip(
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
