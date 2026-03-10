package com.example.comw.di

import com.example.comw.data.repository.TvmRepositoryImpl
import com.example.comw.domain.repository.TvmRepository
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
    abstract fun bindTvmRepository(
        impl: TvmRepositoryImpl
    ): TvmRepository
}