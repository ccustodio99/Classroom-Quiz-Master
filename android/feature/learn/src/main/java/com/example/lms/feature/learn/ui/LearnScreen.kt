package com.example.lms.feature.learn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LearnRoute(
    modifier: Modifier = Modifier,
    onEvent: (() -> Unit)? = null,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    LearnScreen(
        modifier = modifier,
        title = viewModel.title,
        onEvent = onEvent,
    )
}

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
    title: String,
    onEvent: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title)
        Button(onClick = { onEvent?.invoke() }) {
            Text(text = "Explore")
        }
    }
}
