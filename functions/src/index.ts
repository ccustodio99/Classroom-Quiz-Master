import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import type { Firestore } from "firebase-admin/firestore";
import PDFDocument = require("pdfkit");

admin.initializeApp();

const defaultFirestore = admin.firestore();
let firestoreDb: Firestore = defaultFirestore;
const defaultBucketFactory = () => admin.storage().bucket();
let bucketFactory = defaultBucketFactory;
const getBucket = () => bucketFactory();

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
  classroomId?: string;
  topicId?: string;
  title?: string;
  type?: string;
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
  choices?: string[];
  stem?: string;
  type?: string;
  topicId?: string;
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

const parseSelectedValues = (raw: unknown): string[] => {
  if (Array.isArray(raw)) {
    return raw.map((value) => String(value));
  }
  if (typeof raw === "string") {
    const trimmed = raw.trim();
    if (!trimmed) return [];
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map((value) => String(value));
      }
    } catch {
      // fall through to best-effort parsing
    }
    if (trimmed.includes(",")) {
      return trimmed
        .split(",")
        .map((value) => value.trim())
        .filter((value) => value.length > 0);
    }
    return [trimmed];
  }
  return [];
};

const safeTimestampToMillis = (value: unknown): number | undefined => {
  if (typeof value === "number") {
    return value;
  }
  if (value instanceof admin.firestore.Timestamp) {
    return value.toMillis();
  }
  if (value && typeof (value as { toMillis?: () => number }).toMillis === "function") {
    return (value as { toMillis: () => number }).toMillis();
  }
  return undefined;
};

const truncateText = (value: string, maxLength = 160): string => {
  if (value.length <= maxLength) {
    return value;
  }
  return `${value.slice(0, Math.max(maxLength - 3, 0))}...`;
};

const escapeCsv = (value: unknown): string => {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  const stringValue = String(value);
  if (stringValue.includes('"') || stringValue.includes(",") || stringValue.includes("\n")) {
    return `"${stringValue.replace(/"/g, '""')}"`;
  }
  return stringValue;
};

const chunkArray = <T>(input: T[], size: number): T[][] => {
  if (size <= 0) return [input];
  const chunks: T[][] = [];
  for (let i = 0; i < input.length; i += size) {
    chunks.push(input.slice(i, i + size));
  }
  return chunks;
};

const resolveTimeLimitMs = (
  question: QuestionShape,
  assignment?: AssignmentDoc,
  quizDefaultTimePerQ?: number
): number => {
  const assignmentTimeLimit = (assignment as unknown as { defaultTimePerQ?: number })?.defaultTimePerQ;
  const baseSeconds = question.timeLimitSeconds ?? assignmentTimeLimit ?? quizDefaultTimePerQ ?? 30;
  return Math.max(baseSeconds * 1000, 1000);
};

const computeMaxPointsForQuestion = (
  question: QuestionShape,
  assignment?: AssignmentDoc,
  quizDefaultTimePerQ?: number
): number => {
  const limitMs = resolveTimeLimitMs(question, assignment, quizDefaultTimePerQ);
  return computePoints(true, limitMs, 0);
};

const deriveTimeBucket = (timeMs: number, limitMs: number): string => {
  if (timeMs <= 0 || limitMs <= 0) return "UNKNOWN";
  if (timeMs <= limitMs * 0.5) return "FAST";
  if (timeMs <= limitMs * 1.25) return "NORMAL";
  return "SLOW";
};

const toPercentage = (part: number, total: number): number => {
  if (total <= 0) return 0;
  return Math.round((part / total) * 100);
};

