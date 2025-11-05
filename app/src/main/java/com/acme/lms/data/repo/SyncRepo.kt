package com.acme.lms.data.repo

interface SyncRepo {
    suspend fun sync()
}
