package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.auth.AuthResult
import com.example.f1_kotlin.domain.auth.AuthUser
import kotlinx.coroutines.flow.Flow

/** Контракт Auth: Firebase Auth + bootstrap `users/{uid}` в Firestore. */
interface IAuthRepository {
    val currentUser: AuthUser?
    /** Поток сессии (AuthState + IdToken); UI подписывается без polling. */
    val userChanges: Flow<AuthUser?>
    val isSignedIn: Boolean
    val isEmailVerified: Boolean
    /** Predictor доступен только при signed-in и verified email. */
    val canUsePredictor: Boolean

    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    suspend fun sendEmailVerification(): AuthResult
    /** `reload` + force ID token; при verified ещё раз sync user doc. */
    suspend fun refreshEmailVerification(): Boolean
    /** Принудительно обновляет ID token (нужно для Firestore rules `email_verified`). */
    suspend fun refreshIdToken(): Boolean
    suspend fun signOut()
    /** Bootstrap/merge документа `users/{uid}` после login/register. */
    suspend fun ensureUserDocument()
}
