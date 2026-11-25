package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CricketUiState
import com.example.game.TeamSide
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont
import java.util.Locale

@Composable
fun InningsBreakScreen(
    state: CricketUiState,
    onStartSecondInnings: () -> Unit,
    onRequestExit: () -> Unit,
) {
    BackHandler(onBack = onRequestExit)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var showMatchStats by rememberSaveable { mutableStateOf(false) }
    val first = state.firstInningsCompleted ?: state.currentInnings
    val target = state.targetRuns ?: (first.runs + 1)
    val firstTeamName = if (first.battingSide == TeamSide.PLAYER) "YOU" else "CPU"
    val chasingTeamName = if (first.battingSide == TeamSide.PLAYER) "CPU" else "YOU"

    val infinite = rememberInfiniteTransition(label = "break_crest_pulse")
    val crestPulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "break_pulse",
    )

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
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(86.dp)
                        .staggeredEntrance(0),
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .graphicsLayer {
                                scaleX = crestPulse
                                scaleY = crestPulse
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(stadium.sixGold.copy(alpha = 0.3f), Color.Transparent)
                                )
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(stadium.sixGoldContainer)
                            .border(2.dp, stadium.sixGoldBright, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsCricket,
                            contentDescription = null,
                            tint = stadium.sixGoldBright,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.staggeredEntrance(1),
                ) {
                    BroadcastLivePill(
                        label = if (state.isSuperOverDecider) "SUPER OVER • HALF-TIME" else "END OF 1ST INNINGS",
                        accentColor = stadium.sixGold,
                    )
                    Text(
                        text = stringResource(R.string.break_title),
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = cs.onBackground,
                    )
                }

                // 1st Innings Summary Card
                BroadcastCard(
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(2),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "$firstTeamName POSTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                        )
                        Text(
                            text = "${first.runs}/${first.wickets}",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 62.sp,
                            lineHeight = 62.sp,
                            color = cs.onSurface,
                            modifier = Modifier.testTag("innings_break_score"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            BreakStatPill("OVERS", first.oversText)
                            BreakStatPill("CRR", String.format(Locale.US, "%.1f", first.runRate))
                            BreakStatPill("FOURS", first.fours.toString())
                            BreakStatPill("SIXES", first.sixes.toString())
                        }
                    }
                }

                // Target Banner
                BroadcastCard(
                    shape = RoundedCornerShape(20.dp),
                    accentColor = stadium.sixGold,
                    selected = true,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(3),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "TARGET FOR $chasingTeamName",
                            style = MaterialTheme.typography.labelMedium,
                            color = stadium.sixGoldBright,
                        )
                        Text(
                            text = "$target RUNS",
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                            color = cs.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("innings_break_target"),
                        )
                        val ballsSpec = state.activeFormat.maxBalls?.let { "in $it balls" }
                            ?: "with ${state.activeFormat.maxWickets} wicket in hand"
                        Text(
                            text = if (chasingTeamName == "YOU") {
                                "Strap on the pads — chase $target $ballsSpec!"
                            } else {
                                "Time to bowl — defend $target $ballsSpec!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
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
                        .staggeredEntrance(4)
                        .testTag("break_match_stats_button"),
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

                PremiumActionButton(
                    text = stringResource(R.string.break_start_second),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onStartSecondInnings,
                    height = 60.dp,
                    modifier = Modifier
                        .staggeredEntrance(5)
                        .testTag("start_second_innings_button"),
                )
            }
        }
    }

    if (showMatchStats) {
        val snapshot = remember(state) { buildSoloMatchStatisticsSnapshot(state) }
        MatchStatisticsModalSheet(
            snapshot = snapshot,
            onDismiss = { showMatchStats = false },
        )
    }
}

@Composable
private fun BreakStatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = ScoreboardMonoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
