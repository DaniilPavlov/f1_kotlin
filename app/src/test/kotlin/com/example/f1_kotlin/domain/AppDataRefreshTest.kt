package com.example.f1_kotlin.domain

import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AppDataRefreshTest {
    private lateinit var espnRepository: IEspnRepository
    private lateinit var f1Repository: IF1Repository
    private lateinit var refresh: AppDataRefresh

    @Before
    fun setUp() {
        espnRepository = mockk(relaxed = true)
        f1Repository = mockk(relaxed = true)
        refresh = AppDataRefresh(espnRepository, f1Repository)
    }

    @Test
    fun clearAll_clearsEspnAndF1MemoryCaches() = runTest {
        coEvery { espnRepository.clearCaches() } returns Unit
        coEvery { f1Repository.clearInMemoryCaches() } returns Unit

        refresh.clearAll()

        coVerify { espnRepository.clearCaches() }
        coVerify { f1Repository.clearInMemoryCaches() }
    }
}
