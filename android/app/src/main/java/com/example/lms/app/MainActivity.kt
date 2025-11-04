package com.example.lms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
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
            val destinations = rememberDestinations()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        destinations.forEach { destination ->
                            val selected = currentRoute == destination.route ||
                                backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = LmsDestination.Home.route,
                    modifier = Modifier.padding(padding),
                ) {
                    composable(LmsDestination.Home.route) {
                        HomeRoute(
                            onContinueLearning = { navController.navigate(LmsDestination.Learn.route) },
                            onOpenClassroom = { navController.navigate(LmsDestination.Classroom.route) },
                            onOpenProfile = { navController.navigate(LmsDestination.Profile.route) },
                        )
                    }
                    composable("auth") { AuthRoute(onEvent = { navController.navigate(LmsDestination.Home.route) }) }
                    composable(LmsDestination.Learn.route) {
                        LearnRoute(
                            onSelectClass = { navController.navigate(LmsDestination.Classroom.route) },
                            onStartSearch = { navController.navigate(LmsDestination.Learn.route) },
                        )
                    }
                    composable(LmsDestination.Classroom.route) {
                        ClassroomRoute(
                            onOpenLive = { navController.navigate("live") },
                            onViewGrades = { navController.navigate(LmsDestination.Activity.route) },
                        )
                    }
                    composable(LmsDestination.Activity.route) {
                        ActivityRoute(onOpenCertificates = { navController.navigate(LmsDestination.Profile.route) })
                    }
                    composable(LmsDestination.Profile.route) {
                        ProfileRoute(
                            onSignOut = { navController.navigate("auth") },
                            onManageDownloads = { navController.navigate(LmsDestination.Activity.route) },
                        )
                    }
                    composable("live") {
                        LiveRoute(onExit = { navController.popBackStack(LmsDestination.Classroom.route, inclusive = false) })
                    }
                }
            }
        }
    }
}

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private enum class LmsDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Learn("learn", "Learn", Icons.Filled.List),
    Classroom("classroom", "Classroom", Icons.Filled.Class),
    Activity("activity", "Activity", Icons.Filled.ShowChart),
    Profile("profile", "Profile", Icons.Filled.AccountCircle),
}

@Composable
private fun rememberDestinations(): List<Destination> = listOf(
    Destination(LmsDestination.Home.route, LmsDestination.Home.label, LmsDestination.Home.icon),
    Destination(LmsDestination.Learn.route, LmsDestination.Learn.label, LmsDestination.Learn.icon),
    Destination(LmsDestination.Classroom.route, LmsDestination.Classroom.label, LmsDestination.Classroom.icon),
    Destination(LmsDestination.Activity.route, LmsDestination.Activity.label, LmsDestination.Activity.icon),
    Destination(LmsDestination.Profile.route, LmsDestination.Profile.label, LmsDestination.Profile.icon),
)

