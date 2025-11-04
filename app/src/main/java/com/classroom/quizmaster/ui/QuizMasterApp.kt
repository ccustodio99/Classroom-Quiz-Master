package com.classroom.quizmaster.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.classroom.quizmaster.ui.feature.activity.ActivityScreen
import com.classroom.quizmaster.ui.feature.auth.AuthScreen
import com.classroom.quizmaster.ui.feature.classroom.ClassroomScreen
import com.classroom.quizmaster.ui.feature.home.HomeScreen
import com.classroom.quizmaster.ui.feature.join.JoinSessionScreen
import com.classroom.quizmaster.ui.feature.learn.LearnScreen
import com.classroom.quizmaster.ui.feature.profile.ProfileScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Learn : Screen("learn", "Learn", Icons.Default.Search)
    object Classroom : Screen("classroom", "Classroom", Icons.Default.School)
    object Activity : Screen("activity", "Activity", Icons.Default.Analytics)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
    object Auth : Screen("auth", "Auth", Icons.Default.AccountCircle) // Icon doesn't matter
    object JoinSession : Screen("join", "Join", Icons.Default.Add) // Icon doesn't matter
}

val Items = listOf(
    Screen.Home,
    Screen.Learn,
    Screen.Classroom,
    Screen.Activity,
    Screen.Profile
)

@Composable
fun QuizMasterApp(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = Items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(onClick = { navController.navigate(Screen.JoinSession.route) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Quick action")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) { AuthScreen(onAuthenticated = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }) }

            // Main tabs
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Learn.route) { LearnScreen() }
            composable(Screen.Classroom.route) { ClassroomScreen() }
            composable(Screen.Activity.route) { ActivityScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            
            // Other screens
            composable(Screen.JoinSession.route) { JoinSessionScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
fun PlaceholderScreen(screenTitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$screenTitle Screen")
    }
}
