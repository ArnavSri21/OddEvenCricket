package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.AiDifficulty
import com.example.game.CricketUiState
import com.example.game.DeliveryEvent
import com.example.game.GameScreen
import com.example.game.InningsStats
import com.example.game.MatchFormat
import com.example.game.SessionMatchEntry
import com.example.game.TeamSide
import com.example.online.GameRoom
import com.example.online.RoomMode
import com.example.online.RoomStage
import com.example.ui.components.BroadcastCard
import com.example.ui.components.BroadcastLivePill
import com.example.ui.components.BroadcastSectionHeader
import com.example.ui.components.PremiumActionButton
import com.example.ui.components.staggeredEntrance
import com.example.ui.theme.LocalStadiumColors
import com.example.ui.theme.ScoreboardMonoFont
import com.example.ui.theme.StadiumDisplayFont
import java.util.Locale
import kotlin.math.max

data class SideMatchMetrics(
    val name: String,
    val runsScored: Int,
    val ballsFaced: Int,
    val wicketsLost: Int,
    val wicketsTaken: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val fours: Int,
    val sixes: Int,
) {
    val strikeRate: Double
        get() = if (ballsFaced <= 0) 0.0 else (runsScored.toDouble() / ballsFaced.toDouble()) * 100.0

    val runRate: Double
        get() = if (ballsFaced <= 0) 0.0 else (runsScored.toDouble() / ballsFaced.toDouble()) * 6.0

    val bowlingEconomy: Double
        get() = if (ballsBowled <= 0) 0.0 else (runsConceded.toDouble() / ballsBowled.toDouble()) * 6.0

    val battingOversText: String
        get() = "${ballsFaced / 6}.${ballsFaced % 6}"

    val bowlingOversText: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    val boundaryRunsPct: Int
        get() = if (runsScored <= 0) 0 else (((fours * 4 + sixes * 6).toDouble() / runsScored) * 100.0).toInt().coerceIn(0, 100)
}

data class OverBarMetric(
    val overNumber: Int,
    val runs: Int,
    val wickets: Int,
)

data class MatchStatisticsSnapshot(
    val statusPill: String,
    val headline: String,
    val subtitle: String,
    val isLive: Boolean,
    val primarySide: SideMatchMetrics,
    val opponentSide: SideMatchMetrics?,
    val shotDistribution: Map<Int, Int>,
    val wicketDeliveriesCount: Int,
    val overBreakdown: List<OverBarMetric>,
    val recentBalls: List<String>,
    val sessionMatches: List<SessionMatchEntry> = emptyList(),
    val matchesPlayedInSession: Int = 0,
    val matchesWonInSession: Int = 0,
    val winStreakInSession: Int = 0,
) {
    val hasActivity: Boolean
        get() = primarySide.ballsFaced > 0 ||
            primarySide.ballsBowled > 0 ||
            (opponentSide?.ballsFaced ?: 0) > 0 ||
            matchesPlayedInSession > 0
}

