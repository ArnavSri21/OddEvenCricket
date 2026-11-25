const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_01";
const BOB_UID = "bob_uid_02";
const CHARLIE_UID = "charlie_uid_03";
const OUTSIDER_UID = "outsider_uid_04";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTime(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function buildValidGameRoom(roomCode, hostUid, hostName, overrides = {}) {
  const now = pastTime(3000);
  return {
    roomCode,
    hostUid,
    hostName,
    mode: "TEAM_PLAY",
    formatId: "t3_blitz",
    openLobby: true,
    status: "waiting",
    groupCode: "",
    groupMemberUids: [],
    challengedUid: "",
    spectatorUids: [],
    memberUids: [hostUid],
    rosterUids: [hostUid],
    playerNames: { [hostUid]: hostName },
    botUids: [],
    captainAUid: "",
    captainBUid: "",
    draftPoolUids: [],
    teamAUids: [],
    teamBUids: [],
    commonPlayerUid: "",
    tossCallerUid: "",
    tossCall: "",
    tossPickA: 0,
    tossPickB: 0,
    tossWinnerUid: "",
    draftTurnUid: "",
    lastTossSummary: "",
    inningsNumber: 1,
    battingTeam: "",
    battingLineup: [],
    bowlingLineup: [],
    activeBatterUid: "",
    activeBowlerUid: "",
    batterPick: 0,
    bowlerPick: 0,
    runs: 0,
    wickets: 0,
    balls: 0,
    fours: 0,
    sixes: 0,
    maxWickets: 3,
    maxBalls: 18,
    firstInningsRuns: 0,
    firstInningsWickets: 0,
    firstInningsBalls: 0,
    firstInningsTeam: "",
    targetRuns: 0,
    recentBalls: [],
    lastBallSummary: "",
    lastBatterPick: 0,
    lastBowlerPick: 0,
    lastDeliverySeq: 0,
    winnerTeam: "",
    resultHeadline: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("player_profiles").doc(ALICE_UID).get());
  await assertFails(unauth.collection("friend_codes").doc("CRK123").get());
  await assertFails(
    unauth.collection("friendships").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauth.collection("cricket_groups").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauth.collection("game_rooms").where("openLobby", "==", true).where("status", "==", "waiting").get()
  );
  await assertFails(
    unauth.collection("game_rooms").doc("ROOM01").set(buildValidGameRoom("ROOM01", ALICE_UID, "Alice"))
  );
});

test("Player profile and friend code lookup CRUD with strict ownership and no extra fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(5000);

  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Striker",
      friendCode: "ALC123",
      matchesPlayed: 0,
      matchesWon: 0,
      bestScore: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("friend_codes").doc("ALC123").set({
      userId: ALICE_UID,
      displayName: "Alice Striker",
      friendCode: "ALC123",
      createdAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("friend_codes").doc("ALC123").get());
  await assertFails(bobDb.collection("friend_codes").get());
  await assertFails(
    bobDb.collection("player_profiles").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastTime(1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      matchesPlayed: 1,
      matchesWon: 1,
      bestScore: 42,
      updatedAt: pastTime(1000),
    })
  );

  await assertFails(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      email: "alice@example.com",
      updatedAt: pastTime(500),
    })
  );

  await assertFails(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      bestScore: 99,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Friendships: request, recipient accept, cross-user isolation, and scoped list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime(5000);
  const fId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("friendships").doc(fId).set({
      memberUids: [ALICE_UID, BOB_UID],
      requesterUid: ALICE_UID,
      recipientUid: BOB_UID,
      requesterName: "Alice",
      recipientName: "Bob",
      requesterCode: "ALC123",
      recipientCode: "BOB456",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: pastTime(1000),
    })
  );

  await assertFails(charlieDb.collection("friendships").doc(fId).get());
  await assertFails(charlieDb.collection("friendships").get());

  await assertSucceeds(
    bobDb.collection("friendships").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("friendships").doc(fId).update({
      status: "accepted",
      updatedAt: pastTime(1000),
    })
  );

  await assertFails(
    bobDb.collection("friendships").doc(fId).update({
      status: "pending",
      updatedAt: pastTime(500),
    })
  );
});

