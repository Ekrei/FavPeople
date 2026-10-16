package com.example.comw.data.repository

import com.example.comw.data.local.NoteDao
import com.example.comw.data.local.NoteEntity
import com.example.comw.data.local.PeopleDao
import com.example.comw.data.local.toNoteWithPerson
import com.example.comw.data.local.toPersonEntity
import com.example.comw.data.local.toPersonNote
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.model.NoteWithPerson
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.TvmPerson
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val peopleDao: PeopleDao,
    private val timeProvider: TimeProvider
) : NotesRepository {

    override fun observeNote(personId: Long): Flow<PersonNote?> =
        noteDao.observe(personId).map { it?.toPersonNote() }.distinctUntilChanged()

    override fun observeAllNotes(): Flow<List<PersonNote>> =
        noteDao.observeAll().map { rows -> rows.map { it.toPersonNote() } }.distinctUntilChanged()

    override fun observeNotesWithPeople(): Flow<List<NoteWithPerson>> =
        noteDao.observeWithPeople().map { rows -> rows.map { it.toNoteWithPerson() } }

    override suspend fun save(person: TvmPerson, text: String, status: PersonalStatus) {
        val cleanText = text.trim()
        if (cleanText.isEmpty() && status == PersonalStatus.NONE) {
            // Нечего хранить -> не держим "пустые" заметки в базе.
            noteDao.delete(person.id)
            return
        }
        val now = timeProvider.now()
        // Человек должен быть в кэше, чтобы заметка была видна на экране "Моё" офлайн.
        peopleDao.insertIfAbsent(person.toPersonEntity(fetchedAt = now))
        noteDao.upsert(
            NoteEntity(
                personId = person.id,
                text = cleanText,
                status = status.name,
                updatedAt = now
            )
        )
    }

    override suspend fun delete(personId: Long) {
        noteDao.delete(personId)
    }

    override suspend fun notedPersonIds(): List<Long> = noteDao.allPersonIds()
}