fun buildSoloMatchStatisticsSnapshot(state: CricketUiState): MatchStatisticsSnapshot {
    val result = state.matchResult
    val allInnings: List<InningsStats> = when {
        result != null -> listOf(result.firstInnings, result.secondInnings)
        state.firstInningsCompleted != null && state.screen == GameScreen.InningsBreak ->
            listOf(state.firstInningsCompleted)
        state.firstInningsCompleted != null ->
            listOf(state.firstInningsCompleted, state.currentInnings)
        else -> listOf(state.currentInnings)
    }

    val playerInnings = allInnings.firstOrNull { it.battingSide == TeamSide.PLAYER }
    val cpuInnings = allInnings.firstOrNull { it.battingSide == TeamSide.CPU }

    val aiLabel = when (state.selectedDifficulty) {
        AiDifficulty.ROOKIE -> "Rookie AI"
        AiDifficulty.PRO -> "Pro AI"
        AiDifficulty.LEGEND -> "Legend AI"
    }

    val primarySide = SideMatchMetrics(
        name = "YOU",
        runsScored = playerInnings?.runs ?: 0,
        ballsFaced = playerInnings?.balls ?: 0,
        wicketsLost = playerInnings?.wickets ?: 0,
        wicketsTaken = cpuInnings?.wickets ?: 0,
        ballsBowled = cpuInnings?.balls ?: 0,
        runsConceded = cpuInnings?.runs ?: 0,
        fours = playerInnings?.fours ?: 0,
        sixes = playerInnings?.sixes ?: 0,
    )

    val opponentSide = SideMatchMetrics(
        name = aiLabel.uppercase(),
        runsScored = cpuInnings?.runs ?: 0,
        ballsFaced = cpuInnings?.balls ?: 0,
        wicketsLost = cpuInnings?.wickets ?: 0,
        wicketsTaken = playerInnings?.wickets ?: 0,
        ballsBowled = playerInnings?.balls ?: 0,
        runsConceded = playerInnings?.runs ?: 0,
        fours = cpuInnings?.fours ?: 0,
        sixes = cpuInnings?.sixes ?: 0,
    )

    val activeDeliveries: List<DeliveryEvent> = when {
        state.screen == GameScreen.Match -> state.currentInnings.deliveries
        playerInnings != null && playerInnings.deliveries.isNotEmpty() -> playerInnings.deliveries
        else -> allInnings.flatMap { it.deliveries }
    }

    val shotDist = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
    var wicketCount = 0
    activeDeliveries.forEach { d ->
        if (d.isWicket) {
            wicketCount += 1
        } else if (d.runsScored in 1..6) {
            shotDist[d.runsScored] = (shotDist[d.runsScored] ?: 0) + 1
        }
    }

    val overs = activeDeliveries.chunked(6).mapIndexed { idx, balls ->
        OverBarMetric(
            overNumber = idx + 1,
            runs = balls.sumOf { it.runsScored },
            wickets = balls.count { it.isWicket },
        )
    }

    val statusPill = when (state.screen) {
        GameScreen.Match -> if (state.inningsNumber == 1) "LIVE • 1ST INNINGS" else "LIVE • 2ND INNINGS CHASE"
        GameScreen.InningsBreak -> "INNINGS BREAK • HALF-TIME"
        GameScreen.Result -> "FINAL MATCH TELEMETRY"
        else -> "CURRENT MATCH SESSION"
    }

    val formatLabel = when (state.activeFormat) {
        MatchFormat.CLASSIC_1W -> "1-Wicket Classic"
        MatchFormat.SUPER_OVER -> "Super Over"
        MatchFormat.T3_BLITZ -> "T3 Blitz"
        MatchFormat.T5_CLASH -> "T5 Clash"
    }

    return MatchStatisticsSnapshot(
        statusPill = statusPill,
        headline = "MATCH STATISTICS",
        subtitle = "$formatLabel • YOU vs $aiLabel",
        isLive = state.screen == GameScreen.Match || state.screen == GameScreen.InningsBreak,
        primarySide = primarySide,
        opponentSide = opponentSide,
        shotDistribution = shotDist,
        wicketDeliveriesCount = wicketCount,
        overBreakdown = overs,
        recentBalls = activeDeliveries.map { it.badgeLabel },
    )
}

