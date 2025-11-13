package com.classroom.quizmaster.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@Preview(
    name = "Phone - Light",
    showBackground = true,
    widthDp = 360
)
@Preview(
    name = "Tablet - Dark Large",
    showBackground = true,
    widthDp = 900,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 1.6f
)
annotation class QuizPreviews
