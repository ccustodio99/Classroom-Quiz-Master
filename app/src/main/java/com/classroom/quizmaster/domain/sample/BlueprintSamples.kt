package com.classroom.quizmaster.domain.sample

import com.classroom.quizmaster.domain.model.PersonaBlueprint
import com.classroom.quizmaster.domain.model.PersonaType

object BlueprintSamples {
    val personas: List<PersonaBlueprint> = listOf(
        PersonaBlueprint(
            type = PersonaType.Learner,
            highlights = listOf(
                "Accesses microlearning lessons in less than two minutes after install",
                "Completes pre/post diagnostics even when offline",
                "Downloads certificates and keeps streaks active"
            ),
            frustrations = listOf(
                "Slow onboarding",
                "Unclear feedback after assessments"
            ),
            offlineMustHaves = listOf(
                "Cached lessons and assessments",
                "Results sync automatically when online"
            )
        ),
        PersonaBlueprint(
            type = PersonaType.Instructor,
            highlights = listOf(
                "Builds classroom-centric modules with pre/post flow",
                "Runs LAN-backed live sessions with leaderboards",
                "Reviews mastery analytics per objective"
            ),
            frustrations = listOf(
                "Manual assignment reminders",
                "Managing multiple classrooms at once"
            ),
            offlineMustHaves = listOf(
                "Room persistence for modules and attempts",
                "Retry queue for submissions"
            )
        ),
        PersonaBlueprint(
            type = PersonaType.Admin,
            highlights = listOf(
                "Configures org branding and catalogs",
                "Monitors sync health across classrooms",
                "Audits privacy compliance via Firebase security rules"
            ),
            frustrations = listOf(
                "Delayed approvals for new instructors"
            ),
            offlineMustHaves = listOf(
                "Role-based caching of settings",
                "Audit logs that upload automatically once connected"
            )
        )
    )
}

