package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.CricketUiState
import com.example.game.MatchFormat
import com.example.online.GameRoom
import com.example.online.MatchHistoryRecord
import com.example.online.OnlineUiState
import com.example.online.PlayerProfile
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
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private enum class StatsViewSection(val label: String) {
    MATCH_SESSION("Match Session"),
    CAREER("Career"),
}

private enum class StatsModeFilter(val label: String) {
    ALL("All"),
    SOLO("Solo AI"),
    DUEL("1v1 Duel"),
    TEAM("Team"),
}

private data class ComputedPlayerMetrics(
    val matches: Int,
    val wins: Int,
    val losses: Int,
    val ties: Int,
    val winRatePct: Float,
    val winLossRatioLabel: String,
    val totalRuns: Long,
    val avgRunsPerMatch: Float,
    val bestScore: Long,
    val strikeRate: Float,
    val totalFours: Long,
    val totalSixes: Long,
    val chronologicalMatches: List<MatchHistoryRecord>,
)

@Composable
fun PlayerStatsScreen(
    soloState: CricketUiState,
    activeOnlineRoom: GameRoom?,
    currentUserId: String,
    profileState: OnlineUiState<PlayerProfile?>,
    matchHistoryState: OnlineUiState<List<MatchHistoryRecord>>,
    onPlayQuickMatch: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    var selectedSection by rememberSaveable { mutableStateOf(StatsViewSection.MATCH_SESSION) }
    var selectedFilter by rememberSaveable { mutableStateOf(StatsModeFilter.ALL) }
    val sessionSnapshot = remember(soloState, activeOnlineRoom, currentUserId) {
        buildSessionStatisticsSnapshot(
            soloState = soloState,
            activeOnlineRoom = activeOnlineRoom,
            currentUserId = currentUserId,
        )
    }

    val profile = (profileState as? OnlineUiState.Success)?.data
    val allHistory = (matchHistoryState as? OnlineUiState.Success)?.data.orEmpty()

    val filteredHistory = remember(allHistory, selectedFilter) {
        when (selectedFilter) {
            StatsModeFilter.ALL -> allHistory
            StatsModeFilter.SOLO -> allHistory.filter { it.mode.equals("SOLO", ignoreCase = true) }
            StatsModeFilter.DUEL -> allHistory.filter { it.mode.equals("DUEL_1V1", ignoreCase = true) }
            StatsModeFilter.TEAM -> allHistory.filter { it.mode.equals("TEAM_PLAY", ignoreCase = true) }
        }
    }

    val metrics = remember(filteredHistory, profile, selectedFilter) {
        computeMetrics(
            records = filteredHistory,
            profileFallback = if (selectedFilter == StatsModeFilter.ALL) profile else null,
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
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header
                Column(
                    modifier = Modifier.staggeredEntrance(0),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BroadcastLivePill(
                        label = if (selectedSection == StatsViewSection.MATCH_SESSION) {
                            sessionSnapshot.statusPill
                        } else {
                            "CAREER ANALYTICS • LIVE TELEMETRY"
                        },
                        accentColor = if (selectedSection == StatsViewSection.MATCH_SESSION && sessionSnapshot.isLive) {
                            stadium.fourMint
                        } else {
                            stadium.sixGold
                        },
                    )
                    Text(
                        text = if (selectedSection == StatsViewSection.MATCH_SESSION) {
                            "MATCH STATISTICS"
                        } else {
                            "PLAYER STATISTICS"
                        },
                        fontFamily = StadiumDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        color = cs.onBackground,
                    )
                    Text(
                        text = if (selectedSection == StatsViewSection.MATCH_SESSION) {
                            sessionSnapshot.subtitle
                        } else {
                            "Win/loss ratio, average runs per match, and interactive performance trajectories."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                // Top Section Switcher: MATCH SESSION vs CAREER (Spring-Sliding Pill)
                val sectionOptions = remember(stadium) {
                    listOf(
                        SegmentedBarOption(
                            label = StatsViewSection.MATCH_SESSION.label.uppercase(),
                            icon = Icons.Rounded.SportsCricket,
                            accent = stadium.fourMint,
                            brightAccent = stadium.fourMintBright,
                            testTag = "stats_section_match_session",
                        ),
                        SegmentedBarOption(
                            label = StatsViewSection.CAREER.label.uppercase(),
                            icon = Icons.Rounded.BarChart,
                            accent = stadium.sixGold,
                            brightAccent = stadium.sixGoldBright,
                            testTag = "stats_section_career",
                        ),
                    )
                }
                StadiumSegmentedBar(
                    options = sectionOptions,
                    selectedIndex = selectedSection.ordinal,
                    onSelectIndex = { idx ->
                        selectedSection = StatsViewSection.entries.getOrElse(idx) { StatsViewSection.MATCH_SESSION }
                    },
                    modifier = Modifier.staggeredEntrance(1),
                    height = 46.dp,
                )

                AnimatedContent(
                    targetState = selectedSection,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enterX: (Int) -> Int = { if (forward) it / 14 else -it / 14 }
                        val exitX: (Int) -> Int = { if (forward) -it / 14 else it / 14 }
                        (slideInHorizontally(
                            initialOffsetX = enterX,
                            animationSpec = tween(170, easing = StadiumMotion.CrispOut),
                        ) + fadeIn(tween(140))) togetherWith
                            (slideOutHorizontally(
                                targetOffsetX = exitX,
                                animationSpec = tween(100),
                            ) + fadeOut(tween(90)))
                    },
                    label = "stats_section_transition",
                ) { section ->
                    if (section == StatsViewSection.MATCH_SESSION) {
                        MatchStatisticsContent(
                            snapshot = sessionSnapshot,
                            onStartMatch = onPlayQuickMatch,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // Mode Filter Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatsModeFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) stadium.sixGold.copy(alpha = 0.22f) else cs.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) stadium.sixGold else cs.outlineVariant,
                            ),
                            onClick = { selectedFilter = filter },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("stats_filter_${filter.name.lowercase()}"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) stadium.sixGoldBright else cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                when (matchHistoryState) {
                    is OnlineUiState.Loading -> {
                        ShimmerSkeletonCard(height = 120.dp)
                        ShimmerSkeletonCard(height = 240.dp)
                        ShimmerSkeletonCard(height = 260.dp)
                    }
                    is OnlineUiState.Error -> {
                        ErrorNoticeCard(message = matchHistoryState.message)
                    }
                    is OnlineUiState.Success -> {
                        if (metrics.matches == 0) {
                            EmptyStatsCard(
                                filterLabel = selectedFilter.label,
                                onPlayQuickMatch = onPlayQuickMatch,
                            )
                        } else {
                            // KPI Grid Row 1: Win/Loss Ratio + Average Runs/Match
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredEntrance(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StatKpiCard(
                                    label = "WIN / LOSS RATIO",
                                    value = metrics.winLossRatioLabel,
                                    subtitle = "${metrics.wins}W • ${metrics.losses}L • ${metrics.ties}T",
                                    accent = stadium.fourMintBright,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_win_loss_ratio"),
                                )
                                StatKpiCard(
                                    label = "AVG RUNS / MATCH",
                                    value = String.format(Locale.US, "%.1f", metrics.avgRunsPerMatch),
                                    subtitle = "${metrics.totalRuns} total in ${metrics.matches} matches",
                                    accent = stadium.sixGoldBright,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_avg_runs"),
                                )
                            }

                            // KPI Grid Row 2: Best Score + Strike Rate + Boundaries
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredEntrance(3),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CompactStatTile(
                                    label = "HIGHEST",
                                    value = "${metrics.bestScore}",
                                    accent = stadium.sixGold,
                                    modifier = Modifier.weight(1f),
                                )
                                CompactStatTile(
                                    label = "STRIKE RATE",
                                    value = String.format(Locale.US, "%.0f", metrics.strikeRate),
                                    accent = stadium.fourMint,
                                    modifier = Modifier.weight(1f),
                                )
                                CompactStatTile(
                                    label = "4s / 6s",
                                    value = "${metrics.totalFours}/${metrics.totalSixes}",
                                    accent = stadium.sixGoldBright,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            // Chart 1: Interactive Win / Loss / Tie Donut Chart
                            WinLossDonutChartCard(
                                metrics = metrics,
                                modifier = Modifier.staggeredEntrance(4),
                            )

                            // Chart 2: Interactive Runs Per Match & Average Trajectory Chart
                            if (metrics.chronologicalMatches.isNotEmpty()) {
                                RunsPerMatchAnalyticsCard(
                                    matches = metrics.chronologicalMatches,
                                    avgRuns = metrics.avgRunsPerMatch,
                                    modifier = Modifier.staggeredEntrance(5),
                                )
                            }

                            // Chart 3: Mode Breakdown Comparison (only when ALL selected and multiple records exist)
                            if (selectedFilter == StatsModeFilter.ALL && allHistory.isNotEmpty()) {
                                ModeBreakdownCard(
                                    allHistory = allHistory,
                                    modifier = Modifier.staggeredEntrance(6),
                                )
                            }

                            // Recent Matches Log
                            if (filteredHistory.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.staggeredEntrance(7),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    BroadcastSectionHeader(
                                        title = "RECENT MATCHES (${filteredHistory.size})",
                                        accentColor = stadium.sixGold,
                                    )
                                    filteredHistory.take(10).forEach { record ->
                                        RecentMatchRowCard(record = record)
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
}

private fun computeMetrics(
    records: List<MatchHistoryRecord>,
    profileFallback: PlayerProfile?,
): ComputedPlayerMetrics {
    if (records.isEmpty()) {
        val profMatches = profileFallback?.matchesPlayed?.toInt() ?: 0
        val profWins = profileFallback?.matchesWon?.toInt() ?: 0
        val profLosses = (profMatches - profWins).coerceAtLeast(0)
        val winRate = if (profMatches > 0) (profWins.toFloat() / profMatches) * 100f else 0f
        val wlRatio = formatWinLossRatio(profWins, profLosses)
        return ComputedPlayerMetrics(
            matches = profMatches,
            wins = profWins,
            losses = profLosses,
            ties = 0,
            winRatePct = winRate,
            winLossRatioLabel = wlRatio,
            totalRuns = 0L,
            avgRunsPerMatch = 0f,
            bestScore = profileFallback?.bestScore ?: 0L,
            strikeRate = 0f,
            totalFours = 0L,
            totalSixes = 0L,
            chronologicalMatches = emptyList(),
        )
    }

    val wins = records.count { it.outcome.equals("WIN", ignoreCase = true) }
    val losses = records.count { it.outcome.equals("LOSS", ignoreCase = true) }
    val ties = records.count { it.outcome.equals("TIE", ignoreCase = true) }
    val totalMatches = records.size
    val totalRuns = records.sumOf { it.runsScored }
    val totalBalls = records.sumOf { it.ballsFaced }
    val best = max(records.maxOf { it.runsScored }, profileFallback?.bestScore ?: 0L)
    val fours = records.sumOf { it.fours }
    val sixes = records.sumOf { it.sixes }
    val avgRuns = totalRuns.toFloat() / totalMatches.toFloat()
    val sr = if (totalBalls > 0L) (totalRuns.toFloat() / totalBalls.toFloat()) * 100f else 0f
    val winRate = (wins.toFloat() / totalMatches.toFloat()) * 100f

    // Reverse descending Firestore query so charts plot oldest -> newest left-to-right
    val chronological = records.asReversed()

    return ComputedPlayerMetrics(
        matches = totalMatches,
        wins = wins,
        losses = losses,
        ties = ties,
        winRatePct = winRate,
        winLossRatioLabel = formatWinLossRatio(wins, losses),
        totalRuns = totalRuns,
        avgRunsPerMatch = avgRuns,
        bestScore = best,
        strikeRate = sr,
        totalFours = fours,
        totalSixes = sixes,
        chronologicalMatches = chronological,
    )
}

private fun formatWinLossRatio(wins: Int, losses: Int): String = when {
    wins == 0 && losses == 0 -> "0.00"
    losses == 0 -> "∞"
    else -> String.format(Locale.US, "%.2f", wins.toFloat() / losses.toFloat())
}

@Composable
private fun StatKpiCard(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        shape = RoundedCornerShape(20.dp),
        accentColor = accent,
        selected = true,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                fontFamily = StadiumDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                color = accent,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BroadcastCard(
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                fontSize = 10.sp,
            )
            Text(
                text = value,
                fontFamily = ScoreboardMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accent,
            )
        }
    }
}

@Composable
private fun WinLossDonutChartCard(
    metrics: ComputedPlayerMetrics,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    // 0 = All, 1 = Wins, 2 = Losses, 3 = Ties
    var focusedSegment by rememberSaveable { mutableIntStateOf(0) }
    val sweepAnim = remember { Animatable(0f) }

    LaunchedEffect(metrics.wins, metrics.losses, metrics.ties) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        )
    }

    val total = (metrics.wins + metrics.losses + metrics.ties).coerceAtLeast(1).toFloat()
    val winSweep = (metrics.wins / total) * 360f
    val lossSweep = (metrics.losses / total) * 360f
    val tieSweep = (metrics.ties / total) * 360f

    BroadcastCard(
        shape = RoundedCornerShape(24.dp),
        accentColor = stadium.fourMint,
        contentPadding = PaddingValues(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("win_loss_donut_chart_card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BroadcastSectionHeader(
                title = "WIN / LOSS DISTRIBUTION",
                trailingText = "${metrics.matches} MATCHES",
                accentColor = stadium.fourMint,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Animated Donut Ring Canvas
                Box(
                    modifier = Modifier.size(164.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 22.dp.toPx()
                        val pad = strokeWidth / 2f + 6.dp.toPx()
                        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                        val topLeft = Offset(pad, pad)
                        val progress = sweepAnim.value

                        // Background track
                        drawArc(
                            color = cs.surfaceContainerHigh,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth),
                        )

                        var currentAngle = -90f
                        val gap = if (listOf(metrics.wins, metrics.losses, metrics.ties).count { it > 0 } > 1) 4f else 0f

                        // Wins arc (Boundary Mint)
                        if (winSweep > 0f) {
                            val drawSweep = ((winSweep - gap).coerceAtLeast(2f)) * progress
                            val isDimmed = focusedSegment != 0 && focusedSegment != 1
                            val arcWidth = if (focusedSegment == 1) strokeWidth + 4.dp.toPx() else strokeWidth
                            drawArc(
                                color = stadium.fourMint.copy(alpha = if (isDimmed) 0.28f else 0.96f),
                                startAngle = currentAngle,
                                sweepAngle = drawSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = arcWidth, cap = StrokeCap.Round),
                            )
                            currentAngle += winSweep * progress
                        }

                        // Losses arc (Wicket Crimson)
                        if (lossSweep > 0f) {
                            val drawSweep = ((lossSweep - gap).coerceAtLeast(2f)) * progress
                            val isDimmed = focusedSegment != 0 && focusedSegment != 2
                            val arcWidth = if (focusedSegment == 2) strokeWidth + 4.dp.toPx() else strokeWidth
                            drawArc(
                                color = stadium.wicketRed.copy(alpha = if (isDimmed) 0.28f else 0.94f),
                                startAngle = currentAngle,
                                sweepAngle = drawSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = arcWidth, cap = StrokeCap.Round),
                            )
                            currentAngle += lossSweep * progress
                        }

                        // Ties arc (Floodlight Gold)
                        if (tieSweep > 0f) {
                            val drawSweep = ((tieSweep - gap).coerceAtLeast(2f)) * progress
                            val isDimmed = focusedSegment != 0 && focusedSegment != 3
                            val arcWidth = if (focusedSegment == 3) strokeWidth + 4.dp.toPx() else strokeWidth
                            drawArc(
                                color = stadium.sixGold.copy(alpha = if (isDimmed) 0.28f else 0.96f),
                                startAngle = currentAngle,
                                sweepAngle = drawSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = arcWidth, cap = StrokeCap.Round),
                            )
                        }
                    }

                    // Center Telemetry Readout
                    AnimatedContent(
                        targetState = focusedSegment,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
                        label = "donut_center_readout",
                    ) { seg ->
                        val (mainValue, subLabel, mainColor) = when (seg) {
                            1 -> Triple("${metrics.wins}", "WINS", stadium.fourMintBright)
                            2 -> Triple("${metrics.losses}", "LOSSES", stadium.wicketRed)
                            3 -> Triple("${metrics.ties}", "TIES", stadium.sixGoldBright)
                            else -> Triple(
                                "${metrics.winRatePct.roundToInt()}%",
                                "WIN RATE",
                                stadium.fourMintBright,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = mainValue,
                                fontFamily = StadiumDisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                lineHeight = 36.sp,
                                color = mainColor,
                            )
                            Text(
                                text = subLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }

                // Interactive Legend Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DonutLegendRow(
                        label = "Wins",
                        count = metrics.wins,
                        pct = (metrics.wins / total) * 100f,
                        color = stadium.fourMint,
                        selected = focusedSegment == 1,
                        onClick = { focusedSegment = if (focusedSegment == 1) 0 else 1 },
                    )
                    DonutLegendRow(
                        label = "Losses",
                        count = metrics.losses,
                        pct = (metrics.losses / total) * 100f,
                        color = stadium.wicketRed,
                        selected = focusedSegment == 2,
                        onClick = { focusedSegment = if (focusedSegment == 2) 0 else 2 },
                    )
                    DonutLegendRow(
                        label = "Ties",
                        count = metrics.ties,
                        pct = (metrics.ties / total) * 100f,
                        color = stadium.sixGold,
                        selected = focusedSegment == 3,
                        onClick = { focusedSegment = if (focusedSegment == 3) 0 else 3 },
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutLegendRow(
    label: String,
    count: Int,
    pct: Float,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.20f) else cs.background.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, if (selected) color else cs.outlineVariant.copy(alpha = 0.55f)),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurface,
                )
            }
            Text(
                text = "$count (${pct.roundToInt()}%)",
                fontFamily = ScoreboardMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) color else cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RunsPerMatchAnalyticsCard(
    matches: List<MatchHistoryRecord>,
    avgRuns: Float,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    // Plot up to the latest 16 matches chronologically
    val plotted = remember(matches) { matches.takeLast(16) }
    val rollingAverages = remember(plotted) {
        var sum = 0L
        plotted.mapIndexed { idx, rec ->
            sum += rec.runsScored
            sum.toFloat() / (idx + 1).toFloat()
        }
    }
    var selectedIdx by remember(plotted.size) { mutableIntStateOf((plotted.lastIndex).coerceAtLeast(0)) }

    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(plotted) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
    }

    val maxRuns = remember(plotted, avgRuns) {
        max(12f, max(plotted.maxOfOrNull { it.runsScored.toFloat() } ?: 10f, avgRuns) * 1.18f)
    }

    val inspected = plotted.getOrNull(selectedIdx)

    BroadcastCard(
        shape = RoundedCornerShape(24.dp),
        accentColor = stadium.sixGold,
        contentPadding = PaddingValues(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("runs_per_match_chart_card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BroadcastSectionHeader(
                title = "RUNS PER MATCH & AVERAGE",
                trailingText = "AVG ${String.format(Locale.US, "%.1f", avgRuns)}",
                accentColor = stadium.sixGold,
            )

            // Interactive Scrub Tooltip Readout
            if (inspected != null) {
                val outcomeColor = when (inspected.outcome.uppercase()) {
                    "WIN" -> stadium.fourMintBright
                    "LOSS" -> stadium.wicketRed
                    else -> stadium.sixGoldBright
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cs.background.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MATCH #${selectedIdx + 1} • vs ${inspected.opponentName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${inspected.runsScored}/${inspected.wicketsLost} (${inspected.ballsFaced} balls) • Rolling Avg ${
                                    String.format(Locale.US, "%.1f", rollingAverages.getOrElse(selectedIdx) { avgRuns })
                                }",
                                fontFamily = ScoreboardMonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = cs.onSurface,
                            )
                        }
                        TagPill(text = inspected.outcome.uppercase(), accent = outcomeColor)
                    }
                }
            }

            // D3-Inspired Interactive Canvas: Bars + Cubic Bezier Runs Area + Rolling Avg Line + Dashed Overall Avg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            ) {
                // Y-Axis Labels Column (Left)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(190.dp)
                        .width(34.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${maxRuns.roundToInt()}",
                        fontFamily = ScoreboardMonoFont,
                        fontSize = 10.sp,
                        color = cs.onSurfaceVariant,
                    )
                    Text(
                        text = "${(maxRuns * 0.5f).roundToInt()}",
                        fontFamily = ScoreboardMonoFont,
                        fontSize = 10.sp,
                        color = cs.onSurfaceVariant,
                    )
                    Text(
                        text = "0",
                        fontFamily = ScoreboardMonoFont,
                        fontSize = 10.sp,
                        color = cs.onSurfaceVariant,
                    )
                }

                // Plot Area Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 36.dp, top = 10.dp, bottom = 14.dp, end = 6.dp)
                        .pointerInput(plotted.size) {
                            detectTapGestures { pos ->
                                if (plotted.isNotEmpty()) {
                                    val stepX = if (plotted.size == 1) size.width.toFloat() else size.width.toFloat() / (plotted.size - 1)
                                    val idx = if (plotted.size == 1) 0 else (pos.x / stepX).roundToInt().coerceIn(0, plotted.lastIndex)
                                    selectedIdx = idx
                                }
                            }
                        }
                        .pointerInput(plotted.size) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                if (plotted.isNotEmpty()) {
                                    val stepX = if (plotted.size == 1) size.width.toFloat() else size.width.toFloat() / (plotted.size - 1)
                                    val idx = if (plotted.size == 1) 0 else (change.position.x / stepX).roundToInt().coerceIn(0, plotted.lastIndex)
                                    selectedIdx = idx
                                }
                            }
                        },
                ) {
                    val w = size.width
                    val h = size.height
                    val progress = drawProgress.value

                    // Subtle horizontal gridlines
                    val gridColor = cs.outlineVariant.copy(alpha = 0.42f)
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = h * frac
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val count = plotted.size
                    val points = plotted.mapIndexed { idx, rec ->
                        val x = if (count == 1) w * 0.5f else (idx.toFloat() / (count - 1).toFloat()) * w
                        val normalized = (rec.runsScored.toFloat() / maxRuns).coerceIn(0f, 1f)
                        val y = h - (normalized * h * progress)
                        Offset(x, y)
                    }

                    val rollingPoints = rollingAverages.mapIndexed { idx, avgVal ->
                        val x = if (count == 1) w * 0.5f else (idx.toFloat() / (count - 1).toFloat()) * w
                        val normalized = (avgVal / maxRuns).coerceIn(0f, 1f)
                        val y = h - (normalized * h * progress)
                        Offset(x, y)
                    }

                    // Translucent vertical bars for each match
                    val barW = if (count <= 4) 24.dp.toPx() else (w / (count * 1.9f)).coerceIn(8.dp.toPx(), 20.dp.toPx())
                    points.forEachIndexed { idx, pt ->
                        val rec = plotted[idx]
                        val isSel = idx == selectedIdx
                        val barAccent = when (rec.outcome.uppercase()) {
                            "WIN" -> stadium.fourMint
                            "LOSS" -> stadium.wicketRed
                            else -> stadium.sixGold
                        }
                        val barH = (h - pt.y).coerceAtLeast(3.dp.toPx())
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    barAccent.copy(alpha = if (isSel) 0.72f else 0.30f),
                                    barAccent.copy(alpha = if (isSel) 0.18f else 0.05f),
                                ),
                                startY = pt.y,
                                endY = h,
                            ),
                            topLeft = Offset(pt.x - barW / 2f, pt.y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        )
                    }

                    // Cubic Bezier Area + Line for Runs Per Match
                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.lastIndex) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx = (p0.x + p1.x) * 0.5f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    stadium.fourMint.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = h,
                            ),
                        )
                        drawPath(
                            path = linePath,
                            color = stadium.fourMintBright,
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                        )

                        // Smooth Rolling Average Curve (Gold)
                        val rollingPath = Path().apply {
                            moveTo(rollingPoints.first().x, rollingPoints.first().y)
                            for (i in 0 until rollingPoints.lastIndex) {
                                val p0 = rollingPoints[i]
                                val p1 = rollingPoints[i + 1]
                                val cx = (p0.x + p1.x) * 0.5f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(
                            path = rollingPath,
                            color = stadium.sixGold.copy(alpha = 0.85f),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    // Overall Career Average Dashed Reference Line
                    val avgY = h - ((avgRuns / maxRuns).coerceIn(0f, 1f) * h)
                    drawLine(
                        color = stadium.sixGoldBright.copy(alpha = 0.90f),
                        start = Offset(0f, avgY),
                        end = Offset(w, avgY),
                        strokeWidth = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                    )

                    // Vertical scrub crosshair + glowing active dot
                    val selPoint = points.getOrNull(selectedIdx)
                    if (selPoint != null) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(selPoint.x, 0f),
                            end = Offset(selPoint.x, h),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawCircle(
                            color = stadium.fourMintBright.copy(alpha = 0.30f),
                            radius = 11.dp.toPx(),
                            center = selPoint,
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = selPoint,
                        )
                        drawCircle(
                            color = stadium.fourMint,
                            radius = 3.dp.toPx(),
                            center = selPoint,
                        )
                    }
                }
            }

            // Compact Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChartKeyDot(label = "Runs", color = stadium.fourMintBright)
                    ChartKeyDot(label = "Rolling Avg", color = stadium.sixGold)
                }
                Text(
                    text = "Tap or slide chart to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ChartKeyDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ModeBreakdownCard(
    allHistory: List<MatchHistoryRecord>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current

    val rows = remember(allHistory) {
        listOf(
            Triple("Solo vs AI", allHistory.filter { it.mode.equals("SOLO", ignoreCase = true) }, stadium.fourMint),
            Triple("Online 1v1", allHistory.filter { it.mode.equals("DUEL_1V1", ignoreCase = true) }, stadium.sixGold),
            Triple("Team Play", allHistory.filter { it.mode.equals("TEAM_PLAY", ignoreCase = true) }, stadium.fourMintBright),
        )
    }

    BroadcastCard(
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BroadcastSectionHeader(
                title = "MODE BREAKDOWN",
                accentColor = stadium.fourMint,
            )

            rows.forEach { (title, list, accent) ->
                val played = list.size
                val wins = list.count { it.outcome.equals("WIN", ignoreCase = true) }
                val winRate = if (played > 0) wins.toFloat() / played.toFloat() else 0f
                val avg = if (played > 0) list.sumOf { it.runsScored }.toFloat() / played else 0f

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "$title ($played)",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurface,
                        )
                        Text(
                            text = if (played > 0) {
                                "${(winRate * 100f).roundToInt()}% WIN • ${String.format(Locale.US, "%.1f", avg)} AVG"
                            } else {
                                "No matches yet"
                            },
                            fontFamily = ScoreboardMonoFont,
                            fontSize = 11.sp,
                            color = if (played > 0) accent else cs.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(cs.surfaceContainerHigh),
                    ) {
                        if (winRate > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(winRate.coerceIn(0.04f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(accent.copy(alpha = 0.7f), accent),
                                        ),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMatchRowCard(record: MatchHistoryRecord) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    val outcomeColor = when (record.outcome.uppercase()) {
        "WIN" -> stadium.fourMintBright
        "LOSS" -> stadium.wicketRed
        else -> stadium.sixGoldBright
    }
    val modeLabel = when (record.mode.uppercase()) {
        "SOLO" -> "Solo AI"
        "TEAM_PLAY" -> "Team"
        else -> "1v1"
    }
    val format = MatchFormat.fromId(record.formatId)

    BroadcastCard(
        shape = RoundedCornerShape(16.dp),
        accentColor = outcomeColor.copy(alpha = 0.45f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                    color = outcomeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.6f)),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (record.outcome.uppercase()) {
                                "WIN" -> Icons.Rounded.EmojiEvents
                                "LOSS" -> Icons.Rounded.SportsCricket
                                else -> Icons.Rounded.Bolt
                            },
                            contentDescription = null,
                            tint = outcomeColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "vs ${record.opponentName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$modeLabel • ${stringResource(format.nameRes)} • ${record.ballsFaced}b (${record.fours}×4, ${record.sixes}×6)",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${record.runsScored}/${record.wicketsLost}",
                    fontFamily = ScoreboardMonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = cs.onSurface,
                )
                TagPill(text = record.outcome.uppercase(), accent = outcomeColor)
            }
        }
    }
}

@Composable
private fun EmptyStatsCard(
    filterLabel: String,
    onPlayQuickMatch: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val stadium = LocalStadiumColors.current
    BroadcastCard(
        shape = RoundedCornerShape(24.dp),
        accentColor = stadium.sixGold,
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_stats_card"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = stadium.sixGoldContainer,
                border = BorderStroke(1.dp, stadium.sixGold.copy(alpha = 0.6f)),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = stadium.sixGoldBright,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = "No $filterLabel matches recorded yet",
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Complete a Solo AI match, 1v1 Duel, or Team Play match to unlock your interactive Win/Loss donut chart and Runs-per-Match trajectory.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            PremiumActionButton(
                text = "PLAY A MATCH NOW",
                icon = Icons.Rounded.PlayArrow,
                useMintPalette = true,
                height = 54.dp,
                fontSize = 19.sp,
                onClick = onPlayQuickMatch,
                modifier = Modifier.testTag("stats_play_now_button"),
            )
        }
    }
}