test("Room invites: send, scoped read, recipient accept/dismiss, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTime(4000);

  await assertSucceeds(
    aliceDb.collection("room_invites").doc("inv_01").set({
      fromUid: ALICE_UID,
      fromName: "Alice",
      toUid: BOB_UID,
      roomCode: "TEAM99",
      roomMode: "TEAM_PLAY",
      formatId: "t3_blitz",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("room_invites").where("toUid", "==", BOB_UID).get());
  await assertFails(charlieDb.collection("room_invites").doc("inv_01").get());
  await assertSucceeds(
    bobDb.collection("room_invites").doc("inv_01").update({
      status: "accepted",
      updatedAt: pastTime(1000),
    })
  );
});

test("Game rooms: create, join, team draft + common player progression, scoped queries, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const preCreateSnap = await assertSucceeds(
    aliceDb.collection("game_rooms").doc("TEAM99").get()
  );
  assert.equal(preCreateSnap.exists, false);

  await assertSucceeds(
    aliceDb
      .collection("game_rooms")
      .doc("TEAM99")
      .set(buildValidGameRoom("TEAM99", ALICE_UID, "Alice"))
  );

  await assertSucceeds(
    bobDb
      .collection("game_rooms")
      .where("openLobby", "==", true)
      .where("status", "==", "waiting")
      .get()
  );
  await assertSucceeds(
    aliceDb.collection("game_rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("game_rooms").get());

  await assertSucceeds(
    bobDb.collection("game_rooms").doc("TEAM99").update({
      memberUids: [ALICE_UID, BOB_UID],
      rosterUids: [ALICE_UID, BOB_UID],
      playerNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: pastTime(2000),
    })
  );

  await assertSucceeds(
    charlieDb.collection("game_rooms").doc("TEAM99").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      rosterUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      playerNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob", [CHARLIE_UID]: "Charlie" },
      updatedAt: pastTime(1500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("TEAM99").update({
      status: "match_toss",
      captainAUid: ALICE_UID,
      captainBUid: BOB_UID,
      teamAUids: [ALICE_UID],
      teamBUids: [BOB_UID],
      draftPoolUids: [],
      commonPlayerUid: CHARLIE_UID,
      tossCallerUid: BOB_UID,
      updatedAt: pastTime(1000),
    })
  );

  await assertFails(
    aliceDb.collection("game_rooms").doc("TEAM99").update({
      adminOverride: true,
      updatedAt: pastTime(900),
    })
  );

  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(
    outsiderDb.collection("game_rooms").doc("TEAM99").update({
      runs: 999,
      updatedAt: pastTime(850),
    })
  );

  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("TEAM99").update({
      status: "finished",
      winnerTeam: "A",
      resultHeadline: "Team Gold won by 8 runs",
      updatedAt: pastTime(800),
    })
  );

  await assertFails(
    aliceDb.collection("game_rooms").doc("TEAM99").update({
      winnerTeam: "B",
      updatedAt: pastTime(500),
    })
  );
});

test("Cricket groups: create, join by code, scoped list, group challenges, live spectating, and match reactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTime(5000);

  // Alice creates a cricket group
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("GRP777").set({
      groupCode: "GRP777",
      name: "Gully Strikers",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberUids: [ALICE_UID],
      memberNames: { [ALICE_UID]: "Alice" },
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered group list must fail
  await assertFails(bobDb.collection("cricket_groups").get());

  // Bob and Charlie join group by code
  await assertSucceeds(
    bobDb.collection("cricket_groups").doc("GRP777").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: pastTime(4000),
    })
  );
  await assertSucceeds(
    charlieDb.collection("cricket_groups").doc("GRP777").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob", [CHARLIE_UID]: "Charlie" },
      updatedAt: pastTime(3500),
    })
  );

  // Non-owner cannot rename the group
  await assertFails(
    bobDb.collection("cricket_groups").doc("GRP777").update({
      name: "Hijacked Group",
      updatedAt: pastTime(3000),
    })
  );

  // Scoped group list succeeds for member
  await assertSucceeds(
    charlieDb.collection("cricket_groups").where("memberUids", "array-contains", CHARLIE_UID).get()
  );

  // Alice challenges Bob to a 1v1 inside GRP777 (private to group)
  const groupMembers = [ALICE_UID, BOB_UID, CHARLIE_UID];
  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("DUEL88").set(
      buildValidGameRoom("DUEL88", ALICE_UID, "Alice", {
        mode: "DUEL_1V1",
        openLobby: false,
        groupCode: "GRP777",
        groupMemberUids: groupMembers,
        challengedUid: BOB_UID,
      })
    )
  );

  // Charlie cannot hijack Bob's 1v1 challenge slot as a player
  await assertFails(
    charlieDb.collection("game_rooms").doc("DUEL88").update({
      memberUids: [ALICE_UID, CHARLIE_UID],
      rosterUids: [ALICE_UID, CHARLIE_UID],
      playerNames: { [ALICE_UID]: "Alice", [CHARLIE_UID]: "Charlie" },
      updatedAt: pastTime(2200),
    })
  );

  // Bob accepts challenge and joins
  await assertSucceeds(
    bobDb.collection("game_rooms").doc("DUEL88").update({
      memberUids: [ALICE_UID, BOB_UID],
      rosterUids: [ALICE_UID, BOB_UID],
      playerNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: pastTime(2000),
    })
  );

  // Alice starts the match (playing)
  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("DUEL88").update({
      status: "playing",
      captainAUid: ALICE_UID,
      captainBUid: BOB_UID,
      teamAUids: [ALICE_UID],
      teamBUids: [BOB_UID],
      battingTeam: "A",
      activeBatterUid: ALICE_UID,
      activeBowlerUid: BOB_UID,
      updatedAt: pastTime(1800),
    })
  );

  // Charlie (same group) lists group rooms & joins as a live spectator!
  await assertSucceeds(
    charlieDb.collection("game_rooms").where("groupMemberUids", "array-contains", CHARLIE_UID).get()
  );
  await assertSucceeds(
    charlieDb.collection("game_rooms").doc("DUEL88").update({
      spectatorUids: [CHARLIE_UID],
      updatedAt: pastTime(1600),
    })
  );

  // Outsider (not in group) cannot read private group match or spectate it
  await assertFails(outsiderDb.collection("game_rooms").doc("DUEL88").get());
  await assertFails(
    outsiderDb.collection("game_rooms").doc("DUEL88").update({
      spectatorUids: [CHARLIE_UID, OUTSIDER_UID],
      updatedAt: pastTime(1500),
    })
  );

  // Live in-match reactions: Alice (player) and Charlie (group spectator) send reactions; outsider blocked
  await assertSucceeds(
    aliceDb.collection("game_rooms").doc("DUEL88").collection("reactions").doc("rx_01").set({
      roomCode: "DUEL88",
      senderUid: ALICE_UID,
      senderName: "Alice",
      reactionType: "FIRE",
      allowedUids: groupMembers,
      createdAt: pastTime(1200),
    })
  );
  await assertSucceeds(
    charlieDb.collection("game_rooms").doc("DUEL88").collection("reactions").doc("rx_02").set({
      roomCode: "DUEL88",
      senderUid: CHARLIE_UID,
      senderName: "Charlie",
      reactionType: "CLAP",
      allowedUids: groupMembers,
      createdAt: pastTime(1100),
    })
  );

  // Scoped reaction list works for Bob; outsider blocked
  await assertSucceeds(
    bobDb
      .collection("game_rooms")
      .doc("DUEL88")
      .collection("reactions")
      .where("allowedUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    outsiderDb
      .collection("game_rooms")
      .doc("DUEL88")
      .collection("reactions")
      .doc("rx_03")
      .set({
        roomCode: "DUEL88",
        senderUid: OUTSIDER_UID,
        senderName: "Outsider",
        reactionType: "FIRE",
        allowedUids: [OUTSIDER_UID],
        createdAt: pastTime(900),
      })
  );
});

