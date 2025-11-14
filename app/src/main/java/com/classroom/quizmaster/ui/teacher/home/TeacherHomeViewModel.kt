package com.classroom.quizmaster.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.demo.SampleDataSeeder
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClassroomOverviewUi(
    val id: String,
    val name: String,
    val grade: String?,
    val topicCount: Int,
    val quizCount: Int
)

data class HomeActionCard(
    val id: String,
    val title: String,
    val description: String,
    val route: String,
    val ctaLabel: String = "Open",
    val primary: Boolean = false
)

data class TeacherHomeUiState(
    val greeting: String = "Good day",
    val teacherName: String = "",
    val connectivityHeadline: String = "",
    val connectivitySupporting: String = "",
    val statusChips: List<StatusChipUi> = emptyList(),
    val classrooms: List<ClassroomOverviewUi> = emptyList(),
    val actionCards: List<HomeActionCard> = emptyList(),
    val recentQuizzes: List<QuizOverviewUi> = emptyList(),
    val emptyMessage: String = "",
    val isOfflineDemo: Boolean = false,
    val defaultClassroomId: String? = null,
    val defaultTopicId: String? = null,
    val showSampleDataCard: Boolean = false,
    val isSeedingSamples: Boolean = false,
    val sampleSeedMessage: String? = null
)

const val ACTION_CREATE_QUIZ = "teacher_home:create_quiz"
const val ACTION_ASSIGNMENTS = "teacher_home:assignments"
const val ACTION_REPORTS = "teacher_home:reports"

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val quizRepositoryUi: QuizRepositoryUi,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferencesDataSource,
    private val sampleDataSeeder: SampleDataSeeder
) : ViewModel() {

    private val seedStatus = MutableStateFlow(SeedUi())

    val uiState: StateFlow<TeacherHomeUiState> =
        combine(
            quizRepositoryUi.teacherHome,
            authRepository.authState,
            preferences.sampleSeededTeachers,
            seedStatus
        ) { home, auth, seeded, seedUi ->
            val teacherId = auth.userId
            val canSeed = BuildConfig.DEBUG && !teacherId.isNullOrBlank() && !seeded.contains(teacherId)
            home.copy(
                showSampleDataCard = canSeed || seedUi.isSeeding,
                isSeedingSamples = seedUi.isSeeding,
                sampleSeedMessage = seedUi.message
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TeacherHomeUiState()
            )

    fun seedSampleData() {
        viewModelScope.launch {
            seedStatus.update { SeedUi(isSeeding = true, message = null) }
            val result = sampleDataSeeder.seed()
            seedStatus.update {
                if (result.isSuccess) {
                    SeedUi(isSeeding = false, message = "Sample data added")
                } else {
                    SeedUi(isSeeding = false, message = result.exceptionOrNull()?.message ?: "Unable to seed data")
                }
            }
        }
    }

    private data class SeedUi(
        val isSeeding: Boolean = false,
        val message: String? = null
    )
}
