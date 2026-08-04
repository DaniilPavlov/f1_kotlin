package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.auth.AuthResult
import com.example.f1_kotlin.domain.auth.AuthUser
import kotlinx.coroutines.flow.Flow

/** Facade над Firebase Auth + bootstrap `users/{uid}` в Firestore. */
interface IAuthRepository {
    val currentUser: AuthUser?
    val userChanges: Flow<AuthUser?>
    val isSignedIn: Boolean
    val isEmailVerified: Boolean
    val canUsePredictor: Boolean

    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    suspend fun sendEmailVerification(): AuthResult
    suspend fun refreshEmailVerification(): Boolean
    suspend fun signOut()
    suspend fun ensureUserDocument()
}
