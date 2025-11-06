package com.classroom.quizmaster.data.repo.impl

import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Submission
import com.classroom.quizmaster.data.repo.ClassworkRepo
import com.classroom.quizmaster.data.util.Time
import com.classroom.quizmaster.data.util.asFlow
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassworkRepoImpl @Inject constructor(
    private val db: FirebaseFirestore
) : ClassworkRepo {

    override fun assignments(classPath: String): Flow<List<Classwork>> =
        classworkCollection(classPath)
            .orderBy("dueAt", Query.Direction.ASCENDING)
            .asFlow(::classworkFromSnapshot)

    override suspend fun listOnce(classPath: String): List<Classwork> =
        classworkCollection(classPath)
            .get()
            .await()
            .documents
            .map(::classworkFromSnapshot)

    override suspend fun upsert(classwork: Classwork) {
        val collection = classworkCollection(classwork.classId)
        val doc = if (classwork.id.isBlank()) {
            collection.document()
        } else {
            db.document(classwork.id)
        }
        doc.set(classwork.copy(id = doc.path)).await()
    }

    override suspend fun submit(submission: Submission) {
        val classworkDoc = if (submission.classworkId.contains("/")) {
            db.document(submission.classworkId)
        } else {
            classworkCollection(submission.classId).document(submission.classworkId)
        }
        classworkDoc.collection("submissions")
            .document(submission.userId)
            .set(submission.copy(updatedAt = Time.now()))
            .await()
    }

    private fun classworkCollection(classPath: String): CollectionReference {
        val (orgId, classId) = parseClassPath(classPath)
        return db.collection("orgs").document(orgId)
            .collection("classes").document(classId)
            .collection("classwork")
    }

    private fun classworkFromSnapshot(snapshot: DocumentSnapshot): Classwork =
        snapshot.toObject(Classwork::class.java)?.copy(
            id = snapshot.reference.path,
            classId = snapshot.reference.parent.parent?.path.orEmpty()
        )
            ?: Classwork(
                id = snapshot.reference.path,
                classId = snapshot.reference.parent.parent?.path.orEmpty()
            )

    private fun parseClassPath(path: String): Pair<String, String> {
        val segments = path.split("/")
        require(segments.size >= 4) { "Class path must be /orgs/{orgId}/classes/{classId}" }
        val orgIndex = segments.indexOf("orgs")
        val classIndex = segments.indexOf("classes")
        require(orgIndex >= 0 && classIndex >= 0) { "Invalid class path: $path" }
        val orgId = segments.getOrNull(orgIndex + 1) ?: error("Missing orgId in $path")
        val classId = segments.getOrNull(classIndex + 1) ?: error("Missing classId in $path")
        return orgId to classId
    }
}
