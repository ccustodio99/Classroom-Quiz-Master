package com.classroom.quizmaster.ui.student.classrooms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun JoinClassroomRoute(
    onJoin: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    viewModel: JoinClassroomViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    JoinClassroomScreen(
        onJoin = { joinCode ->
            viewModel.joinClassroom(joinCode, onJoin)
        },
        onBack = onBack,
        onSearch = onSearch
    )
}
