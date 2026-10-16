package com.example.comw.domain.repository

import com.example.comw.model.NoteWithPerson
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNote(personId: Long): Flow<PersonNote?>
    fun observeAllNotes(): Flow<List<PersonNote>>
    fun observeNotesWithPeople(): Flow<List<NoteWithPerson>>

    /** Сохранить заметку/статус. Пустой текст + статус NONE = удалить заметку. */
    suspend fun save(person: TvmPerson, text: String, status: PersonalStatus)
    suspend fun delete(personId: Long)
    suspend fun notedPersonIds(): List<Long>
}
