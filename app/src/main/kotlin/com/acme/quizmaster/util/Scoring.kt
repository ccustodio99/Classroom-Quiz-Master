package com.acme.quizmaster.util

import com.acme.quizmaster.domain.AnswerPayload
import com.acme.quizmaster.domain.Assessment
import com.acme.quizmaster.domain.ItemType
import com.acme.quizmaster.domain.ObjectiveScore

fun scoreResponses(assessment: Assessment, answers: List<AnswerPayload>): Pair<Double, Map<String, ObjectiveScore>> {
    val answerMap = answers.associateBy { it.itemId }
    var total = 0.0
    val breakdown = mutableMapOf<String, ObjectiveScore>()

    assessment.items.forEach { item ->
        val response = answerMap[item.id]?.response
        val correct = when (item.type) {
            ItemType.MULTIPLE_CHOICE, ItemType.TRUE_FALSE -> item.answer.equals(response, ignoreCase = true)
            ItemType.NUMERIC -> response?.toDoubleOrNull()?.let { kotlin.math.abs(it - item.answer.toDouble()) <= item.tolerance } ?: false
            ItemType.MATCHING -> response?.split("|")?.map { it.trim() }?.let { provided ->
                val expected = item.matchingPairs.joinToString("|") { pair -> "${pair.prompt}:${pair.answer}" }
                provided.sorted() == expected.split("|").sorted()
            } ?: false
        }
        val objectiveScore = breakdown.getOrPut(item.objective) { ObjectiveScore(item.objective, 0, 0) }
        val updated = objectiveScore.copy(
            correct = objectiveScore.correct + if (correct) 1 else 0,
            total = objectiveScore.total + 1
        )
        breakdown[item.objective] = updated
        if (correct) total += 1
    }

    return total to breakdown
}
