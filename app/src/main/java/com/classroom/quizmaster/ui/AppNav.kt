package com.classroom.quizmaster.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classroom.quizmaster.ui.auth.AuthRoute
import com.classroom.quizmaster.ui.student.end.StudentEndRoute
import com.classroom.quizmaster.ui.student.entry.EntryTab
import com.classroom.quizmaster.ui.student.entry.StudentEntryRoute
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyRoute
import com.classroom.quizmaster.ui.student.play.StudentPlayRoute
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsRoute
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeRoute
import com.classroom.quizmaster.ui.teacher.host.HostLiveRoute
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyRoute
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorRoute
import com.classroom.quizmaster.ui.teacher.reports.ReportsRoute

sealed class AppRoute(val route: String) {
    data object Auth : AppRoute("auth")
    data object TeacherHome : AppRoute("teacher/home")
    data object TeacherQuizCreate : AppRoute("teacher/quiz/create")
    data object TeacherQuizEdit : AppRoute("teacher/quiz/edit/{quizId}") {
        fun build(id: String) = "teacher/quiz/edit/$id"
    }
    data object TeacherLaunch : AppRoute("teacher/launch")
    data object TeacherHost : AppRoute("teacher/host")
    data object TeacherReports : AppRoute("teacher/reports")
    data object TeacherAssignments : AppRoute("teacher/assignments")

    data object StudentEntry : AppRoute("student/entry")
    data object StudentJoinLan : AppRoute("student/joinLan")
    data object StudentJoinCode : AppRoute("student/joinCode")
    data object StudentLobby : AppRoute("student/lobby")
    data object StudentPlay : AppRoute("student/play")
    data object StudentEnd : AppRoute("student/end")
}

@Composable
fun AppNav(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoute.Auth.route,
        modifier = modifier
    ) {
        composable(AppRoute.Auth.route) {
            AuthRoute(
                onTeacherAuthenticated = {
                    navController.navigate(AppRoute.TeacherHome.route) {
                        popUpTo(AppRoute.Auth.route) { inclusive = true }
                    }
                },
                onStudentContinue = { _ ->
                    navController.navigate(AppRoute.StudentEntry.route)
                }
            )
        }
        composable(AppRoute.TeacherHome.route) {
            TeacherHomeRoute(
                onCreateQuiz = { navController.navigate(AppRoute.TeacherQuizCreate.route) },
                onLaunchLive = { navController.navigate(AppRoute.TeacherLaunch.route) },
                onAssignments = { navController.navigate(AppRoute.TeacherAssignments.route) },
                onReports = { navController.navigate(AppRoute.TeacherReports.route) }
            )
        }
        composable(AppRoute.TeacherQuizCreate.route) {
            QuizEditorRoute(
                onSaved = { navController.popBackStack() },
                onDiscarded = { navController.popBackStack() }
            )
        }
        composable(
            route = AppRoute.TeacherQuizEdit.route,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) {
            QuizEditorRoute(
                onSaved = { navController.popBackStack() },
                onDiscarded = { navController.popBackStack() }
            )
        }
        composable(AppRoute.TeacherLaunch.route) {
            LaunchLobbyRoute(
                onHostStarted = { navController.navigate(AppRoute.TeacherHost.route) },
                onHostEnded = { navController.popBackStack() }
            )
        }
        composable(AppRoute.TeacherHost.route) {
            HostLiveRoute(
                onSessionEnded = {
                    navController.navigate(AppRoute.TeacherHome.route) {
                        popUpTo(AppRoute.TeacherHome.route) { inclusive = false }
                    }
                }
            )
        }
        composable(AppRoute.TeacherReports.route) {
            ReportsRoute()
        }
        composable(AppRoute.TeacherAssignments.route) {
            AssignmentsRoute()
        }
        composable(AppRoute.StudentEntry.route) {
            StudentEntryRoute(
                onJoined = { navController.navigate(AppRoute.StudentLobby.route) }
            )
        }
        composable(AppRoute.StudentJoinLan.route) {
            StudentEntryRoute(
                initialTab = EntryTab.Lan,
                onJoined = { navController.navigate(AppRoute.StudentLobby.route) }
            )
        }
        composable(AppRoute.StudentJoinCode.route) {
            StudentEntryRoute(
                initialTab = EntryTab.Code,
                onJoined = { navController.navigate(AppRoute.StudentLobby.route) }
            )
        }
        composable(AppRoute.StudentLobby.route) {
            StudentLobbyRoute(
                onReady = { navController.navigate(AppRoute.StudentPlay.route) }
            )
        }
        composable(AppRoute.StudentPlay.route) {
            StudentPlayRoute()
        }
        composable(AppRoute.StudentEnd.route) {
            StudentEndRoute(
                onPlayAgain = { navController.popBackStack(AppRoute.StudentEntry.route, false) },
                onLeave = {
                    navController.navigate(AppRoute.Auth.route) {
                        popUpTo(AppRoute.Auth.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
