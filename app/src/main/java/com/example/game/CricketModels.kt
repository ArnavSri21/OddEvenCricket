package com.example.game

import androidx.annotation.StringRes
import com.example.R

enum class MatchFormat(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int,
    val maxWickets: Int,
    val maxBalls: Int?, // null = unlimited overs (classic schoolyard hand cricket)
) {
    CLASSIC_1W(
        id = "classic_1w",
        nameRes = R.string.format_classic_name,
        descRes = R.string.format_classic_desc,
        maxWickets = 1,
        maxBalls = null,
    ),
    SUPER_OVER(
        id = "super_over",
        nameRes = R.string.format_super_over_name,
        descRes = R.string.format_super_over_desc,
        maxWickets = 2,
        maxBalls = 6,
    ),
    T3_BLITZ(
        id = "t3_blitz",
        nameRes = R.string.format_t3_name,
        descRes = R.string.format_t3_desc,
        maxWickets = 3,
        maxBalls = 18,
    ),
    T5_CLASH(
        id = "t5_clash",
        nameRes = R.string.format_t5_name,
        descRes = R.string.format_t5_desc,
        maxWickets = 5,
        maxBalls = 30,
    );

    companion object {
        fun fromId(id: String?): MatchFormat =
            entries.firstOrNull { it.id == id } ?: T3_BLITZ
    }
}

enum class AiDifficulty(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val hintRes: Int,
) {
    ROOKIE("rookie", R.string.diff_rookie, R.string.diff_rookie_hint),
    PRO("pro", R.string.diff_pro, R.string.diff_pro_hint),
    LEGEND("legend", R.string.diff_legend, R.string.diff_legend_hint);

    companion object {
        fun fromId(id: String?): AiDifficulty =
            entries.firstOrNull { it.id == id } ?: PRO
    }
}

enum class TossCall {
    ODD,
    EVEN,
}

enum class TeamSide {
    PLAYER,
    CPU,
}

enum class MatchWinner {
    PLAYER,
    CPU,
    TIE,
}

data class DeliveryEvent(
    val ballNumber: Int,
    val playerPick: Int,
    val cpuPick: Int,
    val batterSide: TeamSide,
    val runsScored: Int,
    val isWicket: Boolean,
    val commentary: String,
) {
    val isFour: Boolean get() = !isWicket && runsScored == 4
    val isSix: Boolean get() = !isWicket && runsScored == 6

    val badgeLabel: String
        get() = if (isWicket) "W" else runsScored.toString()
}

data class InningsStats(
    val battingSide: TeamSide,
    val runs: Int = 0,
    val wickets: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val deliveries: List<DeliveryEvent> = emptyList(),
    val cumulativeRuns: List<Int> = emptyList(),
    val bestBoundaryStreak: Int = 0,
) {
    val oversText: String
        get() = "${balls / 6}.${balls % 6}"

    val runRate: Double
        get() = if (balls == 0) 0.0 else (runs.toDouble() / balls.toDouble()) * 6.0

    val strikeRate: Double
        get() = if (balls == 0) 0.0 else (runs.toDouble() / balls.toDouble()) * 100.0
}

data class TossState(
    val playerCall: TossCall? = null,
    val playerThrow: Int? = null,
    val cpuThrow: Int? = null,
    val winner: TeamSide? = null,
    val cpuDecisionBattingFirst: Boolean? = null,
    val isRevealing: Boolean = false,
) {
    val sum: Int?
        get() = if (playerThrow != null && cpuThrow != null) playerThrow + cpuThrow else null

    val sumIsOdd: Boolean?
        get() = sum?.let { it % 2 != 0 }
}

data class MatchResultSummary(
    val winner: MatchWinner,
    val marginHeadline: String,
    val firstInnings: InningsStats,
    val secondInnings: InningsStats,
    val wasSuperOver: Boolean,
)

data class SessionMatchEntry(
    val id: String,
    val modeLabel: String,
    val opponentName: String,
    val outcome: String, // "WIN", "LOSS", "TIE"
    val runsScored: Int,
    val ballsFaced: Int,
    val wicketsTaken: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val fours: Int,
    val sixes: Int,
) {
    val strikeRate: Double
        get() = if (ballsFaced == 0) 0.0 else (runsScored.toDouble() / ballsFaced.toDouble()) * 100.0

    val economyRate: Double
        get() = if (ballsBowled == 0) 0.0 else (runsConceded.toDouble() / ballsBowled.toDouble()) * 6.0
}

data class SessionStats(
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val matchesTied: Int = 0,
    val bestPlayerScore: Int = 0,
    val currentWinStreak: Int = 0,
    val totalRunsScored: Int = 0,
    val totalBallsFaced: Int = 0,
    val totalWicketsTaken: Int = 0,
    val totalBallsBowled: Int = 0,
    val totalRunsConceded: Int = 0,
    val totalFours: Int = 0,
    val totalSixes: Int = 0,
    val recentMatches: List<SessionMatchEntry> = emptyList(),
) {
    val sessionStrikeRate: Double
        get() = if (totalBallsFaced == 0) 0.0 else (totalRunsScored.toDouble() / totalBallsFaced.toDouble()) * 100.0

    val sessionEconomyRate: Double
        get() = if (totalBallsBowled == 0) 0.0 else (totalRunsConceded.toDouble() / totalBallsBowled.toDouble()) * 6.0
}

sealed interface GameScreen {
    data object Home : GameScreen
    data object Toss : GameScreen
    data object Match : GameScreen
    data object InningsBreak : GameScreen
    data object Result : GameScreen
}

data class CricketUiState(
    val screen: GameScreen = GameScreen.Home,
    val selectedFormat: MatchFormat = MatchFormat.T3_BLITZ,
    val activeFormat: MatchFormat = MatchFormat.T3_BLITZ,
    val isSuperOverDecider: Boolean = false,
    val selectedDifficulty: AiDifficulty = AiDifficulty.PRO,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val sessionStats: SessionStats = SessionStats(),
    val tossState: TossState = TossState(),
    val inningsNumber: Int = 1,
    val currentInnings: InningsStats = InningsStats(TeamSide.PLAYER),
    val firstInningsCompleted: InningsStats? = null,
    val targetRuns: Int? = null,
    val lastDelivery: DeliveryEvent? = null,
    val deliverySeq: Int = 0,
    val isDeliveryAnimating: Boolean = false,
    val matchResult: MatchResultSummary? = null,
    val showExitDialog: Boolean = false,
    val showRulesSheet: Boolean = false,
) {
    val isPlayerBatting: Boolean
        get() = currentInnings.battingSide == TeamSide.PLAYER

    val ballsRemaining: Int?
        get() = activeFormat.maxBalls?.let { (it - currentInnings.balls).coerceAtLeast(0) }

    val runsNeeded: Int?
        get() = targetRuns?.let { (it - currentInnings.runs).coerceAtLeast(0) }

    val requiredRunRate: Double?
        get() {
            val needed = runsNeeded ?: return null
            val remaining = ballsRemaining ?: return null
            if (remaining <= 0) return null
            return (needed.toDouble() / remaining.toDouble()) * 6.0
        }
}
