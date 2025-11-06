package com.classroom.quizmaster.data.repo.impl

import com.classroom.quizmaster.data.repo.SyncRepo
import kotlinx.coroutines.delay
import javax.inject.Inject

class SyncRepoImpl @Inject constructor() : SyncRepo {
    override suspend fun sync() {
        // Simulate network delay
        delay(2000)
    }
}
