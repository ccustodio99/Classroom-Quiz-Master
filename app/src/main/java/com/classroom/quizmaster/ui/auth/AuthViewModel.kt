package com.classroom.quizmaster.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.demo.OfflineDemoManager
import com.classroom.quizmaster.data.auth.LocalAuthManager
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.network.ConnectivityMonitor
import com.classroom.quizmaster.data.network.ConnectivityStatus
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

data class LoginFormState(
    val email: String = "",
    val password: String = ""
)

data class SignupFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false
)

data class ProfileFormState(
    val fullName: String = "",
    val role: SignupRole = SignupRole.None,
    val school: String = "",
    val subject: String = "",
    val nickname: String = "",
    val avatarId: String? = null
)

data class AuthUiState(
    val login: LoginFormState = LoginFormState(),
    val signup: SignupFormState = SignupFormState(),
    val profile: ProfileFormState = ProfileFormState(),
    val signupStep: SignupStep = SignupStep.Credentials,
    val avatarOptions: List<AvatarOption> = emptyList(),
    val loading: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val demoModeEnabled: Boolean = false,
    val isOffline: Boolean = false
)

enum class SignupRole { None, Teacher, Student }

enum class SignupStep { Credentials, Profile }

sealed interface AuthEffect {
    data object TeacherAuthenticated : AuthEffect
    data object StudentAuthenticated : AuthEffect
    data object DemoMode : AuthEffect
    data class Error(val message: String) : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val offlineDemoManager: OfflineDemoManager,
    private val localAuthManager: LocalAuthManager,
    private val authRepository: AuthRepository,
    private val sessionRepositoryUi: SessionRepositoryUi,
    private val preferences: AppPreferencesDataSource,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects

    private var latestConnectivity: ConnectivityStatus = connectivityMonitor.status.value

    init {
        viewModelScope.launch {
            connectivityMonitor.status.collect { status ->
                latestConnectivity = status
                _uiState.update { it.copy(isOffline = status.isOffline) }
            }
        }
        viewModelScope.launch {
            sessionRepositoryUi.avatarOptions.collect { avatars ->
                _uiState.update { it.copy(avatarOptions = avatars) }
            }
        }
    }

    fun onAvatarSelected(id: String) {
        _uiState.update { it.copy(profile = it.profile.copy(avatarId = id)) }
    }

