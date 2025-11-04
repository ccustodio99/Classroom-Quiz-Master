package com.classroom.quizmaster.ui.feature.classroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classroom.quizmaster.domain.model.Classwork

@Composable
fun ClassworkTab(classworkItems: List<Classwork>) {
    if (classworkItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No classwork assigned yet.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(classworkItems) { classwork ->
                ClassworkListItem(classwork = classwork)
            }
        }
    }
}

@Composable
fun ClassworkListItem(classwork: Classwork) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = classwork.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Type: ${classwork.type.name.lowercase()}")
            classwork.dueAt?.let {
                Text(text = "Due: ${java.util.Date(it)}")
            }
        }
    }
}
