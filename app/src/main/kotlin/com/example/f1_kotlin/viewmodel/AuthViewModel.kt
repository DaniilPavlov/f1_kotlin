package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.AuthErrorKeys
import com.example.f1_kotlin.data.repository.IAuthRepository
import com.example.f1_kotlin.domain.auth.AuthFormValidators
import com.example.f1_kotlin.domain.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFormUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorKey: String? = null,
    val passwordResetSent: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthFormUiState())
    val uiState: StateFlow<AuthFormUiState> = _uiState.asStateFlow()

    fun setEmail(value: String) {
        _uiState.update { it.copy(email = value, errorKey = null, passwordResetSent = false) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, errorKey = null) }
    }

    fun clearTransientFlags() {
        _uiState.update { it.copy(passwordResetSent = false) }
    }

    /** @return true при успехе. */
    fun signIn(onSuccess: () -> Unit) {
        if (!validateSignIn()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorKey = null) }
            try {
                when (val result = authRepository.signIn(_uiState.value.email, _uiState.value.password)) {
                    AuthResult.Ok -> onSuccess()
                    is AuthResult.Fail -> _uiState.update { it.copy(errorKey = result.errorKey) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        if (!validateRegister()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorKey = null) }
            try {
                when (val result = authRepository.register(_uiState.value.email, _uiState.value.password)) {
                    AuthResult.Ok -> onSuccess()
                    is AuthResult.Fail -> _uiState.update { it.copy(errorKey = result.errorKey) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        when {
            email.trim().isEmpty() -> {
                _uiState.update { it.copy(errorKey = AuthErrorKeys.EMPTY_EMAIL) }
                return
            }
            !AuthFormValidators.isEmailFormatOk(email) -> {
                _uiState.update { it.copy(errorKey = AuthErrorKeys.INVALID_EMAIL) }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorKey = null, passwordResetSent = false) }
            try {
                when (val result = authRepository.sendPasswordResetEmail(email)) {
                    AuthResult.Ok -> _uiState.update { it.copy(passwordResetSent = true) }
                    is AuthResult.Fail -> _uiState.update { it.copy(errorKey = result.errorKey) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateSignIn(): Boolean {
        val state = _uiState.value
        if (state.email.trim().isEmpty() || state.password.isEmpty()) {
            _uiState.update { it.copy(errorKey = AuthErrorKeys.EMPTY_FIELDS) }
            return false
        }
        if (!AuthFormValidators.isEmailFormatOk(state.email)) {
            _uiState.update { it.copy(errorKey = AuthErrorKeys.INVALID_EMAIL) }
            return false
        }
        return true
    }

    private fun validateRegister(): Boolean {
        val state = _uiState.value
        val errorKey = when {
            state.email.trim().isEmpty() || state.password.isEmpty() -> AuthErrorKeys.EMPTY_FIELDS
            !AuthFormValidators.isEmailFormatOk(state.email) -> AuthErrorKeys.INVALID_EMAIL
            AuthFormValidators.isDisposableEmail(state.email) -> AuthErrorKeys.DISPOSABLE_EMAIL
            !AuthFormValidators.isPasswordStrongEnough(state.password) -> AuthErrorKeys.WEAK_PASSWORD
            else -> null
        }
        if (errorKey != null) {
            _uiState.update { it.copy(errorKey = errorKey) }
            return false
        }
        return true
    }
}
