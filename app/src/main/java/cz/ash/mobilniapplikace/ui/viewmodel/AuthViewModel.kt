package cz.ash.mobilniapplikace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.ash.mobilniapplikace.data.CoinsRepository
import cz.ash.mobilniapplikace.data.auth.AuthRepository
import cz.ash.mobilniapplikace.domain.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserAccount? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val coinsRepository: CoinsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(user = authRepository.currentUserOrNull()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        // Restore session from local storage (SharedPreferences) so user stays logged in across restarts.
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.loadFromStorage()
                _state.update { it.copy(isLoading = false, user = authRepository.currentUserOrNull()) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, user = null) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.signIn(email.trim(), password)
                _state.update { it.copy(isLoading = false, user = authRepository.currentUserOrNull()) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Login failed") }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.signUp(email.trim(), password)
                _state.update { it.copy(isLoading = false, user = authRepository.currentUserOrNull()) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Signup failed") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                authRepository.signOut()
                coinsRepository.clearFavorites()
                _state.update { it.copy(isLoading = false, user = null) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message ?: "Logout failed") }
            }
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val coinsRepository: CoinsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository, coinsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}

