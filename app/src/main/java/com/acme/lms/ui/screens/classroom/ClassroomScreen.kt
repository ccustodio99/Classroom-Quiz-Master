package com.acme.lms.ui.screens.classroom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.acme.lms.ui.viewmodel.ClassroomViewModel

@Composable
fun ClassroomScreen(
    navController: NavController,
    viewModel: ClassroomViewModel = hiltViewModel()
) {
    val roster by viewModel.roster.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Class roster")
        LazyColumn {
            items(roster) { member ->
                Text("${member.userId} (${member.role})")
            }
        }
    }
}
