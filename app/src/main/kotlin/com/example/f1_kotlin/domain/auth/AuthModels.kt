package com.example.f1_kotlin.domain.auth

/** Пользователь Firebase Auth для UI / предиктора. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val emailVerified: Boolean,
)

/** Успех или l10n-ключ ошибки (`auth_error_*` / legacy `authError…`). */
sealed class AuthResult {
    data object Ok : AuthResult()
    data class Fail(val errorKey: String) : AuthResult()

    val isSuccess: Boolean get() = this is Ok
    val errorKeyOrNull: String? get() = (this as? Fail)?.errorKey
}
