package com.example.f1_kotlin.viewmodel

import com.example.f1_kotlin.data.repository.IAuthRepository
import com.example.f1_kotlin.domain.auth.AuthUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PredictorAuthGateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: IAuthRepository
    private lateinit var users: MutableStateFlow<AuthUser?>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        users = MutableStateFlow(null)
        authRepository = mockk(relaxed = true)
        every { authRepository.userChanges } returns users
        every { authRepository.canUsePredictor } answers {
            val u = users.value
            u != null && u.emailVerified
        }
        every { authRepository.isSignedIn } answers { users.value != null }
        every { authRepository.currentUser } answers { users.value }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun canUsePredictor_requiresVerifiedEmail() = runTest {
        val vm = PredictorAuthGateViewModel(authRepository)
        advanceUntilIdle()
        assertFalse(vm.canUsePredictor.value)
        assertFalse(vm.isSignedIn.value)

        users.value = AuthUser(uid = "1", email = "a@b.c", emailVerified = false)
        advanceUntilIdle()
        assertTrue(vm.isSignedIn.value)
        assertFalse(vm.canUsePredictor.value)

        users.value = AuthUser(uid = "1", email = "a@b.c", emailVerified = true)
        advanceUntilIdle()
        assertTrue(vm.canUsePredictor.value)
    }
}
