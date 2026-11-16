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

function pastDate() {
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

test("Unauthenticated user: denied across profile and all subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("study_plans").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("exams").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("flashcards").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("pdf_sources").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chat_sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("daily_tasks").get());
});

test("Student profile: owner can create and update, cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ayşe Yılmaz",
      email: "ayse@example.com",
      gradeLevel: "11. Sınıf",
      targetGoal: "YKS Sayısal",
      dailyGoalMinutes: 60,
      xp: 0,
      level: 1,
      streakDays: 1,
      totalStudyMinutes: 0,
      questionsSolved: 0,
      darkThemeMode: "system",
      preferredMode: "Normal Öğretmen",
      badges: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot read Alice's profile (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 100,
      isAdmin: true,
      updatedAt: pastDate(),
    })
  );

  // Valid partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 120,
      level: 2,
      updatedAt: pastDate(),
    })
  );
});

test("Chat sessions and messages: parent gate and immutable messages enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chat_sessions").doc("sess_1").set({
      userId: ALICE_UID,
      title: "Türev Konu Anlatımı",
      subject: "Matematik",
      teachingMode: "Normal Öğretmen",
      explanationDepth: "normal",
      sourcePdfId: "",
      sourcePdfTitle: "",
      sourcePage: 0,
      messageCount: 0,
      lastMessagePreview: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Message under existing session succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chat_sessions")
      .doc("sess_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        role: "user",
        content: "Türevin geometrik anlamını açıklar mısın?",
        teachingMode: "Normal Öğretmen",
        hasAttachment: false,
        attachmentLabel: "",
        sourceCitation: "Genel Bilgi",
        createdAt: ts,
      })
  );

  // Message under non-existent session fails Parent Gate
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chat_sessions")
      .doc("missing_sess")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        role: "user",
        content: "Deneme",
        createdAt: ts,
      })
  );

  // Messages are immutable (update denied)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chat_sessions")
      .doc("sess_1")
      .collection("messages")
      .doc("msg_1")
      .update({ content: "Değiştirildi" })
  );

  // Bob cannot list Alice's sessions
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chat_sessions").get());
});

test("Subcollections (pdf_sources, exams, study_plans, notes, flashcards, daily_tasks): owner CRUD & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pdf_sources").doc("pdf_1").set({
      userId: ALICE_UID,
      title: "11. Sınıf Fizik Fasikülü",
      subject: "Fizik",
      localUriString: "/data/user/0/app/files/pdfs/fizik.pdf",
      pageCount: 24,
      lastStudiedPage: 1,
      summary: "Vektörler ve bağıl hareket özeti",
      topics: ["Vektörler", "Bağıl Hareket"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("exams").doc("exam_1").set({
      userId: ALICE_UID,
      title: "Vektörler Mini Sınavı",
      subject: "Fizik",
      topic: "Vektörler",
      difficulty: "Orta",
      questionCount: 5,
      correctCount: 4,
      scorePercent: 80,
      durationSeconds: 180,
      sourceType: "Konu",
      weakTopics: ["Bileşke Vektör"],
      errorCategories: ["İşlem Hatası"],
      aiReport: "İşlem basamaklarına dikkat edilmeli.",
      wrongQuestionsSummary: "Soru 3: Kosinüs teoremi işareti.",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("study_plans").doc("plan_1").set({
      userId: ALICE_UID,
      subject: "Matematik",
      topic: "Fonksiyonlar Tekrar",
      dayLabel: "Pazartesi",
      durationMinutes: 45,
      priority: "Yüksek",
      planType: "Tekrar",
      isCompleted: false,
      postponedCount: 0,
      examDateText: "2026-11-28",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Newton Yasaları Özeti",
      subject: "Fizik",
      content: "F = m * a temel prensiptir.",
      formulas: ["F = m · a"],
      definitions: ["Eylemsizlik: Cismin hareket durumunu koruma eğilimi"],
      keyConcepts: ["Kuvvet", "İvme"],
      sourcePdfTitle: "11. Sınıf Fizik Fasikülü",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("flashcards").doc("card_1").set({
      userId: ALICE_UID,
      subject: "Fizik",
      cardType: "Formül",
      front: "Newton'un 2. Yasası formülü nedir?",
      back: "F_net = m · a",
      masteryStatus: "new",
      reviewCount: 0,
      nextReviewAtMs: Date.now(),
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "5 Bilgi Kartı Tekrar Et",
      description: "Aralıklı tekrar destenden 5 kart çöz.",
      xpReward: 40,
      dateKey: "2026-11-15",
      isCompleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("exams").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("flashcards").doc("card_1").delete());
});
