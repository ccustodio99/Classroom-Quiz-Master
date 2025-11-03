package com.classroom.quizmaster.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classroom.quizmaster.LocalAppContainer
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.ui.feature.admin.AdminApprovalScreen
import com.classroom.quizmaster.ui.feature.auth.AuthScreen
import com.classroom.quizmaster.ui.feature.builder.ModuleBuilderScreen
import com.classroom.quizmaster.ui.feature.builder.ModuleBuilderViewModel
import com.classroom.quizmaster.ui.feature.classroom.ClassDetailScreen
import com.classroom.quizmaster.ui.feature.classroom.ClassDetailViewModel
import com.classroom.quizmaster.ui.feature.classroom.ClassroomScreen
import com.classroom.quizmaster.ui.feature.classroom.ClassroomViewModel
import com.classroom.quizmaster.ui.feature.dashboard.DashboardScreen
import com.classroom.quizmaster.ui.feature.dashboard.DashboardViewModel
import com.classroom.quizmaster.ui.feature.delivery.DeliveryScreen
import com.classroom.quizmaster.ui.feature.delivery.DeliveryViewModel
import com.classroom.quizmaster.ui.feature.detail.ModuleDetailScreen
import com.classroom.quizmaster.ui.feature.detail.ModuleDetailViewModel
import com.classroom.quizmaster.ui.feature.help.HelpGuideScreen
import com.classroom.quizmaster.ui.feature.join.JoinSessionScreen
import com.classroom.quizmaster.ui.feature.join.JoinSessionViewModel
import com.classroom.quizmaster.ui.feature.livesession.LiveSessionScreen
import com.classroom.quizmaster.ui.feature.livesession.LiveSessionViewModel
import com.classroom.quizmaster.ui.feature.reports.ReportsScreen
import com.classroom.quizmaster.ui.feature.reports.ReportsViewModel
import com.classroom.quizmaster.ui.feature.home.TeacherHomeScreen
import com.classroom.quizmaster.ui.viewModelFactory

