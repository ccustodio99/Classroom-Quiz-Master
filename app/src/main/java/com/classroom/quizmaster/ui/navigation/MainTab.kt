package com.classroom.quizmaster.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Home(route = "home", icon = Icons.Outlined.Home, label = "Home"),
    Learn(route = "learn", icon = Icons.Outlined.LibraryBooks, label = "Learn"),
    Classroom(route = "classroom", icon = Icons.Outlined.Class, label = "Classroom"),
    Activity(route = "activity", icon = Icons.Outlined.Badge, label = "Activity"),
    Profile(route = "profile", icon = Icons.Outlined.Person, label = "Profile")
}

