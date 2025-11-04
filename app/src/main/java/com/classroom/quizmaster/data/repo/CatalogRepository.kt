package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.ActivityTimeline
import com.classroom.quizmaster.domain.model.CourseSummary
import com.classroom.quizmaster.domain.model.HomeFeedItem
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCourses(): Flow<List<CourseSummary>>
    fun observeHomeFeed(userId: String): Flow<List<HomeFeedItem>>
    fun observeActivityTimeline(userId: String): Flow<ActivityTimeline>
}

