package com.acme.lms.data.repo

import com.acme.lms.data.model.Attempt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepo @Inject constructor() {

    fun learningGain(pre: Attempt, post: Attempt): Map<String, Float> {
        val preScore = pre.answers.size
        val postScore = post.answers.size
        val gain = if (preScore == 0) postScore.toFloat() else (postScore - preScore) / preScore.toFloat()
        return mapOf("overall" to gain)
    }
}
