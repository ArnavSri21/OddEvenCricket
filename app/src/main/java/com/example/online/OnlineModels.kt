package com.example.online

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RoomMode(val wireValue: String, val label: String) {
    DUEL_1V1("DUEL_1V1", "1v1 Duel"),
    TEAM_PLAY("TEAM_PLAY", "Team Play");

    companion object {
        fun fromWire(raw: String?): RoomMode =
            entries.firstOrNull { it.wireValue == raw } ?: DUEL_1V1
    }
}

enum class RoomStage(val wireValue: String) {
    WAITING("waiting"),
    CAPTAIN_REVEAL("captain_reveal"),
    DRAFT_TOSS("draft_toss"),
    TEAM_DRAFT("team_draft"),
    MATCH_TOSS("match_toss"),
    PLAYING("playing"),
    INNINGS_BREAK("innings_break"),
    FINISHED("finished");

    companion object {
        fun fromWire(raw: String?): RoomStage =
            entries.firstOrNull { it.wireValue == raw } ?: WAITING
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    val matchesPlayed: Long = 0L,
    val matchesWon: Long = 0L,
    val bestScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FriendCodeLookup(
    val userId: String = "",
    val displayName: String = "",
    val friendCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val requesterUid: String = "",
    val recipientUid: String = "",
    val requesterName: String = "",
    val recipientName: String = "",
    val requesterCode: String = "",
    val recipientCode: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun friendUidFor(myUid: String): String =
        if (requesterUid == myUid) recipientUid else requesterUid

    fun friendNameFor(myUid: String): String =
        if (requesterUid == myUid) recipientName else requesterName

    fun friendCodeFor(myUid: String): String =
        if (requesterUid == myUid) recipientCode else requesterCode
}

@Keep
@IgnoreExtraProperties
data class RoomInvite(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val roomCode: String = "",
    val roomMode: String = RoomMode.DUEL_1V1.wireValue,
    val formatId: String = "t3_blitz",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class ReactionType(
    val wireValue: String,
    val emoji: String,
    val callout: String,
) {
    FIRE("FIRE", "\uD83D\uDD25", "ON FIRE!"),
    CLAP("CLAP", "\uD83D\uDC4F", "CLASS!"),
    SIX("SIX", "\uD83C\uDFCF", "MAX SIX!"),
    WICKET("WICKET", "\uD83D\uDC80", "OUT!"),
    SHOCK("SHOCK", "\uD83D\uDE31", "NO WAY!"),
    LAUGH("LAUGH", "\uD83D\uDE02", "HAHA!"),
    CROWN("CROWN", "\uD83D\uDC51", "KING!"),
    GG("GG", "\uD83C\uDFAF", "GG!");

    companion object {
        fun fromWire(raw: String?): ReactionType =
            entries.firstOrNull { it.wireValue == raw } ?: FIRE
    }
}

@Keep
@IgnoreExtraProperties
data class CricketGroup(
    val groupCode: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    val memberUids: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val mutedUids: List<String> = emptyList(),
    val announcement: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun nameOf(uid: String): String = memberNames[uid] ?: "Player"
    fun isCaptain(uid: String): Boolean = ownerUid == uid
    fun isMuted(uid: String): Boolean = uid in mutedUids
}

@Keep
@IgnoreExtraProperties
data class GroupChatMessage(
    @DocumentId val id: String = "",
    val groupCode: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val isCaptain: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MatchHistoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "SOLO",
    val formatId: String = "t3_blitz",
    val outcome: String = "WIN",
    val runsScored: Long = 0L,
    val wicketsLost: Long = 0L,
    val ballsFaced: Long = 0L,
    val fours: Long = 0L,
    val sixes: Long = 0L,
    val opponentName: String = "AI",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MatchReaction(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val reactionType: String = ReactionType.FIRE.wireValue,
    val allowedUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun type(): ReactionType = ReactionType.fromWire(reactionType)
}

@Keep
@IgnoreExtraProperties
data class GameRoom(
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val mode: String = RoomMode.DUEL_1V1.wireValue,
    val formatId: String = "t3_blitz",
    val openLobby: Boolean = true,
    val status: String = RoomStage.WAITING.wireValue,
    val groupCode: String = "",
    val groupMemberUids: List<String> = emptyList(),
    val challengedUid: String = "",
    val spectatorUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val rosterUids: List<String> = emptyList(),
    val playerNames: Map<String, String> = emptyMap(),
    val botUids: List<String> = emptyList(),
    val captainAUid: String = "",
    val captainBUid: String = "",
    val draftPoolUids: List<String> = emptyList(),
    val teamAUids: List<String> = emptyList(),
    val teamBUids: List<String> = emptyList(),
    val commonPlayerUid: String = "",
    val tossCallerUid: String = "",
    val tossCall: String = "",
    val tossPickA: Long = 0L,
    val tossPickB: Long = 0L,
    val tossWinnerUid: String = "",
    val draftTurnUid: String = "",
    val lastTossSummary: String = "",
    val inningsNumber: Long = 1L,
    val battingTeam: String = "",
    val battingLineup: List<String> = emptyList(),
    val bowlingLineup: List<String> = emptyList(),
    val activeBatterUid: String = "",
    val activeBowlerUid: String = "",
    val batterPick: Long = 0L,
    val bowlerPick: Long = 0L,
    val runs: Long = 0L,
    val wickets: Long = 0L,
    val balls: Long = 0L,
    val fours: Long = 0L,
    val sixes: Long = 0L,
    val maxWickets: Long = 3L,
    val maxBalls: Long = 18L,
    val firstInningsRuns: Long = 0L,
    val firstInningsWickets: Long = 0L,
    val firstInningsBalls: Long = 0L,
    val firstInningsTeam: String = "",
    val targetRuns: Long = 0L,
    val recentBalls: List<String> = emptyList(),
    val lastBallSummary: String = "",
    val lastBatterPick: Long = 0L,
    val lastBowlerPick: Long = 0L,
    val lastDeliverySeq: Long = 0L,
    val winnerTeam: String = "",
    val resultHeadline: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun stage(): RoomStage = RoomStage.fromWire(status)
    fun roomMode(): RoomMode = RoomMode.fromWire(mode)
    fun nameOf(uid: String): String = playerNames[uid] ?: "Player"
    fun isBot(uid: String): Boolean = uid in botUids
    fun isSpectator(uid: String): Boolean = uid !in memberUids
    fun isLiveMatch(): Boolean {
        val s = stage()
        return s != RoomStage.WAITING && s != RoomStage.FINISHED
    }

    fun oversText(): String {
        val b = balls.toInt()
        return "${b / 6}.${b % 6}"
    }
}

sealed interface OnlineUiState<out T> {
    data object Loading : OnlineUiState<Nothing>
    data class Success<T>(val data: T) : OnlineUiState<T>
    data class Error(val message: String) : OnlineUiState<Nothing>
}
