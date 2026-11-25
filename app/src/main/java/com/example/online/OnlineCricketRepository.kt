package com.example.online

import com.example.game.MatchFormat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class OnlineCricketRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val botCallsigns = listOf(
        "Virat [BOT]", "Rohit [BOT]", "Bumrah [BOT]", "Stokes [BOT]",
        "Cummins [BOT]", "Rashid [BOT]", "Surya [BOT]", "Shubman [BOT]", "Archer [BOT]",
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun generateCode(): String =
        (1..6).map { codeChars[Random.nextInt(codeChars.length)] }.joinToString("")

    private fun sanitizeDisplayName(raw: String?): String {
        val cleaned = (raw ?: "Cricket Player").trim().replace(Regex("\\s+"), " ")
        return cleaned.ifBlank { "Cricket Player" }.take(40)
    }

    // --- PROFILE & FRIEND CODE ---

    suspend fun ensurePlayerProfile(displayNameRaw: String?): Result<PlayerProfile> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val profilePath = "player_profiles/$uid"
        return try {
            val profileRef = db.collection("player_profiles").document(uid)
            val snap = profileRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
                if (existing != null) {
                    return Result.success(existing)
                }
            }
            val name = sanitizeDisplayName(displayNameRaw)
            var chosenCode = generateCode()
            // Avoid unlikely collision on friend_codes
            for (attempt in 0 until 5) {
                val codeSnap = db.collection("friend_codes").document(chosenCode).get().await()
                if (!codeSnap.exists()) break
                chosenCode = generateCode()
            }

            val profilePayload = mapOf(
                "userId" to uid,
                "displayName" to name,
                "friendCode" to chosenCode,
                "matchesPlayed" to 0L,
                "matchesWon" to 0L,
                "bestScore" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val codePayload = mapOf(
                "userId" to uid,
                "displayName" to name,
                "friendCode" to chosenCode,
                "createdAt" to FieldValue.serverTimestamp(),
            )

            val batch = db.batch()
            batch.set(profileRef, profilePayload)
            batch.set(db.collection("friend_codes").document(chosenCode), codePayload)
            batch.commit().await()

            val created = profileRef.get().await().toObject(
                PlayerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ) ?: PlayerProfile(userId = uid, displayName = name, friendCode = chosenCode)
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    fun observeMyProfile(userId: String): Flow<PlayerProfile?> =
        db.collection("player_profiles")
            .document(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.GET, "player_profiles/$userId")
                throw error
            }

    fun observeMatchHistory(userId: String): Flow<List<MatchHistoryRecord>> =
        db.collection("player_profiles")
            .document(userId)
            .collection("match_history")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    MatchHistoryRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "player_profiles/$userId/match_history")
                throw error
            }

    suspend fun recordMatchResultAndStats(
        mode: String,
        formatId: String,
        outcome: String,
        runsScored: Int,
        wicketsLost: Int,
        ballsFaced: Int,
        fours: Int,
        sixes: Int,
        opponentName: String,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val historyPath = "player_profiles/$uid/match_history"
        return try {
            val matchId = "mh_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            val cleanOpponent = opponentName.trim().ifBlank { "Opponent" }.take(40)
            val historyPayload = mapOf(
                "userId" to uid,
                "mode" to mode,
                "formatId" to formatId,
                "outcome" to outcome,
                "runsScored" to runsScored.coerceIn(0, 10000).toLong(),
                "wicketsLost" to wicketsLost.coerceIn(0, 10).toLong(),
                "ballsFaced" to ballsFaced.coerceIn(0, 200).toLong(),
                "fours" to fours.coerceIn(0, 200).toLong(),
                "sixes" to sixes.coerceIn(0, 200).toLong(),
                "opponentName" to cleanOpponent,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("player_profiles")
                .document(uid)
                .collection("match_history")
                .document(matchId)
                .set(historyPayload)
                .await()

            recordCompletedOnlineStats(won = outcome == "WIN", playerTeamRuns = runsScored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, historyPath)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedOnlineStats(won: Boolean, playerTeamRuns: Int): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "player_profiles/$uid"
        return try {
            val ref = db.collection("player_profiles").document(uid)
            val snap = ref.get().await()
            val cur = snap.toObject(PlayerProfile::class.java) ?: return Result.success(Unit)
            val nextPlayed = cur.matchesPlayed + 1L
            val nextWon = cur.matchesWon + if (won) 1L else 0L
            val nextBest = maxOf(cur.bestScore, playerTeamRuns.toLong())
            ref.update(
                mapOf(
                    "matchesPlayed" to nextPlayed,
                    "matchesWon" to nextWon,
                    "bestScore" to nextBest,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FRIENDS & INVITES ---

    fun observeFriendships(userId: String): Flow<List<Friendship>> =
        db.collection("friendships")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    Friendship::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "friendships")
                throw error
            }

    fun observeIncomingRoomInvites(userId: String): Flow<List<RoomInvite>> =
        db.collection("room_invites")
            .whereEqualTo("toUid", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    RoomInvite::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).filter { it.status == "pending" }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "room_invites")
                throw error
            }

    suspend fun sendFriendRequestByCode(myProfile: PlayerProfile, rawCode: String): Result<String> {
        val code = rawCode.trim().uppercase()
        if (!code.matches(Regex("^[A-Z0-9]{6}$"))) {
            return Result.failure(IllegalArgumentException("Friend code must be 6 letters/numbers."))
        }
        if (code == myProfile.friendCode) {
            return Result.failure(IllegalArgumentException("That is your own Friend Code!"))
        }
        return try {
            val uid = requireUserId()
            val lookupSnap = db.collection("friend_codes").document(code).get().await()
            if (!lookupSnap.exists()) {
                return Result.failure(IllegalArgumentException("No player found with code $code."))
            }
            val target = lookupSnap.toObject(FriendCodeLookup::class.java)
                ?: return Result.failure(IllegalArgumentException("Could not read player code."))
            if (target.userId == uid) {
                return Result.failure(IllegalArgumentException("You cannot add yourself."))
            }

            val sortedUids = listOf(uid, target.userId).sorted()
            val friendshipId = "${sortedUids[0]}_${sortedUids[1]}"
            val docRef = db.collection("friendships").document(friendshipId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingStatus = existingSnap.getString("status")
                val msg = if (existingStatus == "accepted") {
                    "You are already friends with ${target.displayName}!"
                } else {
                    "A friend request with ${target.displayName} is already pending."
                }
                return Result.failure(IllegalArgumentException(msg))
            }

            val payload = mapOf(
                "memberUids" to sortedUids,
                "requesterUid" to uid,
                "recipientUid" to target.userId,
                "requesterName" to sanitizeDisplayName(myProfile.displayName),
                "recipientName" to sanitizeDisplayName(target.displayName),
                "requesterCode" to myProfile.friendCode,
                "recipientCode" to target.friendCode,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(target.displayName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "friendships")
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendshipId: String): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            require(friendshipId.isNotBlank())
            db.collection("friendships").document(friendshipId).update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFriendship(friendshipId: String): Result<Unit> {
        val path = "friendships/$friendshipId"
        return try {
            requireUserId()
            require(friendshipId.isNotBlank())
            db.collection("friendships").document(friendshipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendRoomInvite(
        myName: String,
        friendUid: String,
        roomCode: String,
        roomMode: RoomMode,
        formatId: String,
    ): Result<Unit> {
        val path = "room_invites"
        return try {
            val uid = requireUserId()
            val inviteId = "${roomCode}_${friendUid.take(10)}_${Random.nextInt(1000, 9999)}"
            val payload = mapOf(
                "fromUid" to uid,
                "fromName" to sanitizeDisplayName(myName),
                "toUid" to friendUid,
                "roomCode" to roomCode,
                "roomMode" to roomMode.wireValue,
                "formatId" to formatId,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("room_invites").document(inviteId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToRoomInvite(inviteId: String, accepted: Boolean): Result<Unit> {
        val path = "room_invites/$inviteId"
        return try {
            requireUserId()
            db.collection("room_invites").document(inviteId).update(
                mapOf(
                    "status" to if (accepted) "accepted" else "dismissed",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- GAME ROOMS & MULTIPLAYER ---

    fun observeOpenWaitingRooms(userId: String): Flow<List<GameRoom>> =
        db.collection("game_rooms")
            .whereEqualTo("openLobby", true)
            .whereEqualTo("status", RoomStage.WAITING.wireValue)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    GameRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "game_rooms")
                throw error
            }

    fun observeMyRooms(userId: String): Flow<List<GameRoom>> =
        db.collection("game_rooms")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    GameRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).filter { it.stage() != RoomStage.FINISHED }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "game_rooms")
                throw error
            }

    fun observeRoom(userId: String, roomCode: String): Flow<GameRoom?> =
        db.collection("game_rooms")
            .document(roomCode)
            .snapshots()
            .map { snap ->
                snap.toObject(
                    GameRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.GET, "game_rooms/$roomCode")
                throw error
            }

    suspend fun createRoom(
        hostName: String,
        mode: RoomMode,
        format: MatchFormat,
        openLobby: Boolean,
        groupCode: String = "",
        groupMemberUids: List<String> = emptyList(),
        challengedUid: String = "",
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val cleanHostName = sanitizeDisplayName(hostName)
            var roomCode = generateCode()
            for (attempt in 0 until 5) {
                val snap = db.collection("game_rooms").document(roomCode).get().await()
                if (!snap.exists()) break
                roomCode = generateCode()
            }

            val sanitizedGroupMembers = if (groupCode.isNotBlank()) {
                (groupMemberUids + uid).distinct().take(20)
            } else {
                emptyList()
            }

            val payload = mapOf(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "hostName" to cleanHostName,
                "mode" to mode.wireValue,
                "formatId" to format.id,
                "openLobby" to openLobby,
                "status" to RoomStage.WAITING.wireValue,
                "groupCode" to groupCode,
                "groupMemberUids" to sanitizedGroupMembers,
                "challengedUid" to challengedUid,
                "spectatorUids" to emptyList<String>(),
                "memberUids" to listOf(uid),
                "rosterUids" to listOf(uid),
                "playerNames" to mapOf(uid to cleanHostName),
                "botUids" to emptyList<String>(),
                "captainAUid" to "",
                "captainBUid" to "",
                "draftPoolUids" to emptyList<String>(),
                "teamAUids" to emptyList<String>(),
                "teamBUids" to emptyList<String>(),
                "commonPlayerUid" to "",
                "tossCallerUid" to "",
                "tossCall" to "",
                "tossPickA" to 0L,
                "tossPickB" to 0L,
                "tossWinnerUid" to "",
                "draftTurnUid" to "",
                "lastTossSummary" to "",
                "inningsNumber" to 1L,
                "battingTeam" to "",
                "battingLineup" to emptyList<String>(),
                "bowlingLineup" to emptyList<String>(),
                "activeBatterUid" to "",
                "activeBowlerUid" to "",
                "batterPick" to 0L,
                "bowlerPick" to 0L,
                "runs" to 0L,
                "wickets" to 0L,
                "balls" to 0L,
                "fours" to 0L,
                "sixes" to 0L,
                "maxWickets" to format.maxWickets.toLong(),
                "maxBalls" to (format.maxBalls?.toLong() ?: 0L),
                "firstInningsRuns" to 0L,
                "firstInningsWickets" to 0L,
                "firstInningsBalls" to 0L,
                "firstInningsTeam" to "",
                "targetRuns" to 0L,
                "recentBalls" to emptyList<String>(),
                "lastBallSummary" to "",
                "lastBatterPick" to 0L,
                "lastBowlerPick" to 0L,
                "lastDeliverySeq" to 0L,
                "winnerTeam" to "",
                "resultHeadline" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("game_rooms").document(roomCode).set(payload).await()
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "game_rooms")
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(rawCode: String, playerName: String): Result<String> {
        val code = rawCode.trim().uppercase()
        if (!code.matches(Regex("^[A-Z0-9]{6}$"))) {
            return Result.failure(IllegalArgumentException("Room code must be 6 characters."))
        }
        val path = "game_rooms/$code"
        return try {
            val uid = requireUserId()
            val cleanName = sanitizeDisplayName(playerName)
            val docRef = db.collection("game_rooms").document(code)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("Room $code not found.")
                val room = snap.toObject(GameRoom::class.java)
                    ?: throw IllegalStateException("Invalid room data.")
                if (uid in room.memberUids) {
                    return@runTransaction code
                }
                if (room.stage() != RoomStage.WAITING) {
                    throw IllegalStateException("Match already started in room $code. Tap Spectate to watch live!")
                }
                if (room.challengedUid.isNotBlank() && room.challengedUid != uid) {
                    throw IllegalStateException("This 1v1 challenge is for another player. Tap Spectate to watch live!")
                }
                val maxCap = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 10
                if (room.rosterUids.size >= maxCap) {
                    throw IllegalStateException("Room $code is already full.")
                }
                val newMembers = room.memberUids + uid
                val newRoster = room.rosterUids + uid
                val newNames = room.playerNames.toMutableMap().apply { put(uid, cleanName) }
                tx.update(
                    docRef,
                    mapOf(
                        "memberUids" to newMembers,
                        "rosterUids" to newRoster,
                        "playerNames" to newNames,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                code
            }.await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CRICKET GROUPS, CHALLENGES, SPECTATING & LIVE REACTIONS ---

    fun observeMyGroups(userId: String): Flow<List<CricketGroup>> =
        db.collection("cricket_groups")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CricketGroup::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "cricket_groups")
                throw error
            }

    fun observeGroupRooms(userId: String): Flow<List<GameRoom>> =
        db.collection("game_rooms")
            .whereArrayContains("groupMemberUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    GameRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).filter { it.stage() != RoomStage.FINISHED }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "game_rooms")
                throw error
            }

    suspend fun createGroup(rawName: String, ownerName: String): Result<String> {
        val cleanName = rawName.trim().replace(Regex("\\s+"), " ").take(40)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a group name first."))
        }
        return try {
            val uid = requireUserId()
            val cleanOwner = sanitizeDisplayName(ownerName)
            var groupCode = generateCode()
            for (attempt in 0 until 5) {
                val snap = db.collection("cricket_groups").document(groupCode).get().await()
                if (!snap.exists()) break
                groupCode = generateCode()
            }
            val payload = mapOf(
                "groupCode" to groupCode,
                "name" to cleanName,
                "ownerUid" to uid,
                "ownerName" to cleanOwner,
                "memberUids" to listOf(uid),
                "memberNames" to mapOf(uid to cleanOwner),
                "mutedUids" to emptyList<String>(),
                "announcement" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.collection("cricket_groups").document(groupCode).set(payload).await()
            Result.success(groupCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "cricket_groups")
            Result.failure(e)
        }
    }

    // --- GROUP CHAT & CAPTAIN ADMIN CONTROLS ---

    fun observeGroupMessages(userId: String, groupCode: String): Flow<List<GroupChatMessage>> =
        db.collection("cricket_groups")
            .document(groupCode)
            .collection("messages")
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    GroupChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    .takeLast(100)
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "cricket_groups/$groupCode/messages")
                throw error
            }

    suspend fun sendGroupMessage(
        group: CricketGroup,
        senderName: String,
        rawText: String,
    ): Result<Unit> {
        val cleanText = rawText.trim().take(300)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        val path = "cricket_groups/${group.groupCode}/messages"
        return try {
            val uid = requireUserId()
            if (group.isMuted(uid)) {
                return Result.failure(IllegalStateException("You are currently muted in this group by the Captain."))
            }
            val msgId = "msg_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            val payload = mapOf(
                "groupCode" to group.groupCode,
                "senderUid" to uid,
                "senderName" to sanitizeDisplayName(senderName),
                "text" to cleanText,
                "isCaptain" to (uid == group.ownerUid),
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("cricket_groups")
                .document(group.groupCode)
                .collection("messages")
                .document(msgId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGroupMessage(groupCode: String, messageId: String): Result<Unit> {
        val path = "cricket_groups/$groupCode/messages/$messageId"
        return try {
            requireUserId()
            db.collection("cricket_groups")
                .document(groupCode)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun kickMemberFromGroup(group: CricketGroup, targetUid: String): Result<Unit> {
        val path = "cricket_groups/${group.groupCode}"
        return try {
            val uid = requireUserId()
            check(group.ownerUid == uid) { "Only the Group Captain can remove members." }
            check(targetUid != group.ownerUid) { "Group Captain cannot kick themselves." }
            val newMembers = group.memberUids.filter { it != targetUid }
            val newNames = group.memberNames.filterKeys { it != targetUid }
            val newMuted = group.mutedUids.filter { it != targetUid }
            db.collection("cricket_groups").document(group.groupCode).update(
                mapOf(
                    "memberUids" to newMembers,
                    "memberNames" to newNames,
                    "mutedUids" to newMuted,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMuteMemberInGroup(group: CricketGroup, targetUid: String): Result<Boolean> {
        val path = "cricket_groups/${group.groupCode}"
        return try {
            val uid = requireUserId()
            check(group.ownerUid == uid) { "Only the Group Captain can mute members." }
            check(targetUid != group.ownerUid) { "Group Captain cannot mute themselves." }
            val willMute = targetUid !in group.mutedUids
            val newMuted = if (willMute) {
                (group.mutedUids + targetUid).distinct().take(20)
            } else {
                group.mutedUids.filter { it != targetUid }
            }
            db.collection("cricket_groups").document(group.groupCode).update(
                mapOf(
                    "mutedUids" to newMuted,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(willMute)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupCaptainSettings(
        group: CricketGroup,
        rawName: String,
        rawAnnouncement: String,
    ): Result<Unit> {
        val cleanName = rawName.trim().replace(Regex("\\s+"), " ").take(40)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be blank."))
        }
        val cleanAnnouncement = rawAnnouncement.trim().take(140)
        val path = "cricket_groups/${group.groupCode}"
        return try {
            val uid = requireUserId()
            check(group.ownerUid == uid) { "Only the Group Captain can update group settings." }
            db.collection("cricket_groups").document(group.groupCode).update(
                mapOf(
                    "name" to cleanName,
                    "announcement" to cleanAnnouncement,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(rawCode: String, memberName: String): Result<String> {
        val code = rawCode.trim().uppercase()
        if (!code.matches(Regex("^[A-Z0-9]{6}$"))) {
            return Result.failure(IllegalArgumentException("Group code must be 6 letters/numbers."))
        }
        val path = "cricket_groups/$code"
        return try {
            val uid = requireUserId()
            val cleanName = sanitizeDisplayName(memberName)
            val docRef = db.collection("cricket_groups").document(code)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) throw IllegalStateException("Group $code not found.")
                val group = snap.toObject(CricketGroup::class.java)
                    ?: throw IllegalStateException("Invalid group data.")
                if (uid in group.memberUids) {
                    return@runTransaction code
                }
                if (group.memberUids.size >= 20) {
                    throw IllegalStateException("Group $code is full (max 20 members).")
                }
                val newMembers = group.memberUids + uid
                val newNames = group.memberNames.toMutableMap().apply { put(uid, cleanName) }
                tx.update(
                    docRef,
                    mapOf(
                        "memberUids" to newMembers,
                        "memberNames" to newNames,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                code
            }.await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addFriendToGroup(group: CricketGroup, friendUid: String, friendName: String): Result<Unit> {
        val path = "cricket_groups/${group.groupCode}"
        return try {
            val uid = requireUserId()
            check(group.ownerUid == uid) { "Only the group owner can add members directly." }
            if (friendUid in group.memberUids) return Result.success(Unit)
            if (group.memberUids.size >= 20) {
                return Result.failure(IllegalStateException("Group is full (max 20 members)."))
            }
            val newMembers = group.memberUids + friendUid
            val newNames = group.memberNames.toMutableMap().apply {
                put(friendUid, sanitizeDisplayName(friendName))
            }
            db.collection("cricket_groups").document(group.groupCode).update(
                mapOf(
                    "memberUids" to newMembers,
                    "memberNames" to newNames,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveOrDeleteGroup(group: CricketGroup): Result<Unit> {
        val path = "cricket_groups/${group.groupCode}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("cricket_groups").document(group.groupCode)
            if (group.ownerUid == uid) {
                docRef.delete().await()
            } else {
                val newMembers = group.memberUids.filter { it != uid }
                val newNames = group.memberNames.filterKeys { it != uid }
                docRef.update(
                    mapOf(
                        "memberUids" to newMembers,
                        "memberNames" to newNames,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun challengeFriend1v1(
        hostName: String,
        friendUid: String,
        format: MatchFormat,
    ): Result<String> {
        val roomRes = createRoom(
            hostName = hostName,
            mode = RoomMode.DUEL_1V1,
            format = format,
            openLobby = false,
            challengedUid = friendUid,
        )
        val code = roomRes.getOrElse { return Result.failure(it) }
        sendRoomInvite(
            myName = hostName,
            friendUid = friendUid,
            roomCode = code,
            roomMode = RoomMode.DUEL_1V1,
            formatId = format.id,
        )
        return Result.success(code)
    }

    suspend fun createGroupChallenge1v1(
        group: CricketGroup,
        hostName: String,
        targetUid: String,
        format: MatchFormat,
    ): Result<String> {
        val roomRes = createRoom(
            hostName = hostName,
            mode = RoomMode.DUEL_1V1,
            format = format,
            openLobby = false,
            groupCode = group.groupCode,
            groupMemberUids = group.memberUids,
            challengedUid = targetUid,
        )
        val code = roomRes.getOrElse { return Result.failure(it) }
        sendRoomInvite(
            myName = hostName,
            friendUid = targetUid,
            roomCode = code,
            roomMode = RoomMode.DUEL_1V1,
            formatId = format.id,
        )
        return Result.success(code)
    }

    suspend fun createGroupMatchAll(
        group: CricketGroup,
        hostName: String,
        format: MatchFormat,
    ): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val roomRes = createRoom(
            hostName = hostName,
            mode = RoomMode.TEAM_PLAY,
            format = format,
            openLobby = false,
            groupCode = group.groupCode,
            groupMemberUids = group.memberUids,
            challengedUid = "",
        )
        val code = roomRes.getOrElse { return Result.failure(it) }
        // Notify every other group member with a room invite
        group.memberUids.filter { it != uid }.forEach { memberUid ->
            sendRoomInvite(
                myName = hostName,
                friendUid = memberUid,
                roomCode = code,
                roomMode = RoomMode.TEAM_PLAY,
                formatId = format.id,
            )
        }
        return Result.success(code)
    }

    suspend fun enterSpectatorMode(roomCode: String): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("game_rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = snap.toObject(GameRoom::class.java) ?: return@runTransaction
                if (room.stage() == RoomStage.FINISHED) return@runTransaction
                if (uid in room.memberUids || uid in room.spectatorUids) return@runTransaction
                if (room.spectatorUids.size >= 20) return@runTransaction
                tx.update(
                    docRef,
                    mapOf(
                        "spectatorUids" to (room.spectatorUids + uid),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveSpectatorMode(roomCode: String): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("game_rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = snap.toObject(GameRoom::class.java) ?: return@runTransaction
                if (room.stage() == RoomStage.FINISHED) return@runTransaction
                if (uid !in room.spectatorUids) return@runTransaction
                tx.update(
                    docRef,
                    mapOf(
                        "spectatorUids" to room.spectatorUids.filter { it != uid },
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeRoomReactions(userId: String, roomCode: String): Flow<List<MatchReaction>> =
        db.collection("game_rooms")
            .document(roomCode)
            .collection("reactions")
            .whereArrayContains("allowedUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    MatchReaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                    .takeLast(25)
            }
            .catch { error ->
                logFlowErrorIfSignedIn(userId, error, OperationType.LIST, "game_rooms/$roomCode/reactions")
                throw error
            }

    suspend fun sendReaction(
        room: GameRoom,
        senderName: String,
        reactionType: ReactionType,
    ): Result<Unit> {
        val path = "game_rooms/${room.roomCode}/reactions"
        return try {
            val uid = requireUserId()
            val cleanName = sanitizeDisplayName(senderName)
            val allowed = (room.memberUids + room.groupMemberUids + room.spectatorUids + uid)
                .distinct()
                .take(25)
            val reactionId = "rx_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            val payload = mapOf(
                "roomCode" to room.roomCode,
                "senderUid" to uid,
                "senderName" to cleanName,
                "reactionType" to reactionType.wireValue,
                "allowedUids" to allowed,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("game_rooms")
                .document(room.roomCode)
                .collection("reactions")
                .document(reactionId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveWaitingRoom(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("game_rooms").document(room.roomCode)
            if (room.hostUid == uid) {
                docRef.delete().await()
            } else {
                val newMembers = room.memberUids.filter { it != uid }
                val newRoster = room.rosterUids.filter { it != uid }
                val newNames = room.playerNames.filterKeys { it != uid }
                docRef.update(
                    mapOf(
                        "memberUids" to newMembers,
                        "rosterUids" to newRoster,
                        "playerNames" to newNames,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addBotToWaitingRoom(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostUid == uid) { "Only the host can add bots." }
            val maxCap = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 10
            if (room.rosterUids.size >= maxCap) {
                return Result.failure(IllegalStateException("Room is already full."))
            }
            val botIndex = room.botUids.size + 1
            val botId = "bot_${room.roomCode}_$botIndex"
            val botName = botCallsigns[(botIndex - 1) % botCallsigns.size]
            val newBots = room.botUids + botId
            val newRoster = room.rosterUids + botId
            val newNames = room.playerNames.toMutableMap().apply { put(botId, botName) }

            db.collection("game_rooms").document(room.roomCode).update(
                mapOf(
                    "botUids" to newBots,
                    "rosterUids" to newRoster,
                    "playerNames" to newNames,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeLastBotFromWaitingRoom(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostUid == uid) { "Only the host can remove bots." }
            val lastBot = room.botUids.lastOrNull() ?: return Result.success(Unit)
            val newBots = room.botUids.dropLast(1)
            val newRoster = room.rosterUids.filter { it != lastBot }
            val newNames = room.playerNames.filterKeys { it != lastBot }

            db.collection("game_rooms").document(room.roomCode).update(
                mapOf(
                    "botUids" to newBots,
                    "rosterUids" to newRoster,
                    "playerNames" to newNames,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Starts a room match:
     * - DUEL_1V1: Assigns Player 1 = Captain A, Player 2 = Captain B, goes directly to MATCH_TOSS.
     * - TEAM_PLAY: Randomly picks 2 captains from rosterUids, puts remaining players in draftPoolUids,
     *   and opens CAPTAIN_REVEAL (before Odd-Even Toss #1 for drafting players one-by-one!).
     */
    suspend fun startMatchFromLobby(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            val uid = requireUserId()
            check(room.hostUid == uid) { "Only host can start the match." }
            val minPlayers = if (room.roomMode() == RoomMode.DUEL_1V1) 2 else 3
            if (room.rosterUids.size < minPlayers) {
                return Result.failure(
                    IllegalStateException("Need at least $minPlayers players to start ${room.roomMode().label}.")
                )
            }
            val format = MatchFormat.fromId(room.formatId)
            val docRef = db.collection("game_rooms").document(room.roomCode)

            if (room.roomMode() == RoomMode.DUEL_1V1) {
                val capA = room.rosterUids[0]
                val capB = room.rosterUids[1]
                docRef.update(
                    mapOf(
                        "status" to RoomStage.MATCH_TOSS.wireValue,
                        "captainAUid" to capA,
                        "captainBUid" to capB,
                        "teamAUids" to listOf(capA),
                        "teamBUids" to listOf(capB),
                        "draftPoolUids" to emptyList<String>(),
                        "commonPlayerUid" to "",
                        "tossCallerUid" to capA,
                        "tossCall" to "",
                        "tossPickA" to 0L,
                        "tossPickB" to 0L,
                        "tossWinnerUid" to "",
                        "lastTossSummary" to "",
                        "maxWickets" to format.maxWickets.toLong(),
                        "maxBalls" to (format.maxBalls?.toLong() ?: 0L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                // TEAM PLAY: Randomly choose 2 captains!
                val shuffled = room.rosterUids.shuffled()
                val capA = shuffled[0]
                val capB = shuffled[1]
                val pool = shuffled.drop(2)

                // Edge case: exactly 3 players total -> 2 captains + 1 player left!
                // Per rule: if odd count leaves 1 player after balanced teams, they are COMMON!
                val isImmediateCommon = pool.size == 1
                val finalPool = if (isImmediateCommon) emptyList() else pool
                val commonUid = if (isImmediateCommon) pool.first() else ""

                docRef.update(
                    mapOf(
                        "status" to RoomStage.CAPTAIN_REVEAL.wireValue,
                        "captainAUid" to capA,
                        "captainBUid" to capB,
                        "teamAUids" to listOf(capA),
                        "teamBUids" to listOf(capB),
                        "draftPoolUids" to finalPool,
                        "commonPlayerUid" to commonUid,
                        "tossCallerUid" to capA,
                        "tossCall" to "",
                        "tossPickA" to 0L,
                        "tossPickB" to 0L,
                        "tossWinnerUid" to "",
                        "draftTurnUid" to "",
                        "lastTossSummary" to "",
                        "maxBalls" to (format.maxBalls?.toLong() ?: 0L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun proceedFromCaptainReveal(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            requireUserId()
            // If draftPoolUids is empty (3 players where the 3rd became Common immediately), skip to Match Toss (#2)!
            // Otherwise go to Odd-Even Toss #1 (DRAFT_TOSS) so captains toss for first player draft pick!
            val nextStage = if (room.draftPoolUids.isEmpty()) {
                RoomStage.MATCH_TOSS
            } else {
                RoomStage.DRAFT_TOSS
            }
            db.collection("game_rooms").document(room.roomCode).update(
                mapOf(
                    "status" to nextStage.wireValue,
                    "tossCallerUid" to room.captainAUid,
                    "tossCall" to "",
                    "tossPickA" to 0L,
                    "tossPickB" to 0L,
                    "tossWinnerUid" to "",
                    "lastTossSummary" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitTossCall(roomCode: String, call: String): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            requireUserId()
            db.collection("game_rooms").document(roomCode).update(
                mapOf(
                    "tossCall" to call,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Submits a captain's toss finger throw (1..6) inside a Firestore transaction.
     * Once both captains have thrown, atomically computes sum parity & sets tossWinnerUid!
     */
    suspend fun submitTossNumber(roomCode: String, actorUid: String, number: Int): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            requireUserId()
            val docRef = db.collection("game_rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = snap.toObject(GameRoom::class.java) ?: return@runTransaction
                if (room.tossCall.isBlank() || room.tossWinnerUid.isNotBlank()) return@runTransaction

                var pickA = room.tossPickA
                var pickB = room.tossPickB
                if (actorUid == room.captainAUid && pickA == 0L) {
                    pickA = number.toLong()
                } else if (actorUid == room.captainBUid && pickB == 0L) {
                    pickB = number.toLong()
                } else {
                    return@runTransaction
                }

                // Auto-fill if the other captain is a room bot
                if (pickA == 0L && room.isBot(room.captainAUid)) {
                    pickA = Random.nextInt(1, 7).toLong()
                }
                if (pickB == 0L && room.isBot(room.captainBUid)) {
                    pickB = Random.nextInt(1, 7).toLong()
                }

                val updates = mutableMapOf<String, Any>(
                    "tossPickA" to pickA,
                    "tossPickB" to pickB,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                if (pickA > 0L && pickB > 0L) {
                    val sum = pickA + pickB
                    val isOdd = sum % 2L != 0L
                    val callerWon = (room.tossCall == "ODD" && isOdd) || (room.tossCall == "EVEN" && !isOdd)
                    val otherCap = if (room.tossCallerUid == room.captainAUid) room.captainBUid else room.captainAUid
                    val winnerUid = if (callerWon) room.tossCallerUid else otherCap
                    val parityText = if (isOdd) "ODD" else "EVEN"
                    val summary = "${room.nameOf(room.captainAUid)} ($pickA) + ${room.nameOf(room.captainBUid)} ($pickB) = $sum ($parityText) • ${room.nameOf(winnerUid)} won!"

                    updates["tossWinnerUid"] = winnerUid
                    updates["lastTossSummary"] = summary.take(180)
                    if (room.stage() == RoomStage.DRAFT_TOSS) {
                        updates["draftTurnUid"] = winnerUid
                    }
                }
                tx.update(docRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceToTeamDraft(roomCode: String): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            requireUserId()
            db.collection("game_rooms").document(roomCode).update(
                mapOf(
                    "status" to RoomStage.TEAM_DRAFT.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Captain picks one player from draftPoolUids.
     * Alternates turns between Captain A and Captain B.
     * If teams become equal in size and EXACTLY 1 player remains in draftPoolUids,
     * automatically designates that last remaining player as the COMMON player (plays both sides)!
     */
    suspend fun draftPlayer(roomCode: String, captainUid: String, pickedPlayerUid: String): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            requireUserId()
            val docRef = db.collection("game_rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = snap.toObject(GameRoom::class.java) ?: return@runTransaction
                if (room.stage() != RoomStage.TEAM_DRAFT) return@runTransaction
                if (room.draftTurnUid != captainUid) return@runTransaction
                if (pickedPlayerUid !in room.draftPoolUids) return@runTransaction

                val nextTeamA = if (captainUid == room.captainAUid) room.teamAUids + pickedPlayerUid else room.teamAUids
                val nextTeamB = if (captainUid == room.captainBUid) room.teamBUids + pickedPlayerUid else room.teamBUids
                var remainingPool = room.draftPoolUids.filter { it != pickedPlayerUid }
                var commonUid = room.commonPlayerUid

                // Odd number of players rule:
                // When both teams are balanced and 1 player is left at last -> set as COMMON!
                if (nextTeamA.size == nextTeamB.size && remainingPool.size == 1) {
                    commonUid = remainingPool.first()
                    remainingPool = emptyList()
                }

                val nextCaptain = if (captainUid == room.captainAUid) room.captainBUid else room.captainAUid
                val draftComplete = remainingPool.isEmpty()

                val updates = mutableMapOf<String, Any>(
                    "teamAUids" to nextTeamA,
                    "teamBUids" to nextTeamB,
                    "draftPoolUids" to remainingPool,
                    "commonPlayerUid" to commonUid,
                    "draftTurnUid" to if (draftComplete) "" else nextCaptain,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                if (draftComplete) {
                    // Move directly to Odd-Even Toss #2 (Batting & Bowling toss)!
                    // Let the captain who lost Toss #1 call Toss #2 for fairness.
                    val secondTossCaller = if (room.tossWinnerUid == room.captainAUid) {
                        room.captainBUid
                    } else {
                        room.captainAUid
                    }
                    updates["status"] = RoomStage.MATCH_TOSS.wireValue
                    updates["tossCallerUid"] = secondTossCaller
                    updates["tossCall"] = ""
                    updates["tossPickA"] = 0L
                    updates["tossPickB"] = 0L
                    updates["tossWinnerUid"] = ""
                    updates["lastTossSummary"] = if (commonUid.isNotBlank()) {
                        "Teams locked! ${room.nameOf(commonUid)} is COMMON (plays for both sides)."
                    } else {
                        "Teams locked! Time for the Batting & Bowling Toss."
                    }.take(180)
                }

                tx.update(docRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Winner of Match Toss (#2) chooses Bat First or Bowl First and kicks off 1st Innings!
     */
    suspend fun chooseMatchBattingOrder(room: GameRoom, winnerBatsFirst: Boolean): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            requireUserId()
            val winnerIsA = room.tossWinnerUid == room.captainAUid
            val firstBattingTeam = if (winnerBatsFirst) {
                if (winnerIsA) "A" else "B"
            } else {
                if (winnerIsA) "B" else "A"
            }
            val batLineup = buildLineupForTeam(room, firstBattingTeam)
            val bowlTeam = if (firstBattingTeam == "A") "B" else "A"
            val bowlLineup = buildLineupForTeam(room, bowlTeam)

            val format = MatchFormat.fromId(room.formatId)
            val effectiveMaxWickets = if (room.roomMode() == RoomMode.TEAM_PLAY) {
                batLineup.size.coerceIn(1, 10)
            } else {
                format.maxWickets
            }

            db.collection("game_rooms").document(room.roomCode).update(
                mapOf(
                    "status" to RoomStage.PLAYING.wireValue,
                    "inningsNumber" to 1L,
                    "battingTeam" to firstBattingTeam,
                    "battingLineup" to batLineup,
                    "bowlingLineup" to bowlLineup,
                    "activeBatterUid" to (batLineup.firstOrNull() ?: ""),
                    "activeBowlerUid" to (bowlLineup.firstOrNull() ?: ""),
                    "batterPick" to 0L,
                    "bowlerPick" to 0L,
                    "runs" to 0L,
                    "wickets" to 0L,
                    "balls" to 0L,
                    "fours" to 0L,
                    "sixes" to 0L,
                    "maxWickets" to effectiveMaxWickets.toLong(),
                    "maxBalls" to (format.maxBalls?.toLong() ?: 0L),
                    "firstInningsRuns" to 0L,
                    "firstInningsWickets" to 0L,
                    "firstInningsBalls" to 0L,
                    "firstInningsTeam" to "",
                    "targetRuns" to 0L,
                    "recentBalls" to emptyList<String>(),
                    "lastBallSummary" to "1st Innings underway! ${room.nameOf(batLineup.firstOrNull() ?: "")} takes strike.",
                    "lastBatterPick" to 0L,
                    "lastBowlerPick" to 0L,
                    "lastDeliverySeq" to 0L,
                    "winnerTeam" to "",
                    "resultHeadline" to "",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Submits a batter or bowler finger pick (1..6) inside a Firestore transaction.
     * Automatically fills bot picks and resolves the delivery when both batter & bowler have thrown!
     */
    suspend fun submitDeliveryPick(roomCode: String, actorUid: String, number: Int): Result<Unit> {
        val path = "game_rooms/$roomCode"
        return try {
            requireUserId()
            val docRef = db.collection("game_rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = snap.toObject(GameRoom::class.java) ?: return@runTransaction
                if (room.stage() != RoomStage.PLAYING) return@runTransaction

                var batPick = room.batterPick
                var bowlPick = room.bowlerPick

                if (actorUid == room.activeBatterUid && batPick == 0L) {
                    batPick = number.toLong()
                } else if (actorUid == room.activeBowlerUid && bowlPick == 0L) {
                    bowlPick = number.toLong()
                } else {
                    return@runTransaction
                }

                // Auto-play if opposing striker/bowler is a room bot
                if (batPick == 0L && room.isBot(room.activeBatterUid)) {
                    batPick = Random.nextInt(1, 7).toLong()
                }
                if (bowlPick == 0L && room.isBot(room.activeBowlerUid)) {
                    bowlPick = Random.nextInt(1, 7).toLong()
                }

                if (batPick == 0L || bowlPick == 0L) {
                    tx.update(
                        docRef,
                        mapOf(
                            "batterPick" to batPick,
                            "bowlerPick" to bowlPick,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                    return@runTransaction
                }

                // Resolve completed delivery!
                val isWicket = batPick == bowlPick
                val runsScored = if (isWicket) 0L else batPick
                val nextRuns = room.runs + runsScored
                val nextWickets = room.wickets + if (isWicket) 1L else 0L
                val nextBalls = room.balls + 1L
                val nextFours = room.fours + if (!isWicket && runsScored == 4L) 1L else 0L
                val nextSixes = room.sixes + if (!isWicket && runsScored == 6L) 1L else 0L
                val badge = if (isWicket) "W" else runsScored.toString()
                val nextRecent = (room.recentBalls + badge).takeLast(60)
                val batterName = room.nameOf(room.activeBatterUid)
                val bowlerName = room.nameOf(room.activeBowlerUid)

                val summary = when {
                    isWicket -> "OUT! $bowlerName bowls $batterName (both showed $batPick)!"
                    runsScored == 6L -> "SIX! $batterName smashes $bowlerName into the stands!"
                    runsScored == 4L -> "FOUR! $batterName cracks a boundary!"
                    else -> "$batterName scores $runsScored off $bowlerName."
                }.take(180)

                val allOut = nextWickets >= room.maxWickets
                val oversDone = room.maxBalls > 0L && nextBalls >= room.maxBalls
                val targetChased = room.inningsNumber == 2L && room.targetRuns > 0L && nextRuns >= room.targetRuns
                val inningsEnded = allOut || oversDone || targetChased

                // Rotate batter on wicket (Team Play lineup), rotate bowler every over (6 balls)
                val nextBatterUid = if (isWicket && !allOut && room.battingLineup.isNotEmpty()) {
                    val idx = nextWickets.toInt().coerceAtMost(room.battingLineup.lastIndex)
                    room.battingLineup[idx]
                } else {
                    room.activeBatterUid
                }
                val nextBowlerUid = if (room.bowlingLineup.isNotEmpty()) {
                    val eligibleBowlers = room.bowlingLineup.filter { it != nextBatterUid }
                        .ifEmpty { room.bowlingLineup }
                    val overIndex = (nextBalls / 6L).toInt()
                    eligibleBowlers[overIndex % eligibleBowlers.size]
                } else {
                    room.activeBowlerUid
                }

                val updates = mutableMapOf<String, Any>(
                    "batterPick" to 0L,
                    "bowlerPick" to 0L,
                    "lastBatterPick" to batPick,
                    "lastBowlerPick" to bowlPick,
                    "lastDeliverySeq" to (room.lastDeliverySeq + 1L),
                    "runs" to nextRuns,
                    "wickets" to nextWickets,
                    "balls" to nextBalls,
                    "fours" to nextFours,
                    "sixes" to nextSixes,
                    "recentBalls" to nextRecent,
                    "lastBallSummary" to summary,
                    "activeBatterUid" to nextBatterUid,
                    "activeBowlerUid" to nextBowlerUid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )

                if (inningsEnded) {
                    if (room.inningsNumber == 1L) {
                        updates["status"] = RoomStage.INNINGS_BREAK.wireValue
                        updates["firstInningsRuns"] = nextRuns
                        updates["firstInningsWickets"] = nextWickets
                        updates["firstInningsBalls"] = nextBalls
                        updates["firstInningsTeam"] = room.battingTeam
                        updates["targetRuns"] = nextRuns + 1L
                    } else {
                        val firstTeam = room.firstInningsTeam
                        val secondTeam = room.battingTeam
                        val firstScore = room.firstInningsRuns
                        val (winner, headline) = computeTeamMatchOutcome(
                            room = room,
                            firstTeam = firstTeam,
                            firstRuns = firstScore,
                            secondTeam = secondTeam,
                            secondRuns = nextRuns,
                            secondWickets = nextWickets,
                            maxWickets = room.maxWickets,
                        )
                        updates["status"] = RoomStage.FINISHED.wireValue
                        updates["winnerTeam"] = winner
                        updates["resultHeadline"] = headline.take(180)
                    }
                }

                tx.update(docRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun startSecondInnings(room: GameRoom): Result<Unit> {
        val path = "game_rooms/${room.roomCode}"
        return try {
            requireUserId()
            val secondBattingTeam = if (room.firstInningsTeam == "A") "B" else "A"
            val secondBowlingTeam = room.firstInningsTeam
            val batLineup = buildLineupForTeam(room, secondBattingTeam)
            val bowlLineup = buildLineupForTeam(room, secondBowlingTeam)
            val format = MatchFormat.fromId(room.formatId)
            val effectiveMaxWickets = if (room.roomMode() == RoomMode.TEAM_PLAY) {
                batLineup.size.coerceIn(1, 10)
            } else {
                format.maxWickets
            }

            db.collection("game_rooms").document(room.roomCode).update(
                mapOf(
                    "status" to RoomStage.PLAYING.wireValue,
                    "inningsNumber" to 2L,
                    "battingTeam" to secondBattingTeam,
                    "battingLineup" to batLineup,
                    "bowlingLineup" to bowlLineup,
                    "activeBatterUid" to (batLineup.firstOrNull() ?: ""),
                    "activeBowlerUid" to (bowlLineup.firstOrNull() ?: ""),
                    "batterPick" to 0L,
                    "bowlerPick" to 0L,
                    "runs" to 0L,
                    "wickets" to 0L,
                    "balls" to 0L,
                    "fours" to 0L,
                    "sixes" to 0L,
                    "maxWickets" to effectiveMaxWickets.toLong(),
                    "recentBalls" to emptyList<String>(),
                    "lastBallSummary" to "Chase is ON! Target: ${room.targetRuns} runs.",
                    "lastBatterPick" to 0L,
                    "lastBowlerPick" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun buildLineupForTeam(room: GameRoom, team: String): List<String> {
        val base = if (team == "A") room.teamAUids else room.teamBUids
        // Common player plays for BOTH teams — bats at the tail of each team's lineup!
        return if (room.commonPlayerUid.isNotBlank() && room.commonPlayerUid !in base) {
            base + room.commonPlayerUid
        } else {
            base
        }
    }

    private fun teamLabel(room: GameRoom, team: String): String {
        return if (room.roomMode() == RoomMode.DUEL_1V1) {
            val uid = if (team == "A") room.captainAUid else room.captainBUid
            room.nameOf(uid)
        } else {
            if (team == "A") "Team Gold" else "Team Mint"
        }
    }

    private fun computeTeamMatchOutcome(
        room: GameRoom,
        firstTeam: String,
        firstRuns: Long,
        secondTeam: String,
        secondRuns: Long,
        secondWickets: Long,
        maxWickets: Long,
    ): Pair<String, String> {
        return when {
            secondRuns > firstRuns -> {
                val wktsLeft = (maxWickets - secondWickets).coerceAtLeast(1L)
                val word = if (wktsLeft == 1L) "wicket" else "wickets"
                secondTeam to "${teamLabel(room, secondTeam)} won by $wktsLeft $word!"
            }
            firstRuns > secondRuns -> {
                val diff = firstRuns - secondRuns
                val word = if (diff == 1L) "run" else "runs"
                firstTeam to "${teamLabel(room, firstTeam)} won by $diff $word!"
            }
            else -> "TIE" to "Match Tied at $firstRuns runs!"
        }
    }

    private fun logFlowErrorIfSignedIn(
        uid: String,
        error: Throwable,
        operationType: OperationType,
        path: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
