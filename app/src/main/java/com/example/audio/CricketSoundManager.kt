package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.game.CricketUiState
import com.example.game.GameScreen
import com.example.game.MatchWinner
import com.example.online.GameRoom
import com.example.online.RoomStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

enum class CricketSoundEffect(val fileName: String, val defaultVolume: Float = 1.0f) {
    BAT_HIT_SINGLE("bat_hit_single_v1.wav", 0.90f),
    BAT_HIT_FOUR("bat_hit_four_v1.wav", 0.96f),
    BAT_HIT_SIX("bat_hit_six_v1.wav", 1.00f),
    WICKET_OUT("wicket_out_v1.wav", 1.00f),
    UMPIRE_SIGNAL("umpire_signal_v1.wav", 0.85f),
    TOSS_FLIP("toss_flip_v1.wav", 0.90f),
    CROWD_APPLAUSE("crowd_applause_v1.wav", 0.85f),
    INNINGS_BREAK_HORN("innings_break_v1.wav", 0.92f),
    VICTORY_FANFARE("victory_fanfare_v1.wav", 0.98f),
    DEFEAT_STING("defeat_sting_v1.wav", 0.82f),
}

/**
 * Low-latency SoundPool-backed Cricket Match Sound Manager.
 *
 * Pre-generates and caches realistic synthesized cricket WAV files on a background I/O thread,
 * loads them into Android's native SoundPool for polyphonic <10ms playback, and cleanly tears
 * down resources via [release].
 */
class CricketSoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val soundIds = ConcurrentHashMap<CricketSoundEffect, Int>()
    private val loadedIds = ConcurrentHashMap.newKeySet<Int>()
    private val enabled = AtomicBoolean(true)
    private val released = AtomicBoolean(false)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedIds.add(sampleId)
            }
        }
        prepareSoundBankAsync()
    }

    private fun prepareSoundBankAsync() {
        scope.launch {
            try {
                val cacheDir = File(appContext.cacheDir, "cricket_sfx_v1").apply {
                    if (!exists()) mkdirs()
                }
                CricketSoundEffect.entries.forEach { effect ->
                    if (released.get()) return@launch
                    val wavFile = File(cacheDir, effect.fileName)
                    if (!wavFile.exists() || wavFile.length() < 64L) {
                        CricketSoundSynthesizer.generateWavToFile(effect, wavFile)
                    }
                    if (!released.get() && wavFile.exists()) {
                        val soundId = soundPool.load(wavFile.absolutePath, 1)
                        if (soundId != 0) {
                            soundIds[effect] = soundId
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize cricket sound bank: ${e.message}")
            }
        }
    }

    fun setEnabled(isEnabled: Boolean) {
        enabled.set(isEnabled)
    }

    fun isEnabled(): Boolean = enabled.get()

    fun play(
        effect: CricketSoundEffect,
        volume: Float = effect.defaultVolume,
        rate: Float = 1.0f,
    ) {
        if (!enabled.get() || released.get()) return
        val soundId = soundIds[effect] ?: return
        if (soundId !in loadedIds) return
        val clampedVol = volume.coerceIn(0f, 1f)
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        try {
            soundPool.play(soundId, clampedVol, clampedVol, 1, 0, clampedRate)
        } catch (e: Exception) {
            Log.w(TAG, "SoundPool play failed for $effect: ${e.message}")
        }
    }

    /**
     * Triggers the appropriate bat-on-ball, boundary cheer, or wicket clatter + umpire signal
     * with subtle organic pitch variation so repeated deliveries feel authentic.
     */
    fun playDeliveryOutcome(runs: Int, isWicket: Boolean) {
        val subtlePitch = 0.96f + Random.nextFloat() * 0.08f
        when {
            isWicket -> play(CricketSoundEffect.WICKET_OUT, rate = 0.98f + Random.nextFloat() * 0.04f)
            runs >= 6 -> play(CricketSoundEffect.BAT_HIT_SIX, rate = subtlePitch)
            runs == 4 -> play(CricketSoundEffect.BAT_HIT_FOUR, rate = subtlePitch)
            runs > 0 -> play(CricketSoundEffect.BAT_HIT_SINGLE, rate = subtlePitch)
        }
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            scope.cancel()
            soundIds.clear()
            loadedIds.clear()
            try {
                soundPool.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing SoundPool: ${e.message}")
            }
        }
    }

    private companion object {
        const val TAG = "CricketSoundManager"
    }
}

/**
 * State-driven audio observer that automatically dispatches realistic cricket SFX
 * across both Solo AI matches and Online Multiplayer / Spectator rooms based on
 * state transitions — without replaying stale effects on recomposition.
 */
@Composable
fun CricketAudioEffectsBridge(
    soundManager: CricketSoundManager,
    soundEnabled: Boolean,
    soloState: CricketUiState,
    activeOnlineRoom: GameRoom?,
    currentUserId: String,
) {
    LaunchedEffect(soundEnabled) {
        soundManager.setEnabled(soundEnabled)
    }

    // --- 1. SOLO AI GAME STATE TRANSITIONS ---
    var prevSoloScreen by remember { mutableStateOf(soloState.screen) }
    LaunchedEffect(soloState.screen) {
        val current = soloState.screen
        if (current != prevSoloScreen) {
            when (current) {
                GameScreen.Toss -> soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL, volume = 0.72f)
                GameScreen.Match -> soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL)
                GameScreen.InningsBreak -> soundManager.play(CricketSoundEffect.INNINGS_BREAK_HORN)
                GameScreen.Result -> {
                    when (soloState.matchResult?.winner) {
                        MatchWinner.PLAYER -> soundManager.play(CricketSoundEffect.VICTORY_FANFARE)
                        MatchWinner.CPU -> soundManager.play(CricketSoundEffect.DEFEAT_STING)
                        MatchWinner.TIE -> {
                            soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL)
                            delay(240L)
                            soundManager.play(CricketSoundEffect.CROWD_APPLAUSE)
                        }
                        null -> Unit
                    }
                }
                GameScreen.Home -> Unit
            }
            prevSoloScreen = current
        }
    }

    // Solo Toss coin flip & toss resolution
    var prevTossRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(soloState.tossState.playerThrow, soloState.tossState.cpuThrow) {
        val hasThrow = soloState.tossState.playerThrow != null && soloState.tossState.cpuThrow != null
        if (hasThrow && !prevTossRevealed) {
            soundManager.play(CricketSoundEffect.TOSS_FLIP)
        }
        prevTossRevealed = hasThrow
    }
    var prevTossWinner by remember { mutableStateOf(soloState.tossState.winner) }
    LaunchedEffect(soloState.tossState.winner) {
        val winner = soloState.tossState.winner
        if (winner != null && winner != prevTossWinner) {
            if (winner == com.example.game.TeamSide.PLAYER) {
                soundManager.play(CricketSoundEffect.CROWD_APPLAUSE, volume = 0.78f)
            }
        }
        prevTossWinner = winner
    }

    // Solo Delivery outcomes (bat hit / four / six / wicket + end-of-over umpire signal)
    var lastPlayedSoloSeq by remember { mutableIntStateOf(soloState.deliverySeq) }
    LaunchedEffect(soloState.deliverySeq) {
        if (soloState.deliverySeq > lastPlayedSoloSeq) {
            lastPlayedSoloSeq = soloState.deliverySeq
            val delivery = soloState.lastDelivery ?: return@LaunchedEffect
            soundManager.playDeliveryOutcome(
                runs = delivery.runsScored,
                isWicket = delivery.isWicket,
            )
            // Umpire "Over!" signal on completion of every 6-ball over (if match continues)
            val balls = soloState.currentInnings.balls
            if (!delivery.isWicket && balls > 0 && balls % 6 == 0 && soloState.screen == GameScreen.Match) {
                delay(760L)
                soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL, volume = 0.65f)
            }
        } else if (soloState.deliverySeq < lastPlayedSoloSeq) {
            lastPlayedSoloSeq = soloState.deliverySeq
        }
    }

    // --- 2. ONLINE MULTIPLAYER & SPECTATOR ROOM TRANSITIONS ---
    var trackedRoomCode by remember { mutableStateOf<String?>(null) }
    var prevOnlineStage by remember { mutableStateOf<RoomStage?>(null) }
    var lastPlayedOnlineSeq by remember { mutableLongStateOf(0L) }
    var prevOnlineTossWinner by remember { mutableStateOf("") }
    var prevOnlineDraftSize by remember { mutableIntStateOf(-1) }

    LaunchedEffect(
        activeOnlineRoom?.roomCode,
        activeOnlineRoom?.status,
        activeOnlineRoom?.lastDeliverySeq,
        activeOnlineRoom?.tossWinnerUid,
        activeOnlineRoom?.draftPoolUids?.size,
    ) {
        val room = activeOnlineRoom
        if (room == null) {
            trackedRoomCode = null
            prevOnlineStage = null
            lastPlayedOnlineSeq = 0L
            prevOnlineTossWinner = ""
            prevOnlineDraftSize = -1
            return@LaunchedEffect
        }

        // First snapshot when entering/joining/spectating a room: seed baselines without firing stale sounds
        if (trackedRoomCode != room.roomCode) {
            trackedRoomCode = room.roomCode
            prevOnlineStage = room.stage()
            lastPlayedOnlineSeq = room.lastDeliverySeq
            prevOnlineTossWinner = room.tossWinnerUid
            prevOnlineDraftSize = room.draftPoolUids.size
            return@LaunchedEffect
        }

        val currentStage = room.stage()

        // Online Stage Transitions
        if (currentStage != prevOnlineStage) {
            when (currentStage) {
                RoomStage.CAPTAIN_REVEAL -> soundManager.play(CricketSoundEffect.CROWD_APPLAUSE)
                RoomStage.DRAFT_TOSS, RoomStage.MATCH_TOSS -> soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL, volume = 0.78f)
                RoomStage.TEAM_DRAFT -> soundManager.play(CricketSoundEffect.CROWD_APPLAUSE, volume = 0.76f)
                RoomStage.PLAYING -> soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL)
                RoomStage.INNINGS_BREAK -> soundManager.play(CricketSoundEffect.INNINGS_BREAK_HORN)
                RoomStage.FINISHED -> {
                    val inA = currentUserId in room.teamAUids || currentUserId == room.captainAUid
                    val inB = currentUserId in room.teamBUids || currentUserId == room.captainBUid
                    val isCommon = currentUserId == room.commonPlayerUid
                    val isSpectator = !inA && !inB && !isCommon
                    when {
                        room.winnerTeam == "TIE" -> {
                            soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL)
                            delay(240L)
                            soundManager.play(CricketSoundEffect.CROWD_APPLAUSE)
                        }
                        isSpectator -> soundManager.play(CricketSoundEffect.VICTORY_FANFARE, volume = 0.85f)
                        (room.winnerTeam == "A" && (inA || isCommon)) ||
                            (room.winnerTeam == "B" && (inB || isCommon)) ->
                            soundManager.play(CricketSoundEffect.VICTORY_FANFARE)
                        else -> soundManager.play(CricketSoundEffect.DEFEAT_STING)
                    }
                }
                RoomStage.WAITING -> Unit
            }
            prevOnlineStage = currentStage
        }

        // Online Toss winner newly decided
        if (room.tossWinnerUid.isNotBlank() && room.tossWinnerUid != prevOnlineTossWinner) {
            prevOnlineTossWinner = room.tossWinnerUid
            soundManager.play(CricketSoundEffect.TOSS_FLIP)
            if (room.tossWinnerUid == currentUserId) {
                delay(300L)
                soundManager.play(CricketSoundEffect.CROWD_APPLAUSE, volume = 0.78f)
            }
        } else if (room.tossWinnerUid.isBlank()) {
            prevOnlineTossWinner = ""
        }

        // Online Team Draft pick animation sound
        val draftSize = room.draftPoolUids.size
        if (currentStage == RoomStage.TEAM_DRAFT && prevOnlineDraftSize > 0 && draftSize < prevOnlineDraftSize) {
            soundManager.play(CricketSoundEffect.CROWD_APPLAUSE, volume = 0.65f)
        }
        prevOnlineDraftSize = draftSize

        // Online Delivery Outcome (works for Players AND Live Spectators!)
        if (room.lastDeliverySeq > lastPlayedOnlineSeq) {
            lastPlayedOnlineSeq = room.lastDeliverySeq
            val batterPick = room.lastBatterPick.toInt()
            val bowlerPick = room.lastBowlerPick.toInt()
            if (batterPick > 0 && bowlerPick > 0) {
                val isWicket = batterPick == bowlerPick
                soundManager.playDeliveryOutcome(
                    runs = if (isWicket) 0 else batterPick,
                    isWicket = isWicket,
                )
                if (!isWicket && room.balls > 0L && room.balls % 6L == 0L && currentStage == RoomStage.PLAYING) {
                    delay(760L)
                    soundManager.play(CricketSoundEffect.UMPIRE_SIGNAL, volume = 0.65f)
                }
            }
        }
    }
}
