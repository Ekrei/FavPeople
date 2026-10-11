package com.example.comw.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM person_notes WHERE personId = :personId")
    fun observe(personId: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM person_notes")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query(
        "SELECT person_notes.personId AS personId, person_notes.text AS text, " +
            "person_notes.status AS status, person_notes.updatedAt AS updatedAt, " +
            "people.name AS personName, people.imageUrl AS personImageUrl " +
            "FROM person_notes LEFT JOIN people ON people.id = person_notes.personId " +
            "ORDER BY person_notes.updatedAt DESC"
    )
    fun observeWithPeople(): Flow<List<NoteWithPersonRow>>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM person_notes WHERE personId = :personId")
    suspend fun delete(personId: Long)

    @Query("SELECT personId FROM person_notes")
    suspend fun allPersonIds(): List<Long>
}
