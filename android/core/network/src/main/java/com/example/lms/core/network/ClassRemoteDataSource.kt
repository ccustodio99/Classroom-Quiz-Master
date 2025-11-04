package com.example.lms.core.network

import com.example.lms.core.model.Class
import com.example.lms.core.model.LmsResult

interface ClassRemoteDataSource {
    suspend fun fetchClasses(org: String): LmsResult<List<Class>>
}
