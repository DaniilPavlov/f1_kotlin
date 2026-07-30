package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.firebase.RemoteConfigService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForceUpdateGateTest {
    private lateinit var remoteConfig: RemoteConfigService
    private lateinit var gate: ForceUpdateGate

    @Before
    fun setUp() {
        remoteConfig = mockk()
        gate = ForceUpdateGate(remoteConfig)
    }

    @Test
    fun check_setsRequiredFromRemoteConfig() {
        every { remoteConfig.isUpdateRequired() } returns true
        gate.check()
        assertTrue(gate.required.value)

        every { remoteConfig.isUpdateRequired() } returns false
        gate.check()
        assertFalse(gate.required.value)
        verify(exactly = 2) { remoteConfig.isUpdateRequired() }
    }

    @Test
    fun onResume_refreshesThenChecks() = runTest {
        coEvery { remoteConfig.refresh() } returns Unit
        every { remoteConfig.isUpdateRequired() } returns true

        gate.onResume()

        coVerify { remoteConfig.refresh() }
        assertTrue(gate.required.value)
    }
}
