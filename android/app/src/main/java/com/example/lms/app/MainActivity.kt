package com.example.lms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lms.feature.activity.ui.ActivityRoute
import com.example.lms.feature.auth.ui.AuthRoute
import com.example.lms.feature.classroom.ui.ClassroomRoute
import com.example.lms.feature.home.ui.HomeRoute
import com.example.lms.feature.learn.ui.LearnRoute
import com.example.lms.feature.live.ui.LiveRoute
import com.example.lms.feature.profile.ui.ProfileRoute
import com.example.lms.ui.theme.ClassroomTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassroomApp()
        }
    }
}

@Composable
fun ClassroomApp() {
    ClassroomTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeRoute(onEvent = { navController.navigate("learn") }) }
                composable("auth") { AuthRoute(onEvent = { navController.navigate("home") }) }
                composable("learn") { LearnRoute(onEvent = { navController.navigate("classroom") }) }
                composable("classroom") { ClassroomRoute(onEvent = { navController.navigate("activity") }) }
                composable("activity") { ActivityRoute(onEvent = { navController.navigate("live") }) }
                composable("profile") { ProfileRoute(onEvent = { navController.navigate("auth") }) }
                composable("live") { LiveRoute(onEvent = { navController.navigate("home") }) }
            }
        }
    }
}

