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

type ScoringMode = "best" | "last" | "avg";

interface SubmissionSnapshotShape {
  attempts?: number;
  bestScore?: number;
  lastScore?: number;
  totalScore?: number;
  averageScore?: number;
  effectiveScore?: number;
}

export interface SubmissionUpdate {
  attempts: number;
  bestScore: number;
  lastScore: number;
  totalScore: number;
  averageScore: number;
  effectiveScore: number;
}

const resolveTotalScore = (snapshot?: SubmissionSnapshotShape): number => {
  if (!snapshot) {
    return 0;
  }
  if (typeof snapshot.totalScore === "number") {
    return snapshot.totalScore;
  }
  if (typeof snapshot.averageScore === "number" && typeof snapshot.attempts === "number") {
    return Math.round(snapshot.averageScore * snapshot.attempts);
  }
  if (typeof snapshot.bestScore === "number") {
    return snapshot.bestScore * Math.max(snapshot.attempts ?? 1, 1);
  }
  if (typeof snapshot.lastScore === "number") {
    return snapshot.lastScore * Math.max(snapshot.attempts ?? 1, 1);
  }
  return 0;
};

export const applyScoringMode = (
  mode: ScoringMode,
  previous: SubmissionSnapshotShape | undefined,
  points: number
): SubmissionUpdate => {
  const prevAttempts = previous?.attempts ?? 0;
  const attempts = prevAttempts + 1;
  const totalScore = resolveTotalScore(previous) + points;
  const bestScore = Math.max(previous?.bestScore ?? 0, points);
  const lastScore = points;
  const averageScore = Math.round(totalScore / attempts);
  let effectiveScore = bestScore;
  if (mode === "last") {
    effectiveScore = lastScore;
  } else if (mode === "avg") {
    effectiveScore = averageScore;
  }
  return {
    attempts,
    bestScore,
    lastScore,
    totalScore,
    averageScore,
    effectiveScore,
  };
};

type AssignmentDoc = {
  quizId: string;
  closeAt?: number;
  attemptsAllowed?: number;
  scoringMode?: ScoringMode;
  teacherId?: string;
  revealAfterSubmit?: boolean;
};

type QuestionShape = {
  id: string;
  answerKey?: string[];
  timeLimitSeconds?: number;
};

type AttemptDoc = {
  uid: string;
  questionId: string;
  selected?: string[];
  timeMs?: number;
  createdAt?: admin.firestore.Timestamp;
};

const fetchQuestion = (questions: QuestionShape[], questionId: string): QuestionShape | undefined => {
  return questions.find((q) => q.id === questionId);
};

const parseQuestions = (quizSnapshot: admin.firestore.DocumentSnapshot): QuestionShape[] => {
  const questionsJson = quizSnapshot.get("questionsJson");
  if (typeof questionsJson === "string") {
    try {
      const parsed = JSON.parse(questionsJson);
      return Array.isArray(parsed) ? parsed : [];
    } catch (err) {
      functions.logger.error("Failed to parse questionsJson", err);
      return [];
    }
  }
  const questions = quizSnapshot.get("questions");
  return Array.isArray(questions) ? (questions as QuestionShape[]) : [];
};

const computeAttemptOutcome = (
  attempt: AttemptDoc,
  question: QuestionShape,
  assignment: AssignmentDoc,
  submittedAt: admin.firestore.Timestamp,
  quizDefaultTimePerQ?: number
) => {
  const answerKey = (question.answerKey ?? []).map((v) => v.toLowerCase()).sort();
  const selected = (attempt.selected ?? []).map((v) => v.toLowerCase()).sort();
  const correct = JSON.stringify(answerKey) === JSON.stringify(selected);
  const assignmentTimeLimit = (assignment as unknown as { defaultTimePerQ?: number })?.defaultTimePerQ;
  const baseSeconds = question.timeLimitSeconds ?? assignmentTimeLimit ?? quizDefaultTimePerQ ?? 30;
  const timeLimitMs = Math.max(baseSeconds * 1000, 1000);
  const timeTakenMs = Math.max(attempt.timeMs ?? timeLimitMs, 0);
  const points = computePoints(correct, timeLimitMs, timeTakenMs);
  const closeAt = assignment.closeAt;
  const late = typeof closeAt === "number" && submittedAt.toMillis() > closeAt;
  return { correct, points, late, timeTakenMs, timeLimitMs };
};

