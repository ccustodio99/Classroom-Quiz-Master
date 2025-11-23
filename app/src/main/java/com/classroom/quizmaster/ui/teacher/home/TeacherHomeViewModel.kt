package com.classroom.quizmaster.ui.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.demo.SampleDataSeeder
import com.classroom.quizmaster.data.network.ConnectivityMonitor
import com.classroom.quizmaster.data.network.ConnectivityStatus
import com.classroom.quizmaster.domain.model.AuthState
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
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
import timber.log.Timber

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

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val quizRepositoryUi: QuizRepositoryUi,
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    private val preferences: AppPreferencesDataSource,
    private val sampleDataSeeder: SampleDataSeeder,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val seedStatus = MutableStateFlow(SeedUi())

    private val combinedHeader = combine(
        quizRepositoryUi.teacherHome,
        authRepository.authState,
        preferences.sampleSeededTeachers,
        seedStatus,
        connectivityMonitor.status
    ) { home: TeacherHomeUiState,
        auth: AuthState,
        seeded: Set<String>,
        seedUi: SeedUi,
        connectivity: ConnectivityStatus ->
        HeaderData(home, auth, seeded, seedUi, connectivity)
    }

    private val combinedContent = combine(
        classroomRepository.classrooms,
        classroomRepository.topics,
        quizRepository.quizzes
    ) { classrooms: List<Classroom>, topics: List<Topic>, quizzes: List<Quiz> ->
        ContentData(classrooms, topics, quizzes)
    }

    init {
        viewModelScope.launch {
            runCatching {
                classroomRepository.refresh()
                quizRepository.refresh()
            }.onFailure { error ->
                Timber.w(error, "Failed to refresh teacher home data")
            }
        }
    }

    val uiState: StateFlow<TeacherHomeUiState> = combine(combinedHeader, combinedContent) { header, content ->
        val classroomOverviews = content.classrooms.map { classroom ->
            val topicCount = content.topics.count { topic -> topic.classroomId == classroom.id }
            val quizCount = content.quizzes.count { quiz -> quiz.classroomId == classroom.id }
            ClassroomOverviewUi(classroom.id, classroom.name, classroom.grade, topicCount, quizCount)
        }
        val teacherId = header.auth.userId
        val hasSeededData = !teacherId.isNullOrBlank() && header.seeded.contains(teacherId)
        val canSeed = BuildConfig.DEBUG && !teacherId.isNullOrBlank() && !hasSeededData
        val showSampleCard = BuildConfig.DEBUG && (
            canSeed || hasSeededData || header.seedUi.isSeeding || header.seedUi.isClearing
        )
        val offline = header.connectivity.isOffline
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
        header.home.copy(
            classrooms = classroomOverviews,
            connectivityHeadline = bannerHeadline,
            connectivitySupporting = bannerSupporting,
            statusChips = chips,
            showSampleDataCard = showSampleCard,
            isSeedingSamples = header.seedUi.isSeeding,
            isClearingSamples = header.seedUi.isClearing,
            canSeedSampleData = canSeed && !header.seedUi.isSeeding && !header.seedUi.isClearing,
            canClearSampleData = hasSeededData && !header.seedUi.isSeeding && !header.seedUi.isClearing,
            sampleSeedMessage = header.seedUi.message
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

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private data class SeedUi(
        val isSeeding: Boolean = false,
        val isClearing: Boolean = false,
        val message: String? = null
    )

    private data class HeaderData(
        val home: TeacherHomeUiState,
        val auth: AuthState,
        val seeded: Set<String>,
        val seedUi: SeedUi,
        val connectivity: ConnectivityStatus
    )

    private data class ContentData(
        val classrooms: List<Classroom>,
        val topics: List<Topic>,
        val quizzes: List<Quiz>
    )
}
