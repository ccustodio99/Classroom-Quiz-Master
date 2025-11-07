package com.classroom.quizmaster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.classroom.quizmaster.ui.navigation.QuizMasterNavGraph
import com.classroom.quizmaster.ui.theme.QuizMasterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            QuizMasterTheme {
                Surface(modifier = Modifier) {
                    QuizMasterNavGraph()
                }
            }
        }
    }
}
