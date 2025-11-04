package com.example.lms.core.network.firebase

import com.example.lms.core.network.ClassRemoteDataSource
import com.example.lms.core.model.LmsResult
import com.example.lms.core.model.Class

class FirebaseClassRemoteDataSource(
    private val dataSource: FirebaseClassDataSource,
) : ClassRemoteDataSource {
    override suspend fun fetchClasses(org: String): LmsResult<List<Class>> = dataSource.fetchClasses(org)
}