fun buildOnlineMatchStatisticsSnapshot(
    room: GameRoom,
    currentUserId: String,
): MatchStatisticsSnapshot {
    val inA = currentUserId in room.teamAUids || currentUserId == room.captainAUid
    val inB = currentUserId in room.teamBUids || currentUserId == room.captainBUid
    val primaryTeamKey = when {
        inA -> "A"
        inB -> "B"
        else -> "A"
    }
    val oppTeamKey = if (primaryTeamKey == "A") "B" else "A"

    fun labelFor(teamKey: String): String = if (room.roomMode() == RoomMode.DUEL_1V1) {
        val uid = if (teamKey == "A") room.captainAUid else room.captainBUid
        room.nameOf(uid).uppercase()
    } else {
        if (teamKey == "A") "TEAM GOLD" else "TEAM MINT"
    }

    fun battingTotalsFor(teamKey: String): Triple<Int, Int, Int> {
        val stage = room.stage()
        return when {
            room.inningsNumber == 1L && stage != RoomStage.INNINGS_BREAK && stage != RoomStage.FINISHED -> {
                if (room.battingTeam == teamKey) {
                    Triple(room.runs.toInt(), room.balls.toInt(), room.wickets.toInt())
                } else {
                    Triple(0, 0, 0)
                }
            }
            stage == RoomStage.INNINGS_BREAK -> {
                if (room.firstInningsTeam == teamKey) {
                    Triple(room.firstInningsRuns.toInt(), room.firstInningsBalls.toInt(), room.firstInningsWickets.toInt())
                } else {
                    Triple(0, 0, 0)
                }
            }
            else -> {
                if (room.firstInningsTeam == teamKey) {
                    Triple(room.firstInningsRuns.toInt(), room.firstInningsBalls.toInt(), room.firstInningsWickets.toInt())
                } else {
                    Triple(room.runs.toInt(), room.balls.toInt(), room.wickets.toInt())
                }
            }
        }
    }

    val (pRuns, pBalls, pWktsLost) = battingTotalsFor(primaryTeamKey)
    val (oRuns, oBalls, oWktsLost) = battingTotalsFor(oppTeamKey)

    val primaryIsCurrentBatter = room.battingTeam == primaryTeamKey
    val primarySide = SideMatchMetrics(
        name = labelFor(primaryTeamKey),
        runsScored = pRuns,
        ballsFaced = pBalls,
        wicketsLost = pWktsLost,
        wicketsTaken = oWktsLost,
        ballsBowled = oBalls,
        runsConceded = oRuns,
        fours = if (primaryIsCurrentBatter) room.fours.toInt() else 0,
        sixes = if (primaryIsCurrentBatter) room.sixes.toInt() else 0,
    )
    val opponentSide = SideMatchMetrics(
        name = labelFor(oppTeamKey),
        runsScored = oRuns,
        ballsFaced = oBalls,
        wicketsLost = oWktsLost,
        wicketsTaken = pWktsLost,
        ballsBowled = pBalls,
        runsConceded = pRuns,
        fours = if (!primaryIsCurrentBatter) room.fours.toInt() else 0,
        sixes = if (!primaryIsCurrentBatter) room.sixes.toInt() else 0,
    )

    val shotDist = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
    var wicketCount = 0
    room.recentBalls.forEach { badge ->
        if (badge == "W") {
            wicketCount += 1
        } else {
            val r = badge.toIntOrNull()
            if (r != null && r in 1..6) {
                shotDist[r] = (shotDist[r] ?: 0) + 1
            }
        }
    }

    val overs = room.recentBalls.chunked(6).mapIndexed { idx, balls ->
        OverBarMetric(
            overNumber = idx + 1,
            runs = balls.sumOf { it.toIntOrNull() ?: 0 },
            wickets = balls.count { it == "W" },
        )
    }

    val stage = room.stage()
    val pill = when (stage) {
        RoomStage.PLAYING -> "LIVE ROOM ${room.roomCode} • INNINGS ${room.inningsNumber}"
        RoomStage.INNINGS_BREAK -> "ROOM ${room.roomCode} • INNINGS BREAK"
        RoomStage.FINISHED -> "ROOM ${room.roomCode} • FULL TIME"
        else -> "ROOM ${room.roomCode} • ${room.roomMode().label.uppercase()}"
    }

    return MatchStatisticsSnapshot(
        statusPill = pill,
        headline = "MATCH STATISTICS",
        subtitle = "${room.roomMode().label} • ${labelFor(primaryTeamKey)} vs ${labelFor(oppTeamKey)}",
        isLive = stage == RoomStage.PLAYING || stage == RoomStage.INNINGS_BREAK,
        primarySide = primarySide,
        opponentSide = opponentSide,
        shotDistribution = shotDist,
        wicketDeliveriesCount = wicketCount,
        overBreakdown = overs,
        recentBalls = room.recentBalls,
    )
}

