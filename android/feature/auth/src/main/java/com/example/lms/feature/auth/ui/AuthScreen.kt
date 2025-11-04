package com.example.lms.feature.auth.ui

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
fun AuthRoute(
    modifier: Modifier = Modifier,
    onEvent: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    AuthScreen(
        modifier = modifier,
        title = viewModel.title,
        onEvent = onEvent,
    )
}

@Composable
fun AuthScreen(
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
