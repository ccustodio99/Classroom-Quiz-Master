package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Module

class ModuleBuilderAgentImpl(
    private val repository: ModuleRepository
) : ModuleBuilderAgent {
    override suspend fun createOrUpdate(module: Module): Result<Unit> {
        val violations = validate(module)
        return if (violations.isNotEmpty()) {
            Result.failure(IllegalArgumentException(violations.joinToString { it.message }))
        } else {
            runCatching { repository.upsert(module) }
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