fun buildSessionStatisticsSnapshot(
    soloState: CricketUiState,
    activeOnlineRoom: GameRoom?,
    currentUserId: String,
): MatchStatisticsSnapshot {
    val session = soloState.sessionStats
    val isLiveSolo = soloState.screen == GameScreen.Match || soloState.screen == GameScreen.InningsBreak

    // Include live in-progress solo innings before match completion so telemetry updates ball-by-ball
    val liveInnings = if (isLiveSolo) {
        listOfNotNull(
            soloState.firstInningsCompleted,
            if (soloState.screen == GameScreen.Match) soloState.currentInnings else null,
        )
    } else {
        emptyList()
    }
    val livePlayerBatting = liveInnings.firstOrNull { it.battingSide == TeamSide.PLAYER }
    val liveCpuBatting = liveInnings.firstOrNull { it.battingSide == TeamSide.CPU }

    val totalRuns = session.totalRunsScored + (livePlayerBatting?.runs ?: 0)
    val totalBallsFaced = session.totalBallsFaced + (livePlayerBatting?.balls ?: 0)
    val totalWicketsTaken = session.totalWicketsTaken + (liveCpuBatting?.wickets ?: 0)
    val totalBallsBowled = session.totalBallsBowled + (liveCpuBatting?.balls ?: 0)
    val totalRunsConceded = session.totalRunsConceded + (liveCpuBatting?.runs ?: 0)
    val totalFours = session.totalFours + (livePlayerBatting?.fours ?: 0)
    val totalSixes = session.totalSixes + (livePlayerBatting?.sixes ?: 0)

    val latestDeliveries: List<DeliveryEvent> = when {
        isLiveSolo -> soloState.currentInnings.deliveries
        soloState.matchResult != null -> {
            val r = soloState.matchResult
            if (r.firstInnings.battingSide == TeamSide.PLAYER) r.firstInnings.deliveries else r.secondInnings.deliveries
        }
        else -> emptyList()
    }

    val shotDist = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0)
    var wicketCount = 0
    latestDeliveries.forEach { d ->
        if (d.isWicket) wicketCount += 1
        else if (d.runsScored in 1..6) shotDist[d.runsScored] = (shotDist[d.runsScored] ?: 0) + 1
    }
    val overs = latestDeliveries.chunked(6).mapIndexed { idx, balls ->
        OverBarMetric(
            overNumber = idx + 1,
            runs = balls.sumOf { it.runsScored },
            wickets = balls.count { it.isWicket },
        )
    }

    val primarySide = SideMatchMetrics(
        name = "YOUR SESSION",
        runsScored = totalRuns,
        ballsFaced = totalBallsFaced,
        wicketsLost = 0,
        wicketsTaken = totalWicketsTaken,
        ballsBowled = totalBallsBowled,
        runsConceded = totalRunsConceded,
        fours = totalFours,
        sixes = totalSixes,
    )

    val matchCountText = when (session.matchesPlayed) {
        0 -> if (isLiveSolo) "LIVE MATCH IN PROGRESS" else "NO COMPLETED MATCHES YET"
        1 -> "1 MATCH PLAYED THIS SESSION"
        else -> "${session.matchesPlayed} MATCHES PLAYED THIS SESSION"
    }

    val liveOnlineActive = activeOnlineRoom != null && activeOnlineRoom.isLiveMatch()
    val pill = when {
        isLiveSolo -> "LIVE MATCH • SESSION TELEMETRY"
        liveOnlineActive -> "ONLINE ROOM ${activeOnlineRoom!!.roomCode} • LIVE"
        else -> "CURRENT MATCH SESSION"
    }

    return MatchStatisticsSnapshot(
        statusPill = pill,
        headline = "MATCH STATISTICS",
        subtitle = "$matchCountText • ${session.matchesWon}W-${session.matchesLost}L-${session.matchesTied}T",
        isLive = isLiveSolo || liveOnlineActive,
        primarySide = primarySide,
        opponentSide = null,
        shotDistribution = shotDist,
        wicketDeliveriesCount = wicketCount,
        overBreakdown = overs,
        recentBalls = latestDeliveries.map { it.badgeLabel },
        sessionMatches = session.recentMatches,
        matchesPlayedInSession = session.matchesPlayed,
        matchesWonInSession = session.matchesWon,
        winStreakInSession = session.currentWinStreak,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchStatisticsModalSheet(
    snapshot: MatchStatisticsSnapshot,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.background,
        contentColor = cs.onBackground,
        scrimColor = Color.Black.copy(alpha = 0.68f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("match_statistics_modal"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    BroadcastLivePill(
                        label = snapshot.statusPill,
                        accentColor = if (snapshot.isLive) stadium.fourMint else stadium.sixGold,
                    )
                    Text(
                        text = snapshot.headline,
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 36.sp,
                        color = cs.onBackground,
                    )
                    Text(
                        text = snapshot.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = cs.surfaceContainer,
                    border = BorderStroke(1.dp, stadium.rimTop),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_match_stats_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_close_match_stats),
                            tint = cs.onSurface,
                        )
                    }
                }
            }

            MatchStatisticsContent(
                snapshot = snapshot,
                onStartMatch = null,
            )
        }
    }
}

