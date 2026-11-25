package com.example.online

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.MatchFormat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val ONLINE_VM_TAG = "OnlineCricketVM"
private const val STOP_TIMEOUT_MS = 5000L

data class OnlineCompletedSessionMatch(
    val modeLabel: String,
    val opponentName: String,
    val outcome: String,
    val runsScored: Int,
    val ballsFaced: Int,
    val wicketsTaken: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val fours: Int,
    val sixes: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineCricketViewModel(
    private val repository: OnlineCricketRepository,
    val currentUserId: String,
    initialDisplayName: String?,
) : ViewModel() {

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val recordedStatsRooms = mutableSetOf<String>()
    private var lastBotActionKey: String? = null

    var onOnlineMatchCompletedListener: ((OnlineCompletedSessionMatch) -> Unit)? = null

    val profileState: StateFlow<OnlineUiState<PlayerProfile?>> =
        repository.observeMyProfile(currentUserId)
            .map<PlayerProfile?, OnlineUiState<PlayerProfile?>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load profile")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val friendshipsState: StateFlow<OnlineUiState<List<Friendship>>> =
        repository.observeFriendships(currentUserId)
            .map<List<Friendship>, OnlineUiState<List<Friendship>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load friends")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val invitesState: StateFlow<OnlineUiState<List<RoomInvite>>> =
        repository.observeIncomingRoomInvites(currentUserId)
            .map<List<RoomInvite>, OnlineUiState<List<RoomInvite>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load invites")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val openRoomsState: StateFlow<OnlineUiState<List<GameRoom>>> =
        repository.observeOpenWaitingRooms(currentUserId)
            .map<List<GameRoom>, OnlineUiState<List<GameRoom>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load open rooms")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val myRoomsState: StateFlow<OnlineUiState<List<GameRoom>>> =
        repository.observeMyRooms(currentUserId)
            .map<List<GameRoom>, OnlineUiState<List<GameRoom>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load active rooms")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    private val _selectedGroupCode = MutableStateFlow<String?>(null)
    val selectedGroupCode: StateFlow<String?> = _selectedGroupCode.asStateFlow()

    val groupsState: StateFlow<OnlineUiState<List<CricketGroup>>> =
        repository.observeMyGroups(currentUserId)
            .map<List<CricketGroup>, OnlineUiState<List<CricketGroup>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load groups")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val groupRoomsState: StateFlow<OnlineUiState<List<GameRoom>>> =
        repository.observeGroupRooms(currentUserId)
            .map<List<GameRoom>, OnlineUiState<List<GameRoom>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load group matches")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val matchHistoryState: StateFlow<OnlineUiState<List<MatchHistoryRecord>>> =
        repository.observeMatchHistory(currentUserId)
            .map<List<MatchHistoryRecord>, OnlineUiState<List<MatchHistoryRecord>>> { OnlineUiState.Success(it) }
            .catch { error -> emit(unwrapFlowError(error, "Failed to load player stats")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Loading)

    val selectedGroupMessagesState: StateFlow<OnlineUiState<List<GroupChatMessage>>> =
        _selectedGroupCode
            .flatMapLatest { code ->
                if (code.isNullOrBlank()) {
                    flowOf(OnlineUiState.Success(emptyList()))
                } else {
                    repository.observeGroupMessages(currentUserId, code)
                        .map<List<GroupChatMessage>, OnlineUiState<List<GroupChatMessage>>> { OnlineUiState.Success(it) }
                        .catch { error -> emit(unwrapFlowError(error, "Failed to load group chat")) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Success(emptyList()))

    val activeRoomState: StateFlow<OnlineUiState<GameRoom?>> =
        _activeRoomCode
            .flatMapLatest { code ->
                if (code.isNullOrBlank()) {
                    flowOf(OnlineUiState.Success(null))
                } else {
                    repository.observeRoom(currentUserId, code)
                        .map<GameRoom?, OnlineUiState<GameRoom?>> { OnlineUiState.Success(it) }
                        .catch { error -> emit(unwrapFlowError(error, "Failed to sync room")) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnlineUiState.Success(null))

    val activeRoomReactions: StateFlow<List<MatchReaction>> =
        _activeRoomCode
            .flatMapLatest { code ->
                if (code.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeRoomReactions(currentUserId, code)
                        .catch { emit(emptyList()) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        // Ensure Firestore public player profile & friend code exist
        viewModelScope.launch {
            repository.ensurePlayerProfile(initialDisplayName)
        }

        // Host-driven automatic bot progression + post-match career stats recording
        viewModelScope.launch {
            activeRoomState.collect { state ->
                val room = (state as? OnlineUiState.Success)?.data ?: return@collect
                if (room.stage() == RoomStage.FINISHED && room.roomCode !in recordedStatsRooms) {
                    recordedStatsRooms.add(room.roomCode)
                    recordStatsIfParticipant(room)
                }
                if (room.hostUid == currentUserId) {
                    maybeDriveBotAction(room)
                }
            }
        }
    }

    private fun currentDisplayName(): String {
        val profile = (profileState.value as? OnlineUiState.Success)?.data
        return profile?.displayName?.ifBlank { "Cricket Player" } ?: "Cricket Player"
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun openRoom(roomCode: String) {
        lastBotActionKey = null
        _activeRoomCode.value = roomCode
    }

    fun spectateRoom(roomCode: String) {
        lastBotActionKey = null
        _activeRoomCode.value = roomCode
        viewModelScope.launch {
            repository.enterSpectatorMode(roomCode)
        }
    }

    fun closeRoomView() {
        val currentRoom = (activeRoomState.value as? OnlineUiState.Success)?.data
        if (currentRoom != null && currentUserId in currentRoom.spectatorUids) {
            val code = currentRoom.roomCode
            viewModelScope.launch {
                repository.leaveSpectatorMode(code)
            }
        }
        lastBotActionKey = null
        _activeRoomCode.value = null
    }

    // --- GROUPS, CHALLENGES & REACTIONS ACTIONS ---

    fun selectGroup(groupCode: String?) {
        _selectedGroupCode.value = groupCode
    }

    fun createGroup(nameInput: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createGroup(nameInput, currentDisplayName())
            _isBusy.value = false
            res.onSuccess { code ->
                _selectedGroupCode.value = code
                _feedbackMessage.value = "Group created! Share code $code."
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not create group."
            }
        }
    }

    fun joinGroup(codeInput: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinGroupByCode(codeInput, currentDisplayName())
            _isBusy.value = false
            res.onSuccess { code ->
                _selectedGroupCode.value = code
                _feedbackMessage.value = "Joined group $code!"
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not join group."
            }
        }
    }

    fun addFriendToGroup(group: CricketGroup, friendUid: String, friendName: String) {
        viewModelScope.launch {
            val res = repository.addFriendToGroup(group, friendUid, friendName)
            res.onSuccess { _feedbackMessage.value = "Added $friendName to ${group.name}!" }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to add friend." }
        }
    }

    fun leaveOrDeleteGroup(group: CricketGroup) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.leaveOrDeleteGroup(group)
            _isBusy.value = false
            res.onSuccess {
                if (_selectedGroupCode.value == group.groupCode) {
                    _selectedGroupCode.value = null
                }
                _feedbackMessage.value = if (group.ownerUid == currentUserId) {
                    "Deleted ${group.name}."
                } else {
                    "Left ${group.name}."
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not leave group."
            }
        }
    }

    fun challengeFriendDirect(friendUid: String, friendName: String, format: MatchFormat) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.challengeFriend1v1(currentDisplayName(), friendUid, format)
            _isBusy.value = false
            res.onSuccess { code ->
                _activeRoomCode.value = code
                _feedbackMessage.value = "1v1 Challenge sent to $friendName!"
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not send challenge."
            }
        }
    }

    fun challengeGroupMember(
        group: CricketGroup,
        targetUid: String,
        targetName: String,
        format: MatchFormat,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createGroupChallenge1v1(group, currentDisplayName(), targetUid, format)
            _isBusy.value = false
            res.onSuccess { code ->
                _activeRoomCode.value = code
                _feedbackMessage.value = "Challenged $targetName! Group members can spectate live."
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not start challenge."
            }
        }
    }

    fun challengeEveryoneInGroup(group: CricketGroup, format: MatchFormat) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createGroupMatchAll(group, currentDisplayName(), format)
            _isBusy.value = false
            res.onSuccess { code ->
                _activeRoomCode.value = code
                _feedbackMessage.value = "Group Team Match created! Invited everyone in ${group.name}."
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not start group match."
            }
        }
    }

    fun sendMatchReaction(reactionType: ReactionType) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendReaction(room, currentDisplayName(), reactionType)
        }
    }

    fun sendGroupChatMessage(group: CricketGroup, text: String) {
        viewModelScope.launch {
            repository.sendGroupMessage(group, currentDisplayName(), text).onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not send message."
            }
        }
    }

    fun deleteGroupChatMessage(groupCode: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteGroupMessage(groupCode, messageId).onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not delete message."
            }
        }
    }

    fun kickGroupMember(group: CricketGroup, targetUid: String, targetName: String) {
        viewModelScope.launch {
            repository.kickMemberFromGroup(group, targetUid)
                .onSuccess { _feedbackMessage.value = "Removed $targetName from ${group.name}." }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to remove member." }
        }
    }

    fun toggleMuteGroupMember(group: CricketGroup, targetUid: String, targetName: String) {
        viewModelScope.launch {
            repository.toggleMuteMemberInGroup(group, targetUid)
                .onSuccess { muted ->
                    _feedbackMessage.value = if (muted) {
                        "Muted $targetName in group chat."
                    } else {
                        "Unmuted $targetName in group chat."
                    }
                }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to update mute status." }
        }
    }

    fun updateGroupCaptainSettings(group: CricketGroup, newName: String, newAnnouncement: String) {
        viewModelScope.launch {
            repository.updateGroupCaptainSettings(group, newName, newAnnouncement)
                .onSuccess { _feedbackMessage.value = "Updated group settings!" }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to update group settings." }
        }
    }

    fun recordSoloMatchCompleted(
        formatId: String,
        outcome: String,
        runsScored: Int,
        wicketsLost: Int,
        ballsFaced: Int,
        fours: Int,
        sixes: Int,
        opponentName: String,
    ) {
        viewModelScope.launch {
            repository.recordMatchResultAndStats(
                mode = "SOLO",
                formatId = formatId,
                outcome = outcome,
                runsScored = runsScored,
                wicketsLost = wicketsLost,
                ballsFaced = ballsFaced,
                fours = fours,
                sixes = sixes,
                opponentName = opponentName,
            )
        }
    }

    // --- FRIENDS ACTIONS ---

    fun sendFriendRequest(codeInput: String) {
        val profile = (profileState.value as? OnlineUiState.Success)?.data
        if (profile == null) {
            _feedbackMessage.value = "Still loading your player profile…"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.sendFriendRequestByCode(profile, codeInput)
            _isBusy.value = false
            res.onSuccess { targetName ->
                _feedbackMessage.value = "Friend request sent to $targetName!"
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not send friend request."
            }
        }
    }

    fun acceptFriend(friendshipId: String) {
        viewModelScope.launch {
            val res = repository.acceptFriendRequest(friendshipId)
            res.onSuccess { _feedbackMessage.value = "Friend request accepted!" }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to accept request." }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            val res = repository.removeFriendship(friendshipId)
            res.onSuccess { _feedbackMessage.value = "Removed from friends." }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to remove friend." }
        }
    }

    fun inviteFriendToActiveRoom(friendUid: String, friendName: String) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.sendRoomInvite(
                myName = currentDisplayName(),
                friendUid = friendUid,
                roomCode = room.roomCode,
                roomMode = room.roomMode(),
                formatId = room.formatId,
            )
            res.onSuccess { _feedbackMessage.value = "Invited $friendName to room ${room.roomCode}!" }
                .onFailure { _feedbackMessage.value = it.message ?: "Failed to send invite." }
        }
    }

    fun acceptInviteAndJoin(invite: RoomInvite) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.respondToRoomInvite(invite.id, accepted = true)
            val joinRes = repository.joinRoomByCode(invite.roomCode, currentDisplayName())
            _isBusy.value = false
            joinRes.onSuccess { code ->
                _activeRoomCode.value = code
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Could not join room."
            }
        }
    }

    fun dismissInvite(inviteId: String) {
        viewModelScope.launch {
            repository.respondToRoomInvite(inviteId, accepted = false)
        }
    }

    // --- ROOM LIFECYCLE ACTIONS ---

    fun createRoom(mode: RoomMode, format: MatchFormat, openLobby: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createRoom(
                hostName = currentDisplayName(),
                mode = mode,
                format = format,
                openLobby = openLobby,
            )
            _isBusy.value = false
            res.onSuccess { code ->
                _activeRoomCode.value = code
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Failed to create room."
            }
        }
    }

    fun joinRoom(roomCodeInput: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinRoomByCode(roomCodeInput, currentDisplayName())
            _isBusy.value = false
            res.onSuccess { code ->
                _activeRoomCode.value = code
            }.onFailure { err ->
                _feedbackMessage.value = err.message ?: "Failed to join room."
            }
        }
    }

    fun leaveActiveWaitingRoom() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data
        if (room == null) {
            _activeRoomCode.value = null
            return
        }
        viewModelScope.launch {
            if (currentUserId in room.memberUids) {
                if (room.stage() == RoomStage.WAITING) {
                    repository.leaveWaitingRoom(room)
                }
            } else if (currentUserId in room.spectatorUids) {
                repository.leaveSpectatorMode(room.roomCode)
            }
            _activeRoomCode.value = null
        }
    }

    fun addBotPlayer() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.addBotToWaitingRoom(room).onFailure {
                _feedbackMessage.value = it.message ?: "Could not add bot."
            }
        }
    }

    fun removeBotPlayer() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.removeLastBotFromWaitingRoom(room).onFailure {
                _feedbackMessage.value = it.message ?: "Could not remove bot."
            }
        }
    }

    fun startMatch() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.startMatchFromLobby(room)
            _isBusy.value = false
            res.onFailure { _feedbackMessage.value = it.message ?: "Cannot start match." }
        }
    }

    fun proceedFromCaptainReveal() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.proceedFromCaptainReveal(room).onFailure {
                _feedbackMessage.value = it.message ?: "Could not proceed to toss."
            }
        }
    }

    fun submitTossCall(call: String) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.submitTossCall(room.roomCode, call).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to submit toss call."
            }
        }
    }

    fun submitTossNumber(number: Int) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.submitTossNumber(room.roomCode, currentUserId, number).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to submit toss throw."
            }
        }
    }

    fun advanceToTeamDraft() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.advanceToTeamDraft(room.roomCode).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to start draft."
            }
        }
    }

    fun draftPlayer(playerUid: String) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.draftPlayer(room.roomCode, currentUserId, playerUid).onFailure {
                _feedbackMessage.value = it.message ?: "Could not draft player."
            }
        }
    }

    fun chooseBatOrBowl(winnerBatsFirst: Boolean) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.chooseMatchBattingOrder(room, winnerBatsFirst).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to start innings."
            }
        }
    }

    fun submitDeliveryPick(number: Int) {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.submitDeliveryPick(room.roomCode, currentUserId, number).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to submit delivery."
            }
        }
    }

    fun startSecondInnings() {
        val room = (activeRoomState.value as? OnlineUiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.startSecondInnings(room).onFailure {
                _feedbackMessage.value = it.message ?: "Failed to start 2nd innings."
            }
        }
    }

    // --- HOST BOT AUTOMATION ---

    private fun maybeDriveBotAction(room: GameRoom) {
        val stage = room.stage()
        val key = "${room.roomCode}:${stage.wireValue}:${room.tossCall}:${room.tossPickA}:${room.tossPickB}:${room.tossWinnerUid}:${room.draftTurnUid}:${room.draftPoolUids.size}:${room.lastDeliverySeq}:${room.activeBatterUid}:${room.activeBowlerUid}"
        if (key == lastBotActionKey) return

        when (stage) {
            RoomStage.DRAFT_TOSS, RoomStage.MATCH_TOSS -> {
                if (room.tossCall.isBlank() && room.isBot(room.tossCallerUid)) {
                    lastBotActionKey = key
                    viewModelScope.launch {
                        delay(850L)
                        repository.submitTossCall(room.roomCode, if (Random.nextBoolean()) "ODD" else "EVEN")
                    }
                } else if (room.tossCall.isNotBlank() && room.tossWinnerUid.isBlank()) {
                    // If BOTH captains are bots, trigger throw for A (which auto-fills B too!)
                    if (room.isBot(room.captainAUid) && room.isBot(room.captainBUid)) {
                        lastBotActionKey = key
                        viewModelScope.launch {
                            delay(900L)
                            repository.submitTossNumber(room.roomCode, room.captainAUid, Random.nextInt(1, 7))
                        }
                    }
                } else if (room.tossWinnerUid.isNotBlank() && room.isBot(room.tossWinnerUid)) {
                    lastBotActionKey = key
                    viewModelScope.launch {
                        delay(1100L)
                        if (stage == RoomStage.DRAFT_TOSS) {
                            repository.advanceToTeamDraft(room.roomCode)
                        } else {
                            repository.chooseMatchBattingOrder(room, Random.nextBoolean())
                        }
                    }
                }
            }

            RoomStage.TEAM_DRAFT -> {
                if (room.draftTurnUid.isNotBlank() && room.isBot(room.draftTurnUid) && room.draftPoolUids.isNotEmpty()) {
                    lastBotActionKey = key
                    viewModelScope.launch {
                        delay(900L)
                        val pick = room.draftPoolUids.random()
                        repository.draftPlayer(room.roomCode, room.draftTurnUid, pick)
                    }
                }
            }

            RoomStage.PLAYING -> {
                // If both current batter and bowler are bots, play the ball automatically so spectators watch live!
                if (room.isBot(room.activeBatterUid) && room.isBot(room.activeBowlerUid)) {
                    lastBotActionKey = key
                    viewModelScope.launch {
                        delay(1000L)
                        repository.submitDeliveryPick(room.roomCode, room.activeBatterUid, Random.nextInt(1, 7))
                    }
                }
            }

            else -> Unit
        }
    }

    private fun recordStatsIfParticipant(room: GameRoom) {
        val inA = currentUserId in room.teamAUids
        val inB = currentUserId in room.teamBUids
        val isCommon = currentUserId == room.commonPlayerUid
        if (!inA && !inB && !isCommon) return

        val outcome = when {
            room.winnerTeam == "TIE" -> "TIE"
            room.winnerTeam == "A" && (inA || isCommon) -> "WIN"
            room.winnerTeam == "B" && (inB || isCommon) -> "WIN"
            else -> "LOSS"
        }
        val battedFirst = (inA && room.firstInningsTeam == "A") || (inB && room.firstInningsTeam == "B")
        val myTeamRuns = if (battedFirst) room.firstInningsRuns else room.runs
        val myTeamWickets = if (battedFirst) room.firstInningsWickets else room.wickets
        val myTeamBalls = if (battedFirst) room.firstInningsBalls else room.balls
        val oppRuns = if (battedFirst) room.runs else room.firstInningsRuns
        val oppWickets = if (battedFirst) room.wickets else room.firstInningsWickets
        val oppBalls = if (battedFirst) room.balls else room.firstInningsBalls
        val opponentLabel = if (room.roomMode() == RoomMode.DUEL_1V1) {
            val otherUid = if (inA) room.captainBUid else room.captainAUid
            room.nameOf(otherUid)
        } else {
            if (inA) "Team Mint" else "Team Gold"
        }
        val foursVal = if (!battedFirst) room.fours.toInt() else 0
        val sixesVal = if (!battedFirst) room.sixes.toInt() else 0

        onOnlineMatchCompletedListener?.invoke(
            OnlineCompletedSessionMatch(
                modeLabel = room.roomMode().label,
                opponentName = opponentLabel,
                outcome = outcome,
                runsScored = myTeamRuns.toInt(),
                ballsFaced = myTeamBalls.toInt(),
                wicketsTaken = oppWickets.toInt(),
                ballsBowled = oppBalls.toInt(),
                runsConceded = oppRuns.toInt(),
                fours = foursVal,
                sixes = sixesVal,
            )
        )

        viewModelScope.launch {
            repository.recordMatchResultAndStats(
                mode = room.mode,
                formatId = room.formatId,
                outcome = outcome,
                runsScored = myTeamRuns.toInt(),
                wicketsLost = myTeamWickets.toInt(),
                ballsFaced = myTeamBalls.toInt(),
                fours = foursVal,
                sixes = sixesVal,
                opponentName = opponentLabel,
            )
        }
    }

    private fun <T> unwrapFlowError(error: Throwable, fallback: String): OnlineUiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(ONLINE_VM_TAG, fallback, rootError)
        return OnlineUiState.Error(rootError.message ?: fallback)
    }
}
