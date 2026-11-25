package com.example.game

import kotlin.random.Random

class CricketAiEngine(
    private val random: Random = Random.Default,
) {
    fun pickTossNumber(): Int = random.nextInt(1, 7)

    fun chooseCpuBatFirst(format: MatchFormat): Boolean {
        return when (format) {
            MatchFormat.CLASSIC_1W -> random.nextFloat() < 0.55f
            MatchFormat.SUPER_OVER -> random.nextFloat() < 0.45f
            MatchFormat.T3_BLITZ, MatchFormat.T5_CLASH -> random.nextFloat() < 0.4f
        }
    }

    fun pickMatchNumber(
        difficulty: AiDifficulty,
        isCpuBatting: Boolean,
        recentPlayerPicks: List<Int>,
        runsNeeded: Int?,
        ballsRemaining: Int?,
    ): Int {
        return when (difficulty) {
            AiDifficulty.ROOKIE -> pickRookie(isCpuBatting)
            AiDifficulty.PRO -> pickPro(isCpuBatting, recentPlayerPicks, runsNeeded, ballsRemaining)
            AiDifficulty.LEGEND -> pickLegend(isCpuBatting, recentPlayerPicks, runsNeeded, ballsRemaining)
        }
    }

    private fun pickRookie(isCpuBatting: Boolean): Int {
        if (!isCpuBatting && random.nextFloat() < 0.25f) {
            // Rookie bowlers slightly over-bowl 1-4, giving the player occasional boundary openings
            return random.nextInt(1, 5)
        }
        return random.nextInt(1, 7)
    }

    private fun pickPro(
        isCpuBatting: Boolean,
        recentPlayerPicks: List<Int>,
        runsNeeded: Int?,
        ballsRemaining: Int?,
    ): Int {
        val window = recentPlayerPicks.takeLast(6)
        if (!isCpuBatting) {
            // CPU is bowling: try to match player's favorite recent pick 32% of the time
            if (window.size >= 3 && random.nextFloat() < 0.32f) {
                val favorite = mostFrequentPick(window)
                if (favorite != null) return favorite
            }
            return random.nextInt(1, 7)
        } else {
            // CPU is batting: avoid player's most common recent bowling pick 35% of the time
            val avoided = if (window.size >= 3 && random.nextFloat() < 0.35f) {
                mostFrequentPick(window)
            } else {
                null
            }
            return pickWeightedBattingNumber(
                avoided = avoided,
                runsNeeded = runsNeeded,
                ballsRemaining = ballsRemaining,
                aggressiveBias = 0.55f,
            )
        }
    }

    private fun pickLegend(
        isCpuBatting: Boolean,
        recentPlayerPicks: List<Int>,
        runsNeeded: Int?,
        ballsRemaining: Int?,
    ): Int {
        val window = recentPlayerPicks.takeLast(8)
        if (!isCpuBatting) {
            // CPU is bowling with tactical reads
            // 1. Detect consecutive repeat habit
            if (window.size >= 2 && window.last() == window[window.size - 2] && random.nextFloat() < 0.42f) {
                return window.last()
            }
            // 2. Detect alternating habit (A, B, A -> anticipate B)
            if (window.size >= 3 && window[window.size - 1] == window[window.size - 3] && random.nextFloat() < 0.38f) {
                return window[window.size - 2]
            }
            // 3. Target high-frequency boundary shot
            if (window.size >= 3 && random.nextFloat() < 0.40f) {
                val favorite = mostFrequentPick(window)
                if (favorite != null) return favorite
            }
            // 4. Pressure bowling if batter needs big hits
            if (runsNeeded != null && ballsRemaining != null && ballsRemaining > 0) {
                val rrr = runsNeeded.toDouble() / ballsRemaining.toDouble()
                if (rrr >= 4.2 && random.nextFloat() < 0.50f) {
                    return if (random.nextBoolean()) 6 else 4
                }
            }
            return random.nextInt(1, 7)
        } else {
            // CPU is batting smartly
            val predictedBowlerPick = when {
                window.size >= 2 && window.last() == window[window.size - 2] -> window.last()
                window.size >= 3 -> mostFrequentPick(window)
                else -> null
            }
            val avoid = if (random.nextFloat() < 0.55f) predictedBowlerPick else null
            return pickWeightedBattingNumber(
                avoided = avoid,
                runsNeeded = runsNeeded,
                ballsRemaining = ballsRemaining,
                aggressiveBias = 0.70f,
            )
        }
    }

    private fun pickWeightedBattingNumber(
        avoided: Int?,
        runsNeeded: Int?,
        ballsRemaining: Int?,
        aggressiveBias: Float,
    ): Int {
        val candidates = (1..6).filter { it != avoided }.ifEmpty { (1..6).toList() }

        // If finishing runs are within reach on a single shot, look for a smart finisher
        if (runsNeeded != null && runsNeeded in 1..6 && random.nextFloat() < 0.65f) {
            val finishers = candidates.filter { it >= runsNeeded }
            if (finishers.isNotEmpty()) {
                return finishers[random.nextInt(finishers.size)]
            }
        }

        // High required run rate -> lean toward 4, 5, 6
        val needsBigHits = runsNeeded != null && ballsRemaining != null && ballsRemaining > 0 &&
            (runsNeeded.toDouble() / ballsRemaining.toDouble()) >= 3.8

        val preferred = if (needsBigHits || random.nextFloat() < aggressiveBias) {
            candidates.filter { it >= 4 }.ifEmpty { candidates }
        } else {
            candidates
        }
        return preferred[random.nextInt(preferred.size)]
    }

    private fun mostFrequentPick(picks: List<Int>): Int? {
        return picks
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }
}
