package com.classroom.quizmaster.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class QuizMasterAppState internal constructor(
    val navController: NavHostController,
    private val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState
) {
    var currentShell by mutableStateOf(AppShell.Neutral)
        private set

    var teacherTopBarVisible by mutableStateOf(false)
        private set

    var studentTopBarVisible by mutableStateOf(false)
        private set

    var neutralTopBarVisible by mutableStateOf(true)
        private set

    var studentBottomBarVisible by mutableStateOf(false)
        private set

    var connectivityBannerState: ConnectivityBannerState by mutableStateOf(ConnectivityBannerState.Hidden)
        private set

    val canNavigateBack: Boolean
        get() = navController.previousBackStackEntry != null

    fun updateShellForRoute(route: String?) {
        val value = route.orEmpty()
        when {
            value.startsWith("teacher/") -> {
                currentShell = AppShell.Teacher
                teacherTopBarVisible = value != AppRoute.TeacherHost.route
                studentTopBarVisible = false
                neutralTopBarVisible = false
                studentBottomBarVisible = false
            }
            value.startsWith("student/") -> {
                currentShell = AppShell.Student
                teacherTopBarVisible = false
                studentTopBarVisible = value != AppRoute.StudentPlay.route
                neutralTopBarVisible = false
                studentBottomBarVisible =
                    value != AppRoute.StudentPlay.route && value != AppRoute.StudentEnd.route
            }
            else -> {
                currentShell = AppShell.Neutral
                teacherTopBarVisible = false
                studentTopBarVisible = false
                neutralTopBarVisible = true
                studentBottomBarVisible = false
            }
        }
    }

    fun navigateBack() {
        if (canNavigateBack) {
            navController.popBackStack()
        }
    }

    fun navigateToBottomRoute(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    fun showMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun updateConnectivity(state: ConnectivityBannerState) {
        connectivityBannerState = state
    }
}

@Composable
fun rememberQuizMasterAppState(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): QuizMasterAppState {
    return remember(navController, snackbarHostState, coroutineScope) {
        QuizMasterAppState(
            navController = navController,
            coroutineScope = coroutineScope,
            snackbarHostState = snackbarHostState
        )
    }
}

@Stable
enum class AppShell { Neutral, Teacher, Student }

sealed interface ConnectivityBannerState {
    data object Hidden : ConnectivityBannerState
    data class Visible(
        val headline: String,
        val supportingText: String,
        val statusChips: List<com.classroom.quizmaster.ui.model.StatusChipUi>
    ) : ConnectivityBannerState
}
