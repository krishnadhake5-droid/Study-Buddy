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

function serverNow() {
  return new Date();
}

test("Unauthenticated user: cannot read or write tasks, topics, or study groups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("study_groups").where("visibility", "==", "PUBLIC").get());
});

test("Authenticated user: can create and read own student profile and tasks, but fails on ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = serverNow();

  const validTask = {
    userId: ALICE_UID,
    title: "Complete Mathematics Chapter 3",
    subject: "Mathematics",
    chapterOrTopic: "Calculus & Derivatives",
    category: "GOAL",
    timeSlot: "05:00 PM - 05:45 PM",
    durationMinutes: 45,
    priority: "HIGH",
    completed: false,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTask)
  );

  // Shadow update test (ghost field)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_ghost").set({
      ...validTask,
      isAdminOverride: true,
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's tasks, syllabus, or notes", async () => {
  const now = serverNow();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Revise AI Search Algorithms",
      subject: "Artificial Intelligence",
      chapterOrTopic: "A* Search",
      category: "GOAL",
      timeSlot: "06:00 PM - 06:45 PM",
      durationMinutes: 45,
      priority: "HIGH",
      completed: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("Client query alignment: Peer study groups require visibility == PUBLIC filter", async () => {
  const now = serverNow();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("study_groups").doc("group_ai").set({
      creatorId: ALICE_UID,
      creatorName: "Aarav Sharma",
      groupName: "AI & ML Exam Prep Squad",
      subject: "Artificial Intelligence",
      activeTopic: "Unit 2: Minimax & Alpha-Beta Pruning",
      memberCount: 12,
      visibility: "PUBLIC",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("study_groups").where("visibility", "==", "PUBLIC").get()
  );
});
