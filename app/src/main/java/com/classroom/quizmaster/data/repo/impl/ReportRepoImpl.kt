package com.classroom.quizmaster.data.repo.impl

import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.ClassworkType
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.repo.ClassReportData
import com.classroom.quizmaster.data.repo.LearningGainRow
import com.classroom.quizmaster.data.repo.ReportRepo
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepoImpl @Inject constructor(
    private val db: FirebaseFirestore
) : ReportRepo {

    override suspend fun getClassReportData(classId: String): ClassReportData {
        val classPath = normalizeClassPath(classId)
        val snapshot = db.document(classPath).get().await()
        require(snapshot.exists()) { "Class $classId not found" }

        val (orgId, _) = parseClassPath(classPath)
        val classInfo = snapshot.toObject(Class::class.java)?.copy(
            id = classPath,
            orgId = orgId
        ) ?: Class(id = classPath, orgId = orgId)

        val roster = snapshot.reference.collection("roster")
            .get()
            .await()
            .documents
            .mapNotNull { it.toRoster(classPath) }

        val classwork = snapshot.reference.collection("classwork")
            .get()
            .await()
            .documents
            .mapNotNull { it.toClasswork(classPath) }

        return ClassReportData(
            classInfo = classInfo,
            roster = roster,
            classwork = classwork
        )
    }

    override suspend fun getLearningGainRows(classId: String): List<LearningGainRow> {
        val classPath = normalizeClassPath(classId)
        val classRef = db.document(classPath)

        val classworkDocs = classRef.collection("classwork")
            .get()
            .await()
            .documents

        val attemptsByUser = mutableMapOf<String, MutableList<Pair<ClassworkType, Attempt>>>()

        for (doc in classworkDocs) {
            val classwork = doc.toObject(Classwork::class.java)?.copy(
                id = doc.reference.path,
                classId = classPath
            ) ?: continue

            if (classwork.type != ClassworkType.PRETEST && classwork.type != ClassworkType.POSTTEST) {
                continue
            }

            val attemptDocs = doc.reference.collection("attempts").get().await()
            for (attemptSnapshot in attemptDocs.documents) {
                val attempt = attemptSnapshot.toObject(Attempt::class.java)?.copy(
                    id = attemptSnapshot.reference.path,
                    classId = classPath,
                    classworkId = doc.reference.path
                ) ?: continue

                attemptsByUser
                    .getOrPut(attempt.userId) { mutableListOf() }
                    .add(classwork.type to attempt)
            }
        }

        return attemptsByUser.entries.mapNotNull { (userId, attempts) ->
            val preAttempt = attempts
                .filter { it.first == ClassworkType.PRETEST }
                .maxByOrNull { it.second.submittedAt ?: it.second.startedAt }
                ?.second

            val postAttempt = attempts
                .filter { it.first == ClassworkType.POSTTEST }
                .maxByOrNull { it.second.submittedAt ?: it.second.startedAt }
                ?.second

            if (preAttempt == null || postAttempt == null) {
                null
            } else {
                val preScore = preAttempt.score ?: preAttempt.answers.size.toDouble()
                val postScore = postAttempt.score ?: postAttempt.answers.size.toDouble()
                val gain = if (preScore == 0.0) postScore else (postScore - preScore) / preScore
                LearningGainRow(
                    userId = userId,
                    preScore = preScore,
                    postScore = postScore,
                    gain = gain
                )
            }
        }.sortedBy { it.userId }
    }

    private fun DocumentSnapshot.toRoster(classPath: String): Roster? =
        toObject(Roster::class.java)?.copy(
            id = id,
            classId = classPath
        )

    private fun DocumentSnapshot.toClasswork(classPath: String): Classwork? =
        toObject(Classwork::class.java)?.copy(
            id = reference.path,
            classId = classPath
        )

    private fun normalizeClassPath(raw: String): String =
        if (raw.contains("/")) raw.trimStart('/') else "orgs/$DEFAULT_ORG_ID/classes/${raw.trim()}"

    private fun parseClassPath(path: String): Pair<String, String> {
        val segments = path.trimStart('/').split("/")
        require(segments.size >= 4) { "Class path must be /orgs/{orgId}/classes/{classId}" }
        val orgIndex = segments.indexOf("orgs")
        val classIndex = segments.indexOf("classes")
        require(orgIndex >= 0 && classIndex >= 0) { "Invalid class path: $path" }
        val orgId = segments.getOrNull(orgIndex + 1) ?: error("Missing orgId in $path")
        val classId = segments.getOrNull(classIndex + 1) ?: error("Missing classId in $path")
        return orgId to classId
    }
}
