package com.classroom.quizmaster

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class QuizMasterTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, QuizMasterApp::class.java.name, context)
    }
}
