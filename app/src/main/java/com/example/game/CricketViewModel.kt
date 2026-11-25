package com.example.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoloCompletedMatch(
    val formatId: String,
    val outcome: String,
    val runsScored: Int,
    val wicketsLost: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val opponentName: String,
)

class CricketViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val aiEngine: CricketAiEngine = CricketAiEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(CricketUiState())
    val uiState: StateFlow<CricketUiState> = _uiState.asStateFlow()

    var onMatchCompletedListener: ((SoloCompletedMatch) -> Unit)? = null

    private var animationJob: Job? = null
    private val playerPicksCurrentInnings = mutableListOf<Int>()
    private var currentBoundaryStreak = 0

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        selectedFormat = prefs.format,
                        selectedDifficulty = prefs.difficulty,
                        hapticsEnabled = prefs.hapticsEnabled,
                        soundEnabled = prefs.soundEnabled,
                    )
                }
            }
        }
    }

    fun selectFormat(format: MatchFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
        viewModelScope.launch { preferencesRepository.saveFormat(format) }
    }

    fun selectDifficulty(difficulty: AiDifficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        viewModelScope.launch { preferencesRepository.saveDifficulty(difficulty) }
    }

    fun toggleHaptics() {
        val next = !_uiState.value.hapticsEnabled
        _uiState.update { it.copy(hapticsEnabled = next) }
        viewModelScope.launch { preferencesRepository.saveHaptics(next) }
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        _uiState.update { it.copy(soundEnabled = next) }
        viewModelScope.launch { preferencesRepository.saveSound(next) }
    }

    fun setRulesSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showRulesSheet = visible) }
    }

    fun requestExitMatch() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun confirmExitToHome() {
        animationJob?.cancel()
        playerPicksCurrentInnings.clear()
        currentBoundaryStreak = 0
        _uiState.update {
            it.copy(
                screen = GameScreen.Home,
                showExitDialog = false,
                isDeliveryAnimating = false,
                isSuperOverDecider = false,
                tossState = TossState(),
                lastDelivery = null,
                firstInningsCompleted = null,
                targetRuns = null,
                matchResult = null,
            )
        }
    }

    fun startNewMatchToToss() {
        animationJob?.cancel()
        val chosenFormat = _uiState.value.selectedFormat
        _uiState.update {
            it.copy(
                screen = GameScreen.Toss,
                activeFormat = chosenFormat,
                isSuperOverDecider = false,
                tossState = TossState(),
                firstInningsCompleted = null,
                targetRuns = null,
                lastDelivery = null,
                matchResult = null,
                showExitDialog = false,
            )
        }
    }

    fun startSuperOverDecider() {
        animationJob?.cancel()
        val lastResult = _uiState.value.matchResult
        // Team that batted second in the tied match bats first in Super Over (standard cricket rule)
        val firstBatter = lastResult?.secondInnings?.battingSide ?: TeamSide.PLAYER
        playerPicksCurrentInnings.clear()
        currentBoundaryStreak = 0
        _uiState.update {
            it.copy(
                screen = GameScreen.Match,
                activeFormat = MatchFormat.SUPER_OVER,
                isSuperOverDecider = true,
                inningsNumber = 1,
                currentInnings = InningsStats(battingSide = firstBatter),
                firstInningsCompleted = null,
                targetRuns = null,
                lastDelivery = null,
                isDeliveryAnimating = false,
                matchResult = null,
            )
        }
    }

    fun selectTossCall(call: TossCall) {
        val current = _uiState.value.tossState
        if (current.winner != null || current.isRevealing) return
        _uiState.update { it.copy(tossState = it.tossState.copy(playerCall = call)) }
    }

    fun throwTossNumber(playerPick: Int) {
        val state = _uiState.value
        val call = state.tossState.playerCall ?: return
        if (state.tossState.winner != null || state.tossState.isRevealing) return

        val cpuPick = aiEngine.pickTossNumber()
        val sumIsOdd = (playerPick + cpuPick) % 2 != 0
        val playerWon = (call == TossCall.ODD && sumIsOdd) || (call == TossCall.EVEN && !sumIsOdd)
        val winner = if (playerWon) TeamSide.PLAYER else TeamSide.CPU
        val cpuDecisionBat = if (playerWon) null else aiEngine.chooseCpuBatFirst(state.activeFormat)

        animationJob?.cancel()
        _uiState.update {
            it.copy(
                tossState = it.tossState.copy(
                    playerThrow = playerPick,
                    cpuThrow = cpuPick,
                    isRevealing = true,
                )
            )
        }

        animationJob = viewModelScope.launch {
            delay(360L)
            _uiState.update {
                it.copy(
                    tossState = it.tossState.copy(
                        winner = winner,
                        cpuDecisionBattingFirst = cpuDecisionBat,
                        isRevealing = false,
                    )
                )
            }
        }
    }

    fun chooseBattingOrder(playerBatsFirst: Boolean) {
        val firstBatter = if (playerBatsFirst) TeamSide.PLAYER else TeamSide.CPU
        playerPicksCurrentInnings.clear()
        currentBoundaryStreak = 0
        _uiState.update {
            it.copy(
                screen = GameScreen.Match,
                inningsNumber = 1,
                currentInnings = InningsStats(battingSide = firstBatter),
                firstInningsCompleted = null,
                targetRuns = null,
                lastDelivery = null,
                deliverySeq = 0,
                isDeliveryAnimating = false,
                matchResult = null,
            )
        }
    }

    fun startMatchAfterCpuToss() {
        val cpuBatsFirst = _uiState.value.tossState.cpuDecisionBattingFirst ?: true
        chooseBattingOrder(playerBatsFirst = !cpuBatsFirst)
    }

    fun playDelivery(playerPick: Int) {
        val state = _uiState.value
        if (state.screen != GameScreen.Match || state.isDeliveryAnimating) return

        val isPlayerBatting = state.isPlayerBatting
        val cpuPick = aiEngine.pickMatchNumber(
            difficulty = state.selectedDifficulty,
            isCpuBatting = !isPlayerBatting,
            recentPlayerPicks = playerPicksCurrentInnings.toList(),
            runsNeeded = state.runsNeeded,
            ballsRemaining = state.ballsRemaining,
        )
        playerPicksCurrentInnings.add(playerPick)

        val batterPick = if (isPlayerBatting) playerPick else cpuPick
        val isWicket = playerPick == cpuPick
        val runsScored = if (isWicket) 0 else batterPick
        val nextBallNum = state.currentInnings.balls + 1

        if (!isWicket && (runsScored == 4 || runsScored == 6)) {
            currentBoundaryStreak += 1
        } else {
            currentBoundaryStreak = 0
        }

        val commentary = buildCommentary(
            isWicket = isWicket,
            runsScored = runsScored,
            isPlayerBatting = isPlayerBatting,
            matchNumber = playerPick,
        )

        val event = DeliveryEvent(
            ballNumber = nextBallNum,
            playerPick = playerPick,
            cpuPick = cpuPick,
            batterSide = state.currentInnings.battingSide,
            runsScored = runsScored,
            isWicket = isWicket,
            commentary = commentary,
        )

        val updatedRuns = state.currentInnings.runs + runsScored
        val updatedWickets = state.currentInnings.wickets + if (isWicket) 1 else 0
        val updatedInnings = state.currentInnings.copy(
            runs = updatedRuns,
            wickets = updatedWickets,
            balls = nextBallNum,
            fours = state.currentInnings.fours + if (event.isFour) 1 else 0,
            sixes = state.currentInnings.sixes + if (event.isSix) 1 else 0,
            deliveries = state.currentInnings.deliveries + event,
            cumulativeRuns = state.currentInnings.cumulativeRuns + updatedRuns,
            bestBoundaryStreak = maxOf(state.currentInnings.bestBoundaryStreak, currentBoundaryStreak),
        )

        val allOut = updatedWickets >= state.activeFormat.maxWickets
        val oversDone = state.activeFormat.maxBalls != null && nextBallNum >= state.activeFormat.maxBalls
        val targetChased = state.targetRuns != null && updatedRuns >= state.targetRuns
        val inningsEnded = allOut || oversDone || targetChased

        _uiState.update {
            it.copy(
                currentInnings = updatedInnings,
                lastDelivery = event,
                deliverySeq = it.deliverySeq + 1,
                isDeliveryAnimating = true,
            )
        }

        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            delay(if (isWicket || inningsEnded) 820L else 360L)
            if (inningsEnded) {
                handleInningsComplete(updatedInnings)
            } else {
                _uiState.update { it.copy(isDeliveryAnimating = false) }
            }
        }
    }

    private fun handleInningsComplete(completedInnings: InningsStats) {
        val state = _uiState.value
        if (state.inningsNumber == 1) {
            _uiState.update {
                it.copy(
                    screen = GameScreen.InningsBreak,
                    firstInningsCompleted = completedInnings,
                    targetRuns = completedInnings.runs + 1,
                    isDeliveryAnimating = false,
                )
            }
        } else {
            val first = state.firstInningsCompleted ?: InningsStats(TeamSide.PLAYER)
            val second = completedInnings
            val result = computeMatchResult(
                firstInnings = first,
                secondInnings = second,
                maxWickets = state.activeFormat.maxWickets,
                wasSuperOver = state.isSuperOverDecider,
            )

            val playerInnings = if (first.battingSide == TeamSide.PLAYER) first else second
            val cpuInnings = if (first.battingSide == TeamSide.CPU) first else second
            val playerInningsScore = playerInnings.runs
            val outcomeWire = when (result.winner) {
                MatchWinner.PLAYER -> "WIN"
                MatchWinner.CPU -> "LOSS"
                MatchWinner.TIE -> "TIE"
            }
            val aiLabel = when (state.selectedDifficulty) {
                AiDifficulty.ROOKIE -> "Rookie AI"
                AiDifficulty.PRO -> "Pro AI"
                AiDifficulty.LEGEND -> "Legend AI"
            }
            val prevStats = state.sessionStats
            val entry = SessionMatchEntry(
                id = "solo_${prevStats.matchesPlayed + 1}_${System.currentTimeMillis()}",
                modeLabel = if (state.isSuperOverDecider) "Super Over" else "Solo AI",
                opponentName = aiLabel,
                outcome = outcomeWire,
                runsScored = playerInnings.runs,
                ballsFaced = playerInnings.balls,
                wicketsTaken = cpuInnings.wickets,
                ballsBowled = cpuInnings.balls,
                runsConceded = cpuInnings.runs,
                fours = playerInnings.fours,
                sixes = playerInnings.sixes,
            )
            val newStats = prevStats.copy(
                matchesPlayed = prevStats.matchesPlayed + 1,
                matchesWon = prevStats.matchesWon + if (result.winner == MatchWinner.PLAYER) 1 else 0,
                matchesLost = prevStats.matchesLost + if (result.winner == MatchWinner.CPU) 1 else 0,
                matchesTied = prevStats.matchesTied + if (result.winner == MatchWinner.TIE) 1 else 0,
                bestPlayerScore = maxOf(prevStats.bestPlayerScore, playerInningsScore),
                currentWinStreak = when (result.winner) {
                    MatchWinner.PLAYER -> prevStats.currentWinStreak + 1
                    MatchWinner.CPU -> 0
                    MatchWinner.TIE -> prevStats.currentWinStreak
                },
                totalRunsScored = prevStats.totalRunsScored + playerInnings.runs,
                totalBallsFaced = prevStats.totalBallsFaced + playerInnings.balls,
                totalWicketsTaken = prevStats.totalWicketsTaken + cpuInnings.wickets,
                totalBallsBowled = prevStats.totalBallsBowled + cpuInnings.balls,
                totalRunsConceded = prevStats.totalRunsConceded + cpuInnings.runs,
                totalFours = prevStats.totalFours + playerInnings.fours,
                totalSixes = prevStats.totalSixes + playerInnings.sixes,
                recentMatches = (listOf(entry) + prevStats.recentMatches).take(20),
            )

            _uiState.update {
                it.copy(
                    screen = GameScreen.Result,
                    matchResult = result,
                    sessionStats = newStats,
                    isDeliveryAnimating = false,
                )
            }

            onMatchCompletedListener?.invoke(
                SoloCompletedMatch(
                    formatId = state.activeFormat.id,
                    outcome = outcomeWire,
                    runsScored = playerInnings.runs,
                    wicketsLost = playerInnings.wickets,
                    ballsFaced = playerInnings.balls,
                    fours = playerInnings.fours,
                    sixes = playerInnings.sixes,
                    opponentName = aiLabel,
                )
            )
        }
    }

    fun recordOnlineSessionMatch(
        modeLabel: String,
        opponentName: String,
        outcome: String,
        runsScored: Int,
        ballsFaced: Int,
        wicketsTaken: Int,
        ballsBowled: Int,
        runsConceded: Int,
        fours: Int,
        sixes: Int,
    ) {
        _uiState.update { state ->
            val prev = state.sessionStats
            val entry = SessionMatchEntry(
                id = "online_${prev.matchesPlayed + 1}_${System.currentTimeMillis()}",
                modeLabel = modeLabel,
                opponentName = opponentName,
                outcome = outcome,
                runsScored = runsScored,
                ballsFaced = ballsFaced,
                wicketsTaken = wicketsTaken,
                ballsBowled = ballsBowled,
                runsConceded = runsConceded,
                fours = fours,
                sixes = sixes,
            )
            state.copy(
                sessionStats = prev.copy(
                    matchesPlayed = prev.matchesPlayed + 1,
                    matchesWon = prev.matchesWon + if (outcome == "WIN") 1 else 0,
                    matchesLost = prev.matchesLost + if (outcome == "LOSS") 1 else 0,
                    matchesTied = prev.matchesTied + if (outcome == "TIE") 1 else 0,
                    bestPlayerScore = maxOf(prev.bestPlayerScore, runsScored),
                    currentWinStreak = when (outcome) {
                        "WIN" -> prev.currentWinStreak + 1
                        "LOSS" -> 0
                        else -> prev.currentWinStreak
                    },
                    totalRunsScored = prev.totalRunsScored + runsScored,
                    totalBallsFaced = prev.totalBallsFaced + ballsFaced,
                    totalWicketsTaken = prev.totalWicketsTaken + wicketsTaken,
                    totalBallsBowled = prev.totalBallsBowled + ballsBowled,
                    totalRunsConceded = prev.totalRunsConceded + runsConceded,
                    totalFours = prev.totalFours + fours,
                    totalSixes = prev.totalSixes + sixes,
                    recentMatches = (listOf(entry) + prev.recentMatches).take(20),
                )
            )
        }
    }

    fun startSecondInnings() {
        val first = _uiState.value.firstInningsCompleted ?: return
        val secondBatter = if (first.battingSide == TeamSide.PLAYER) TeamSide.CPU else TeamSide.PLAYER
        playerPicksCurrentInnings.clear()
        currentBoundaryStreak = 0
        _uiState.update {
            it.copy(
                screen = GameScreen.Match,
                inningsNumber = 2,
                currentInnings = InningsStats(battingSide = secondBatter),
                lastDelivery = null,
                isDeliveryAnimating = false,
            )
        }
    }

    private fun computeMatchResult(
        firstInnings: InningsStats,
        secondInnings: InningsStats,
        maxWickets: Int,
        wasSuperOver: Boolean,
    ): MatchResultSummary {
        return when {
            secondInnings.runs > firstInnings.runs -> {
                val wicketsInHand = (maxWickets - secondInnings.wickets).coerceAtLeast(1)
                val wicketWord = if (wicketsInHand == 1) "wicket" else "wickets"
                val winner = if (secondInnings.battingSide == TeamSide.PLAYER) MatchWinner.PLAYER else MatchWinner.CPU
                val subject = if (winner == MatchWinner.PLAYER) "You won" else "CPU won"
                MatchResultSummary(
                    winner = winner,
                    marginHeadline = "$subject by $wicketsInHand $wicketWord",
                    firstInnings = firstInnings,
                    secondInnings = secondInnings,
                    wasSuperOver = wasSuperOver,
                )
            }
            firstInnings.runs > secondInnings.runs -> {
                val runDiff = firstInnings.runs - secondInnings.runs
                val runWord = if (runDiff == 1) "run" else "runs"
                val winner = if (firstInnings.battingSide == TeamSide.PLAYER) MatchWinner.PLAYER else MatchWinner.CPU
                val subject = if (winner == MatchWinner.PLAYER) "You won" else "CPU won"
                MatchResultSummary(
                    winner = winner,
                    marginHeadline = "$subject by $runDiff $runWord",
                    firstInnings = firstInnings,
                    secondInnings = secondInnings,
                    wasSuperOver = wasSuperOver,
                )
            }
            else -> {
                MatchResultSummary(
                    winner = MatchWinner.TIE,
                    marginHeadline = "Scores level at ${firstInnings.runs}!",
                    firstInnings = firstInnings,
                    secondInnings = secondInnings,
                    wasSuperOver = wasSuperOver,
                )
            }
        }
    }

    private fun buildCommentary(
        isWicket: Boolean,
        runsScored: Int,
        isPlayerBatting: Boolean,
        matchNumber: Int,
    ): String {
        if (isWicket) {
            return if (isPlayerBatting) {
                "OUT! Both showed $matchNumber — clean bowled!"
            } else {
                "GOT 'EM! Both showed $matchNumber — timber!"
            }
        }
        return when (runsScored) {
            6 -> if (isPlayerBatting) "MAXIMUM! Massive 6 into the stands!" else "CPU launches a towering 6!"
            5 -> if (isPlayerBatting) "Rare 5! Brilliant running & overthrows!" else "CPU sneaks a cheeky 5!"
            4 -> if (isPlayerBatting) "CRACKING FOUR! Raced to the rope!" else "CPU pierces the gap for 4!"
            3 -> if (isPlayerBatting) "Three run well through deep cover." else "CPU pushes hard for 3."
            2 -> if (isPlayerBatting) "Crisp couple off the back foot." else "CPU works it away for 2."
            else -> if (isPlayerBatting) "Quick single — keeps the strike." else "CPU taps a single."
        }
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CricketViewModel(preferencesRepository = preferencesRepository) as T
        }
    }
}
