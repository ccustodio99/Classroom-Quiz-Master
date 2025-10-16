package com.classroom.quizmaster

import android.content.Context
import com.classroom.quizmaster.agents.AssessmentAgent
import com.classroom.quizmaster.agents.AssessmentAgentImpl
import com.classroom.quizmaster.agents.AssignmentAgent
import com.classroom.quizmaster.agents.AssignmentAgentImpl
import com.classroom.quizmaster.agents.GamificationAgent
import com.classroom.quizmaster.agents.GamificationAgentImpl
import com.classroom.quizmaster.agents.ItemBankAgent
import com.classroom.quizmaster.agents.ItemBankAgentImpl
import com.classroom.quizmaster.agents.LessonAgent
import com.classroom.quizmaster.agents.LessonAgentImpl
import com.classroom.quizmaster.agents.LiveSessionAgent
import com.classroom.quizmaster.agents.LiveSessionAgentImpl
import com.classroom.quizmaster.agents.ModuleBuilderAgent
import com.classroom.quizmaster.agents.ModuleBuilderAgentImpl
import com.classroom.quizmaster.agents.ReportExportAgent
import com.classroom.quizmaster.agents.ReportExportAgentImpl
import com.classroom.quizmaster.agents.ScoringAnalyticsAgent
import com.classroom.quizmaster.agents.ScoringAnalyticsAgentImpl
import com.classroom.quizmaster.agents.SyncAgent
import com.classroom.quizmaster.agents.SyncAgentImpl
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.repo.AssignmentRepository
import com.classroom.quizmaster.data.repo.AssignmentRepositoryImpl
import com.classroom.quizmaster.data.repo.AttemptRepository
import com.classroom.quizmaster.data.repo.AttemptRepositoryImpl
import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.data.repo.ModuleRepositoryImpl
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private val database = QuizMasterDatabase.build(context, json)

    val moduleRepository: ModuleRepository = ModuleRepositoryImpl(database.moduleDao(), json)
    val attemptRepository: AttemptRepository = AttemptRepositoryImpl(database.attemptDao(), json)
    val assignmentRepository: AssignmentRepository = AssignmentRepositoryImpl(database.assignmentDao(), json)

    val moduleBuilderAgent: ModuleBuilderAgent = ModuleBuilderAgentImpl(moduleRepository)
    val assessmentAgent: AssessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)
    val lessonAgent: LessonAgent = LessonAgentImpl(moduleRepository)
    val liveSessionAgent: LiveSessionAgent = LiveSessionAgentImpl()
    val assignmentAgent: AssignmentAgent = AssignmentAgentImpl(assignmentRepository)
    val scoringAnalyticsAgent: ScoringAnalyticsAgent = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
    val reportExportAgent: ReportExportAgent = ReportExportAgentImpl(context)
    val itemBankAgent: ItemBankAgent = ItemBankAgentImpl()
    val gamificationAgent: GamificationAgent = GamificationAgentImpl()
    val syncAgent: SyncAgent = SyncAgentImpl()
}