export const scoreAttempt = functions.firestore
  .document("assignments/{assignmentId}/attempts/{attemptId}")
  .onCreate(async (snap, context) => {
    const attempt = snap.data() as AttemptDoc | undefined;
    if (!attempt) {
      return;
    }

    const assignmentId = context.params.assignmentId as string;
    const attemptRef = snap.ref;
    const uid = attempt.uid;
    const questionId = attempt.questionId;
    const submittedAt = attempt.createdAt ?? admin.firestore.Timestamp.now();

    await db.runTransaction(async (txn) => {
      const freshAttempt = await txn.get(attemptRef);
      if (freshAttempt.exists && freshAttempt.get("scoredAt")) {
        return;
      }

      const assignmentRef = db.collection("assignments").doc(assignmentId);
      const assignmentSnap = await txn.get(assignmentRef);
      if (!assignmentSnap.exists) {
        functions.logger.error("Missing assignment", { assignmentId });
        throw new functions.https.HttpsError("not-found", "assignment-missing");
      }

      const assignment = assignmentSnap.data() as AssignmentDoc;
      const quizId = assignment.quizId;
      const quizSnap = await txn.get(db.collection("quizzes").doc(quizId));
      if (!quizSnap.exists) {
        functions.logger.error("Missing quiz", { quizId });
        throw new functions.https.HttpsError("not-found", "quiz-missing");
      }

      const questions = parseQuestions(quizSnap);
      const question = fetchQuestion(questions, questionId);
      if (!question) {
        functions.logger.error("Missing question", { questionId });
        throw new functions.https.HttpsError("not-found", "question-missing");
      }

      const quizDefaultTimePerQ = quizSnap.get("defaultTimePerQ");
      const { correct, points, late } = computeAttemptOutcome(
        attempt,
        question,
        assignment,
        submittedAt,
        typeof quizDefaultTimePerQ === "number" ? quizDefaultTimePerQ : undefined
      );
      const submissionRef = assignmentRef.collection("submissions").doc(uid);
      const submissionSnap = await txn.get(submissionRef);
      const attemptsAllowed = assignment.attemptsAllowed;
      const existingAttempts = submissionSnap.get("attempts") ?? 0;

      if (typeof attemptsAllowed === "number" && attemptsAllowed > 0 && existingAttempts >= attemptsAllowed) {
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

      const previousSubmission = submissionSnap.exists ? (submissionSnap.data() as SubmissionSnapshotShape) : undefined;
      const scoringMode = assignment.scoringMode ?? "best";
      const update = applyScoringMode(scoringMode, previousSubmission, points);

      txn.set(
        submissionRef,
        {
          attempts: update.attempts,
          bestScore: update.bestScore,
          lastScore: update.lastScore,
          totalScore: update.totalScore,
          averageScore: update.averageScore,
          effectiveScore: update.effectiveScore,
          scoringMode,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    });
  });

export const exportReport = functions.https.onCall(async (data: { sessionId?: string }, context) => {
  if (!context.auth || context.auth.token.firebase?.sign_in_provider === "anonymous") {
    throw new functions.https.HttpsError("permission-denied", "Teacher authentication required");
  }

  const sessionId = data.sessionId;
  if (!sessionId) {
    throw new functions.https.HttpsError("invalid-argument", "sessionId required");
  }

  const sessionRef = db.collection("sessions").doc(sessionId);
  const sessionSnap = await sessionRef.get();
  if (!sessionSnap.exists) {
    throw new functions.https.HttpsError("not-found", "session-missing");
  }

  const teacherId = sessionSnap.get("teacherId");
  if (teacherId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Teacher mismatch");
  }

  const attemptsSnap = await sessionRef.collection("attempts").get();
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
      attempt.createdAt && attempt.createdAt.toMillis ? attempt.createdAt.toMillis() : attempt.createdAt ?? ""
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

  return { csvUrl, pdfUrl, accuracy };
});
