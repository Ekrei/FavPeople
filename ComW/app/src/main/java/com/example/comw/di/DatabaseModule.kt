package com.example.comw.di

import android.content.Context
import androidx.room.Room
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.local.FavoriteDao
import com.example.comw.data.local.HistoryDao
import com.example.comw.util.SystemTimeProvider
import com.example.comw.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль: объясняет Hilt, КАК создать базу данных и DAO.
 * Благодаря @Singleton база создаётся один раз на всё приложение.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
