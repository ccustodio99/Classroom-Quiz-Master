package com.acme.lms.data.repo

import com.example.lms.core.model.Attempt

interface AnalyticsRepo {
    fun learningGain(pre: Attempt, post: Attempt): Map<String, Float>
}
