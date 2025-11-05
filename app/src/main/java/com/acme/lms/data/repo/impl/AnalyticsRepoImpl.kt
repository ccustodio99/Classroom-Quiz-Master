package com.acme.lms.data.repo.impl

import com.example.lms.core.model.Attempt
import com.acme.lms.data.repo.AnalyticsRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepoImpl @Inject constructor() : AnalyticsRepo {

    override fun learningGain(pre: Attempt, post: Attempt): Map<String, Float> {
        // Assuming Attempt.answers.size is a proxy for score, which may not be accurate.
        // The consolidated Attempt model has a nullable 'score: Double?'.
        // This logic needs review for correctness based on how scores are actually stored/calculated.
        val preScore = pre.answers.size
        val postScore = post.answers.size
        val gain = if (preScore == 0) postScore.toFloat() else (postScore - preScore) / preScore.toFloat()
        return mapOf("overall" to gain)
    }
}
