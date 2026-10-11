package com.example.comw.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM view_history ORDER BY viewedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    /** REPLACE: повторный просмотр перезаписывает строку (обновляет время), а не плодит дубли. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM view_history")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM view_history")
    suspend fun count(): Int
}
