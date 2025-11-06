package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.Attempt

interface AnalyticsRepo {
    fun learningGain(pre: Attempt, post: Attempt): Map<String, Float>
}
