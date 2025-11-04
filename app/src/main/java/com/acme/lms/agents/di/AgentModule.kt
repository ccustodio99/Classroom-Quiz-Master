package com.acme.lms.agents.di

import com.acme.lms.agents.AssessmentAgent
import com.acme.lms.agents.ClassroomAgent
import com.acme.lms.agents.ClassworkAgent
import com.acme.lms.agents.DataSyncAgent
import com.acme.lms.agents.LiveSessionAgent
import com.acme.lms.agents.PresenceAgent
import com.acme.lms.agents.ReportExportAgent
import com.acme.lms.agents.ScoringAnalyticsAgent
import com.acme.lms.agents.impl.AssessmentAgentImpl
import com.acme.lms.agents.impl.ClassroomAgentImpl
import com.acme.lms.agents.impl.ClassworkAgentImpl
import com.acme.lms.agents.impl.DataSyncAgentImpl
import com.acme.lms.agents.impl.LiveSessionAgentImpl
import com.acme.lms.agents.impl.PresenceAgentImpl
import com.acme.lms.agents.impl.ReportExportAgentImpl
import com.acme.lms.agents.impl.ScoringAnalyticsAgentImpl
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
