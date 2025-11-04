package com.example.lms.core.network.firebase

import com.example.lms.core.model.Class
import com.example.lms.core.model.LmsResult
import com.example.lms.core.sync.ClassRemoteDataSource

class FirebaseClassRemoteDataSource(
    private val dataSource: FirebaseClassDataSource,
) : ClassRemoteDataSource {
    override suspend fun fetchClasses(org: String): LmsResult<List<Class>> = dataSource.fetchClasses(org)
}

