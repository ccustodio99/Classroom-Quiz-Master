package com.classroom.quizmaster.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.data.demo.OfflineDemoManager
import com.classroom.quizmaster.data.auth.LocalAuthManager
import com.classroom.quizmaster.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    val nickname: String = ""
)

data class AuthUiState(
    val login: LoginFormState = LoginFormState(),
    val signup: SignupFormState = SignupFormState(),
    val profile: ProfileFormState = ProfileFormState(),
    val signupStep: SignupStep = SignupStep.Credentials,
    val loading: Boolean = false,
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
    val demoModeEnabled: Boolean = false
)

enum class SignupRole { None, Teacher, Student }

enum class SignupStep { Credentials, Profile }

sealed interface AuthEffect {
    data object TeacherAuthenticated : AuthEffect
    data object DemoMode : AuthEffect
    data class Error(val message: String) : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val offlineDemoManager: OfflineDemoManager,
    private val localAuthManager: LocalAuthManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects

    fun updateLoginEmail(value: String) {
        _uiState.value = _uiState.value.copy(login = _uiState.value.login.copy(email = value))
    }

    fun updateLoginPassword(value: String) {
        _uiState.value = _uiState.value.copy(login = _uiState.value.login.copy(password = value))
    }

    fun updateSignupEmail(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(email = value))
    }

    fun updateSignupPassword(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(password = value))
    }

    fun updateSignupConfirm(value: String) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(confirmPassword = value))
    }

    fun toggleTerms(value: Boolean) {
        _uiState.value = _uiState.value.copy(signup = _uiState.value.signup.copy(acceptedTerms = value))
    }

    fun updateProfileName(value: String) {
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(fullName = value))
    }

    fun updateProfileRole(value: SignupRole) {
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(role = value))
    }

    fun updateProfileSchool(value: String) {
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(school = value))
    }

    fun updateProfileSubject(value: String) {
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(subject = value))
    }

    fun updateProfileNickname(value: String) {
        _uiState.value = _uiState.value.copy(profile = _uiState.value.profile.copy(nickname = value))
    }

    fun backToSignupCredentials() {
        _uiState.value = _uiState.value.copy(
            signupStep = SignupStep.Credentials,
            errorMessage = null,
            loading = false
        )
    }

    fun signInTeacher() = launchWithProgress {
        val login = _uiState.value.login
        if (!login.email.contains("@") || login.password.length < 6) {
            emitError("Enter a valid email and 6+ char password.")
            return@launchWithProgress
        }
        val email = login.email.trim()
        val password = login.password
        runCatching { authRepository.signInWithEmail(email, password) }
            .onSuccess {
                cacheCredentialsFromFirebase(email, password)
                _effects.emit(AuthEffect.TeacherAuthenticated)
            }
            .onFailure { error ->
                val fallback = localAuthManager.tryOfflineLogin(email, password)
                if (fallback) {
                    _uiState.value = _uiState.value.copy(
                        bannerMessage = "Working offline. Changes will sync after you reconnect."
                    )
                    _effects.emit(AuthEffect.TeacherAuthenticated)
                } else {
                    emitError(error.message ?: "Unable to sign in.")
                }
            }
    }

    fun signUpTeacher() = launchWithProgress {
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
                delay(600)
                val signup = _uiState.value.signup
                val displayName = profile.fullName.ifBlank { profile.school.ifBlank { signup.email.substringBefore('@') } }
                val email = signup.email.trim()
                val password = signup.password
                runCatching {
                    authRepository.signUpWithEmail(email, password, displayName)
                }.onSuccess {
                    localAuthManager.cacheCredentials(email, password, displayName)
                    _uiState.value = _uiState.value.copy(
                        signup = SignupFormState(),
                        profile = ProfileFormState(),
                        signupStep = SignupStep.Credentials
                    )
                    _effects.emit(AuthEffect.TeacherAuthenticated)
                }.onFailure { error ->
                    emitError(error.message ?: "Unable to create account.")
                }
            }
        }
    }

    private suspend fun cacheCredentialsFromFirebase(email: String, password: String) {
        val authState = authRepository.authState.first { it.isAuthenticated && !it.userId.isNullOrBlank() }
        val displayName = authState.teacherProfile?.displayName
            ?: authState.displayName
            ?: email.substringBefore('@')
        localAuthManager.cacheCredentials(email, password, displayName)
    }
}
