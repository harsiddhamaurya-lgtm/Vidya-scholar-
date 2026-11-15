const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
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

test("Unauthenticated user: cannot read users, vidyaIds, chats, tickets, or onlineMatches", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("vidyaIds").doc("aarav_4821").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.collection("matchmakingTickets").get());
  await assertFails(unauthDb.collection("onlineMatches").get());
});

test("Authenticated user: can create and update own user profile with publicKey, cannot read/write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Aarav Scholar",
      email: "aarav@example.com",
      vidyaId: "aarav#4821",
      vidyaHandle: "aarav",
      vidyaDiscriminator: "4821",
      vidyaKey: "aarav_4821",
      contactCode: "aarav#4821",
      avatarMode: "PRESET",
      avatarPresetId: "vedic_strategist",
      focusRealm: "MATHEMATICS",
      onboardingCompleted: true,
      streak: 1,
      xp: 100,
      level: "Bronze Scholar",
      hearts: 5,
      completedLessons: [],
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtest",
      keyVersion: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ name: "Hijacked", updatedAt: pastTimestamp() }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      avatarMode: "PHOTO",
      localAvatarPhotoPath: "/data/user/0/com.aistudio.edupulse.vbwl/files/avatars/avatar_alice.jpg",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Vidya ID registry: atomic claim with publicKey, cross-user hijack blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("vidyaIds").doc("aarav_4821").set({
      uid: ALICE_UID,
      vidyaId: "aarav#4821",
      vidyaHandle: "aarav",
      vidyaDiscriminator: "4821",
      displayName: "Aarav Scholar",
      level: "Bronze Scholar",
      avatarMode: "BUILDER",
      avatarPresetId: "vedic_strategist",
      avatarCrestId: "yantra",
      avatarFrameId: "astrolabe",
      avatarToneId: "saffron",
      focusRealm: "PHYSICS",
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtest",
      keyVersion: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("vidyaIds").doc("aarav_4821").get());
  await assertFails(bobDb.collection("vidyaIds").get());
  await assertSucceeds(aliceDb.collection("vidyaIds").where("uid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("vidyaIds").doc("aarav_4821").delete());
});

test("Varta E2EE chats and encrypted messages: wrappedKeys, ciphertext validation, outsider blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_e2ee_1").set({
      createdBy: ALICE_UID,
      participants: [ALICE_UID, BOB_UID],
      name: "Bob Scholar",
      peerVidyaId: "bob#9999",
      lastMessage: "BASE64CIPHERTEXTPREVIEW==",
      lastMessageIv: "AAAAAAAAAAAAAAAA",
      lastTimestamp: 1700000000000,
      type: "individual",
      subjectTag: "Physics",
      unreadCount: 0,
      isOnline: true,
      timestampLabel: "Just now",
      wrappedKeys: {
        [ALICE_UID]: "WRAPPEDKEYALICE==",
        [BOB_UID]: "WRAPPEDKEYBOB==",
      },
      keyVersion: 1,
      isEncrypted: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(bobDb.collection("chats").where("participants", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("chats").doc("chat_e2ee_1").get());
  await assertFails(bobDb.collection("chats").get());

  // Encrypted message missing ciphertext/iv must fail
  await assertFails(
    aliceDb.collection("chats").doc("chat_e2ee_1").collection("messages").doc("msg_bad").set({
      senderId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      text: "[Encrypted]",
      isEncrypted: true,
      timestamp: 1700000001000,
      createdAt: pastTimestamp(),
    })
  );

  // Valid encrypted message succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_e2ee_1").collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "Aarav Scholar",
      senderVidyaId: "aarav#4821",
      memberUids: [ALICE_UID, BOB_UID],
      text: "[Encrypted]",
      ciphertext: "AESGCMCIPHERTEXT==",
      iv: "AAAAAAAAAAAAAAAA",
      keyVersion: 1,
      isEncrypted: true,
      timestamp: 1700000001000,
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc("chat_e2ee_1").collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb.collection("chats").doc("chat_e2ee_1").collection("messages")
      .where("memberUids", "array-contains", CHARLIE_UID)
      .get()
  );
});

test("Matchmaking tickets & onlineMatches: atomic claim, strict turn enforcement, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastTimestamp();

  // Alice queues a matchmaking ticket
  await assertSucceeds(
    aliceDb.collection("matchmakingTickets").doc("ticket_alice").set({
      uid: ALICE_UID,
      vidyaId: "aarav#4821",
      displayName: "Aarav Scholar",
      gameType: "SHATRANJ",
      eloRating: 1200,
      requiredPlayers: 2,
      publicKey: "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAalice",
      status: "searching",
      matchId: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob queries open searching tickets
  await assertSucceeds(
    bobDb.collection("matchmakingTickets")
      .where("status", "==", "searching")
      .where("gameType", "==", "SHATRANJ")
      .get()
  );

  // Updating ticket to 'matched' WITHOUT creating the onlineMatch in the same batch MUST fail
  await assertFails(
    bobDb.collection("matchmakingTickets").doc("ticket_alice").update({
      status: "matched",
      matchId: "match_1",
      updatedAt: pastTimestamp(),
    })
  );

  // Atomic batch: Bob creates onlineMatches/match_1 AND marks ticket_alice as matched
  const batch = bobDb.batch();
  const matchRef = bobDb.collection("onlineMatches").doc("match_1");
  const ticketRef = bobDb.collection("matchmakingTickets").doc("ticket_alice");
  const matchTs = pastTimestamp();
  batch.set(matchRef, {
    createdBy: BOB_UID,
    participantUids: [ALICE_UID, BOB_UID],
    gameType: "SHATRANJ",
    status: "active",
    currentTurnUid: ALICE_UID,
    moveSeq: 0,
    wrappedKeys: {
      [ALICE_UID]: "WRAPPEDMATCHALICE==",
      [BOB_UID]: "WRAPPEDMATCHBOB==",
    },
    keyVersion: 1,
    encryptedState: "INITIALENCRYPTEDBOARD==",
    stateIv: "AAAAAAAAAAAAAAAA",
    stateDigest: "0123456789abcdef0123456789abcdef",
    winnerUid: "",
    createdAt: matchTs,
    updatedAt: matchTs,
  });
  batch.update(ticketRef, {
    status: "matched",
    matchId: "match_1",
    updatedAt: matchTs,
  });
  await assertSucceeds(batch.commit());

  // Outsider Charlie cannot read the match
  await assertFails(charlieDb.collection("onlineMatches").doc("match_1").get());

  // Bob cannot submit move when it is Alice's turn
  await assertFails(
    bobDb.collection("onlineMatches").doc("match_1").update({
      currentTurnUid: ALICE_UID,
      moveSeq: 1,
      encryptedState: "SPOOFEDMOVE==",
      stateIv: "BBBBBBBBBBBBBBBB",
      stateDigest: "deadbeef",
      status: "active",
      winnerUid: "",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice cannot skip moveSeq (must be existing.moveSeq + 1)
  await assertFails(
    aliceDb.collection("onlineMatches").doc("match_1").update({
      currentTurnUid: BOB_UID,
      moveSeq: 2,
      encryptedState: "SKIPPEDSEQ==",
      stateIv: "BBBBBBBBBBBBBBBB",
      stateDigest: "deadbeef",
      status: "active",
      winnerUid: "",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice submits valid turn 1 and completes match
  await assertSucceeds(
    aliceDb.collection("onlineMatches").doc("match_1").update({
      currentTurnUid: BOB_UID,
      moveSeq: 1,
      encryptedState: "VALIDMOVE1==",
      stateIv: "BBBBBBBBBBBBBBBB",
      stateDigest: "abcdef1234567890",
      status: "completed",
      winnerUid: ALICE_UID,
      updatedAt: pastTimestamp(),
    })
  );

  // Terminal lock: neither player can modify completed match
  await assertFails(
    bobDb.collection("onlineMatches").doc("match_1").update({
      currentTurnUid: ALICE_UID,
      moveSeq: 2,
      encryptedState: "TAMPERED==",
      stateIv: "CCCCCCCCCCCCCCCC",
      stateDigest: "tamper",
      status: "completed",
      winnerUid: BOB_UID,
      updatedAt: pastTimestamp(),
    })
  );
});