    fun updateLoginEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            login = _uiState.value.login.copy(email = value),
            errorMessage = null
        )
    }

    fun updateLoginPassword(value: String) {
        _uiState.value = _uiState.value.copy(
            login = _uiState.value.login.copy(password = value),
            errorMessage = null
        )
    }

    fun updateSignupEmail(value: String) {
        _uiState.value = _uiState.value.copy(
            signup = _uiState.value.signup.copy(email = value),
            errorMessage = null
        )
    }

    fun updateSignupPassword(value: String) {
        _uiState.value = _uiState.value.copy(
            signup = _uiState.value.signup.copy(password = value),
            errorMessage = null
        )
    }

    fun updateSignupConfirm(value: String) {
        _uiState.value = _uiState.value.copy(
            signup = _uiState.value.signup.copy(confirmPassword = value),
            errorMessage = null
        )
    }

    fun toggleTerms(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            signup = _uiState.value.signup.copy(acceptedTerms = value),
            errorMessage = null
        )
    }

    fun updateProfileName(value: String) {
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(fullName = value),
            errorMessage = null
        )
    }

    fun updateProfileRole(value: SignupRole) {
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(role = value),
            errorMessage = null
        )
    }

    fun updateProfileSchool(value: String) {
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(school = value),
            errorMessage = null
        )
    }

    fun updateProfileSubject(value: String) {
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(subject = value),
            errorMessage = null
        )
    }

    fun updateProfileNickname(value: String) {
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile.copy(nickname = value),
            errorMessage = null
        )
    }

    fun backToSignupCredentials() {
        _uiState.value = _uiState.value.copy(
            signupStep = SignupStep.Credentials,
            errorMessage = null,
            loading = false
        )
    }

    fun signIn() = launchWithProgress {
        val login = _uiState.value.login
        if (!login.email.contains("@") || login.password.length < 6) {
            emitError("Enter a valid email and 6+ char password.")
            return@launchWithProgress
        }
        val email = login.email.trim()
        val password = login.password

        if (latestConnectivity.isOffline) {
            performOfflineFallback(email, password)
            return@launchWithProgress
        }

        runCatching { authRepository.signInWithEmail(email, password) }
            .onSuccess {
                val authState = cacheCredentialsFromFirebase(email, password, persistOffline = true)
                val signupRole = if (authState.role == UserRole.TEACHER) SignupRole.Teacher else SignupRole.Student
                rememberUserRole(authState.userId, signupRole)
                when (authState.role) {
                    UserRole.TEACHER -> _effects.emit(AuthEffect.TeacherAuthenticated)
                    UserRole.STUDENT -> _effects.emit(AuthEffect.StudentAuthenticated)
                }
            }
            .onFailure { error ->
                if (error.isNetworkIssue()) {
                    performOfflineFallback(email, password)
                } else {
                    emitError(mapAuthError(error))
                }
            }
    }

    fun signUp() = launchWithProgress {
        when (_uiState.value.signupStep) {
            SignupStep.Credentials -> handleCredentialsStep()
            SignupStep.Profile -> handleProfileStep()
        }
    }

    fun continueOfflineDemo() = launchWithProgress {
        delay(300)
        offlineDemoManager.enableDemoMode()
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    demoModeEnabled = true,
                    bannerMessage = "Demo lessons unlocked offline"
                )
                _effects.emit(AuthEffect.DemoMode)
            }
            .onFailure { error ->
                emitError(error.message ?: "Unable to prepare offline demo.")
            }
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = message, loading = false)
            _effects.emit(AuthEffect.Error(message))
        }
    }

    private fun launchWithProgress(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            runCatching { block() }
                .onFailure {
                    emitError(it.message ?: "Something went wrong")
                }
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    private suspend fun handleCredentialsStep() {
        val signup = _uiState.value.signup
        when {
            !signup.email.contains("@") -> emitError("Use a school email.")
            signup.password.length < 8 -> emitError("Password must be at least 8 characters.")
            signup.password != signup.confirmPassword -> emitError("Passwords do not match.")
            !signup.acceptedTerms -> emitError("Accept the terms to continue.")
            else -> {
                delay(400)
                val current = _uiState.value
                _uiState.value = current.copy(
                    signupStep = SignupStep.Profile,
                    profile = if (current.profile.role == SignupRole.None) {
                        current.profile.copy(role = SignupRole.Teacher)
                    } else {
                        current.profile
                    }
                )
            }
        }
    }

    private suspend fun handleProfileStep() {
        val profile = _uiState.value.profile
        when {
            profile.role == SignupRole.None -> emitError("Select whether you're a teacher or student.")
            profile.role == SignupRole.Teacher && profile.fullName.isBlank() -> emitError("Enter your name to continue.")
            profile.role == SignupRole.Teacher && profile.school.isBlank() -> emitError("Tell us your school or class.")
            profile.role == SignupRole.Student && profile.nickname.isBlank() -> emitError("Add a nickname so your teacher recognizes you.")
            else -> {
                if (latestConnectivity.isOffline) {
                    emitError("Connect to the internet to finish creating your account.")
                    return
                }
                delay(600)
                val signup = _uiState.value.signup
                val displayName = profile.fullName.ifBlank { profile.school.ifBlank { signup.email.substringBefore('@') } }
                val email = signup.email.trim()
                val password = signup.password
                runCatching {
                    authRepository.signUpWithEmail(email, password, displayName)
                }.onSuccess {
                    val authState = authRepository.authState.first { it.isAuthenticated && !it.userId.isNullOrBlank() }
                    if (profile.role == SignupRole.Teacher) {
                        localAuthManager.cacheCredentials(email, password, displayName)
                    }
                    rememberUserRole(authState.userId, profile.role)
                    _uiState.value = _uiState.value.copy(
                        signup = SignupFormState(),
                        profile = ProfileFormState(),
                        signupStep = SignupStep.Credentials
                    )
                    when (profile.role) {
                        SignupRole.Teacher -> _effects.emit(AuthEffect.TeacherAuthenticated)
                        SignupRole.Student -> _effects.emit(AuthEffect.StudentAuthenticated)
                        SignupRole.None -> Unit
                    }
                }.onFailure { error ->
                    emitError(mapAuthError(error))
                }
            }
        }
    }

    private suspend fun cacheCredentialsFromFirebase(
        email: String,
        password: String,
        persistOffline: Boolean
    ): com.classroom.quizmaster.domain.model.AuthState {
        val authState = authRepository.authState.first { it.isAuthenticated && !it.userId.isNullOrBlank() }
        if (persistOffline && authState.role == UserRole.TEACHER) {
            val displayName = authState.teacherProfile?.displayName
                ?: authState.displayName
                ?: email.substringBefore('@')
            localAuthManager.cacheCredentials(email, password, displayName)
        }
        return authState
    }

    private suspend fun rememberUserRole(userId: String?, role: SignupRole) {
        val resolvedRole = when (role) {
            SignupRole.Teacher -> UserRole.TEACHER
            SignupRole.Student -> UserRole.STUDENT
            SignupRole.None -> null
        }
        if (userId != null && resolvedRole != null) {
            preferences.setUserRole(userId, resolvedRole)
            if (resolvedRole == UserRole.STUDENT) {
                val profile = _uiState.value.profile
                sessionRepositoryUi.updateStudentProfile(profile.nickname, profile.avatarId)
            }
        }
    }

    private suspend fun performOfflineFallback(email: String, password: String) {
        val fallback = localAuthManager.tryOfflineLogin(email, password)
        if (fallback) {
            _uiState.value = _uiState.value.copy(
                bannerMessage = "Working offline. Changes will sync after you reconnect."
            )
            _effects.emit(AuthEffect.TeacherAuthenticated)
        } else {
            emitError(OFFLINE_REQUIRED_MESSAGE)
        }
    }

    private fun mapAuthError(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
        is FirebaseAuthInvalidUserException -> "We couldn't find that account. Check your email or sign up."
        is FirebaseAuthUserCollisionException -> "That email is already registered. Try signing in instead."
        is FirebaseTooManyRequestsException -> "Too many attempts right now. Please wait a moment and try again."
        is FirebaseNetworkException,
        is SocketTimeoutException,
        is UnknownHostException,
        is IOException -> OFFLINE_REQUIRED_MESSAGE
        else -> {
            Timber.w(error, "Unhandled auth error")
            error.message ?: "Unable to complete the request."
        }
    }

    private fun Throwable.isNetworkIssue(): Boolean =
        this is FirebaseNetworkException ||
            this is IOException ||
            this is UnknownHostException ||
            this is SocketTimeoutException

    private companion object {
        private const val OFFLINE_REQUIRED_MESSAGE = "No internet connection. Check your network and try again."
    }
}
