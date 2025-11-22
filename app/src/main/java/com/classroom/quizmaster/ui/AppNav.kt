package com.classroom.quizmaster.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
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
import com.classroom.quizmaster.ui.model.QuizCategoryUi
import com.classroom.quizmaster.ui.materials.detail.MaterialDetailRoute
import com.classroom.quizmaster.ui.materials.editor.MaterialEditorRoute
import com.classroom.quizmaster.ui.student.end.StudentEndRoute
import com.classroom.quizmaster.ui.student.entry.EntryTab
import com.classroom.quizmaster.ui.student.entry.StudentEntryRoute
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyRoute
import com.classroom.quizmaster.ui.student.play.StudentPlayRoute
import com.classroom.quizmaster.ui.student.materials.StudentMaterialsRoute
import com.classroom.quizmaster.ui.teacher.assignments.AssignmentsRoute
import com.classroom.quizmaster.ui.teacher.assignments.editor.AssignmentEditorRoute
import com.classroom.quizmaster.ui.teacher.classrooms.CreateClassroomRoute
import com.classroom.quizmaster.ui.teacher.classrooms.EditClassroomRoute
import com.classroom.quizmaster.ui.teacher.classrooms.archived.ArchivedClassroomsRoute
import com.classroom.quizmaster.ui.teacher.classrooms.detail.TeacherClassroomDetailRoute
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeRoute
import com.classroom.quizmaster.ui.teacher.host.HostLiveRoute
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyRoute
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorRoute
import com.classroom.quizmaster.ui.teacher.reports.ReportsRoute
import com.classroom.quizmaster.ui.teacher.materials.TeacherMaterialsRoute
import com.classroom.quizmaster.ui.teacher.topics.create.CreateTopicRoute
import com.classroom.quizmaster.ui.teacher.topics.detail.TeacherTopicDetailRoute
import com.classroom.quizmaster.ui.teacher.topics.edit.EditTopicRoute

