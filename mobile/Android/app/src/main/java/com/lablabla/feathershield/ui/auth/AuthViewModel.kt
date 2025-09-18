package com.lablabla.feathershield.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.lablabla.feathershield.data.repository.AuthRepository // Assuming this is your repository's path
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun handleAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChange -> _uiState.update { it.copy(email = action.email) }
            is LoginAction.OnPasswordChange -> _uiState.update { it.copy(password = action.password) }
            is LoginAction.OnTogglePasswordVisibility -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginAction.OnSignInClick -> signIn()
            is LoginAction.OnSignUpClick -> signUp()
            is LoginAction.OnGoogleSignInResult -> {
                action.idToken?.let { signInWithGoogle(it) }
                    ?: _uiState.update { it.copy(authState = AuthState.Error("Google Sign-In failed: No ID token found.")) }
            }
            is LoginAction.OnGoogleSignInError -> _uiState.update { it.copy(authState = AuthState.Error(action.message)) }
            is LoginAction.OnErrorShown -> _uiState.update { it.copy(authState = AuthState.Idle) }
            is LoginAction.OnGoogleSignInClick -> { /* Handled in the UI Layer */ }
        }
    }

    private fun signIn() {
        val currentState = _uiState.value
        // Basic validation
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Email and password cannot be empty.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            try {
                val result = authRepository.signIn(currentState.email, currentState.password)
                _uiState.update { it.copy(authState = AuthState.Success(result)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authState = AuthState.Error(e.message ?: "An unknown error occurred.")) }
            }
        }
    }

    private fun signUp() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(authState = AuthState.Error("Email and password cannot be empty.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            try {
                val result = authRepository.createUser(currentState.email, currentState.password)
                _uiState.update { it.copy(authState = AuthState.Success(result)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authState = AuthState.Error(e.message ?: "An unknown error occurred.")) }
            }
        }
    }

    private fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            try {
                val result = authRepository.signInWithGoogle(idToken)
                _uiState.update { it.copy(authState = AuthState.Success(result)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authState = AuthState.Error(e.message ?: "An unknown error occurred.")) }
            }
        }
    }

    // This function can remain if you need direct access to the user elsewhere.
    fun getCurrentUser() : FirebaseUser? {
        return authRepository.getCurrentUser()
    }
}

// ui/auth/AuthState.kt
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val authResult: AuthResult) : AuthState()
    data class Error(val message: String) : AuthState()
}