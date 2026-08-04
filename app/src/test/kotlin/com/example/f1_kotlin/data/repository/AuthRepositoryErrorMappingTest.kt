package com.example.f1_kotlin.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthRepositoryErrorMappingTest {
    @Test
    fun mapsAndroidAndWebCodes() {
        assertEquals(AuthErrorKeys.INVALID_EMAIL, AuthRepository.mapErrorCode("ERROR_INVALID_EMAIL"))
        assertEquals(AuthErrorKeys.INVALID_EMAIL, AuthRepository.mapErrorCode("invalid-email"))
        assertEquals(AuthErrorKeys.INVALID_CREDENTIAL, AuthRepository.mapErrorCode("invalid-credential"))
        assertEquals(AuthErrorKeys.EMAIL_IN_USE, AuthRepository.mapErrorCode("email-already-in-use"))
        assertEquals(AuthErrorKeys.GENERIC, AuthRepository.mapErrorCode("unknown-code"))
    }
}
