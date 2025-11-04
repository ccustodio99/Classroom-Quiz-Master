package com.example.lms.feature.classroom.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class StreamPost(
    val author: String,
    val message: String,
    val timestamp: String,
)

data class ClassworkCard(
    val title: String,
    val type: String,
    val due: String,
)

data class GradeRow(
    val category: String,
    val score: String,
    val weight: String,
)

data class ClassroomUiState(
    val className: String,
    val section: String,
    val joinCode: String,
    val stream: List<StreamPost>,
    val classwork: List<ClassworkCard>,
    val teachers: List<String>,
    val learners: List<String>,
    val grades: List<GradeRow>,
)

@HiltViewModel
class ClassroomViewModel @Inject constructor() : ViewModel() {
    val uiState = ClassroomUiState(
        className = "Biology Lab",
        section = "Period 3",
        joinCode = "BIO-3A7",
        stream = listOf(
            StreamPost("Ms. Rivera", "Tomorrow's field journal is now available in Classwork.", "7 min ago"),
            StreamPost("System", "Alex completed the Photosynthesis pretest.", "20 min ago"),
        ),
        classwork = listOf(
            ClassworkCard("Pretest: Chloroplast Tour", "Pretest", "Due today"),
            ClassworkCard("Lab Prep: Light Reaction Stations", "Material", "Pinned"),
            ClassworkCard("Live Quiz: Energy Transfer", "Live", "Opens in 1h"),
        ),
        teachers = listOf("Ms. Rivera", "Mr. Chan"),
        learners = listOf("Alex Kim", "Jordan Lee", "Priya Patel", "Taylor Diaz"),
        grades = listOf(
            GradeRow("Assessments", "92%", "50%"),
            GradeRow("Labs", "88%", "30%"),
            GradeRow("Participation", "100%", "20%"),
        ),
    )
}

