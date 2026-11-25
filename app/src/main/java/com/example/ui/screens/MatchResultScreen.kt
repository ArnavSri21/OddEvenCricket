package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CricketUiState
import com.example.game.InningsStats
import com.example.game.MatchWinner
import com.example.game.TeamSide
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.StadiumAtmosphereBackground
import com.example.ui.components.VictoryConfettiOverlay
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont
import java.util.Locale

@Composable
fun MatchResultScreen(
    state: CricketUiState,
    onPlaySuperOver: () -> Unit,
    onRematch: () -> Unit,
    onBackToHome: () -> Unit,
) {
    BackHandler(onBack = onBackToHome)

    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    var showMatchStats by rememberSaveable { mutableStateOf(false) }
    val result = state.matchResult ?: return

    val heroImageRes = if (result.winner == MatchWinner.PLAYER) {
        R.drawable.img_trophy_banner
    } else {
        R.drawable.img_stadium_hero
    }

    val bannerHeadline = when (result.winner) {
        MatchWinner.PLAYER -> stringResource(R.string.result_you_won)
        MatchWinner.CPU -> stringResource(R.string.result_cpu_won)
        MatchWinner.TIE -> stringResource(R.string.result_tied)
    }

    val verdictAccent = when (result.winner) {
        MatchWinner.PLAYER -> stadium.sixGoldBright
        MatchWinner.CPU -> stadium.wicketRedBright
        MatchWinner.TIE -> stadium.fourMintBright
    }

    val playerInnings = if (result.firstInnings.battingSide == TeamSide.PLAYER) {
        result.firstInnings
    } else {
        result.secondInnings
    }
    val cpuInnings = if (result.firstInnings.battingSide == TeamSide.CPU) {
        result.firstInnings
    } else {
        result.secondInnings
    }

    StadiumAtmosphereBackground(
        accentColor = verdictAccent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Trophy / Stadium Result Header with Victory Confetti
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp),
                ) {
                    Image(
                        painter = painterResource(id = heroImageRes),
                        contentDescription = stringResource(R.string.cd_trophy_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        cs.background.copy(alpha = 0.32f),
                                        cs.background.copy(alpha = 0.56f),
                                        cs.background,
                                    )
                                )
                            ),
                    )

                    if (result.winner == MatchWinner.PLAYER) {
                        VictoryConfettiOverlay()
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .staggeredEntrance(0),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (result.wasSuperOver) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = cs.primaryContainer,
                                border = BorderStroke(1.dp, cs.primary),
                            ) {
                                Text(
                                    text = "SUPER OVER DECIDER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = bannerHeadline,
                            fontFamily = StadiumDisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp,
                            lineHeight = 50.sp,
                            color = verdictAccent,
                            modifier = Modifier.testTag("result_verdict_title"),
                        )
                        Text(
                            text = result.marginHeadline.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onBackground,
                            modifier = Modifier.testTag("result_margin_text"),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Super Over Tie-Breaker CTA when Tied
                    if (result.winner == MatchWinner.TIE) {
                        PremiumActionButton(
                            text = stringResource(R.string.result_play_super_over),
                            icon = Icons.Rounded.Bolt,
                            onClick = onPlaySuperOver,
                            useMintPalette = true,
                            height = 58.dp,
                            modifier = Modifier
                                .staggeredEntrance(1)
                                .testTag("play_super_over_button"),
                        )
                    }

                    // Side-by-Side Scorecard
                    BroadcastSectionHeader(
                        title = stringResource(R.string.result_scorecard),
                        accentColor = stadium.sixGold,
                        modifier = Modifier.staggeredEntrance(1),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        InningsScorecardBox(
                            title = "YOU",
                            innings = playerInnings,
                            accent = stadium.sixGold,
                            isWinner = result.winner == MatchWinner.PLAYER,
                            modifier = Modifier.weight(1f),
                        )
                        InningsScorecardBox(
                            title = "CPU",
                            innings = cpuInnings,
                            accent = stadium.fourMint,
                            isWinner = result.winner == MatchWinner.CPU,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Animated Run Progression Worm Chart
                    BroadcastSectionHeader(
                        title = stringResource(R.string.result_comparison),
                        accentColor = stadium.fourMint,
                        modifier = Modifier.staggeredEntrance(3),
                    )
                    RunWormChartCard(
                        playerInnings = playerInnings,
                        cpuInnings = cpuInnings,
                        modifier = Modifier.staggeredEntrance(3),
                    )

                    OutlinedButton(
                        onClick = { showMatchStats = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .staggeredEntrance(4)
                            .testTag("result_match_stats_button"),
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
                            fontSize = 20.sp,
                            color = stadium.sixGoldBright,
                        )
                    }

                    // Bottom Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredEntrance(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onBackToHome,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("result_home_button"),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.5.dp, stadium.rimTop),
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = cs.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.result_home),
                                fontFamily = StadiumDisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = cs.onSurface,
                            )
                        }

                        PremiumActionButton(
                            text = stringResource(R.string.result_rematch),
                            icon = Icons.Rounded.Refresh,
                            onClick = onRematch,
                            height = 56.dp,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("result_rematch_button"),
                        )
                    }
                }
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
private fun InningsScorecardBox(
    title: String,
    innings: InningsStats,
    accent: Color,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        accentColor = if (isWinner) accent else null,
        selected = isWinner,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Text(
                    text = "${innings.oversText} OV",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }

            Text(
                text = "${innings.runs}/${innings.wickets}",
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 42.sp,
                color = cs.onSurface,
            )

            ScoreMetricRow("Strike Rate", String.format(Locale.US, "%.0f", innings.strikeRate))
            ScoreMetricRow("Fours / Sixes", "${innings.fours} / ${innings.sixes}")
            ScoreMetricRow("Boundary Run", "${innings.bestBoundaryStreak}x")
        }
    }
}

