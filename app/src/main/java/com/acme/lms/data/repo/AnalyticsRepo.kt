package com.acme.lms.data.repo

import com.acme.lms.data.model.Attempt

interface AnalyticsRepo {
    fun learningGain(pre: Attempt, post: Attempt): Map<String, Float>
}