test("Group chat and Captain Admin powers: messaging, announcement, mute enforcement, admin delete, and kick", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTime(5000);

  // Alice (Captain/Admin) creates group and Bob joins
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("CHT101").set({
      groupCode: "CHT101",
      name: "Night Watchmen",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      memberUids: [ALICE_UID],
      memberNames: { [ALICE_UID]: "Alice" },
      mutedUids: [],
      announcement: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("cricket_groups").doc("CHT101").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      updatedAt: pastTime(4000),
    })
  );

  // Captain pins an announcement; non-captain blocked
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("CHT101").update({
      announcement: "Match at 8 PM sharp!",
      updatedAt: pastTime(3500),
    })
  );
  await assertFails(
    bobDb.collection("cricket_groups").doc("CHT101").update({
      announcement: "Fake announcement",
      updatedAt: pastTime(3400),
    })
  );

  // Alice sends a captain message; Bob sends a member message (Bob cannot forge isCaptain=true)
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_1").set({
      groupCode: "CHT101",
      senderUid: ALICE_UID,
      senderName: "Alice",
      text: "Welcome to the squad!",
      isCaptain: true,
      createdAt: pastTime(3000),
    })
  );
  await assertFails(
    bobDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_forged").set({
      groupCode: "CHT101",
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "I am the captain now",
      isCaptain: true,
      createdAt: pastTime(2900),
    })
  );
  await assertSucceeds(
    bobDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_2").set({
      groupCode: "CHT101",
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "Ready to bat!",
      isCaptain: false,
      createdAt: pastTime(2800),
    })
  );

  // Member reads chat; outsider cannot read or write chat
  await assertSucceeds(bobDb.collection("cricket_groups").doc("CHT101").collection("messages").get());
  await assertFails(outsiderDb.collection("cricket_groups").doc("CHT101").collection("messages").get());

  // Captain Admin deletes Bob's message; Bob cannot delete Captain's message
  await assertFails(bobDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_1").delete());
  await assertSucceeds(aliceDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_2").delete());

  // Captain Admin mutes Bob -> Bob blocked from sending messages
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("CHT101").update({
      mutedUids: [BOB_UID],
      updatedAt: pastTime(2000),
    })
  );
  await assertFails(
    bobDb.collection("cricket_groups").doc("CHT101").collection("messages").doc("msg_3").set({
      groupCode: "CHT101",
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "Am I muted?",
      isCaptain: false,
      createdAt: pastTime(1500),
    })
  );

  // Captain Admin kicks Bob -> Bob loses read access to group messages
  await assertSucceeds(
    aliceDb.collection("cricket_groups").doc("CHT101").update({
      memberUids: [ALICE_UID],
      memberNames: { [ALICE_UID]: "Alice" },
      mutedUids: [],
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(bobDb.collection("cricket_groups").doc("CHT101").collection("messages").get());
});

test("Player match_history: owner-only create/read, immutable records, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(3000);

  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).collection("match_history").doc("mh_01").set({
      userId: ALICE_UID,
      mode: "SOLO",
      formatId: "t3_blitz",
      outcome: "WIN",
      runsScored: 38,
      wicketsLost: 1,
      ballsFaced: 12,
      fours: 3,
      sixes: 2,
      opponentName: "Pro AI",
      createdAt: ts,
    })
  );

  // Owner can list own history; Bob cannot read or write Alice's history
  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).collection("match_history").get()
  );
  await assertFails(
    bobDb.collection("player_profiles").doc(ALICE_UID).collection("match_history").get()
  );
  await assertFails(
    bobDb.collection("player_profiles").doc(ALICE_UID).collection("match_history").doc("mh_02").set({
      userId: ALICE_UID,
      mode: "SOLO",
      formatId: "t3_blitz",
      outcome: "LOSS",
      runsScored: 0,
      wicketsLost: 3,
      ballsFaced: 3,
      fours: 0,
      sixes: 0,
      opponentName: "Hacker",
      createdAt: pastTime(1000),
    })
  );

  // History records are immutable
  await assertFails(
    aliceDb.collection("player_profiles").doc(ALICE_UID).collection("match_history").doc("mh_01").update({
      runsScored: 999,
    })
  );
});

