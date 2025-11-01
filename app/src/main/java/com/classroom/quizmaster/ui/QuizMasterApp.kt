package com.classroom.quizmaster.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.classroom.quizmaster.LocalAppContainer
import com.classroom.quizmaster.ui.feature.builder.ModuleBuilderScreen
import com.classroom.quizmaster.ui.feature.builder.ModuleBuilderViewModel
import com.classroom.quizmaster.ui.feature.dashboard.DashboardScreen
import com.classroom.quizmaster.ui.feature.dashboard.DashboardViewModel
import com.classroom.quizmaster.ui.feature.delivery.DeliveryScreen
import com.classroom.quizmaster.ui.feature.delivery.DeliveryViewModel
import com.classroom.quizmaster.ui.feature.detail.ModuleDetailScreen
import com.classroom.quizmaster.ui.feature.detail.ModuleDetailViewModel
import com.classroom.quizmaster.ui.feature.join.JoinSessionScreen
import com.classroom.quizmaster.ui.feature.join.JoinSessionViewModel
import com.classroom.quizmaster.ui.feature.livesession.LiveSessionScreen
import com.classroom.quizmaster.ui.feature.livesession.LiveSessionViewModel
import com.classroom.quizmaster.ui.feature.reports.ReportsScreen
import com.classroom.quizmaster.ui.feature.reports.ReportsViewModel

@Composable
fun QuizMasterApp(navController: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<DashboardViewModel>(
                    factory = viewModelFactory { DashboardViewModel(container, snackbarHostState) }
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onCreateModule = { navController.navigate(Screen.Builder.route) },
                    onOpenModule = { moduleId -> navController.navigate(Screen.ModuleDetail.createRoute(moduleId)) },
                    onJoinSession = { navController.navigate(Screen.Join.route) }
                )
            }
            composable(Screen.Builder.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ModuleBuilderViewModel>(
                    factory = viewModelFactory { ModuleBuilderViewModel(container) }
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
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                    ?.takeIf { it.isNotBlank() }
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
            composable(Screen.Join.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<JoinSessionViewModel>(
                    factory = viewModelFactory { JoinSessionViewModel() }
                )
                JoinSessionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Builder : Screen("builder")
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
    data object Join : Screen("join")
}
