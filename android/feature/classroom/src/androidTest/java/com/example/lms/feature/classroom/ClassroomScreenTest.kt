package com.example.lms.feature.classroom

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.lms.feature.classroom.ui.ClassroomScreen
import org.junit.Rule
import org.junit.Test

class ClassroomScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitle() {
        composeRule.setContent { ClassroomScreen(title = "Classroom") }
        composeRule.onNodeWithText("Classroom").assertIsDisplayed()
    }
}

