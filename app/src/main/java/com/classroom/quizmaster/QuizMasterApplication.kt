package com.classroom.quizmaster

import android.app.Application

class QuizMasterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
