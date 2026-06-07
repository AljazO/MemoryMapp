package si.uni_lj.fe.tnuv.memorymapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import si.uni_lj.fe.tnuv.memorymapp.data.AuthRepository
import si.uni_lj.fe.tnuv.memorymapp.data.UserProfile

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.signUp(email, password, fullName)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Sign up failed"
            }
            _isLoading.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = repository.login(email, password)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
            _isLoading.value = false
        }
    }

    fun updateProfile(fullName: String, username: String, bio: String) {
        val current = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val updated = current.copy(
                fullName = fullName,
                username = username,
                bio = bio
            )
            repository.updateProfile(updated)
            _isLoading.value = false
        }
    }

    fun logout() {
        repository.logout()
    }
}
