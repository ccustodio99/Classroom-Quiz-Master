package com.acme.lms.data.repo

import com.acme.lms.data.model.Class
import com.acme.lms.data.model.Roster
import com.acme.lms.data.model.User
import com.acme.lms.data.util.asFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassRepo @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun orgDocument(org: String) = db.collection("orgs").document(org)

    fun myClasses(org: String): Flow<List<Class>> =
        orgDocument(org)
            .collection("classes")
            .whereArrayContains("memberIds", auth.currentUser?.uid ?: "")
            .orderBy("subject", Query.Direction.ASCENDING)
            .asFlow(::classFromSnapshot)

    suspend fun createClass(org: String, owner: User, subject: String, section: String): Class {
        val doc = orgDocument(org).collection("classes").document()
        val newClass = Class(
            id = doc.path,
            orgId = org,
            code = doc.id.takeLast(6).uppercase(),
            subject = subject,
            section = section,
            ownerId = owner.id,
            memberIds = listOf(owner.id),
            coTeachers = emptyList()
        )
        doc.set(newClass).await()
        doc.collection("roster").document(owner.id).set(
            Roster(
                id = owner.id,
                classId = doc.path,
                userId = owner.id,
                role = "teacher"
            )
        ).await()
        return newClass
    }

    suspend fun joinByCode(org: String, user: User, code: String): Roster {
        val classes = orgDocument(org)
            .collection("classes")
            .whereEqualTo("code", code.uppercase())
            .limit(1)
            .get()
            .await()

        val classDoc = classes.documents.firstOrNull()
            ?: error("Invalid join code")

        val roster = Roster(
            id = user.id,
            classId = classDoc.reference.path,
            userId = user.id,
            role = "learner"
        )

        db.runBatch { batch ->
            val rosterRef = classDoc.reference.collection("roster").document(user.id)
            batch.set(rosterRef, roster)
            batch.update(classDoc.reference, "memberIds", FieldValue.arrayUnion(user.id))
        }.await()
        return roster
    }

    suspend fun getRoster(classPath: String): List<Roster> {
        val (org, classId) = parseClassPath(classPath)
        return orgDocument(org)
            .collection("classes").document(classId)
            .collection("roster")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Roster::class.java)?.copy(id = it.id) }
    }

    private fun classFromSnapshot(snapshot: DocumentSnapshot): Class =
        snapshot.toObject(Class::class.java)?.copy(
            id = snapshot.reference.path,
            orgId = snapshot.reference.parent?.parent?.id.orEmpty()
        )
            ?: Class(
                id = snapshot.reference.path,
                orgId = snapshot.reference.parent?.parent?.id.orEmpty(),
                subject = "",
                section = "",
                ownerId = ""
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
