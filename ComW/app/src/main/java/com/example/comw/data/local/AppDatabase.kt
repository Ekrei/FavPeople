package com.example.comw.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * База данных приложения (финальная версия).
 *
 *  people / main_list      — кэш данных из API (offline-first)
 *  favorites               — избранное
 *  view_history            — история просмотров
 *  search_history          — история поисковых запросов
 *  person_notes            — личные заметки и статусы
 *  collections             — подборки пользователя
 *  collection_items        — связь подборка <-> человек (многие-ко-многим)
 */
@Database(
    entities = [
        PersonEntity::class,
        MainListEntryEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        SearchQueryEntity::class,
        NoteEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun noteDao(): NoteDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val NAME = "favpeople.db"
    }
}
