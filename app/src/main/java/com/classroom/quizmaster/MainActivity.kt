package com.classroom.quizmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.classroom.quizmaster.ui.QuizMasterApp
import com.classroom.quizmaster.ui.theme.QuizMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as QuizMasterApplication
        setContent {
            CompositionLocalProvider(LocalAppContainer provides app.container) {
                QuizMasterTheme {
                    QuizMasterApp()
                }
            }
        }
    }
}