@Composable
fun MatchStatisticsContent(
    snapshot: MatchStatisticsSnapshot,
    onStartMatch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val primary = snapshot.primarySide

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_statistics_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!snapshot.hasActivity) {
            BroadcastCard(
                shape = RoundedCornerShape(24.dp),
                accentColor = stadium.sixGold,
                contentPadding = PaddingValues(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(0),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(stadium.sixGoldContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsCricket,
                            contentDescription = null,
                            tint = stadium.sixGoldBright,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = "NO SESSION BALLS BOWLED YET",
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = cs.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Step onto the pitch to start tracking live runs scored, wickets taken, strike rates, and over-by-over momentum for this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (onStartMatch != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PremiumActionButton(
                            text = "PLAY A MATCH",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = onStartMatch,
                            height = 52.dp,
                            fontSize = 20.sp,
                            modifier = Modifier.testTag("session_stats_play_cta"),
                        )
                    }
                }
            }
            return@Column
        }

        // 1. Hero 3-KPI Telemetry Bento: RUNS SCORED | WICKETS TAKEN | STRIKE RATE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredEntrance(0),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MatchStatHeroKpiCard(
                label = "RUNS SCORED",
                value = primary.runsScored.toString(),
                footnote = "${primary.battingOversText} OV • ${primary.fours}x4 ${primary.sixes}x6",
                accent = stadium.sixGold,
                accentBright = stadium.sixGoldBright,
                testTag = "match_stat_runs",
                modifier = Modifier.weight(1f),
            )
            MatchStatHeroKpiCard(
                label = "WICKETS TAKEN",
                value = primary.wicketsTaken.toString(),
                footnote = if (primary.ballsBowled > 0) {
                    String.format(Locale.US, "ECON %.1f • %s OV", primary.bowlingEconomy, primary.bowlingOversText)
                } else {
                    "BOWLING NEXT"
                },
                accent = stadium.wicketRed,
                accentBright = stadium.wicketRedBright,
                testTag = "match_stat_wickets",
                modifier = Modifier.weight(1f),
            )
            MatchStatHeroKpiCard(
                label = "STRIKE RATE",
                value = if (primary.ballsFaced > 0) {
                    String.format(Locale.US, "%.1f", primary.strikeRate)
                } else {
                    "—"
                },
                footnote = if (primary.ballsFaced > 0) {
                    String.format(Locale.US, "CRR %.1f • %d%% BDY", primary.runRate, primary.boundaryRunsPct)
                } else {
                    "BATTING NEXT"
                },
                accent = stadium.fourMint,
                accentBright = stadium.fourMintBright,
                testTag = "match_stat_strike_rate",
                modifier = Modifier.weight(1f),
            )
        }

        // 2. Head-to-Head Side Comparison (shown for live/completed matches)
        val opponent = snapshot.opponentSide
        if (opponent != null) {
            BroadcastSectionHeader(
                title = "HEAD-TO-HEAD MATCHUP",
                accentColor = stadium.sixGold,
                modifier = Modifier.staggeredEntrance(1),
            )
            HeadToHeadComparisonCard(
                left = primary,
                right = opponent,
                modifier = Modifier.staggeredEntrance(1),
            )
        } else {
            // Session Bowling & Efficiency Summary Strip
            BroadcastCard(
                shape = RoundedCornerShape(20.dp),
                accentColor = stadium.fourMint,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(1),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CompactTelemetryMetric("MATCHES", snapshot.matchesPlayedInSession.toString(), cs.onSurface)
                    CompactTelemetryMetric("WON", snapshot.matchesWonInSession.toString(), stadium.fourMintBright)
                    CompactTelemetryMetric("STREAK", "${snapshot.winStreakInSession}x", stadium.sixGoldBright)
                    CompactTelemetryMetric(
                        "ECONOMY",
                        if (primary.ballsBowled > 0) String.format(Locale.US, "%.1f", primary.bowlingEconomy) else "—",
                        stadium.wicketRedBright,
                    )
                }
            }
        }

        // 3. Over-by-Over Manhattan Runs & Wickets Chart
        if (snapshot.overBreakdown.isNotEmpty()) {
            BroadcastSectionHeader(
                title = "OVER-BY-OVER RUNS & WICKETS",
                accentColor = stadium.fourMint,
                modifier = Modifier.staggeredEntrance(2),
            )
            OverManhattanChartCard(
                overs = snapshot.overBreakdown,
                modifier = Modifier.staggeredEntrance(2),
            )
        }

        // 4. Shot & Delivery Distribution Breakdown
        val totalShotBalls = snapshot.shotDistribution.values.sum() + snapshot.wicketDeliveriesCount
        if (totalShotBalls > 0) {
            BroadcastSectionHeader(
                title = "SCORING & WICKET DISTRIBUTION",
                accentColor = stadium.sixGold,
                modifier = Modifier.staggeredEntrance(3),
            )
            ShotDistributionCard(
                shotDistribution = snapshot.shotDistribution,
                wickets = snapshot.wicketDeliveriesCount,
                boundaryRunsPct = primary.boundaryRunsPct,
                modifier = Modifier.staggeredEntrance(3),
            )
        }

        // 5. Recent Ball-by-Ball Feed (Live / Completed Match)
        if (snapshot.recentBalls.isNotEmpty()) {
            BroadcastSectionHeader(
                title = "DELIVERY TIMELINE (${snapshot.recentBalls.size} BALLS)",
                accentColor = stadium.fourMint,
                modifier = Modifier.staggeredEntrance(4),
            )
            BroadcastCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEntrance(4),
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(snapshot.recentBalls.size) { index ->
                        val badge = snapshot.recentBalls[index]
                        val bg = when (badge) {
                            "W" -> stadium.wicketRedContainer
                            "6" -> stadium.sixGoldContainer
                            "4" -> stadium.fourMintContainer
                            else -> cs.surfaceContainerHigh
                        }
                        val fg = when (badge) {
                            "W" -> stadium.wicketRedBright
                            "6" -> stadium.sixGoldBright
                            "4" -> stadium.fourMintBright
                            else -> cs.onSurface
                        }
                        val border = when (badge) {
                            "W" -> stadium.wicketRed
                            "6" -> stadium.sixGold
                            "4" -> stadium.fourMint
                            else -> cs.outlineVariant
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = bg,
                                border = BorderStroke(1.dp, border),
                                modifier = Modifier.size(34.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = badge,
                                        fontFamily = ScoreboardMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = fg,
                                    )
                                }
                            }
                            Text(
                                text = "${(index / 6)}.${(index % 6) + 1}",
                                fontFamily = ScoreboardMonoFont,
                                fontSize = 9.sp,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 6. Session Match Ledger (Current Session Breakdown)
        if (snapshot.sessionMatches.isNotEmpty()) {
            BroadcastSectionHeader(
                title = "SESSION MATCH LOG (${snapshot.sessionMatches.size})",
                accentColor = stadium.sixGold,
                modifier = Modifier.staggeredEntrance(4),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.staggeredEntrance(4),
            ) {
                snapshot.sessionMatches.forEach { entry ->
                    SessionMatchLogRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun MatchStatHeroKpiCard(
    label: String,
    value: String,
    footnote: String,
    accent: Color,
    accentBright: Color,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        shape = RoundedCornerShape(18.dp),
        accentColor = accent,
        selected = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        modifier = modifier.testTag(testTag),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = accentBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                color = cs.onSurface,
                maxLines = 1,
            )
            Text(
                text = footnote,
                fontFamily = ScoreboardMonoFont,
                fontSize = 9.5.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompactTelemetryMetric(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = StadiumDisplayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeadToHeadComparisonCard(
    left: SideMatchMetrics,
    right: SideMatchMetrics,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = left.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = stadium.sixGoldBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
                Text(
                    text = right.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = stadium.fourMintBright,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            ComparisonBarRow(
                label = "RUNS SCORED",
                leftText = "${left.runsScored}/${left.wicketsLost}",
                rightText = "${right.runsScored}/${right.wicketsLost}",
                leftVal = left.runsScored.toFloat(),
                rightVal = right.runsScored.toFloat(),
                leftColor = stadium.sixGold,
                rightColor = stadium.fourMint,
            )
            ComparisonBarRow(
                label = "WICKETS TAKEN",
                leftText = left.wicketsTaken.toString(),
                rightText = right.wicketsTaken.toString(),
                leftVal = left.wicketsTaken.toFloat(),
                rightVal = right.wicketsTaken.toFloat(),
                leftColor = stadium.sixGold,
                rightColor = stadium.fourMint,
            )
            ComparisonBarRow(
                label = "STRIKE RATE",
                leftText = if (left.ballsFaced > 0) String.format(Locale.US, "%.1f", left.strikeRate) else "—",
                rightText = if (right.ballsFaced > 0) String.format(Locale.US, "%.1f", right.strikeRate) else "—",
                leftVal = left.strikeRate.toFloat(),
                rightVal = right.strikeRate.toFloat(),
                leftColor = stadium.sixGold,
                rightColor = stadium.fourMint,
            )
            ComparisonBarRow(
                label = "RUN RATE (CRR)",
                leftText = if (left.ballsFaced > 0) String.format(Locale.US, "%.1f", left.runRate) else "—",
                rightText = if (right.ballsFaced > 0) String.format(Locale.US, "%.1f", right.runRate) else "—",
                leftVal = left.runRate.toFloat(),
                rightVal = right.runRate.toFloat(),
                leftColor = stadium.sixGold,
                rightColor = stadium.fourMint,
            )
            ComparisonBarRow(
                label = "BOWLING ECON",
                leftText = if (left.ballsBowled > 0) String.format(Locale.US, "%.1f", left.bowlingEconomy) else "—",
                rightText = if (right.ballsBowled > 0) String.format(Locale.US, "%.1f", right.bowlingEconomy) else "—",
                leftVal = left.bowlingEconomy.toFloat(),
                rightVal = right.bowlingEconomy.toFloat(),
                leftColor = stadium.sixGold,
                rightColor = stadium.fourMint,
            )
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    leftText: String,
    rightText: String,
    leftVal: Float,
    rightVal: Float,
    leftColor: Color,
    rightColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    val total = (leftVal + rightVal).coerceAtLeast(0.001f)
    val leftFrac = if (leftVal <= 0f && rightVal <= 0f) 0.5f else (leftVal / total).coerceIn(0.06f, 0.94f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = leftText,
                fontFamily = ScoreboardMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = cs.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
            )
            Text(
                text = rightText,
                fontFamily = ScoreboardMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = cs.onSurface,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(cs.background.copy(alpha = 0.7f)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(leftFrac)
                    .height(6.dp)
                    .background(leftColor, RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)),
            )
            Box(
                modifier = Modifier
                    .weight((1f - leftFrac).coerceAtLeast(0.06f))
                    .height(6.dp)
                    .background(rightColor, RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)),
            )
        }
    }
}

@Composable
private fun OverManhattanChartCard(
    overs: List<OverBarMetric>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(overs) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 680, easing = FastOutSlowInEasing))
    }

    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RUNS PER OVER",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stadium.wicketRed),
                    )
                    Text(
                        text = "WICKET",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            val maxRuns = max(overs.maxOfOrNull { it.runs } ?: 12, 12)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp),
            ) {
                val topReserve = 22f
                val chartH = (size.height - topReserve - 8f).coerceAtLeast(16f)
                val baseLineY = topReserve + chartH

                for (i in 0..2) {
                    val y = topReserve + chartH * (i / 2f)
                    drawLine(
                        color = cs.outlineVariant.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f,
                    )
                }

                val slotW = size.width / overs.size.coerceAtLeast(1)
                val barW = (slotW * 0.52f).coerceIn(14f, 44f)

                overs.forEachIndexed { idx, over ->
                    val centerX = slotW * idx + slotW / 2f
                    val frac = (over.runs.toFloat() / maxRuns.toFloat()).coerceIn(0.05f, 1f) * progress.value
                    val barH = chartH * frac
                    val topY = baseLineY - barH

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(stadium.sixGoldBright, stadium.fourMint),
                            startY = topY,
                            endY = baseLineY,
                        ),
                        topLeft = Offset(centerX - barW / 2f, topY),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(8f, 8f),
                    )

                    // Draw crimson wicket markers stacked above the over bar
                    repeat(over.wickets.coerceAtMost(3)) { wIdx ->
                        val cy = (topY - 10f - wIdx * 11f).coerceAtLeast(6f)
                        drawCircle(
                            color = stadium.wicketRed.copy(alpha = 0.35f),
                            radius = 8.5f,
                            center = Offset(centerX, cy),
                        )
                        drawCircle(
                            color = stadium.wicketRedBright,
                            radius = 5f,
                            center = Offset(centerX, cy),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                overs.forEach { over ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${over.runs}",
                            fontFamily = ScoreboardMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = stadium.sixGoldBright,
                        )
                        Text(
                            text = "OV ${over.overNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotDistributionCard(
    shotDistribution: Map<Int, Int>,
    wickets: Int,
    boundaryRunsPct: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val items = listOf(
        Triple("1s", shotDistribution[1] ?: 0, cs.onSurfaceVariant),
        Triple("2s", shotDistribution[2] ?: 0, cs.onSurfaceVariant),
        Triple("3s", shotDistribution[3] ?: 0, cs.onSurface),
        Triple("4s", shotDistribution[4] ?: 0, stadium.fourMintBright),
        Triple("5s", shotDistribution[5] ?: 0, cs.onSurface),
        Triple("6s", shotDistribution[6] ?: 0, stadium.sixGoldBright),
        Triple("W", wickets, stadium.wicketRedBright),
    )
    val maxCount = max(items.maxOfOrNull { it.second } ?: 1, 1)

    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "BALL OUTCOME FREQUENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = stadium.sixGoldContainer,
                    border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.55f)),
                ) {
                    Text(
                        text = "$boundaryRunsPct% BOUNDARY RUNS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = stadium.sixGoldBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                items.forEach { (label, count, barColor) ->
                    val frac = if (count == 0) 0.08f else (count.toFloat() / maxCount.toFloat()).coerceIn(0.18f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            text = count.toString(),
                            fontFamily = ScoreboardMonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (count > 0) barColor else cs.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height((52f * frac).dp)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(
                                    if (count > 0) barColor.copy(alpha = 0.85f)
                                    else cs.surfaceContainerHigh
                                ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (count > 0) cs.onSurface else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionMatchLogRow(
    entry: SessionMatchEntry,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val badgeAccent = when (entry.outcome) {
        "WIN" -> stadium.fourMint
        "LOSS" -> stadium.wicketRed
        else -> stadium.sixGold
    }
    val badgeBright = when (entry.outcome) {
        "WIN" -> stadium.fourMintBright
        "LOSS" -> stadium.wicketRedBright
        else -> stadium.sixGoldBright
    }

    BroadcastCard(
        shape = RoundedCornerShape(16.dp),
        accentColor = badgeAccent,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = entry.outcome,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = badgeBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = "${entry.modeLabel} vs ${entry.opponentName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = String.format(
                        Locale.US,
                        "SR %.1f • %d WKT TAKEN • %dx4 %dx6",
                        entry.strikeRate,
                        entry.wicketsTaken,
                        entry.fours,
                        entry.sixes,
                    ),
                    fontFamily = ScoreboardMonoFont,
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.runsScored} RUNS",
                    fontFamily = StadiumDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = stadium.sixGoldBright,
                )
                Text(
                    text = "${entry.ballsFaced} BALLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}