@Composable
private fun ScoreMetricRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = ScoreboardMonoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = cs.onSurface,
        )
    }
}

@Composable
private fun RunWormChartCard(
    playerInnings: InningsStats,
    cpuInnings: InningsStats,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    // Smooth progressive worm draw animation from ball 0 -> final ball
    val chartProgress = remember { Animatable(0f) }
    LaunchedEffect(playerInnings.balls, cpuInnings.balls) {
        chartProgress.snapTo(0f)
        chartProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, delayMillis = 160, easing = FastOutSlowInEasing),
        )
    }

    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendChip(color = stadium.sixGold, label = "YOU (${playerInnings.runs})")
                LegendChip(color = stadium.fourMint, label = "CPU (${cpuInnings.runs})")
                LegendChip(color = stadium.wicketRed, label = "WICKET")
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
            ) {
                val maxBalls = maxOf(playerInnings.balls, cpuInnings.balls, 1)
                val maxRuns = maxOf(playerInnings.runs, cpuInnings.runs, 6)
                val padX = 12f
                val padY = 12f
                val chartW = size.width - padX * 2
                val chartH = size.height - padY * 2
                val revealLimit = maxBalls * chartProgress.value

                // Horizontal subtle grid lines
                for (i in 0..3) {
                    val y = padY + chartH * (i / 3f)
                    drawLine(
                        color = cs.outlineVariant.copy(alpha = 0.55f),
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.5f,
                    )
                }

                fun drawInningsLine(innings: InningsStats, lineColor: Color) {
                    if (innings.cumulativeRuns.isEmpty() || revealLimit <= 0f) return
                    val path = Path()
                    var prevX = padX
                    var prevY = padY + chartH
                    path.moveTo(prevX, prevY)
                    var tipX = prevX
                    var tipY = prevY

                    for (idx in innings.cumulativeRuns.indices) {
                        val ballNum = idx + 1
                        val totalRuns = innings.cumulativeRuns[idx]
                        val targetX = padX + chartW * (ballNum.toFloat() / maxBalls.toFloat())
                        val targetY = padY + chartH * (1f - (totalRuns.toFloat() / maxRuns.toFloat()))
                        if (ballNum <= revealLimit) {
                            path.lineTo(targetX, targetY)
                            prevX = targetX
                            prevY = targetY
                            tipX = targetX
                            tipY = targetY
                        } else {
                            val frac = (revealLimit - (ballNum - 1)).coerceIn(0f, 1f)
                            if (frac > 0f) {
                                tipX = prevX + (targetX - prevX) * frac
                                tipY = prevY + (targetY - prevY) * frac
                                path.lineTo(tipX, tipY)
                            }
                            break
                        }
                    }

                    // Soft glow stroke underneath
                    drawPath(
                        path = path,
                        color = lineColor.copy(alpha = 0.25f),
                        style = Stroke(width = 13f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 5.5f, cap = StrokeCap.Round),
                    )

                    // Glowing live end-point marker
                    drawCircle(color = lineColor.copy(alpha = 0.35f), radius = 11f, center = Offset(tipX, tipY))
                    drawCircle(color = lineColor, radius = 5.5f, center = Offset(tipX, tipY))

                    // Wicket dots revealed progressively
                    innings.deliveries.forEachIndexed { idx, event ->
                        val ballNum = idx + 1
                        if (event.isWicket && ballNum <= revealLimit) {
                            val totalRuns = innings.cumulativeRuns.getOrElse(idx) { 0 }
                            val x = padX + chartW * (ballNum.toFloat() / maxBalls.toFloat())
                            val y = padY + chartH * (1f - (totalRuns.toFloat() / maxRuns.toFloat()))
                            drawCircle(color = stadium.wicketRed, radius = 8.5f, center = Offset(x, y))
                            drawCircle(color = cs.background, radius = 3.2f, center = Offset(x, y))
                        }
                    }
                }

                drawInningsLine(cpuInnings, stadium.fourMint)
                drawInningsLine(playerInnings, stadium.sixGold)
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
