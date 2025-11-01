package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem
import kotlin.math.abs
import kotlin.math.max

class ModuleBuilderAgentImpl(
    private val repository: ModuleRepository
) : ModuleBuilderAgent {
    override suspend fun createOrUpdate(module: Module): Result<Unit> {
        val violations = validate(module)
        return if (violations.isNotEmpty()) {
            Result.failure(IllegalArgumentException(violations.joinToString { it.message }))
        } else {
            try {
                repository.upsert(module)
                Result.success(Unit)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }

    override fun validate(module: Module): List<Violation> {
        val issues = mutableListOf<Violation>()
        if (module.topic.isBlank()) {
            issues += Violation("topic", "Topic is required")
        }
        if (module.objectives.isEmpty()) {
            issues += Violation("objectives", "At least one learning objective is required")
        }
        val preCount = module.preTest.items.size
        val postCount = module.postTest.items.size
        if (preCount != postCount) {
            issues += Violation("assessments", "Pre and post test must have the same number of items for parallel forms")
        } else {
            val mirroredItems = countMirroredItems(module.preTest.items, module.postTest.items)
            if (mirroredItems > 0) {
                issues += Violation(
                    "assessments",
                    "Post-test must vary from the pre-test; $mirroredItems item(s) are identical."
                )
            }
        }
        val referencedObjectives = (module.preTest.items + module.postTest.items).map { it.objective }.toSet()
        val missingObjectives = module.objectives.filterNot { it in referencedObjectives }
        if (missingObjectives.isNotEmpty()) {
            issues += Violation(
                "objectives",
                "Some objectives are not assessed: ${missingObjectives.joinToString()}"
            )
        }
        val miniChecks = module.lesson.slides.mapNotNull { it.miniCheck }
        if (miniChecks.isEmpty()) {
            issues += Violation("lesson", "Add at least one slide with a mini check prompt to reinforce objectives")
        }
        val lessonObjectives = miniChecks.flatMap { miniCheck ->
            miniCheck.objectives.map { it.trim() }.filter { it.isNotEmpty() }
        }.toSet()
        val uncoveredObjectives = module.objectives.filterNot { objective ->
            objective in lessonObjectives
        }
        if (uncoveredObjectives.isNotEmpty()) {
            issues += Violation(
                "lesson",
                "Mini checks should tag objectives they reinforce: ${uncoveredObjectives.joinToString()}"
            )
        }
        val interactive = module.lesson.interactiveActivities
        if (interactive.isEmpty()) {
            issues += Violation("interactive", "Add at least one interactive activity to keep learners engaged")
        }
        val hasScored = interactive.any { it.isScored }
        if (!hasScored) {
            issues += Violation("interactive", "Include at least one scored interactive activity for practice")
        }
        val hasReflective = interactive.any { !it.isScored }
        if (!hasReflective) {
            issues += Violation("interactive", "Include at least one reflection activity for discussion")
        }
        return issues
    }
}

private fun countMirroredItems(preItems: List<Item>, postItems: List<Item>): Int {
    val remaining = postItems.toMutableList()
    var mirrored = 0
    preItems.forEach { preItem ->
        val matchIndex = remaining.indexOfFirst { postItem -> areItemsEquivalent(preItem, postItem) }
        if (matchIndex >= 0) {
            mirrored += 1
            remaining.removeAt(matchIndex)
        }
    }
    return mirrored
}

private fun areItemsEquivalent(first: Item, second: Item): Boolean {
    if (first::class != second::class) return false
    return when (first) {
        is MultipleChoiceItem -> {
            val other = second as MultipleChoiceItem
            first.prompt.equals(other.prompt, ignoreCase = true) &&
                first.choices.map { it.trim() } == other.choices.map { it.trim() } &&
                first.correctIndex == other.correctIndex
        }
        is TrueFalseItem -> {
            val other = second as TrueFalseItem
            first.prompt.equals(other.prompt, ignoreCase = true) && first.answer == other.answer
        }
        is NumericItem -> {
            val other = second as NumericItem
            first.prompt.equals(other.prompt, ignoreCase = true) &&
                abs(first.answer - other.answer) <= max(first.tolerance, other.tolerance)
        }
        is MatchingItem -> {
            val other = second as MatchingItem
            first.prompt.equals(other.prompt, ignoreCase = true) && first.pairs == other.pairs
        }
    }
}
