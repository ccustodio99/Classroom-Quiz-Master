import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import PDFDocument = require("pdfkit");

admin.initializeApp();

const db = admin.firestore();
const getBucket = () => admin.storage().bucket();

export const computePoints = (correct: boolean, timeLimitMs: number, timeTakenMs: number): number => {
  if (!correct) {
    return 0;
  }
  const limit = Math.max(timeLimitMs, 1000);
  const elapsed = Math.max(timeTakenMs, 0);
  const timeLeft = Math.max(limit - elapsed, 0);
  return Math.min(Math.ceil(400 + 600 * (timeLeft / limit)), 1000);
};

export const computeAccuracy = (correctCount: number, total: number): number => {
  if (total <= 0) {
    return 0;
  }
  return Math.round((correctCount / total) * 100);
};

export const scoreAttempt = functions.firestore
  .document("assignments/{assignmentId}/attempts/{attemptId}")
  .onCreate(async (snap: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
    const attempt = snap.data();
    if (!attempt) {
      return;
    }
    const assignmentId = context.params.assignmentId as string;
    const attemptRef = snap.ref;
    const uid: string = attempt.uid;
    const questionId: string = attempt.questionId;
    const submittedAt: admin.firestore.Timestamp = attempt.createdAt ?? admin.firestore.Timestamp.now();

    await db.runTransaction(async (txn) => {
      const freshAttempt = await txn.get(attemptRef);
      if (freshAttempt.exists && freshAttempt.get("scoredAt")) {
        return;
      }

      const assignmentRef = db.collection("assignments").doc(assignmentId);
      const assignmentSnap = await txn.get(assignmentRef);
      if (!assignmentSnap.exists) {
        functions.logger.error("Missing assignment", { assignmentId });
        throw new Error("assignment-missing");
      }

      const quizId: string = assignmentSnap.get("quizId");
      const quizSnap = await txn.get(db.collection("quizzes").doc(quizId));
      if (!quizSnap.exists) {
        functions.logger.error("Missing quiz", { quizId });
        throw new Error("quiz-missing");
      }

      const questionsJson = quizSnap.get("questionsJson") ?? "[]";
      const questions: Array<any> = JSON.parse(questionsJson);
      const question = questions.find((q) => q.id === questionId);
      if (!question) {
        functions.logger.error("Missing question", { questionId });
        throw new Error("question-missing");
      }

      const answerKey: string[] = (question.answerKey ?? []).map((v: string) => v.toLowerCase()).sort();
      const selected: string[] = (attempt.selected ?? []).map((v: string) => v.toLowerCase()).sort();
      const correct = JSON.stringify(answerKey) === JSON.stringify(selected);
      const timeLimitSeconds: number = question.timeLimitSeconds ?? assignmentSnap.get("defaultTimePerQ") ?? 30;
      const timeLimitMs = Math.max(timeLimitSeconds * 1000, 1000);
      const timeTakenMs: number = attempt.timeMs ?? timeLimitMs;
      const timeLeft = Math.max(timeLimitMs - timeTakenMs, 0);
      const closeAt: number = assignmentSnap.get("closeAt");
      const late = typeof closeAt === "number" && submittedAt.toMillis() > closeAt;

      const points = computePoints(correct, timeLimitMs, timeTakenMs);

      const submissionRef = assignmentRef.collection("submissions").doc(uid);
      const submissionSnap = await txn.get(submissionRef);
      const currentAttempts = submissionSnap.get("attempts") ?? 0;
      const attemptsAllowed = assignmentSnap.get("attemptsAllowed");
      if (typeof attemptsAllowed === "number" && attemptsAllowed > 0 && currentAttempts >= attemptsAllowed) {
        txn.update(attemptRef, {
          points: 0,
          correct: false,
          late,
          rejected: true,
          scoredAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return;
      }

      txn.update(attemptRef, {
        points,
        correct,
        late,
        scoredAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      const attempts = currentAttempts + 1;
      const bestScore = Math.max(submissionSnap.get("bestScore") ?? 0, points);
      txn.set(
        submissionRef,
        {
          attempts,
          lastScore: points,
          bestScore,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    });
  });

export const exportReport = functions.https.onCall(async (data: { sessionId?: string }, context: functions.https.CallableContext) => {
  if (!context.auth || context.auth.token.firebase?.sign_in_provider === "anonymous") {
    throw new functions.https.HttpsError("permission-denied", "Teacher authentication required");
  }

  const { sessionId } = data;
  if (!sessionId) {
    throw new functions.https.HttpsError("invalid-argument", "sessionId required");
  }

  const attemptsSnap = await db.collection("sessions").doc(sessionId).collection("attempts").get();
  const header = ["attemptId", "uid", "questionId", "selected", "points", "timeMs", "late", "createdAt"];
  const rows = attemptsSnap.docs.map((doc) => {
    const attempt = doc.data();
    return [
      doc.id,
      attempt.uid ?? "",
      attempt.questionId ?? "",
      JSON.stringify(attempt.selected ?? []),
      attempt.points ?? 0,
      attempt.timeMs ?? 0,
      attempt.late ?? false,
      (attempt.createdAt && attempt.createdAt.toMillis) ? attempt.createdAt.toMillis() : attempt.createdAt ?? ""
    ].join(",");
  });
  const csv = [header.join(","), ...rows].join("\n");

  const attemptCount = attemptsSnap.size;
  const correctCount = attemptsSnap.docs.filter((doc) => doc.get("correct") === true).length;
  const accuracy = computeAccuracy(correctCount, attemptCount);

  const pdfBuffer: Buffer = await new Promise((resolve, reject) => {
    const doc = new PDFDocument({ margin: 32 });
    const buffers: Buffer[] = [];
    doc.on("data", (chunk: Buffer) => buffers.push(chunk));
    doc.on("end", () => resolve(Buffer.concat(buffers)));
    doc.on("error", reject);

    doc.fontSize(20).text("Session Report", { align: "center" });
    doc.moveDown();
    doc.fontSize(12);
    doc.text(`Session ID: ${sessionId}`);
    doc.text(`Total Attempts: ${attemptCount}`);
    doc.text(`Correct Attempts: ${correctCount}`);
    doc.text(`Accuracy: ${accuracy}%`);
    doc.moveDown();
    doc.text("Generated at: " + new Date().toISOString());
    doc.end();
  });

  const timestamp = Date.now();
  const csvPath = `reports/${sessionId}/session-report-${timestamp}.csv`;
  const pdfPath = `reports/${sessionId}/session-report-${timestamp}.pdf`;

  const bucket = getBucket();

  await bucket.file(csvPath).save(csv, {
    contentType: "text/csv",
    gzip: true,
    resumable: false,
  });
  await bucket.file(pdfPath).save(pdfBuffer, {
    contentType: "application/pdf",
    resumable: false,
  });

  const expires = Date.now() + 1000 * 60 * 60 * 24 * 7;
  const [csvUrl] = await bucket.file(csvPath).getSignedUrl({
    action: "read",
    expires,
  });
  const [pdfUrl] = await bucket.file(pdfPath).getSignedUrl({
    action: "read",
    expires,
  });

  return { csvUrl, pdfUrl };
});
