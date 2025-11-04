package com.classroom.quizmaster

import android.content.Context
import com.classroom.quizmaster.agents.AssessmentAgentImpl
import com.classroom.quizmaster.agents.AssignmentAgentImpl
import com.classroom.quizmaster.agents.AuthAgentImpl
import com.classroom.quizmaster.agents.ClassroomAgentImpl
import com.classroom.quizmaster.agents.FirebaseSyncAgent
import com.classroom.quizmaster.agents.GamificationAgentImpl
import com.classroom.quizmaster.agents.ItemBankAgentImpl
import com.classroom.quizmaster.agents.LessonAgentImpl
import com.classroom.quizmaster.agents.LiveSessionAgentImpl
import com.classroom.quizmaster.agents.ModuleBuilderAgentImpl
import com.classroom.quizmaster.agents.ReportExportAgentImpl
import com.classroom.quizmaster.agents.ScoringAnalyticsAgentImpl
import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.BlueprintLocalStore
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.repo.AssignmentRepository
import com.classroom.quizmaster.data.repo.AssignmentRepositoryImpl
import com.classroom.quizmaster.data.repo.CatalogRepository
import com.classroom.quizmaster.data.repo.CatalogRepositoryImpl
import com.classroom.quizmaster.data.repo.AttemptRepository
import com.classroom.quizmaster.data.repo.AttemptRepositoryImpl
import com.classroom.quizmaster.data.repo.AccountRepository
import com.classroom.quizmaster.data.repo.AccountRepositoryImpl
import com.classroom.quizmaster.data.repo.ClassroomRepository
import com.classroom.quizmaster.data.repo.ClassroomRepositoryImpl
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.data.repo.ModuleRepositoryImpl
import com.classroom.quizmaster.data.util.PasswordHasher
import com.classroom.quizmaster.domain.model.AccountStatus
import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.agent.DataSyncAgent
import com.classroom.quizmaster.domain.agent.impl.DataSyncAgentImpl
import com.classroom.quizmaster.data.remote.FirestoreSyncService
import com.classroom.quizmaster.lan.LiveSessionLanFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import java.util.UUID

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val database = QuizMasterDatabase.build(appContext)
    private val lanFactory = LiveSessionLanFactory()

    private val blueprintStore = BlueprintLocalStore(appContext, json)
    val blueprintLocalData: BlueprintLocalDataSource = BlueprintLocalDataSource(blueprintStore, json)
    private val firestore by lazy { Firebase.firestore }
    private val firestoreSyncService: FirestoreSyncService by lazy { FirestoreSyncService(firestore, json) }
    private val dataSyncAgentInternal: DataSyncAgent by lazy {
        DataSyncAgentImpl(blueprintLocalData, firestoreSyncService)
    }
    val dataSyncAgent: DataSyncAgent
        get() = dataSyncAgentInternal

    val moduleRepository: ModuleRepository = ModuleRepositoryImpl(database.moduleDao(), json)
    val attemptRepository: AttemptRepository = AttemptRepositoryImpl(database.attemptDao(), json)
    val assignmentRepository: AssignmentRepository = AssignmentRepositoryImpl(database.assignmentDao(), json)
    val accountRepository: AccountRepository = AccountRepositoryImpl(database.accountDao())
    val classroomRepository: ClassroomRepository = ClassroomRepositoryImpl(database.classroomDao(), json)

    val moduleBuilderAgent = ModuleBuilderAgentImpl(moduleRepository)
    val assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)
    val lessonAgent = LessonAgentImpl(moduleRepository)
    val liveSessionAgent = LiveSessionAgentImpl()
    val assignmentAgent = AssignmentAgentImpl(assignmentRepository)
    val scoringAnalyticsAgent = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
    val reportExportAgent = ReportExportAgentImpl(appContext)
    val itemBankAgent = ItemBankAgentImpl()
    val gamificationAgent = GamificationAgentImpl()
    val catalogRepository: CatalogRepository = CatalogRepositoryImpl(moduleRepository, assignmentRepository, gamificationAgent)
    val syncAgent = FirebaseSyncAgent(moduleRepository, json)
    val authAgent = AuthAgentImpl(accountRepository)
    val classroomAgent = ClassroomAgentImpl(classroomRepository, moduleRepository)

    init {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
        dataSyncAgentInternal.start()
        ensureDefaultAdmin()
    }

    private fun ensureDefaultAdmin() = runBlocking {
        val existing = accountRepository.findByEmail(DEFAULT_ADMIN_EMAIL)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val account = UserAccount(
                id = UUID.randomUUID().toString(),
                email = DEFAULT_ADMIN_EMAIL,
                displayName = "Administrator",
                role = UserRole.Admin,
                status = AccountStatus.Active,
                hashedPassword = PasswordHasher.hash("admin123"),
                createdAt = now,
                approvedAt = now,
                approvedBy = DEFAULT_ADMIN_EMAIL,
                lastLoginAt = null
            )
            accountRepository.create(account)
        }
    }

    companion object {
        private const val DEFAULT_ADMIN_EMAIL = "admin@classroom.local"
    }
}
