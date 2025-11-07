import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();

export const scoreAssignmentAttempt = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid ?? data.uid;
  if (!uid) {
    throw new functions.https.HttpsError("failed-precondition", "Missing auth");
  }
  const {assignmentId, answers} = data;
  if (!assignmentId || !Array.isArray(answers)) {
    throw new functions.https.HttpsError("invalid-argument", "assignmentId and answers required");
  }
  const assignmentSnap = await db.collection("assignments").doc(assignmentId).get();
  if (!assignmentSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Assignment missing");
  }
  const questionsSnap = await db.collection("quizzes")
    .doc(assignmentSnap.get("quizId"))
    .collection("questions")
    .get();
  let score = 0;
  questionsSnap.docs.forEach((doc, index) => {
    const key: string[] = doc.get("answerKey") ?? [];
    const response: string[] = answers[index] ?? [];
    if (JSON.stringify(key.sort()) === JSON.stringify(response.sort())) {
      score += 100;
    }
  });
  await db.collection("assignments")
    .doc(assignmentId)
    .collection("submissions")
    .doc(uid)
    .set({
      lastScore: score,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
  return {score};
});
