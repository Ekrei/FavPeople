package com.example.comw.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) — набор SQL-запросов к таблице favorites.
 * Room сам генерирует реализацию этого интерфейса при сборке (kapt).
 */
@Dao
interface FavoriteDao {

    /** Flow: Room сам пришлёт новый список, как только таблица изменится. */
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE personId = :personId)")
    fun observeIsFavorite(personId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE personId = :personId)")
    suspend fun isFavorite(personId: Long): Boolean

    /** IGNORE: если человек уже в избранном, вставка игнорируется (возвращает -1). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE personId = :personId")
    suspend fun delete(personId: Long): Int

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int
}
