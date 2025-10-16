package com.acme.quizmaster

import com.acme.quizmaster.agents.ItemBankAgent
import com.acme.quizmaster.domain.Assessment
import com.acme.quizmaster.domain.FeedbackMode
import com.acme.quizmaster.domain.Item
import com.acme.quizmaster.domain.ItemType
import com.acme.quizmaster.domain.Lesson
import com.acme.quizmaster.domain.LessonSlide
import com.acme.quizmaster.domain.Module
import com.acme.quizmaster.domain.ModuleSettings
import com.acme.quizmaster.domain.Option

fun buildSampleModule(itemBankAgent: ItemBankAgent): Module {
    val items = listOf(
        Item(
            type = ItemType.MULTIPLE_CHOICE,
            stem = "What is the slope of a line passing through (0,0) and (2,4)?",
            objective = "LO1",
            options = listOf(
                Option("a", "1"),
                Option("b", "2"),
                Option("c", "4"),
                Option("d", "0.5")
            ),
            answer = "b",
            explanation = "Slope = rise/run = 4/2 = 2"
        ),
        Item(
            type = ItemType.TRUE_FALSE,
            stem = "The function f(x) = 3x + 1 is linear.",
            objective = "LO1",
            answer = "true",
            explanation = "Linear functions have the form ax + b"
        ),
        Item(
            type = ItemType.NUMERIC,
            stem = "Solve for x: 2x + 3 = 11",
            objective = "LO2",
            answer = "4",
            tolerance = 0.01,
            explanation = "2x = 8, x = 4"
        )
    )

    itemBankAgent.addItems(items)

    val preTest = Assessment(
        name = "Pre-Test",
        items = items
    )

    val postItems = items.map { item ->
        item.copy(id = java.util.UUID.randomUUID().toString())
    }

    val postTest = Assessment(
        name = "Post-Test",
        items = postItems
    )

    val lesson = Lesson(
        title = "Linear Functions",
        slides = listOf(
            LessonSlide("Concept", "Slope measures steepness", "LO1"),
            LessonSlide("Practice", "Solve linear equations", "LO2")
        )
    )

    return Module(
        topic = "Linear Functions",
        objectives = listOf("LO1", "LO2"),
        preTest = preTest,
        lesson = lesson,
        postTest = postTest,
        settings = ModuleSettings(
            leaderboardEnabled = true,
            allowRetakes = true,
            locale = "en-PH",
            feedbackMode = FeedbackMode.AFTER_SECTION
        )
    )
}
