package com.acme.lms.data.repo.impl

import com.acme.lms.data.model.Class
import com.acme.lms.data.model.JoinPolicy
import com.acme.lms.data.model.Roster
import com.acme.lms.data.model.RosterRole
import com.acme.lms.data.model.User
import com.acme.lms.data.repo.ClassRepo
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
class ClassRepoImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ClassRepo {

    private fun orgDocument(org: String) = db.collection("orgs").document(org)

    override fun myClasses(org: String): Flow<List<Class>> =
        orgDocument(org)
            .collection("classes")
            .whereArrayContains("memberIds", auth.currentUser?.uid ?: "")
            .orderBy("subject", Query.Direction.ASCENDING)
            .asFlow(::classFromSnapshot)

    override suspend fun createClass(org: String, owner: User, subject: String, section: String): Class {
        val doc = orgDocument(org).collection("classes").document()
        val newClass = Class(
            id = doc.id, // Fixed: Use doc.id for the document ID
            orgId = org,
            code = doc.id.takeLast(6).uppercase(),
            subject = subject,
            section = section,
            ownerId = owner.id,
            memberIds = listOf(owner.id),
            coTeachers = emptyList(),
            joinPolicy = JoinPolicy.OPEN // Added default join policy
        )
        doc.set(newClass).await()
        doc.collection("roster").document(owner.id).set(
            Roster(
                id = owner.id, // Roster document ID is the userId for direct access
                classId = doc.id, // Fixed: Use doc.id for classId
                userId = owner.id,
                role = RosterRole.OWNER // Fixed: Use enum RosterRole
            )
        ).await()
        return newClass
    }

    override suspend fun joinByCode(org: String, user: User, code: String): Roster {
        val classes = orgDocument(org)
            .collection("classes")
            .whereEqualTo("code", code.uppercase())
            .limit(1)
            .get()
            .await()

        val classDoc = classes.documents.firstOrNull()
            ?: error("Invalid join code")

        val roster = Roster(
            id = user.id, // Roster document ID is the userId for direct access
            classId = classDoc.id, // Fixed: Use classDoc.id for classId
            userId = user.id,
            role = RosterRole.LEARNER // Fixed: Use enum RosterRole
        )

        db.runBatch { batch ->
            val rosterRef = classDoc.reference.collection("roster").document(user.id)
            batch.set(rosterRef, roster)
            batch.update(classDoc.reference, "memberIds", FieldValue.arrayUnion(user.id))
        }.await()
        return roster
    }

    override suspend fun getRoster(classPath: String): List<Roster> {
        val (org, classId) = parseClassPath(classPath)
        return orgDocument(org)
            .collection("classes").document(classId)
            .collection("roster")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Roster::class.java)?.copy(
                id = it.id, // Roster document ID
                classId = classId // Fixed: Use extracted classId, not parent path
            ) }
    }

    private fun classFromSnapshot(snapshot: DocumentSnapshot): Class =
        snapshot.toObject(Class::class.java)?.copy(
            id = snapshot.id, // Fixed: Use snapshot.id for the document ID
            orgId = snapshot.reference.parent?.parent?.id.orEmpty() // Fixed: Extract orgId from parent
        )
            ?: Class(
                id = snapshot.id, // Fixed: Use snapshot.id
                orgId = snapshot.reference.parent?.parent?.id.orEmpty(), // Fixed: Extract orgId
                subject = "",
                section = "",
                ownerId = "",
                code = "", // Added default for consistency with model
                joinPolicy = JoinPolicy.OPEN, // Added default for consistency with model
                memberIds = emptyList() // Added default for consistency with model
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
