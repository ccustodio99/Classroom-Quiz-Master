package com.classroom.quizmaster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.classroom.quizmaster.ui.auth.AuthDestinations
import com.classroom.quizmaster.ui.auth.ForgotPasswordScreen
import com.classroom.quizmaster.ui.auth.LoginScreen
import com.classroom.quizmaster.ui.auth.SignupScreen
import com.classroom.quizmaster.ui.navigation.BottomNav
import com.classroom.quizmaster.ui.screens.activity.ActivityScreen
import com.classroom.quizmaster.ui.screens.classroom.ClassroomScreen
import com.classroom.quizmaster.ui.screens.home.HomeScreen
import com.classroom.quizmaster.ui.screens.learn.LearnScreen
import com.classroom.quizmaster.ui.screens.lesson.LessonPlayerScreen
import com.classroom.quizmaster.ui.screens.profile.ProfileScreen
import com.classroom.quizmaster.ui.viewmodel.AuthState
import com.classroom.quizmaster.ui.viewmodel.AuthStateViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MobileLmsApp() }
    }
}

@Composable
fun MobileLmsApp() {
    val authStateViewModel: AuthStateViewModel = hiltViewModel()
    val authState by authStateViewModel.state.collectAsState()

    when (authState) {
        AuthState.Loading -> LoadingScreen()
        AuthState.SignedOut -> AuthFlow()
        is AuthState.SignedIn -> MainFlow()
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthFlow() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthDestinations.Login
    ) {
        composable(AuthDestinations.Login) { LoginScreen(navController) }
        composable(AuthDestinations.SignUp) { SignupScreen(navController) }
        composable(AuthDestinations.ForgotPassword) { ForgotPasswordScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFlow() {
    val navController = rememberNavController()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mobile LMS") }) },
        bottomBar = { BottomNav(navController) }
    ) { padding ->
        NavHost(
            modifier = Modifier.padding(padding),
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { HomeScreen(navController) }
            composable("learn") { LearnScreen(navController) }
            composable("classroom") { ClassroomScreen(navController) }
            composable("activity") { ActivityScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("firstLesson/{classId}/{workId}") { backStack ->
                val classId = backStack.arguments?.getString("classId").orEmpty()
                val workId = backStack.arguments?.getString("workId").orEmpty()
                LessonPlayerScreen(classId, workId)
            }
        }
    }
}
