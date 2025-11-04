package com.classroom.quizmaster.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.HomeFeedItem
import com.classroom.quizmaster.domain.model.PersonaBlueprint
import com.classroom.quizmaster.domain.model.PersonaType
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.sample.BlueprintSamples
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeTabViewModel(
    container: AppContainer,
    private val userId: String,
    private val userRole: UserRole
) : ViewModel() {

    private val catalogRepository = container.catalogRepository
    private val classroomRepository = container.classroomRepository

    private val _uiState = MutableStateFlow(HomeTabUiState(isLoading = true))
    val uiState: StateFlow<HomeTabUiState> = _uiState.asStateFlow()

    init {
        observeHome()
    }

    private fun observeHome() {
        viewModelScope.launch {
            combine(
                catalogRepository.observeHomeFeed(userId),
                classroomRepository.observeActive()
            ) { feed, classrooms ->
                val personaType = mapRoleToPersonaType(userRole)
                val personaBlueprint = BlueprintSamples.personas.firstOrNull { it.type == personaType }
                val filteredClassrooms = when (userRole) {
                    UserRole.Teacher -> classrooms.filter { it.ownerId == userId }
                    else -> classrooms
                }
                HomeTabUiState(
                    homeFeed = feed,
                    classrooms = filteredClassrooms,
                    persona = personaBlueprint,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    private fun mapRoleToPersonaType(role: UserRole): PersonaType = when (role) {
        UserRole.Student -> PersonaType.Learner
        UserRole.Teacher -> PersonaType.Instructor
        UserRole.Admin -> PersonaType.Admin
    }
}

data class HomeTabUiState(
    val homeFeed: List<HomeFeedItem> = emptyList(),
    val classrooms: List<ClassroomProfile> = emptyList(),
    val persona: PersonaBlueprint? = null,
    val isLoading: Boolean = false
)
