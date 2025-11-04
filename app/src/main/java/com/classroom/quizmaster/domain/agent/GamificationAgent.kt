package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.Badge

interface GamificationAgent {
    fun onReportsAvailable(report: ClassReport)
    fun unlocksFor(studentId: String): List<Badge>
}
