package com.classroom.quizmaster.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classroom.quizmaster.ui.auth.AuthRoute
import com.classroom.quizmaster.ui.student.entry.StudentEntryScreen
import com.classroom.quizmaster.ui.student.join.JoinLanRoute
import com.classroom.quizmaster.ui.student.play.StudentPlayRoute
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeRoute
import com.classroom.quizmaster.ui.teacher.live.HostLiveScreen
import com.classroom.quizmaster.ui.teacher.live.LiveHostViewModel
import com.classroom.quizmaster.ui.teacher.reports.ReportsScreen
import com.classroom.quizmaster.ui.teacher.quiz.CreateQuizRoute
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuizMasterNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = QuizDestination.Auth.route
    ) {
        composable(QuizDestination.Auth.route) {
            AuthRoute(
                onTeacherAuthenticated = {
                    navController.navigate(QuizDestination.TeacherHome.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onStudentContinue = {
                    navController.navigate(QuizDestination.StudentEntry.route)
                }
            )
        }
        composable(QuizDestination.TeacherHome.route) {
            TeacherHomeRoute(
                onCreateQuiz = { navController.navigate(QuizDestination.TeacherCreateQuiz.route) },
                onLaunchLive = { quiz ->
                    navController.navigate("${QuizDestination.TeacherLobby.route}?quizId=${quiz.id}")
                },
                onReports = { navController.navigate(QuizDestination.TeacherReports.route) },
                onLogout = {
                    navController.navigate(QuizDestination.Auth.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(QuizDestination.TeacherCreateQuiz.route) {
            CreateQuizRoute(onDone = { navController.popBackStack() })
        }
        composable(QuizDestination.TeacherReports.route) {
            ReportsScreen(
                averages = listOf(72f, 81f, 65f),
                commonlyMissed = listOf(
                    "Fractions" to 40f,
                    "Orders of operations" to 30f
                )
            )
        }
        composable(QuizDestination.StudentEntry.route) {
            StudentEntryScreen(
                onJoinLan = { navController.navigate(QuizDestination.StudentJoinLan.route) },
                onJoinCode = { /* TODO */ }
            )
        }
        composable(QuizDestination.StudentJoinLan.route) {
            JoinLanRoute(onJoined = { navController.navigate(QuizDestination.StudentPlay.route) })
        }
        composable(QuizDestination.StudentPlay.route) {
            StudentPlayRoute()
        }
        composable(
            route = "${QuizDestination.TeacherLobby.route}?quizId={quizId}",
            arguments = listOf(navArgument("quizId") { nullable = true })
        ) { entry ->
            val quizId = entry.arguments?.getString("quizId").orEmpty()
            val viewModel: LiveHostViewModel = hiltViewModel()
            LaunchedEffect(quizId) {
                if (quizId.isNotBlank()) {
                    viewModel.startSession(quizId, classroomId = "demo-class", nickname = "Host")
                }
            }
            val state = viewModel.uiState.collectAsState()
            HostLiveScreen(
                session = state.value.session,
                participants = state.value.participants,
                lanMeta = state.value.lanMeta,
                onReveal = {},
                onNext = {},
                onKick = viewModel::kick,
                onEnd = {
                    viewModel.endSession()
                    navController.popBackStack()
                }
            )
        }
    }
}