@Composable
fun QuizMasterApp(navController: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SessionViewModel>(
        factory = viewModelFactory { SessionViewModel(container) }
    )
    val sessionState by sessionViewModel.uiState.collectAsState()

    LaunchedEffect(sessionState.currentUser) {
        val user = sessionState.currentUser
        val currentRoute = navController.currentDestination?.route
        if (user == null) {
            if (currentRoute != Screen.Auth.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            val target = when (user.role) {
                UserRole.Admin -> Screen.Admin.route
                UserRole.Teacher -> Screen.TeacherHome.route
                UserRole.Student -> Screen.Join.route
            }
            if (currentRoute != target) {
                navController.navigate(target) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    sessionViewModel = sessionViewModel,
                    onAuthenticated = {}
                )
            }
            composable(Screen.Admin.route) {
                AdminApprovalScreen(
                    sessionViewModel = sessionViewModel,
                    onBack = { sessionViewModel.logout() }
                )
            }
            composable(Screen.Classrooms.route) {
                val teacherId = sessionState.currentUser?.takeIf { it.role == UserRole.Teacher }?.id
                if (teacherId == null) {
                    HelpGuideScreen(onBack = { navController.popBackStack() })
                } else {
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ClassroomViewModel>(
                        factory = viewModelFactory { ClassroomViewModel(container, teacherId) }
                    )
                    ClassroomScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onBack = { navController.popBackStack() },
                        onManageModules = { navController.navigate(Screen.Dashboard.route) }
                    )
                }
            }
            composable(Screen.Dashboard.route) {
                val teacherId = sessionState.currentUser?.takeIf { it.role == UserRole.Teacher }?.id
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<DashboardViewModel>(
                    factory = viewModelFactory { DashboardViewModel(container, snackbarHostState, teacherId) }
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onCreateModule = { navController.navigate(Screen.Builder.createRoute()) },
                    onOpenModule = { moduleId -> navController.navigate(Screen.ModuleDetail.createRoute(moduleId)) },
                    onJoinSession = { navController.navigate(Screen.Join.route) },
                    onOpenHelp = { navController.navigate(Screen.Help.route) },
                    onManageClasses = { navController.navigate(Screen.Classrooms.route) }
                )
            }
            composable(Screen.TeacherHome.route) {
                val teacherId = sessionState.currentUser?.takeIf { it.role == UserRole.Teacher }?.id
                if (teacherId == null) {
                    HelpGuideScreen(onBack = { navController.popBackStack() })
                } else {
                    val classroomViewModel = androidx.lifecycle.viewmodel.compose.viewModel<ClassroomViewModel>(
                        factory = viewModelFactory { ClassroomViewModel(container, teacherId) }
                    )
                    val dashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel<DashboardViewModel>(
                        factory = viewModelFactory { DashboardViewModel(container, snackbarHostState, teacherId) }
                    )
                    TeacherHomeScreen(
                        classroomViewModel = classroomViewModel,
                        dashboardViewModel = dashboardViewModel,
                        snackbarHostState = snackbarHostState,
                        onOpenClassDetail = { classId -> navController.navigate(Screen.ClassDetail.createRoute(classId)) },
                        onOpenClassManager = { navController.navigate(Screen.Classrooms.route) },
                        onCreateModule = { navController.navigate(Screen.Builder.createRoute()) },
                        onOpenModule = { moduleId -> navController.navigate(Screen.ModuleDetail.createRoute(moduleId)) },
                        onStartDelivery = { moduleId -> navController.navigate(Screen.Delivery.createRoute(moduleId)) },
                        onOpenReports = { moduleId -> navController.navigate(Screen.Reports.createRoute(moduleId)) },
                        onJoinSession = { navController.navigate(Screen.Join.route) }
                    )
                }
            }

            composable(
                route = Screen.Builder.route,
                arguments = listOf(
                    navArgument("moduleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId")
                val teacherId = sessionState.currentUser?.takeIf { it.role == UserRole.Teacher }?.id
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ModuleBuilderViewModel>(
                    factory = viewModelFactory { ModuleBuilderViewModel(container, moduleId, teacherId) }
                )
                ModuleBuilderScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ModuleDetail.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ModuleDetailViewModel>(
                    factory = viewModelFactory { ModuleDetailViewModel(container, moduleId, snackbarHostState) }
                )
                ModuleDetailScreen(
                    viewModel = viewModel,
                    onStartDelivery = { navController.navigate(Screen.Delivery.createRoute(moduleId)) },
                    onViewReports = { navController.navigate(Screen.Reports.createRoute(moduleId)) },
                    onOpenLiveSession = { sessionId ->
                        navController.navigate(Screen.LiveSession.createRoute(moduleId, sessionId))
                    },
                    onEditModule = { navController.navigate(Screen.Builder.createRoute(moduleId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Delivery.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<DeliveryViewModel>(
                    factory = viewModelFactory { DeliveryViewModel(container, moduleId) }
                )
                DeliveryScreen(
                    viewModel = viewModel,
                    onFinished = { navController.popBackStack(Screen.ModuleDetail.createRoute(moduleId), inclusive = false) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.LiveSession.route,
                arguments = listOf(
                    navArgument("moduleId") { type = NavType.StringType },
                    navArgument("sessionId") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
                val sessionId = backStackEntry.arguments?.getString("sessionId")?.takeIf { it.isNotBlank() }
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<LiveSessionViewModel>(
                    factory = viewModelFactory { LiveSessionViewModel(container, moduleId, sessionId) }
                )
                LiveSessionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: return@composable
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ReportsViewModel>(
                    factory = viewModelFactory { ReportsViewModel(container, moduleId, snackbarHostState) }
                )
                ReportsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ClassDetail.route) { backStackEntry ->
                val classroomId = backStackEntry.arguments?.getString("classroomId") ?: return@composable
                val teacherId = sessionState.currentUser?.takeIf { it.role == UserRole.Teacher }?.id
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ClassDetailViewModel>(
                    factory = viewModelFactory { ClassDetailViewModel(container, classroomId, teacherId) }
                )
                ClassDetailScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onCreateModule = { navController.navigate(Screen.Builder.createRoute()) },
                    onOpenModule = { moduleId -> navController.navigate(Screen.ModuleDetail.createRoute(moduleId)) },
                    onStartDelivery = { moduleId -> navController.navigate(Screen.Delivery.createRoute(moduleId)) },
                    onOpenReports = { moduleId -> navController.navigate(Screen.Reports.createRoute(moduleId)) },
                    onManageClass = { navController.navigate(Screen.Classrooms.route) }
                )
            }
            composable(Screen.Join.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<JoinSessionViewModel>(
                    factory = viewModelFactory { JoinSessionViewModel() }
                )
                JoinSessionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Help.route) {
                HelpGuideScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Admin : Screen("admin")
    data object Classrooms : Screen("classrooms")
    data object Dashboard : Screen("dashboard")
    data object TeacherHome : Screen("teacherHome")
    data object Builder : Screen("builder?moduleId={moduleId}") {
        fun createRoute(moduleId: String? = null): String {
            val encoded = moduleId?.let { Uri.encode(it) } ?: ""
            return "builder?moduleId=$encoded"
        }
    }
    data object ModuleDetail : Screen("module/{moduleId}") {
        fun createRoute(moduleId: String) = "module/$moduleId"
    }
    data object Delivery : Screen("delivery/{moduleId}") {
        fun createRoute(moduleId: String) = "delivery/$moduleId"
    }
    data object LiveSession : Screen("liveSession/{moduleId}?sessionId={sessionId}") {
        fun createRoute(moduleId: String, sessionId: String?): String {
            val encoded = sessionId?.let { Uri.encode(it) } ?: ""
            return "liveSession/$moduleId?sessionId=$encoded"
        }
    }
    data object Reports : Screen("reports/{moduleId}") {
        fun createRoute(moduleId: String) = "reports/$moduleId"
    }
    data object ClassDetail : Screen("classDetail/{classroomId}") {
        fun createRoute(classroomId: String) = "classDetail/$classroomId"
    }
    data object Join : Screen("join")
    data object Help : Screen("help")
}




