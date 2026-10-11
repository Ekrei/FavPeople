package com.example.comw.di

import com.example.comw.data.repository.FavoritesRepositoryImpl
import com.example.comw.data.repository.HistoryRepositoryImpl
import com.example.comw.data.repository.TvmRepositoryImpl
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.domain.repository.TvmRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Binds говорит Hilt: "когда кто-то просит интерфейс X — отдай реализацию Y".
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTvmRepository(impl: TvmRepositoryImpl): TvmRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