const EXPORT_VERSION = "v2";

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

    await firestoreDb.runTransaction(async (txn) => {
      const freshAttempt = await txn.get(attemptRef);
      if (freshAttempt.exists && freshAttempt.get("scoredAt")) {
        return;
      }

      const assignmentRef = firestoreDb.collection("assignments").doc(assignmentId);
      const assignmentSnap = await txn.get(assignmentRef);
      if (!assignmentSnap.exists) {
        functions.logger.error("Missing assignment", { assignmentId });
        throw new functions.https.HttpsError("not-found", "assignment-missing");
      }

      const assignment = assignmentSnap.data() as AssignmentDoc;
      const quizId = assignment.quizId;
      const quizSnap = await txn.get(firestoreDb.collection("quizzes").doc(quizId));
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

export const exportReport = functions.https.onCall(async (data: { sessionId?: string; assignmentId?: string }, context) => {
  if (!context.auth || context.auth.token.firebase?.sign_in_provider === "anonymous") {
    throw new functions.https.HttpsError("permission-denied", "Teacher authentication required");
  }

  const sessionId =
    typeof data?.sessionId === "string" && data.sessionId.trim().length > 0 ? data.sessionId.trim() : undefined;
  const assignmentId =
    typeof (data as { assignmentId?: string })?.assignmentId === "string" &&
    (data as { assignmentId?: string }).assignmentId?.trim()?.length
      ? (data as { assignmentId?: string }).assignmentId!.trim()
      : undefined;

  if (!sessionId && !assignmentId) {
    throw new functions.https.HttpsError("invalid-argument", "sessionId or assignmentId required");
  }

  const exportedAt = new Date();
  const exportedAtIso = exportedAt.toISOString();

  const fetchStudentNames = async (uids: string[]): Promise<Record<string, string>> => {
    const unique = Array.from(new Set(uids.filter((id) => id)));
    if (unique.length === 0) return {};
    const results: Record<string, string> = {};
    await Promise.all(
      chunkArray(unique, 10).map(async (chunk) => {
        const snap = await firestoreDb
          .collection("students")
          .where(admin.firestore.FieldPath.documentId(), "in", chunk)
          .get();
        snap.docs.forEach((doc) => {
          const displayName = doc.get("displayName") ?? doc.get("name") ?? doc.get("email");
          results[doc.id] =
            typeof displayName === "string" && displayName.trim().length > 0 ? displayName : (doc.id as string);
        });
      })
    );
    return results;
  };

  const fetchTopicNames = async (ids: string[]): Promise<Record<string, string>> => {
    const unique = Array.from(new Set(ids.filter((id) => id)));
    if (unique.length === 0) return {};
    const results: Record<string, string> = {};
    await Promise.all(
      chunkArray(unique, 10).map(async (chunk) => {
        const snap = await firestoreDb
          .collection("topics")
          .where(admin.firestore.FieldPath.documentId(), "in", chunk)
          .get();
        snap.docs.forEach((doc) => {
          const name = doc.get("name");
          if (typeof name === "string" && name.trim().length > 0) {
            results[doc.id] = name;
          }
        });
      })
    );
    return results;
  };

  const resolveAssignmentType = (raw?: string): string => {
    if (!raw) return "QUIZ";
    const upper = raw.toUpperCase();
    if (upper === "PRE" || upper === "PRE_TEST") return "PRE_TEST";
    if (upper === "POST" || upper === "POST_TEST") return "POST_TEST";
    if (upper === "PRACTICE") return "PRACTICE";
    if (upper === "EXAM" || upper === "TEST") return "EXAM";
    return upper;
  };

  type AttemptForExport = {
    attemptId: string;
    uid: string;
    questionId: string;
    selected: string[];
    points: number;
    timeMs: number;
    late: boolean;
    correct?: boolean;
    createdAtMs: number;
  };

  const resolveOptionLabel = (question: QuestionShape | undefined, value: string): string => {
    if (!question) return value;
    const choices = question.choices ?? [];
    const idx = choices.findIndex((choice) => choice.toLowerCase() === value.toLowerCase());
    if (idx >= 0) {
      const letter = String.fromCharCode(65 + idx);
      return `${letter}. ${truncateText(choices[idx], 60)}`;
    }
    return value;
  };

  let contextType: "session" | "assignment" = sessionId ? "session" : "assignment";
  let targetId = sessionId ?? assignmentId!;
  let quizId = "";
  let classroomId = "";
  let teacherId = context.auth.uid;
  let sessionName = "";
  let assignmentTitle = "";
  let assignmentType = "QUIZ";
  let assignmentDueDate: number | undefined;
  let resolvedAssignmentId: string | undefined = assignmentId;
  let quizCategory: string | undefined;
  let quizDefaultTimePerQ: number | undefined;
  let quizTopicId: string | undefined;
  let assignmentDoc: AssignmentDoc | undefined;

  let attemptsSnap: admin.firestore.QuerySnapshot;
  let classroomSnap: admin.firestore.DocumentSnapshot | undefined;

  if (contextType === "session") {
    const sessionRef = firestoreDb.collection("sessions").doc(targetId);
    const sessionSnap = await sessionRef.get();
    if (!sessionSnap.exists) {
      throw new functions.https.HttpsError("not-found", "session-missing");
    }
    teacherId = sessionSnap.get("teacherId") ?? teacherId;
    if (teacherId && teacherId !== context.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Teacher mismatch");
    }
    quizId = sessionSnap.get("quizId") ?? "";
    classroomId = sessionSnap.get("classroomId") ?? "";
    sessionName = sessionSnap.get("name") ?? sessionSnap.get("title") ?? "";
    resolvedAssignmentId = resolvedAssignmentId ?? (sessionSnap.get("assignmentId") as string | undefined);
    assignmentDueDate =
      safeTimestampToMillis(sessionSnap.get("closeAt")) ?? safeTimestampToMillis(sessionSnap.get("dueAt"));
    attemptsSnap = await sessionRef.collection("attempts").get();
    classroomSnap = classroomId ? await firestoreDb.collection("classrooms").doc(classroomId).get() : undefined;
  } else {
    const assignmentRef = firestoreDb.collection("assignments").doc(targetId);
    const assignmentSnap = await assignmentRef.get();
    if (!assignmentSnap.exists) {
      throw new functions.https.HttpsError("not-found", "assignment-missing");
    }
    assignmentDoc = assignmentSnap.data() as AssignmentDoc;
    classroomId = assignmentDoc.classroomId;
    quizId = assignmentDoc.quizId;
    resolvedAssignmentId = targetId;
    assignmentDueDate =
      safeTimestampToMillis(assignmentDoc.closeAt) ?? safeTimestampToMillis(assignmentSnap.get("closeAt"));
    assignmentTitle = (assignmentSnap.get("title") as string) ?? "";
    assignmentType = resolveAssignmentType(assignmentSnap.get("type") as string);
    attemptsSnap = await assignmentRef.collection("attempts").get();
    classroomSnap = classroomId ? await firestoreDb.collection("classrooms").doc(classroomId).get() : undefined;
  }

  if (!quizId) {
    throw new functions.https.HttpsError("failed-precondition", "quiz-missing");
  }

  if (!classroomSnap || !classroomSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "classroom-missing");
  }
  const classroomName = (classroomSnap.get("name") as string) ?? "Classroom";
  const classroomSubject = classroomSnap.get("subject") as string | undefined;
  const classroomGrade = classroomSnap.get("grade") as string | undefined;
  const roster = Array.isArray(classroomSnap.get("students"))
    ? ((classroomSnap.get("students") as unknown[]).map((id) => String(id)).filter(Boolean))
    : [];
  const classroomTeacher = classroomSnap.get("teacherId") as string | undefined;
  if (classroomTeacher && classroomTeacher !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Teacher mismatch");
  }
  teacherId = classroomTeacher ?? teacherId;

  const quizSnap = await firestoreDb.collection("quizzes").doc(quizId).get();
  if (!quizSnap.exists) {
    throw new functions.https.HttpsError("not-found", "quiz-missing");
  }
  quizCategory = quizSnap.get("category") as string | undefined;
  quizDefaultTimePerQ = typeof quizSnap.get("defaultTimePerQ") === "number" ? (quizSnap.get("defaultTimePerQ") as number) : undefined;
  quizTopicId = quizSnap.get("topicId") as string | undefined;
  const quizTitle = (quizSnap.get("title") as string) ?? "Assessment";
  assignmentTitle = assignmentTitle || quizTitle;
  sessionName = sessionName || assignmentTitle;
  const typeSource =
    assignmentType && assignmentType !== "QUIZ"
      ? assignmentType
      : quizCategory ?? assignmentType;
  assignmentType = resolveAssignmentType(typeSource);

  const questions = parseQuestions(quizSnap);
  const questionMap = new Map<string, QuestionShape & { number: number }>();
  questions.forEach((question, index) => {
    questionMap.set(question.id, { ...question, number: index + 1 });
  });

  const topicLookup = await fetchTopicNames([
    ...(quizTopicId ? [quizTopicId] : []),
    ...questions.map((q) => q.topicId).filter((id): id is string => Boolean(id)),
  ]);

  const attemptRows: AttemptForExport[] = attemptsSnap.docs
    .map((doc) => {
      const raw = doc.data() as Record<string, unknown>;
      return {
        attemptId: doc.id,
        uid: String(raw.uid ?? ""),
        questionId: String(raw.questionId ?? ""),
        selected: parseSelectedValues(raw.selected),
        points: typeof raw.points === "number" ? raw.points : 0,
        timeMs: typeof raw.timeMs === "number" ? (raw.timeMs as number) : 0,
        late: Boolean(raw.late),
        correct: typeof raw.correct === "boolean" ? (raw.correct as boolean) : undefined,
        createdAtMs:
          safeTimestampToMillis(raw.createdAt) ??
          safeTimestampToMillis(raw.scoredAt) ??
          safeTimestampToMillis((raw as { created?: unknown }).created) ??
          Date.now(),
      };
    })
    .filter((attempt) => attempt.uid && attempt.questionId);

  if (attemptRows.length === 0) {
    throw new functions.https.HttpsError("failed-precondition", "NO_DATA");
  }

  const studentNames = await fetchStudentNames(attemptRows.map((a) => a.uid));
  const teacherSnap = teacherId ? await firestoreDb.collection("teachers").doc(teacherId).get() : null;
  const teacherName =
    teacherSnap?.exists && typeof teacherSnap.get("displayName") === "string" && teacherSnap.get("displayName").trim().length
      ? (teacherSnap.get("displayName") as string)
      : "Teacher";

  const attemptNumberByStudent = new Map<string, number>();
  const uniqueQuestionIds = new Set<string>();
  const questionMaxPoints = new Map<string, number>();
  const studentPointTotals = new Map<string, number>();
  const questionAttemptMap = new Map<
    string,
    Array<{ attempt: AttemptForExport; isCorrect: boolean; selectedValues: string[] }>
  >();

  questions.forEach((question) => {
    questionMaxPoints.set(question.id, computeMaxPointsForQuestion(question, assignmentDoc, quizDefaultTimePerQ));
  });

  const sortedAttempts = attemptRows.sort((a, b) => a.createdAtMs - b.createdAtMs);
  const rows: Record<string, unknown>[] = [];
  let correctAttempts = 0;

  sortedAttempts.forEach((attempt) => {
    const question = questionMap.get(attempt.questionId) ?? {
      id: attempt.questionId,
      answerKey: [],
      timeLimitSeconds: 30,
      choices: [],
      stem: attempt.questionId,
      type: undefined,
      topicId: undefined,
      number: 0,
    };
    const correctIds = (question.answerKey ?? []).map((ans) => String(ans));
    const normalizedAnswer = correctIds.map((v) => v.toLowerCase()).sort();
    const normalizedSelected = attempt.selected.map((v) => v.toLowerCase()).sort();
    const inferredCorrect =
      normalizedAnswer.length > 0 && JSON.stringify(normalizedSelected) === JSON.stringify(normalizedAnswer);
    const isCorrect = typeof attempt.correct === "boolean" ? attempt.correct : inferredCorrect;
    if (isCorrect) {
      correctAttempts += 1;
    }
    const timeLimitMs = resolveTimeLimitMs(question, assignmentDoc, quizDefaultTimePerQ);
    const maxPoints =
      questionMaxPoints.get(attempt.questionId) ??
      computeMaxPointsForQuestion(question, assignmentDoc, quizDefaultTimePerQ);
    const percentageScore = maxPoints > 0 ? Number(((attempt.points / maxPoints) * 100).toFixed(2)) : 0;
    const attemptNumber = (attemptNumberByStudent.get(attempt.uid) ?? 0) + 1;
    attemptNumberByStudent.set(attempt.uid, attemptNumber);
    const timeBucket = deriveTimeBucket(attempt.timeMs, timeLimitMs);
    const topicName =
      (question.topicId && topicLookup[question.topicId]) ??
      (quizTopicId && topicLookup[quizTopicId]) ??
      "";
    const selectedLabels = attempt.selected.map((value) => resolveOptionLabel(question, value));

    const row: Record<string, unknown> = {
      session_id: sessionId ?? "",
      session_name: sessionName || quizTitle,
      assignment_id: resolvedAssignmentId ?? "",
      assignment_title: assignmentTitle || quizTitle,
      assignment_type: assignmentType,
      classroom_id: classroomId,
      classroom_name: classroomName,
      subject: classroomSubject ?? "",
      grade_level: classroomGrade ?? "",
      due_date: assignmentDueDate ? new Date(assignmentDueDate).toISOString() : "",
      student_id: attempt.uid,
      student_name: studentNames[attempt.uid] ?? attempt.uid,
      student_number: "",
      student_group: "",
      question_id: attempt.questionId,
      question_number: (question as { number?: number }).number ?? 0,
      question_text: truncateText(question.stem ?? "", 180),
      topic: topicName,
      max_points: maxPoints,
      is_multiple_choice:
        (question.type && question.type.toLowerCase() !== "tf") ||
        (question.choices?.length ?? 0) > 2 ||
        (correctIds.length > 1),
      selected_option_ids: JSON.stringify(attempt.selected),
      selected_option_labels: JSON.stringify(selectedLabels),
      correct_option_ids: JSON.stringify(correctIds),
      is_correct: isCorrect,
      points_earned: attempt.points,
      percentage_score: percentageScore,
      late: Boolean(attempt.late),
      time_ms: attempt.timeMs,
      time_seconds: Math.round((attempt.timeMs ?? 0) / 1000),
      time_bucket: timeBucket,
      attempt_id: attempt.attemptId,
      attempt_number: attemptNumber,
      created_at: new Date(attempt.createdAtMs).toISOString(),
      exported_at: exportedAtIso,
      export_version: EXPORT_VERSION,
    };
    rows.push(row);
    uniqueQuestionIds.add(attempt.questionId);
    studentPointTotals.set(attempt.uid, (studentPointTotals.get(attempt.uid) ?? 0) + attempt.points);
    const current = questionAttemptMap.get(attempt.questionId) ?? [];
    current.push({ attempt, isCorrect, selectedValues: attempt.selected });
    questionAttemptMap.set(attempt.questionId, current);
  });

  const header = [
    "session_id",
    "session_name",
    "assignment_id",
    "assignment_title",
    "assignment_type",
    "classroom_id",
    "classroom_name",
    "subject",
    "grade_level",
    "due_date",
    "student_id",
    "student_name",
    "student_number",
    "student_group",
    "question_id",
    "question_number",
    "question_text",
    "topic",
    "max_points",
    "is_multiple_choice",
    "selected_option_ids",
    "selected_option_labels",
    "correct_option_ids",
    "is_correct",
    "points_earned",
    "percentage_score",
    "late",
    "time_ms",
    "time_seconds",
    "time_bucket",
    "attempt_id",
    "attempt_number",
    "created_at",
    "exported_at",
    "export_version",
  ];
  const csvLines = [header.join(",")];
  rows.forEach((row) => {
    csvLines.push(header.map((key) => escapeCsv(row[key])).join(","));
  });
  const csv = csvLines.join("\n");

  const accuracy = toPercentage(correctAttempts, rows.length);
  const totalAvailablePoints =
    Array.from(questionMaxPoints.values()).reduce((sum, value) => sum + value, 0) ||
    uniqueQuestionIds.size * 1000 ||
    1;

  const studentScores = Array.from(studentPointTotals.entries()).map(([uid, points]) => {
    const percent = totalAvailablePoints > 0 ? Math.round((points / totalAvailablePoints) * 100) : 0;
    return {
      uid,
      name: studentNames[uid] ?? uid,
      percent,
    };
  });
  const averageScore =
    studentScores.length === 0 ? 0 : Math.round(studentScores.reduce((sum, entry) => sum + entry.percent, 0) / studentScores.length);
  const sortedScores = studentScores.map((entry) => entry.percent).sort((a, b) => a - b);
  const medianScore =
    sortedScores.length === 0
      ? 0
      : sortedScores.length % 2 === 0
        ? Math.round((sortedScores[sortedScores.length / 2 - 1] + sortedScores[sortedScores.length / 2]) / 2)
        : sortedScores[Math.floor(sortedScores.length / 2)];
  const highestScore = sortedScores.length ? sortedScores[sortedScores.length - 1] : 0;
  const lowestScore = sortedScores.length ? sortedScores[0] : 0;
  const completionRate = toPercentage(studentScores.length, roster.length || studentScores.length);

  const scoreBands = [
    { label: "90-100", min: 90, max: 100 },
    { label: "70-89", min: 70, max: 89 },
    { label: "50-69", min: 50, max: 69 },
    { label: "0-49", min: 0, max: 49 },
  ].map((band) => ({
    ...band,
    count: studentScores.filter((entry) => entry.percent >= band.min && entry.percent <= band.max).length,
  }));

  const topStudents = [...studentScores].sort((a, b) => b.percent - a.percent).slice(0, 5);
  const bottomStudents = [...studentScores].sort((a, b) => a.percent - b.percent).slice(0, 5);

  const topicSummary =
    (quizTopicId || questions.some((q) => q.topicId)) && (quizTopicId || questions[0]?.topicId)
      ? [
          {
            topic: (quizTopicId && topicLookup[quizTopicId]) || topicLookup[questions[0]?.topicId ?? ""] || "Topic",
            avgScore: averageScore,
            belowSixty: studentScores.filter((entry) => entry.percent < 60).length,
          },
        ]
      : [];

  const questionDifficultyRows = Array.from(questionAttemptMap.entries())
    .map(([questionId, attempts]) => {
      const correctCount = attempts.filter((attempt) => attempt.isCorrect).length;
      const total = attempts.length;
      const correctRate = toPercentage(correctCount, total);
      const wrongSelections = new Map<string, number>();
      attempts.forEach(({ isCorrect, selectedValues }) => {
        if (isCorrect) return;
        const value = selectedValues[0];
        if (!value) return;
        const key = resolveOptionLabel(questionMap.get(questionId), value);
        wrongSelections.set(key, (wrongSelections.get(key) ?? 0) + 1);
      });
      const [topWrong, topWrongCount] =
        Array.from(wrongSelections.entries()).sort((a, b) => b[1] - a[1])[0] ?? [undefined, 0];
      return {
        questionId,
        correctRate,
        preview: truncateText(questionMap.get(questionId)?.stem ?? questionId, 80),
        topWrong,
        topWrongRate: total > 0 ? toPercentage(topWrongCount ?? 0, total) : 0,
      };
    })
    .filter((row) => row.correctRate <= 30 || row.correctRate >= 90)
    .sort((a, b) => a.correctRate - b.correctRate);

  const notes: string[] = [];
  if (completionRate < 70) {
    notes.push("Completion is below 70%. Consider reminding students to submit.");
  }
  if (averageScore < 70) {
    notes.push("Average score is below 70%. Plan a quick review or reteach session.");
  }
  if (questionDifficultyRows.some((row) => row.correctRate <= 30)) {
    notes.push("Some questions were very difficult. Review the flagged items with the class.");
  }
  if (notes.length === 0) {
    notes.push("Use this space to capture follow-ups from the assessment.");
  }

  const pdfBuffer: Buffer = await new Promise((resolve, reject) => {
    const doc = new PDFDocument({ margin: 32 });
    const buffers: Buffer[] = [];
    doc.on("data", (chunk: Buffer) => buffers.push(chunk));
    doc.on("end", () => resolve(Buffer.concat(buffers)));
    doc.on("error", reject);

    const addHeading = (title: string) => {
      doc.moveDown(0.6);
      doc.font("Helvetica-Bold").fontSize(14).text(title);
      doc.moveDown(0.2);
      doc.font("Helvetica").fontSize(11);
    };

    const twoColumnStats = (stats: { label: string; value: string }[]) => {
      const contentWidth = doc.page.width - doc.page.margins.left - doc.page.margins.right;
      const columnWidth = contentWidth / 2;
      stats.forEach((stat, index) => {
        const column = index % 2;
        const x = doc.page.margins.left + column * columnWidth;
        const y = doc.y;
        doc.font("Helvetica-Bold").text(stat.label, x, y, { width: columnWidth, continued: false });
        doc.font("Helvetica").text(stat.value, { width: columnWidth });
        if (column === 1) {
          doc.moveDown(0.3);
        }
      });
      doc.moveDown(0.4);
    };

    doc.fontSize(20).text(assignmentTitle || "Assessment Report");
    doc.moveDown(0.3);
    doc.fontSize(12);
    doc.text(`${classroomName}${classroomSubject ? " - " + classroomSubject : ""}`);
    doc.text(`Teacher: ${teacherName}`);
    doc.text(`Assessment date: ${assignmentDueDate ? new Date(assignmentDueDate).toISOString().slice(0, 10) : exportedAtIso.slice(0, 10)}`);
    doc.text(`Exported at: ${exportedAtIso}`);
    doc.moveDown();

    addHeading("Summary");
    twoColumnStats([
      { label: "Students in class", value: roster.length ? `${roster.length}` : `${studentScores.length}` },
      { label: "Submitted", value: `${studentScores.length}` },
      { label: "Completion rate", value: `${completionRate}%` },
      { label: "Average score", value: `${averageScore}%` },
      { label: "Median score", value: `${medianScore}%` },
      { label: "Highest / Lowest", value: `${highestScore}% / ${lowestScore}%` },
      { label: "Accuracy", value: `${accuracy}%` },
    ]);

    addHeading("Score distribution");
    scoreBands.forEach((band) => {
      doc.text(`${band.label.padEnd(6, " ")} : ${band.count} students`);
    });

    addHeading("Top students");
    if (topStudents.length === 0) {
      doc.text("No submissions yet.");
    } else {
      topStudents.forEach((student) => doc.text(`${student.name} — ${student.percent}%`));
    }

    addHeading("Students who may need support");
    if (bottomStudents.length === 0) {
      doc.text("No students to highlight.");
    } else {
      bottomStudents.forEach((student) => doc.text(`${student.name} — ${student.percent}%`));
    }

    addHeading("Topic summary");
    if (topicSummary.length === 0) {
      doc.text("No topic tags available.");
    } else {
      topicSummary.forEach((row) =>
        doc.text(`${row.topic}: Avg ${row.avgScore}% - Students <60%: ${row.belowSixty}`)
      );
    }

    addHeading("Question difficulty");
    if (questionDifficultyRows.length === 0) {
      doc.text("No questions flagged as unusually easy or difficult.");
    } else {
      questionDifficultyRows.slice(0, 8).forEach((row) => {
        const difficultyLabel = row.correctRate <= 30 ? "Hard" : "Easy";
        const wrong = row.topWrong ? ` - Top wrong: ${row.topWrong} (${row.topWrongRate}%)` : "";
        doc.text(`${difficultyLabel} - ${row.preview} - Correct ${row.correctRate}%${wrong}`);
      });
    }

    addHeading("Notes");
    notes.forEach((note) => doc.text(`- ${note}`));
    const remainingLines = Math.max(0, 4 - notes.length);
    for (let i = 0; i < remainingLines; i++) {
      doc.text("- _______________________________________________");
    }

    doc.end();
  });

  const timestamp = Date.now();
  const basePath = `reports/${targetId}/assessment-report-${timestamp}`;
  const csvPath = `${basePath}.csv`;
  const pdfPath = `${basePath}.pdf`;

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

  return { csvUrl, pdfUrl, accuracy, exportVersion: EXPORT_VERSION, exportedAt: exportedAtIso };
});

export const setFirestoreForTests = (override: Firestore) => {
  firestoreDb = override;
};

export const setBucketFactoryForTests = (factory: typeof bucketFactory) => {
  bucketFactory = factory;
};

export const resetTestOverrides = () => {
  firestoreDb = defaultFirestore;
  bucketFactory = defaultBucketFactory;
};
