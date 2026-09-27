package com.example.guardian.parent.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardian.parent.data.AuthRepository
import com.example.guardian.parent.data.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    /** null = unknown/loading, true = signed in, false = signed out. Drives top-level navigation. */
    val isSignedIn: StateFlow<Boolean?> =
        repo.authState()
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    fun signIn(email: String, password: String) = submit { repo.signIn(email, password) }
    fun signUp(email: String, password: String) = submit { repo.signUp(email, password) }
    fun signOut() = repo.signOut()

    private fun submit(action: suspend () -> AuthResult) {
        if (_ui.value.loading) return
        _ui.value = AuthUiState(loading = true)
        viewModelScope.launch {
            when (val result = action()) {
                is AuthResult.Success -> _ui.value = AuthUiState(loading = false)
                is AuthResult.Error -> _ui.value = AuthUiState(loading = false, error = result.message)
            }
        }
    }
}
