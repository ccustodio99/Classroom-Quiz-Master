package com.classroom.quizmaster.ui.navigation

sealed class QuizDestination(val route: String) {
    data object Auth : QuizDestination("auth")
    data object Splash : QuizDestination("splash")
    data object TeacherHome : QuizDestination("teacher/home")
    data object TeacherCreateQuiz : QuizDestination("teacher/createQuiz")
    data object TeacherLobby : QuizDestination("teacher/lobby")
    data object TeacherHostLive : QuizDestination("teacher/hostLive")
    data object TeacherReports : QuizDestination("teacher/reports")
    data object StudentEntry : QuizDestination("student/entry")
    data object StudentJoinLan : QuizDestination("student/joinLan")
    data object StudentPlay : QuizDestination("student/play")
    data object StudentEnd : QuizDestination("student/end")
}
