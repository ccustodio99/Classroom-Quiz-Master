package com.classroom.quizmaster.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.classroom.quizmaster.ui.auth.AuthRoute
import com.classroom.quizmaster.ui.neutral.NeutralWelcomeScreen
import com.classroom.quizmaster.ui.neutral.NeutralWelcomeViewModel
import com.classroom.quizmaster.ui.neutral.OfflineDemoEvent
import com.classroom.quizmaster.ui.student.end.StudentEndRoute
import com.classroom.quizmaster.ui.student.entry.EntryTab
import com.classroom.quizmaster.ui.student.entry.StudentEntryRoute
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyRoute
import com.classroom.quizmaster.ui.student.play.StudentPlayRoute
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsRoute
import com.classroom.quizmaster.ui.teacher.classrooms.CreateClassroomRoute
import com.classroom.quizmaster.ui.teacher.classrooms.detail.TeacherClassroomDetailRoute
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeRoute
import com.classroom.quizmaster.ui.teacher.host.HostLiveRoute
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyRoute
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorRoute
import com.classroom.quizmaster.ui.teacher.reports.ReportsRoute
import com.classroom.quizmaster.ui.teacher.topics.detail.TeacherTopicDetailRoute

sealed class AppRoute(val route: String) {
    data object Welcome : AppRoute("neutral/welcome")
    data object Auth : AppRoute("auth")
    data object TeacherHome : AppRoute("teacher/home")
    data object TeacherClassroomCreate : AppRoute("teacher/classrooms/create")
    data object TeacherClassroomDetail : AppRoute("teacher/classrooms/{classroomId}") {
        fun build(classroomId: String) = "teacher/classrooms/$classroomId"
    }
    data object TeacherTopicDetail : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}") {
        fun build(classroomId: String, topicId: String) = "teacher/classrooms/$classroomId/topics/$topicId"
    }
    data object TeacherQuizCreate : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}/quizzes/create") {
        fun build(classroomId: String, topicId: String) = "teacher/classrooms/$classroomId/topics/$topicId/quizzes/create"
    }
    data object TeacherQuizEdit : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}/quizzes/{quizId}/edit") {
        fun build(classroomId: String, topicId: String, quizId: String) =
            "teacher/classrooms/$classroomId/topics/$topicId/quizzes/$quizId/edit"
    }
    data object TeacherLaunch : AppRoute("teacher/classrooms/{classroomId}/launch?topicId={topicId}&quizId={quizId}") {
        fun build(classroomId: String, topicId: String? = null, quizId: String? = null): String {
            val params = buildList<String> {
                topicId?.takeIf { it.isNotBlank() }?.let { add("topicId=$it") }
                quizId?.takeIf { it.isNotBlank() }?.let { add("quizId=$it") }
            }
            val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
            return "teacher/classrooms/$classroomId/launch$query"
        }
    }
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
fun AppNav(
    modifier: Modifier = Modifier,
    appState: QuizMasterAppState = rememberQuizMasterAppState()
) {
    val navController = appState.navController

    val studentBottomItems = remember {
        listOf(
            BottomNavItem(AppRoute.StudentEntry.route, Icons.Outlined.Home, "Join"),
            BottomNavItem(AppRoute.StudentLobby.route, Icons.Outlined.Groups, "Lobby"),
            BottomNavItem(AppRoute.StudentPlay.route, Icons.Outlined.EmojiEvents, "Play")
        )
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            appState.updateShellForRoute(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        navController.currentDestination?.let { appState.updateShellForRoute(it.route) }
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    QuizMasterRootScaffold(
        appState = appState,
        modifier = modifier,
        studentBottomItems = studentBottomItems,
        onStudentDestinationSelected = { route ->
            if (route != navController.currentDestination?.route) {
                appState.navigateToBottomRoute(route)
            }
        }
    ) { contentModifier ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Welcome.route,
            modifier = contentModifier
        ) {
            composable(AppRoute.Welcome.route) {
                val welcomeViewModel: NeutralWelcomeViewModel = hiltViewModel()
                val loading by welcomeViewModel.loading.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    welcomeViewModel.events.collect { event ->
                        when (event) {
                            OfflineDemoEvent.Success -> {
                                appState.showMessage("Offline demo enabled")
                                navController.navigate(AppRoute.TeacherHome.route) {
                                    launchSingleTop = true
                                    popUpTo(AppRoute.Welcome.route) { inclusive = false }
                                }
                            }
                            is OfflineDemoEvent.Error -> appState.showMessage(event.message)
                        }
                    }
                }

                NeutralWelcomeScreen(
                    onTeacherFlow = { navController.navigate(AppRoute.Auth.route) },
                    onStudentFlow = { navController.navigate(AppRoute.StudentEntry.route) },
                    onOfflineDemo = welcomeViewModel::enableOfflineDemo,
                    isOfflineDemoLoading = loading
                )
            }
            composable(AppRoute.Auth.route) {
                AuthRoute(
                    onTeacherAuthenticated = {
                        navController.navigate(AppRoute.TeacherHome.route) {
                            popUpTo(AppRoute.Auth.route) { inclusive = true }
                        }
                    },
                    onStudentEntry = {
                        navController.navigate(AppRoute.StudentEntry.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.TeacherHome.route) {
                TeacherHomeRoute(
                    onCreateClassroom = {
                        navController.navigate(AppRoute.TeacherClassroomCreate.route)
                    },
                    onCreateQuiz = { classroomId, topicId ->
                        navController.navigate(AppRoute.TeacherQuizCreate.build(classroomId, topicId))
                    },
                    onAssignments = { navController.navigate(AppRoute.TeacherAssignments.route) },
                    onReports = { navController.navigate(AppRoute.TeacherReports.route) },
                    onClassroomSelected = { classroomId ->
                        navController.navigate(AppRoute.TeacherClassroomDetail.build(classroomId))
                    }
                )
            }
            composable(AppRoute.TeacherClassroomCreate.route) {
                CreateClassroomRoute(
                    onDone = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.TeacherClassroomDetail.route,
                arguments = listOf(navArgument("classroomId") { type = NavType.StringType })
            ) { entry ->
                val classroomId = entry.arguments?.getString("classroomId").orEmpty()
                if (classroomId.isBlank()) {
                    navController.popBackStack()
                } else {
                    TeacherClassroomDetailRoute(
                        onBack = { navController.popBackStack() },
                        onTopicSelected = { topicId ->
                            navController.navigate(AppRoute.TeacherTopicDetail.build(classroomId, topicId))
                        },
                        onLaunchLive = { navController.navigate(AppRoute.TeacherLaunch.build(classroomId)) }
                    )
                }
            }
            composable(
                route = AppRoute.TeacherTopicDetail.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType }
                )
            ) { entry ->
                val classroomId = entry.arguments?.getString("classroomId").orEmpty()
                val topicId = entry.arguments?.getString("topicId").orEmpty()
                if (classroomId.isBlank() || topicId.isBlank()) {
                    navController.popBackStack()
                } else {
                    TeacherTopicDetailRoute(
                        onBack = { navController.popBackStack() },
                        onCreateQuiz = { classId, topicIdArg ->
                            navController.navigate(AppRoute.TeacherQuizCreate.build(classId, topicIdArg))
                        },
                        onEditQuiz = { classId, topicIdArg, quizId ->
                            navController.navigate(AppRoute.TeacherQuizEdit.build(classId, topicIdArg, quizId))
                        },
                        onLaunchLive = { classId, topicIdArg, quizId ->
                            navController.navigate(AppRoute.TeacherLaunch.build(classId, topicIdArg, quizId))
                        },
                        onViewAssignments = { navController.navigate(AppRoute.TeacherAssignments.route) }
                    )
                }
            }
            composable(
                route = AppRoute.TeacherQuizCreate.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType }
                )
            ) {
                QuizEditorRoute(
                    onSaved = { navController.popBackStack() },
                    onDiscarded = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.TeacherQuizEdit.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType },
                    navArgument("quizId") { type = NavType.StringType }
                )
            ) {
                QuizEditorRoute(
                    onSaved = { navController.popBackStack() },
                    onDiscarded = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.TeacherLaunch.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("quizId") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
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
                    onJoined = { navController.navigate(AppRoute.StudentLobby.route) },
                    onTeacherSignIn = {
                        navController.navigate(AppRoute.Auth.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.StudentJoinLan.route) {
                StudentEntryRoute(
                    initialTab = EntryTab.Lan,
                    onJoined = { navController.navigate(AppRoute.StudentLobby.route) },
                    onTeacherSignIn = {
                        navController.navigate(AppRoute.Auth.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoute.StudentJoinCode.route) {
                StudentEntryRoute(
                    initialTab = EntryTab.Code,
                    onJoined = { navController.navigate(AppRoute.StudentLobby.route) },
                    onTeacherSignIn = {
                        navController.navigate(AppRoute.Auth.route) {
                            launchSingleTop = true
                        }
                    }
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
}
