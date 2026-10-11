package com.example.comw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Описание базы данных Room: какие таблицы (entities) в ней есть и её версия.
 * Сам объект базы создаётся в DatabaseModule (Hilt), а не вручную в UI.
 */
@Database(
    entities = [FavoriteEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "favpeople.db"
    }
}
