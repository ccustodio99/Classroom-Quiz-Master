package com.classroom.quizmaster.agents

class SyncAgentImpl : SyncAgent {
    override suspend fun pushModule(moduleId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun pullUpdates(): Result<Int> {
        return Result.success(0)
    }
}
