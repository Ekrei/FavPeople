package com.example.comw.di

import com.example.comw.data.repository.CollectionsRepositoryImpl
import com.example.comw.data.repository.FavoritesRepositoryImpl
import com.example.comw.data.repository.HistoryRepositoryImpl
import com.example.comw.data.repository.NotesRepositoryImpl
import com.example.comw.data.repository.OfflinePeopleRepository
import com.example.comw.data.repository.SearchHistoryRepositoryImpl
import com.example.comw.data.settings.DataStoreSettingsRepository
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.domain.repository.SearchHistoryRepository
import com.example.comw.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** "Интерфейс -> реализация" для всех репозиториев. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPeopleRepository(impl: OfflinePeopleRepository): PeopleRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindCollectionsRepository(impl: CollectionsRepositoryImpl): CollectionsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
}
