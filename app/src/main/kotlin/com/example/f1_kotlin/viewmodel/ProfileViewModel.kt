package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.repository.IAuthRepository
import com.example.f1_kotlin.domain.NotificationsPreference
import com.example.f1_kotlin.domain.auth.AuthUser
import com.example.f1_kotlin.notifications.RaceReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val toastMessageKey: String? = null,
    val isBusy: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
    private val notificationsPreference: NotificationsPreference,
    private val reminderScheduler: RaceReminderScheduler,
) : ViewModel() {
    val user: StateFlow<AuthUser?> = authRepository.userChanges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUser)

    val raceRemindersEnabled = notificationsPreference.raceRemindersEnabled
    val practiceRemindersEnabled = notificationsPreference.practiceRemindersEnabled

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val canToggleReminders: Boolean
        get() = notificationsPreference.canToggle

    val canTogglePractice: Boolean
        get() = notificationsPreference.canTogglePractice

    val effectivelyEnabled: Boolean
        get() = notificationsPreference.effectivelyEnabled

    val practiceEffectivelyEnabled: Boolean
        get() = notificationsPreference.practiceRemindersEffectivelyEnabled

    fun clearToast() {
        _uiState.update { it.copy(toastMessageKey = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val result = authRepository.sendEmailVerification()
                _uiState.update {
                    it.copy(
                        toastMessageKey = if (result.isSuccess) {
                            TOAST_VERIFICATION_SENT
                        } else {
                            result.errorKeyOrNull ?: TOAST_GENERIC
                        },
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun refreshVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val ok = authRepository.refreshEmailVerification()
                _uiState.update {
                    it.copy(
                        toastMessageKey = if (ok) TOAST_EMAIL_VERIFIED else TOAST_STILL_NOT_VERIFIED,
                    )
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun setRaceRemindersEnabled(enabled: Boolean) {
        notificationsPreference.setRaceRemindersEnabled(enabled)
        reminderScheduler.sync()
    }

    fun setPracticeRemindersEnabled(enabled: Boolean) {
        notificationsPreference.setPracticeRemindersEnabled(enabled)
        reminderScheduler.sync()
    }

    fun onLocaleChanged() {
        reminderScheduler.sync()
    }

    companion object {
        const val TOAST_VERIFICATION_SENT = "profileVerificationSent"
        const val TOAST_EMAIL_VERIFIED = "profileEmailVerified"
        const val TOAST_STILL_NOT_VERIFIED = "profileStillNotVerified"
        const val TOAST_GENERIC = "authErrorGeneric"
    }
}
