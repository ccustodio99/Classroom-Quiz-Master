package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Roster

data class ClassReportData(
    val classInfo: Class,
    val roster: List<Roster>,
    val classwork: List<Classwork>
)

data class LearningGainRow(
    val userId: String,
    val preScore: Double,
    val postScore: Double,
    val gain: Double
)

interface ReportRepo {
    suspend fun getClassReportData(classId: String): ClassReportData
    suspend fun getLearningGainRows(classId: String): List<LearningGainRow>
}
