package com.acme.lms.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.acme.lms.ui.navigation.BottomNav
import com.acme.lms.ui.screens.activity.ActivityScreen
import com.acme.lms.ui.screens.classroom.ClassroomScreen
import com.acme.lms.ui.screens.home.HomeScreen
import com.acme.lms.ui.screens.learn.LearnScreen
import com.acme.lms.ui.screens.lesson.LessonPlayerScreen
import com.acme.lms.ui.screens.profile.ProfileScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MobileLmsApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileLmsApp() {
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