sealed class AppRoute(val route: String) {
    data object Welcome : AppRoute("neutral/welcome")
    data object Auth : AppRoute("auth")
    data object TeacherHome : AppRoute("teacher/home")
    data object TeacherClassroomCreate : AppRoute("teacher/classrooms/create")
    data object TeacherClassroomDetail : AppRoute("teacher/classrooms/{classroomId}") {
        fun build(classroomId: String) = "teacher/classrooms/$classroomId"
    }
    data object TeacherClassroomEdit : AppRoute("teacher/classrooms/{classroomId}/edit") {
        fun build(classroomId: String) = "teacher/classrooms/$classroomId/edit"
    }
    data object TeacherTopicDetail : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}") {
        fun build(classroomId: String, topicId: String) = "teacher/classrooms/$classroomId/topics/$topicId"
    }
    data object TeacherTopicCreate : AppRoute("teacher/classrooms/{classroomId}/topics/create") {
        fun build(classroomId: String) = "teacher/classrooms/$classroomId/topics/create"
    }
    data object TeacherTopicEdit : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}/edit") {
        fun build(classroomId: String, topicId: String) =
            "teacher/classrooms/$classroomId/topics/$topicId/edit"
    }
    data object TeacherQuizCreate : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}/quizzes/create?category={category}") {
        fun build(classroomId: String, topicId: String, category: String? = null): String {
            val base = "teacher/classrooms/$classroomId/topics/$topicId/quizzes/create"
            val resolved = category?.takeIf { it.isNotBlank() }
            return if (resolved != null) "$base?category=$resolved" else base
        }
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
    data object TeacherMaterials : AppRoute("teacher/materials")
    data object TeacherMaterialCreate : AppRoute("teacher/materials/create?classroomId={classroomId}&topicId={topicId}") {
        fun build(classroomId: String?, topicId: String?): String {
            val classroomParam = classroomId?.takeIf { it.isNotBlank() } ?: ""
            val topicParam = topicId?.takeIf { it.isNotBlank() } ?: ""
            return "teacher/materials/create?classroomId=$classroomParam&topicId=$topicParam"
        }
    }
    data object TeacherMaterialEdit : AppRoute("teacher/materials/{materialId}/edit") {
        fun build(materialId: String) = "teacher/materials/$materialId/edit"
    }
    data object MaterialDetail : AppRoute("materials/{materialId}?role={role}") {
        fun build(materialId: String, role: String = "teacher") = "materials/$materialId?role=$role"
    }
    data object TeacherAssignmentCreate : AppRoute("teacher/classrooms/{classroomId}/topics/{topicId}/assignments/create") {
        fun build(classroomId: String, topicId: String) =
            "teacher/classrooms/$classroomId/topics/$topicId/assignments/create"
    }
    data object TeacherAssignmentEdit : AppRoute("teacher/assignments/{assignmentId}/edit") {
        fun build(assignmentId: String) = "teacher/assignments/$assignmentId/edit"
    }
    data object TeacherArchived : AppRoute("teacher/classrooms/archived")

    data object StudentEntry : AppRoute("student/entry")
    data object StudentJoinLan : AppRoute("student/joinLan")
    data object StudentJoinCode : AppRoute("student/joinCode")
    data object StudentLobby : AppRoute("student/lobby")
    data object StudentPlay : AppRoute("student/play")
    data object StudentEnd : AppRoute("student/end")
    data object StudentMaterials : AppRoute("student/materials")
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
            BottomNavItem(AppRoute.StudentPlay.route, Icons.Outlined.EmojiEvents, "Play"),
            BottomNavItem(AppRoute.StudentMaterials.route, Icons.AutoMirrored.Outlined.LibraryBooks, "Materials")
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
                    onLogin = { navController.navigate(AppRoute.Auth.route) },
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
                    onStudentAuthenticated = {
                        navController.navigate(AppRoute.StudentEntry.route) {
                            popUpTo(AppRoute.Auth.route) { inclusive = true }
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
                    onMaterials = { navController.navigate(AppRoute.TeacherMaterials.route) },
                    onViewArchived = { navController.navigate(AppRoute.TeacherArchived.route) },
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
                        onCreateTopic = {
                            navController.navigate(AppRoute.TeacherTopicCreate.build(classroomId))
                        },
                        onEditClassroom = {
                            navController.navigate(AppRoute.TeacherClassroomEdit.build(classroomId))
                        },
                        onCreatePreTest = { classId, topicId ->
                            if (topicId.isNotBlank()) {
                                navController.navigate(
                                    AppRoute.TeacherQuizCreate.build(
                                        classId,
                                        topicId,
                                        QuizCategoryUi.PreTest.routeValue
                                    )
                                )
                            }
                        },
                        onCreatePostTest = { classId, topicId ->
                            if (topicId.isNotBlank()) {
                                navController.navigate(
                                    AppRoute.TeacherQuizCreate.build(
                                        classId,
                                        topicId,
                                        QuizCategoryUi.PostTest.routeValue
                                    )
                                )
                            }
                        },
                        onEditTest = { classId, topicId, quizId ->
                            if (topicId.isNotBlank()) {
                                navController.navigate(
                                    AppRoute.TeacherQuizEdit.build(classId, topicId, quizId)
                                )
                            }
                        }
                    )
                }
            }
            composable(
                route = AppRoute.TeacherClassroomEdit.route,
                arguments = listOf(navArgument("classroomId") { type = NavType.StringType })
            ) { entry ->
                val classroomId = entry.arguments?.getString("classroomId").orEmpty()
                if (classroomId.isBlank()) {
                    navController.popBackStack()
                } else {
                    EditClassroomRoute(
                        onDone = { navController.popBackStack() },
                        onArchived = {
                            val removedEdit = navController.popBackStack()
                            if (removedEdit) {
                                navController.popBackStack()
                            }
                        }
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
                        onEditTopic = {
                            navController.navigate(AppRoute.TeacherTopicEdit.build(classroomId, topicId))
                        },
                        onCreateMaterial = { classId, topicIdArg ->
                            navController.navigate(AppRoute.TeacherMaterialCreate.build(classId, topicIdArg))
                        },
                        onOpenMaterial = { materialId ->
                            navController.navigate(AppRoute.MaterialDetail.build(materialId, role = "teacher"))
                        },
                        onCreateQuiz = { classId, topicIdArg ->
                            navController.navigate(AppRoute.TeacherQuizCreate.build(classId, topicIdArg))
                        },
                        onEditQuiz = { classId, topicIdArg, quizId ->
                            navController.navigate(AppRoute.TeacherQuizEdit.build(classId, topicIdArg, quizId))
                        },
                        onLaunchLive = { classId, topicIdArg, quizId ->
                            navController.navigate(AppRoute.TeacherLaunch.build(classId, topicIdArg, quizId))
                        },
                        onViewAssignments = { navController.navigate(AppRoute.TeacherAssignments.route) },
                        onCreateAssignment = { classId, topicIdArg ->
                            navController.navigate(AppRoute.TeacherAssignmentCreate.build(classId, topicIdArg))
                        },
                        onEditAssignment = { assignmentId ->
                            navController.navigate(AppRoute.TeacherAssignmentEdit.build(assignmentId))
                        }
                    )
                }
            }
            composable(
                route = AppRoute.TeacherTopicCreate.route,
                arguments = listOf(navArgument("classroomId") { type = NavType.StringType })
            ) {
                CreateTopicRoute(onDone = { navController.popBackStack() })
            }
            composable(
                route = AppRoute.TeacherTopicEdit.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument(
                        "topicId"
                    ) { type = NavType.StringType }
                )
            ) {
                EditTopicRoute(
                    onDone = { navController.popBackStack() },
                    onArchived = {
                        val removedEdit = navController.popBackStack()
                        if (removedEdit) {
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable(
                route = AppRoute.TeacherQuizCreate.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType },
                    navArgument("category") {
                        type = NavType.StringType
                        defaultValue = QuizCategoryUi.Standard.routeValue
                    }
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
                AssignmentsRoute(
                    onAssignmentSelected = { assignmentId ->
                        navController.navigate(AppRoute.TeacherAssignmentEdit.build(assignmentId))
                    }
                )
            }
            composable(AppRoute.TeacherMaterials.route) {
                TeacherMaterialsRoute(
                    onBack = { navController.popBackStack() },
                    onCreateMaterial = { classroomId, topicId ->
                        navController.navigate(AppRoute.TeacherMaterialCreate.build(classroomId, topicId))
                    },
                    onMaterialSelected = { materialId ->
                        navController.navigate(AppRoute.MaterialDetail.build(materialId, role = "teacher"))
                    },
                    onEditMaterial = { materialId ->
                        navController.navigate(AppRoute.TeacherMaterialEdit.build(materialId))
                    }
                )
            }
            composable(
                route = AppRoute.TeacherMaterialCreate.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("topicId") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                MaterialEditorRoute(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = AppRoute.TeacherMaterialEdit.route,
                arguments = listOf(navArgument("materialId") { type = NavType.StringType })
            ) {
                MaterialEditorRoute(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = AppRoute.TeacherAssignmentCreate.route,
                arguments = listOf(
                    navArgument("classroomId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType }
                )
            ) {
                AssignmentEditorRoute(
                    onDone = { navController.popBackStack() },
                    onArchived = { navController.popBackStack() }
                )
            }
            composable(
                route = AppRoute.TeacherAssignmentEdit.route,
                arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
            ) {
                AssignmentEditorRoute(
                    onDone = { navController.popBackStack() },
                    onArchived = {
                        navController.popBackStack()
                    }
                )
            }
            composable(AppRoute.TeacherArchived.route) {
                ArchivedClassroomsRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = AppRoute.MaterialDetail.route,
                arguments = listOf(
                    navArgument("materialId") { type = NavType.StringType },
                    navArgument("role") { type = NavType.StringType; defaultValue = "teacher" }
                )
            ) { entry ->
                val materialId = entry.arguments?.getString("materialId").orEmpty()
                if (materialId.isBlank()) {
                    navController.popBackStack()
                } else {
                    val role = entry.arguments?.getString("role").orEmpty()
                    val allowTeacherActions = role == "teacher"
                    MaterialDetailRoute(
                        allowEditing = allowTeacherActions,
                        allowShare = allowTeacherActions,
                        onBack = { navController.popBackStack() },
                        onEdit = { id ->
                            if (allowTeacherActions) {
                                navController.navigate(AppRoute.TeacherMaterialEdit.build(id))
                            }
                        },
                        onArchived = { navController.popBackStack() }
                    )
                }
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
            composable(AppRoute.StudentMaterials.route) {
                StudentMaterialsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenMaterial = { materialId ->
                        navController.navigate(AppRoute.MaterialDetail.build(materialId, role = "student"))
                    }
                )
            }
        }
    }
}
