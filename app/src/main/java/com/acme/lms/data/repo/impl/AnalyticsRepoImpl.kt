package com.acme.lms.data.repo.impl

import com.acme.lms.data.model.Attempt
import com.acme.lms.data.repo.AnalyticsRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepoImpl @Inject constructor() : AnalyticsRepo {

    override fun learningGain(pre: Attempt, post: Attempt): Map<String, Float> {
        // Assuming Attempt.answers.size is a proxy for score, which may not be accurate.
        // The consolidated Attempt model has a nullable 'score: Double?'.
        // This logic needs review for correctness based on how scores are actually stored/calculated.
        val preScore = pre.score ?: pre.answers.size.toDouble()
        val postScore = post.score ?: post.answers.size.toDouble()
        val gain = if (preScore == 0.0) postScore.toFloat() else ((postScore - preScore) / preScore).toFloat()
        return mapOf("overall" to gain)
    }
}
