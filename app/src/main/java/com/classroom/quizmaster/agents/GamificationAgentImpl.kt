package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.ClassReport
import java.util.concurrent.ConcurrentHashMap

class GamificationAgentImpl : GamificationAgent {
    private val badgeMap = ConcurrentHashMap<String, MutableMap<String, Badge>>()

    override fun onReportsAvailable(report: ClassReport) {
        val improvements = report.attempts.mapNotNull { summary ->
            val pre = summary.prePercent ?: return@mapNotNull null
            val post = summary.postPercent ?: return@mapNotNull null
            summary.student.id to (post - pre)
        }
        val topImprover = improvements.maxByOrNull { it.second }
        if (topImprover != null && topImprover.second > 5) {
            award(topImprover.first, Badge(
                id = "top-improver",
                title = "Top Improver",
                description = "Pinakamalaking pag-angat ng marka"
            ))
        }
        report.attempts.forEach { summary ->
            val post = summary.postPercent ?: return@forEach
            if (post >= 90) {
                award(summary.student.id, Badge(
                    id = "star-of-the-day",
                    title = "Star ng Araw",
                    description = "Post-test score 90%+"
                ))
            }
        }
    }

    override fun unlocksFor(studentId: String): List<Badge> =
        badgeMap[studentId]?.values?.toList() ?: emptyList()

    private fun award(studentId: String, badge: Badge) {
        val studentBadges = badgeMap.getOrPut(studentId) { ConcurrentHashMap() }
        studentBadges[badge.id] = badge
    }
}
