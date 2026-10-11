package com.example.comw.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query(
        "SELECT c.id AS id, c.name AS name, c.description AS description, " +
            "c.createdAt AS createdAt, COUNT(ci.personId) AS itemCount " +
            "FROM collections c LEFT JOIN collection_items ci ON ci.collectionId = c.id " +
            "GROUP BY c.id ORDER BY c.createdAt DESC"
    )
    fun observeSummaries(): Flow<List<CollectionSummaryRow>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeCollection(id: Long): Flow<CollectionEntity?>

    @Query(
        "SELECT people.* FROM collection_items " +
            "INNER JOIN people ON people.id = collection_items.personId " +
            "WHERE collection_items.collectionId = :collectionId " +
            "ORDER BY collection_items.addedAt DESC"
    )
    fun observeMembers(collectionId: Long): Flow<List<PersonEntity>>

    @Query("SELECT collectionId FROM collection_items WHERE personId = :personId")
    fun observeCollectionIdsFor(personId: Long): Flow<List<Long>>

    @Query("SELECT name FROM collections WHERE id != :excludeId")
    suspend fun namesExcept(excludeId: Long): List<String>

    @Insert
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name, description = :description WHERE id = :id")
    suspend fun update(id: Long, name: String, description: String): Int

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: CollectionItemEntity): Long

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND personId = :personId")
    suspend fun deleteItem(collectionId: Long, personId: Long): Int

    @Query("SELECT COUNT(*) FROM collection_items WHERE collectionId = :collectionId")
    suspend fun countItems(collectionId: Long): Int

    @Query("SELECT DISTINCT personId FROM collection_items")
    suspend fun allMemberIds(): List<Long>
}
