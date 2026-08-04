package com.example.f1_kotlin.di

import com.example.f1_kotlin.data.repository.AuthRepository
import com.example.f1_kotlin.data.repository.EspnRepository
import com.example.f1_kotlin.data.repository.F1Repository
import com.example.f1_kotlin.data.repository.IAuthRepository
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.data.repository.IF1Repository
import com.example.f1_kotlin.data.repository.IPredictorLeaderboardRepository
import com.example.f1_kotlin.data.repository.IPredictorRepository
import com.example.f1_kotlin.data.repository.PredictorLeaderboardRepository
import com.example.f1_kotlin.data.repository.PredictorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindF1Repository(impl: F1Repository): IF1Repository

    @Binds
    @Singleton
    abstract fun bindEspnRepository(impl: EspnRepository): IEspnRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepository): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindPredictorRepository(impl: PredictorRepository): IPredictorRepository

    @Binds
    @Singleton
    abstract fun bindPredictorLeaderboardRepository(
        impl: PredictorLeaderboardRepository,
    ): IPredictorLeaderboardRepository
}
