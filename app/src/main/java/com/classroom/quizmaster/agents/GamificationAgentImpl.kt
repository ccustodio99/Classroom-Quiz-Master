package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.ClassReport
import java.util.concurrent.ConcurrentHashMap

class GamificationAgentImpl : GamificationAgent {
    private val badgeMap = ConcurrentHashMap<String, MutableList<Badge>>()

    override fun onReportsAvailable(report: ClassReport) {
        val improvements = report.attempts.mapNotNull { summary ->
            val pre = summary.prePercent ?: return@mapNotNull null
            val post = summary.postPercent ?: return@mapNotNull null
            summary.student.id to (post - pre)
        }
        val topImprover = improvements.maxByOrNull { it.second }
        badgeMap.clear()
        if (topImprover != null && topImprover.second > 5) {
            badgeMap.getOrPut(topImprover.first) { mutableListOf() } += Badge(
                id = "top-improver",
                title = "Top Improver",
                description = "Pinakamalaking pag-angat ng marka"
            )
        }
        report.attempts.forEach { summary ->
            val post = summary.postPercent ?: return@forEach
            if (post >= 90) {
                badgeMap.getOrPut(summary.student.id) { mutableListOf() } += Badge(
                    id = "star-of-the-day",
                    title = "Star ng Araw",
                    description = "Post-test score 90%+"
                )
            }
        }
    }

    override fun unlocksFor(studentId: String): List<Badge> = badgeMap[studentId]?.toList() ?: emptyList()
}
