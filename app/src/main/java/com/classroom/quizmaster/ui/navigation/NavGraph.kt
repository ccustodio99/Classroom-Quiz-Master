package com.classroom.quizmaster.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Person

data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home", "Home", Icons.Default.Home),
    NavItem("learn", "Learn", Icons.Default.MenuBook),
    NavItem("classroom", "Classroom", Icons.Default.Group),
    NavItem("activity", "Activity", Icons.Default.Quiz),
    NavItem("profile", "Profile", Icons.Default.Person)
)

@Composable
fun BottomNav(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    NavigationBar {
        navItems.forEach { item ->
            val selected = backStackEntry?.destination?.route == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
