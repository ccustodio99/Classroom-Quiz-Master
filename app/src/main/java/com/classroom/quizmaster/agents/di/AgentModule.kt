package com.classroom.quizmaster.agents.di

import com.classroom.quizmaster.agents.AssessmentAgent
import com.classroom.quizmaster.agents.ClassroomAgent
import com.classroom.quizmaster.agents.ClassworkAgent
import com.classroom.quizmaster.agents.DataSyncAgent
import com.classroom.quizmaster.agents.LiveSessionAgent
import com.classroom.quizmaster.agents.PresenceAgent
import com.classroom.quizmaster.agents.ReportExportAgent
import com.classroom.quizmaster.agents.ScoringAnalyticsAgent
import com.classroom.quizmaster.agents.impl.AssessmentAgentImpl
import com.classroom.quizmaster.agents.impl.ClassroomAgentImpl
import com.classroom.quizmaster.agents.impl.ClassworkAgentImpl
import com.classroom.quizmaster.agents.impl.DataSyncAgentImpl
import com.classroom.quizmaster.agents.impl.LiveSessionAgentImpl
import com.classroom.quizmaster.agents.impl.PresenceAgentImpl
import com.classroom.quizmaster.agents.impl.ReportExportAgentImpl
import com.classroom.quizmaster.agents.impl.ScoringAnalyticsAgentImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {

    @Binds
    @Singleton
    abstract fun bindClassroomAgent(impl: ClassroomAgentImpl): ClassroomAgent

    @Binds
    @Singleton
    abstract fun bindClassworkAgent(impl: ClassworkAgentImpl): ClassworkAgent

    @Binds
    @Singleton
    abstract fun bindAssessmentAgent(impl: AssessmentAgentImpl): AssessmentAgent

    @Binds
    @Singleton
    abstract fun bindLiveSessionAgent(impl: LiveSessionAgentImpl): LiveSessionAgent

    @Binds
    @Singleton
    abstract fun bindScoringAnalyticsAgent(impl: ScoringAnalyticsAgentImpl): ScoringAnalyticsAgent

    @Binds
    @Singleton
    abstract fun bindReportExportAgent(impl: ReportExportAgentImpl): ReportExportAgent

    @Binds
    @Singleton
    abstract fun bindDataSyncAgent(impl: DataSyncAgentImpl): DataSyncAgent

    @Binds
    @Singleton
    abstract fun bindPresenceAgent(impl: PresenceAgentImpl): PresenceAgent
}
