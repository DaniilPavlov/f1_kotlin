package com.example.f1_kotlin.domain.auth

import androidx.annotation.StringRes
import com.example.f1_kotlin.R
import com.example.f1_kotlin.data.repository.AuthErrorKeys

@StringRes
fun authErrorStringRes(key: String?): Int = when (key) {
    AuthErrorKeys.EMPTY_FIELDS -> R.string.auth_error_empty_fields
    AuthErrorKeys.EMPTY_EMAIL -> R.string.auth_error_empty_email
    AuthErrorKeys.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthErrorKeys.USER_DISABLED -> R.string.auth_error_user_disabled
    AuthErrorKeys.USER_NOT_FOUND -> R.string.auth_error_user_not_found
    AuthErrorKeys.WRONG_PASSWORD -> R.string.auth_error_wrong_password
    AuthErrorKeys.INVALID_CREDENTIAL -> R.string.auth_error_invalid_credential
    AuthErrorKeys.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthErrorKeys.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthErrorKeys.DISPOSABLE_EMAIL -> R.string.auth_error_disposable_email
    AuthErrorKeys.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthErrorKeys.NETWORK -> R.string.auth_error_network
    AuthErrorKeys.GENERIC -> R.string.auth_error_generic
    null -> 0
    else -> R.string.auth_error_generic
}
