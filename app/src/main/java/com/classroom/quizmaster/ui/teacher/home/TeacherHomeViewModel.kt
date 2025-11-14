package com.classroom.quizmaster.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.demo.SampleDataSeeder
import com.classroom.quizmaster.data.network.ConnectivityMonitor
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.StatusChipType
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
    val isClearingSamples: Boolean = false,
    val canSeedSampleData: Boolean = false,
    val canClearSampleData: Boolean = false,
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
    private val sampleDataSeeder: SampleDataSeeder,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val seedStatus = MutableStateFlow(SeedUi())

    private val baseState = combine(
        quizRepositoryUi.teacherHome,
        authRepository.authState,
        preferences.sampleSeededTeachers,
        seedStatus
    ) { home, auth, seeded, seedUi ->
        CombinedHomeState(home, auth, seeded, seedUi)
    }

    val uiState: StateFlow<TeacherHomeUiState> =
        combine(baseState, connectivityMonitor.status) { combined, connectivity ->
            val teacherId = combined.auth.userId
            val hasSeededData = !teacherId.isNullOrBlank() && combined.seededTeachers.contains(teacherId)
            val canSeed = BuildConfig.DEBUG && !teacherId.isNullOrBlank() && !hasSeededData
            val showSampleCard = BuildConfig.DEBUG && (
                canSeed || hasSeededData || combined.seedUi.isSeeding || combined.seedUi.isClearing
                )
            val offline = connectivity.isOffline
            val bannerHeadline = if (offline) "You're offline" else ""
            val bannerSupporting = if (offline) {
                "We'll keep everything saved locally and sync once you're online."
            } else {
                ""
            }
            val chips = if (offline) {
                listOf(StatusChipUi("offline", "Offline", StatusChipType.Offline))
            } else {
                emptyList()
            }
            combined.base.copy(
                connectivityHeadline = bannerHeadline,
                connectivitySupporting = bannerSupporting,
                statusChips = chips,
                showSampleDataCard = showSampleCard,
                isSeedingSamples = combined.seedUi.isSeeding,
                isClearingSamples = combined.seedUi.isClearing,
                canSeedSampleData = canSeed && !combined.seedUi.isSeeding && !combined.seedUi.isClearing,
                canClearSampleData = hasSeededData && !combined.seedUi.isSeeding && !combined.seedUi.isClearing,
                sampleSeedMessage = combined.seedUi.message
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
                    SeedUi(message = "Sample data added")
                } else {
                    SeedUi(message = result.exceptionOrNull()?.message ?: "Unable to seed data")
                }
            }
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            seedStatus.update { SeedUi(isClearing = true, message = null) }
            val result = sampleDataSeeder.clearSeededData()
            seedStatus.update {
                if (result.isSuccess) {
                    SeedUi(message = "Sample data removed")
                } else {
                    SeedUi(message = result.exceptionOrNull()?.message ?: "Unable to remove sample data")
                }
            }
        }
    }

    private data class SeedUi(
        val isSeeding: Boolean = false,
        val isClearing: Boolean = false,
        val message: String? = null
    )

    private data class CombinedHomeState(
        val base: TeacherHomeUiState,
        val auth: AuthState,
        val seededTeachers: Set<String>,
        val seedUi: SeedUi
    )
}
