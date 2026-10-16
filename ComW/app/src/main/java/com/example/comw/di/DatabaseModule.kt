package com.example.comw.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.local.CollectionDao
import com.example.comw.data.local.FavoriteDao
import com.example.comw.data.local.HistoryDao
import com.example.comw.data.local.NoteDao
import com.example.comw.data.local.PeopleDao
import com.example.comw.data.local.SearchHistoryDao
import com.example.comw.util.SystemTimeProvider
import com.example.comw.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль локального хранения: Room (база + DAO) и DataStore (настройки).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // схема изменилась с ДЗ 4 (версия 1 -> 2): старая база пересоздаётся
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePeopleDao(db: AppDatabase): PeopleDao = db.peopleDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
