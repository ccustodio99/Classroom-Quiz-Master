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
            val mirroredItems = module.preTest.items.zip(module.postTest.items)
                .count { (pre, post) -> areItemsEquivalent(pre, post) }
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
        val lessonObjectives = module.lesson.slides.flatMap { slide ->
            slide.miniCheck?.let { listOf(it.prompt) } ?: emptyList()
        }
        if (lessonObjectives.isEmpty()) {
            issues += Violation("lesson", "Add at least one slide with a mini check prompt to reinforce objectives")
        }
        return issues
    }
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
