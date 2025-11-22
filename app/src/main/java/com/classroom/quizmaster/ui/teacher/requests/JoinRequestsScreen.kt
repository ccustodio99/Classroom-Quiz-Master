package com.classroom.quizmaster.ui.teacher.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classroom.quizmaster.domain.model.JoinRequestStatus
import com.classroom.quizmaster.ui.components.EmptyState
import com.classroom.quizmaster.ui.components.SimpleTopBar

@Composable
fun JoinRequestsRoute(
    onBack: () -> Unit,
    viewModel: JoinRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    JoinRequestsScreen(
        uiState = uiState,
        onBack = onBack,
        onApprove = viewModel::approveRequest,
        onDeny = viewModel::denyRequest
    )
}

@Composable
fun JoinRequestsScreen(
    uiState: JoinRequestsUiState,
    onBack: () -> Unit,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit
) {
    Scaffold(
        topBar = {
            SimpleTopBar(
                title = "Join Requests",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.joinRequests.isEmpty()) {
            EmptyState(
                title = "No join requests",
                message = "Students who try to join your classrooms will appear here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.joinRequests, key = { it.id }) { request ->
                    JoinRequestCard(request = request, onApprove = onApprove, onDeny = onDeny)
                }
            }
        }
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequestUi,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit
) {
    val isPending = request.status == JoinRequestStatus.PENDING
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Student: ${request.studentName}", style = MaterialTheme.typography.titleMedium)
            Text("Classroom: ${request.classroomName}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = when (request.status) {
                    JoinRequestStatus.PENDING -> "Awaiting approval"
                    JoinRequestStatus.APPROVED -> "Approved"
                    JoinRequestStatus.DENIED -> "Denied"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApprove(request.id) }, enabled = isPending) {
                    Text("Approve")
                }
                Button(onClick = { onDeny(request.id) }, enabled = isPending) {
                    Text("Deny")
                }
            }
        }
    }
}
