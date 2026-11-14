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

test("Unauthenticated user: cannot read users or vidyaIds or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("vidyaIds").doc("aarav_4821").get());
  await assertFails(unauthDb.collection("chats").get());
});

test("Authenticated user: can create and update own user profile, cannot read/write another user's profile", async () => {
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
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user PII read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ name: "Hijacked", updatedAt: pastTimestamp() }));

  // Ghost field / shadow update rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      avatarMode: "PHOTO",
      localAvatarPhotoPath: "/data/user/0/com.aistudio.edupulse.vbwl/files/avatars/avatar_alice.jpg",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Vidya ID registry: atomic claim, public non-PII get lookup, cross-user hijack blocked, scoped list", async () => {
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
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Exact lookup by signed-in peer succeeds
  await assertSucceeds(bobDb.collection("vidyaIds").doc("aarav_4821").get());

  // Unscoped list fails; uid-scoped list succeeds
  await assertFails(bobDb.collection("vidyaIds").get());
  await assertSucceeds(aliceDb.collection("vidyaIds").where("uid", "==", ALICE_UID).get());

  // Bob cannot overwrite or delete Alice's Vidya ID
  await assertFails(
    bobDb.collection("vidyaIds").doc("aarav_4821").set({
      uid: BOB_UID,
      vidyaId: "aarav#4821",
      vidyaHandle: "aarav",
      vidyaDiscriminator: "4821",
      displayName: "Bob",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("vidyaIds").doc("aarav_4821").delete());

  // PII field injection rejected on vidyaIds
  await assertFails(
    bobDb.collection("vidyaIds").doc("bob_9999").set({
      uid: BOB_UID,
      vidyaId: "bob#9999",
      vidyaHandle: "bob",
      vidyaDiscriminator: "9999",
      displayName: "Bob",
      email: "bob@secret.com",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Chats and messages: participant-scoped queries and creates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").set({
      createdBy: ALICE_UID,
      participants: [ALICE_UID, BOB_UID],
      name: "Bob Scholar",
      peerVidyaId: "bob#9999",
      lastMessage: "Namaste!",
      lastTimestamp: 1700000000000,
      type: "individual",
      subjectTag: "Physics",
      unreadCount: 0,
      isOnline: true,
      timestampLabel: "Just now",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Scoped participant query succeeds
  await assertSucceeds(
    bobDb.collection("chats").where("participants", "array-contains", BOB_UID).get()
  );

  // Unscoped query rejected
  await assertFails(bobDb.collection("chats").get());

  // Message write & read with memberUids succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "Aarav Scholar",
      senderVidyaId: "aarav#4821",
      memberUids: [ALICE_UID, BOB_UID],
      text: "Ready for Shatranj?",
      timestamp: 1700000001000,
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc("chat_1").collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
});
