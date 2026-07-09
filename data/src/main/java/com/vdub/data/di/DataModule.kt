package com.vdub.data.di

import com.vdub.data.repository.DiarizationRepositoryImpl
import com.vdub.data.repository.HistoryRepositoryImpl
import com.vdub.data.repository.MediaRepositoryImpl
import com.vdub.data.repository.ModelRepositoryImpl
import com.vdub.data.repository.SettingsRepositoryImpl
import com.vdub.domain.repository.DiarizationRepository
import com.vdub.domain.repository.HistoryRepository
import com.vdub.domain.repository.MediaRepository
import com.vdub.domain.repository.ModelRepository
import com.vdub.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindDiarizationRepository(impl: DiarizationRepositoryImpl): DiarizationRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
