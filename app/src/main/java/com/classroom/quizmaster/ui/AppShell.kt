package com.classroom.quizmaster.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.classroom.quizmaster.ui.components.ConnectivityBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun QuizMasterRootScaffold(
    appState: QuizMasterAppState,
    modifier: Modifier = Modifier,
    studentBottomItems: List<BottomNavItem> = emptyList(),
    onStudentDestinationSelected: (String) -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) },
        topBar = {
            when (appState.currentShell) {
                AppShell.Teacher -> if (appState.teacherTopBarVisible) {
                    TeacherTopBar(appState)
                }
                AppShell.Student -> if (appState.studentTopBarVisible) {
                    StudentTopBar()
                }
                AppShell.Neutral -> if (appState.neutralTopBarVisible) {
                    NeutralTopBar()
                }
            }
        },
        bottomBar = {
            if (appState.currentShell == AppShell.Student && appState.studentBottomBarVisible) {
                StudentBottomBar(
                    appState = appState,
                    items = studentBottomItems,
                    onItemSelected = onStudentDestinationSelected
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = appState.connectivityBannerState is ConnectivityBannerState.Visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val banner = appState.connectivityBannerState
                if (banner is ConnectivityBannerState.Visible) {
                    ConnectivityBanner(
                        headline = banner.headline,
                        supportingText = banner.supportingText,
                        statusChips = banner.statusChips,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun TeacherTopBar(appState: QuizMasterAppState) {
    val viewModel: TeacherTopBarViewModel = hiltViewModel()
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appState.canNavigateBack) {
                IconButton(onClick = { appState.navigateBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Classroom Quiz Master",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Open settings"
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Profile") },
                    onClick = {
                        menuExpanded = false
                        appState.navController.navigate(AppRoute.TeacherProfile.route) {
                            launchSingleTop = true
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sign out") },
                    onClick = {
                        menuExpanded = false
                        viewModel.logout {
                            appState.navController.navigate(AppRoute.Auth.route) {
                                launchSingleTop = true
                                popUpTo(AppRoute.Auth.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StudentTopBar() {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ready to join?",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Connect over LAN or enter a code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NeutralTopBar() {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome to Classroom Quiz Master",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Text(
                    text = "Teachers create & host quizzes. Students join with a code or over LAN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StudentBottomBar(
    appState: QuizMasterAppState,
    items: List<BottomNavItem>,
    onItemSelected: (String) -> Unit
) {
    if (items.isEmpty()) return
    Surface(shadowElevation = 6.dp, tonalElevation = 3.dp) {
        NavigationBar {
            val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onItemSelected(item.route) },
                    icon = {
                        Icon(imageVector = item.icon, contentDescription = item.label)
                    },
                    label = {
                        Text(text = item.label, maxLines = 1)
                    }
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@HiltViewModel
class TeacherTopBarViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
                .onFailure { Timber.w(it, "Failed to logout") }
            onComplete()
        }
    }
}
