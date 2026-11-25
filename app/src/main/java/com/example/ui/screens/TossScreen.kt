package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CricketUiState
import com.example.game.TeamSide
import com.example.game.TossCall
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.HandGestureIllustration
import com.example.ui.components.NumberPadGrid
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.StadiumMotion
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont

@Composable
fun TossScreen(
    state: CricketUiState,
    onSelectCall: (TossCall) -> Unit,
    onThrowTossNumber: (Int) -> Unit,
    onChooseBatOrBowl: (playerBatsFirst: Boolean) -> Unit,
    onStartAfterCpuToss: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val toss = state.tossState

    val revealScale = remember { Animatable(1f) }
    LaunchedEffect(toss.playerThrow, toss.cpuThrow, toss.winner) {
        if (toss.playerThrow != null || toss.winner != null) {
            revealScale.snapTo(0.84f)
            revealScale.animateTo(
                targetValue = 1f,
                animationSpec = StadiumMotion.ExpressivePop,
            )
        }
    }

    StadiumAtmosphereBackground {
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cs.surfaceContainer,
                        border = BorderStroke(1.dp, stadium.rimTop),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("toss_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = cs.onSurface,
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.toss_title),
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = cs.onBackground,
                        )
                        Text(
                            text = stringResource(state.activeFormat.nameRes).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = stadium.sixGoldBright,
                        )
                    }

                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Step 1: Call Odd or Even
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.staggeredEntrance(1),
                ) {
                    BroadcastSectionHeader(
                        title = stringResource(R.string.toss_step_call),
                        accentColor = stadium.sixGold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TossCallCard(
                            title = stringResource(R.string.toss_odd),
                            subtitle = "Sum: 3, 5, 7, 9, 11",
                            selected = toss.playerCall == TossCall.ODD,
                            enabled = toss.winner == null && !toss.isRevealing,
                            onClick = { onSelectCall(TossCall.ODD) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toss_call_odd"),
                        )
                        TossCallCard(
                            title = stringResource(R.string.toss_even),
                            subtitle = "Sum: 2, 4, 6, 8, 10, 12",
                            selected = toss.playerCall == TossCall.EVEN,
                            enabled = toss.winner == null && !toss.isRevealing,
                            onClick = { onSelectCall(TossCall.EVEN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toss_call_even"),
                        )
                    }
                }

                // Hand Throw Reveal Stage
                val stageAccent = when (toss.winner) {
                    TeamSide.PLAYER -> stadium.fourMint
                    TeamSide.CPU -> stadium.sixGold
                    null -> null
                }
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    accentColor = stageAccent,
                    selected = toss.winner != null,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(revealScale.value),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TossHandColumn(
                                label = stringResource(R.string.label_you),
                                number = toss.playerThrow,
                                accent = stadium.sixGold,
                                isMirrored = false,
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+",
                                    fontFamily = StadiumDisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    color = cs.onSurfaceVariant,
                                )
                                val sum = toss.sum
                                AnimatedVisibility(
                                    visible = sum != null,
                                    enter = scaleIn(
                                        initialScale = 0.6f,
                                        animationSpec = StadiumMotion.ExpressivePop,
                                    ) + fadeIn(),
                                ) {
                                    if (sum != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = cs.background.copy(alpha = 0.9f),
                                            border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.7f)),
                                        ) {
                                            Text(
                                                text = "= $sum (${if (toss.sumIsOdd == true) "ODD" else "EVEN"})",
                                                fontFamily = ScoreboardMonoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = stadium.sixGoldBright,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            TossHandColumn(
                                label = stringResource(R.string.label_cpu),
                                number = toss.cpuThrow,
                                accent = stadium.fourMint,
                                isMirrored = true,
                            )
                        }

                        val statusHint = when {
                            toss.playerCall == null -> "Pick ODD or EVEN above to begin the toss."
                            toss.playerThrow == null -> "You called ${toss.playerCall.name}. Now throw 1–6 below!"
                            toss.isRevealing -> "Counting fingers…"
                            toss.winner == TeamSide.PLAYER -> stringResource(R.string.toss_you_won)
                            else -> stringResource(R.string.toss_cpu_won)
                        }

                        Text(
                            text = statusHint,
                            style = if (toss.winner != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyMedium,
                            color = when (toss.winner) {
                                TeamSide.PLAYER -> stadium.fourMintBright
                                TeamSide.CPU -> stadium.sixGoldBright
                                null -> cs.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("toss_status_text"),
                        )
                    }
                }

                // Step 2: Throw Fingers OR Post-Toss Decision
                if (toss.winner == null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.staggeredEntrance(3),
                    ) {
                        BroadcastSectionHeader(
                            title = stringResource(R.string.toss_step_throw),
                            accentColor = stadium.fourMint,
                        )
                        NumberPadGrid(
                            enabled = toss.playerCall != null && !toss.isRevealing,
                            hapticsEnabled = state.hapticsEnabled,
                            onPickNumber = onThrowTossNumber,
                            accentIsBatting = true,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = toss.winner != null,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                    ) { it / 3 },
                ) {
                    if (toss.winner == TeamSide.PLAYER) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.toss_choose_action),
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                PremiumActionButton(
                                    text = stringResource(R.string.toss_bat_first),
                                    icon = Icons.Rounded.SportsCricket,
                                    onClick = { onChooseBatOrBowl(true) },
                                    useMintPalette = false,
                                    fontSize = 21.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("choose_bat_first_button"),
                                )
                                PremiumActionButton(
                                    text = stringResource(R.string.toss_bowl_first),
                                    icon = Icons.Rounded.Shield,
                                    onClick = { onChooseBatOrBowl(false) },
                                    useMintPalette = true,
                                    fontSize = 21.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("choose_bowl_first_button"),
                                )
                            }
                        }
                    } else {
                        val cpuChoiceText = if (toss.cpuDecisionBattingFirst == true) {
                            "CPU elected to BAT first. You will BOWL first!"
                        } else {
                            "CPU elected to BOWL first. You will BAT first!"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            BroadcastCard(
                                shape = RoundedCornerShape(16.dp),
                                accentColor = stadium.sixGold,
                                contentPadding = PaddingValues(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = cpuChoiceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cs.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            PremiumActionButton(
                                text = stringResource(R.string.toss_start_match),
                                icon = Icons.Rounded.PlayArrow,
                                onClick = onStartAfterCpuToss,
                                height = 58.dp,
                                modifier = Modifier.testTag("start_match_after_cpu_toss_button"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TossCallCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    BroadcastCard(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        accentColor = stadium.sixGold,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 33.sp,
                color = if (selected) stadium.sixGoldBright else cs.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TossHandColumn(
    label: String,
    number: Int?,
    accent: Color,
    isMirrored: Boolean,
) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = cs.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        HandGestureIllustration(
            number = number,
            accentColor = accent,
            isMirrored = isMirrored,
            modifier = Modifier.size(78.dp),
        )
        Text(
            text = number?.toString() ?: "?",
            fontFamily = StadiumDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            color = if (number != null) accent else cs.onSurfaceVariant,
        )
    }
}
