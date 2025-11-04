package com.example.lms.feature.classroom

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.classroom.ui.ClassroomScreen
import com.example.lms.feature.classroom.ui.ClassroomUiState
import com.example.lms.feature.classroom.ui.ClassworkCard
import com.example.lms.feature.classroom.ui.GradeRow
import com.example.lms.feature.classroom.ui.StreamPost
import org.junit.Rule
import org.junit.Test

class ClassroomScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersStreamTab() {
        val state = ClassroomUiState(
            className = "Demo Class",
            section = "Section A",
            joinCode = "CODE",
            stream = listOf(StreamPost("Teacher", "Welcome", "Now")),
            classwork = listOf(ClassworkCard("Live Quiz", "Live", "Today")),
            teachers = listOf("Teacher"),
            learners = listOf("Student"),
            grades = listOf(GradeRow("Assessments", "90%", "50%")),
        )
        composeRule.setContent {
            ClassroomScreen(
                state = state,
                onOpenLive = {},
                onViewGrades = {},
            )
        }
        composeRule.onNodeWithText("Demo Class • Section A").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}

