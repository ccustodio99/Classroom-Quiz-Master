package com.classroom.quizmaster.data.repo.impl

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.JoinPolicy
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.RosterRole
import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.repo.ClassRepo
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import com.classroom.quizmaster.data.util.asFlow
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
    private fun classCollection(org: String) = orgDocument(org).collection("classes")
    private fun classDocument(org: String, classId: String) = classCollection(org).document(classId)

    override fun myClasses(org: String): Flow<List<Class>> =
        classCollection(resolveOrg(org))
            .whereArrayContains("memberIds", auth.currentUser?.uid ?: "")
            .orderBy("subject", Query.Direction.ASCENDING)
            .asFlow(::classFromSnapshot)

    override suspend fun createClass(org: String, owner: User, subject: String, section: String): Class {
        val orgId = resolveOrg(org)
        val doc = classCollection(orgId).document()
        val classPath = doc.path
        val newClass = Class(
            id = classPath,
            orgId = orgId,
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
                classId = classPath,
                userId = owner.id,
                role = RosterRole.OWNER // Fixed: Use enum RosterRole
            )
        ).await()
        return newClass
    }

    override suspend fun joinByCode(org: String, user: User, code: String): Roster {
        val orgId = resolveOrg(org)
        val classes = classCollection(orgId)
            .whereEqualTo("code", code.uppercase())
            .limit(1)
            .get()
            .await()

        val classDoc = classes.documents.firstOrNull()
            ?: error("Invalid join code")

        val classPath = classDoc.reference.path
        val roster = Roster(
            id = user.id, // Roster document ID is the userId for direct access
            classId = classPath,
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
        val classAddress = resolveClassAddress(classPath)
        return classDocument(classAddress.orgId, classAddress.classId)
            .collection("roster")
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(Roster::class.java)?.copy(
                id = it.id, // Roster document ID
                classId = classAddress.path // Fixed: Use resolved class path
            ) }
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
                ownerId = "",
                code = "", // Added default for consistency with model
                joinPolicy = JoinPolicy.OPEN, // Added default for consistency with model
                memberIds = emptyList() // Added default for consistency with model
            )

    private fun resolveClassAddress(classReference: String): ClassAddress {
        return if (classReference.contains("/")) {
            val (orgId, classId) = parseClassPath(classReference)
            ClassAddress(orgId, classId, classDocument(orgId, classId).path)
        } else {
            val orgId = resolveOrg(null)
            ClassAddress(orgId, classReference, classDocument(orgId, classReference).path)
        }
    }

    private fun parseClassPath(path: String): Pair<String, String> {
        val trimmed = path.trimStart('/')
        val segments = trimmed.split("/")
        require(segments.size >= 4) { "Class path must be /orgs/{orgId}/classes/{classId}" }
        val orgIndex = segments.indexOf("orgs")
        val classIndex = segments.indexOf("classes")
        require(orgIndex >= 0 && classIndex >= 0) { "Invalid class path: $path" }
        val orgId = segments.getOrNull(orgIndex + 1) ?: error("Missing orgId in $path")
        val classId = segments.getOrNull(classIndex + 1) ?: error("Missing classId in $path")
        return orgId to classId
    }

    private fun resolveOrg(explicit: String?): String {
        val candidate = explicit?.takeIf { it.isNotBlank() }
        if (!candidate.isNullOrBlank()) return candidate

        val currentUser = auth.currentUser
        val tenant = currentUser?.tenantId?.takeIf { it.isNotBlank() }
        if (!tenant.isNullOrBlank()) return tenant

        val emailDomain = currentUser?.email
            ?.substringAfter("@", missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
        if (!emailDomain.isNullOrBlank()) return emailDomain.lowercase()

        return DEFAULT_ORG_ID
    }

    private data class ClassAddress(
        val orgId: String,
        val classId: String,
        val path: String
    )
}
