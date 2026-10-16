package com.example.comw.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.local.PersonEntity
import com.example.comw.data.repository.CollectionsRepositoryImpl
import com.example.comw.data.repository.NotesRepositoryImpl
import com.example.comw.model.PersonalStatus
import com.example.comw.testutil.FakeTimeProvider
import com.example.comw.testutil.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Финал: пользовательские сущности в Room — подборки (связь многие-ко-многим) и заметки. */
@RunWith(AndroidJUnit4::class)
class PersonalDataRoomTest {

    private val clock = FakeTimeProvider(current = 1_000L)
    private lateinit var db: AppDatabase
    private lateinit var collections: CollectionsRepositoryImpl
    private lateinit var notes: NotesRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        collections = CollectionsRepositoryImpl(db.collectionDao(), db.peopleDao(), clock)
        notes = NotesRepositoryImpl(db.noteDao(), db.peopleDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    // ---------- подборки ----------
    @Test
    fun `человек добавляется в подборку один раз и попадает в кэш для офлайна`() = runTest {
        val id = collections.create("Актёры").getOrThrow()

        assertTrue(collections.addPerson(id, TestData.alice))
        assertFalse(collections.addPerson(id, TestData.alice))

        assertEquals(1, db.collectionDao().countItems(id))
        assertNotNull(db.peopleDao().getPerson(TestData.alice.id))
        assertEquals(setOf(id), collections.observeCollectionIdsFor(TestData.alice.id).first())
    }

    @Test
    fun `один человек может быть в нескольких подборках, удаление подборки каскадно чистит связи`() = runTest {
        val actors = collections.create("Актёры").getOrThrow()
        val directors = collections.create("Режиссёры").getOrThrow()
        collections.addPerson(actors, TestData.alice)
        collections.addPerson(directors, TestData.alice)
        collections.addPerson(actors, TestData.bob)

        collections.delete(actors)

        assertEquals(0, db.collectionDao().countItems(actors)) // ForeignKey CASCADE
        assertEquals(setOf(directors), collections.observeCollectionIdsFor(TestData.alice.id).first())
        assertNotNull("люди остаются в кэше", db.peopleDao().getPerson(TestData.bob.id))
    }

    @Test
    fun `повторяющееся название отклоняется и при создании, и при переименовании`() = runTest {
        collections.create("Актёры").getOrThrow()
        val other = collections.create("Режиссёры").getOrThrow()

        assertTrue(collections.create("  актёры ").isFailure)
        assertTrue(collections.update(other, "АКТЁРЫ", "").isFailure)
        assertTrue("своё имя менять можно", collections.update(other, "Режиссёры", "любимые").isSuccess)
    }

    @Test
    fun `Flow - список подборок со счётчиком обновляется сам`() = runTest {
        collections.observeCollections().test {
            assertEquals(emptyList<Int>(), awaitItem().map { it.itemCount })

            val id = collections.create("Актёры").getOrThrow()
            assertEquals(listOf(0), awaitItem().map { it.itemCount })

            collections.addPerson(id, TestData.alice)
            assertEquals(listOf(1), awaitItem().map { it.itemCount })

            clock.advanceBy(10)
            collections.addPerson(id, TestData.bob)
            assertEquals(listOf(2), awaitItem().map { it.itemCount })

            cancelAndIgnoreRemainingEvents()
        }
        val details = collections.observeCollection(1).first()!!
        assertEquals("новые сверху", listOf(2L, 1L), details.members.map { it.id })
    }

    // ---------- заметки ----------
    @Test
    fun `заметка сохраняется, видна вместе с именем и не затирает полные данные человека`() = runTest {
        db.peopleDao().upsertPerson(
            PersonEntity(1, "Alice", null, null, "2020-01-01", "Female", null, null, 5L, fetchedAt = 1L)
        )

        notes.save(TestData.alice, "  Посмотреть фильмы ", PersonalStatus.LIKED)

        val note = notes.observeNote(1).first()!!
        assertEquals("Посмотреть фильмы", note.text)
        assertEquals(PersonalStatus.LIKED, note.status)
        assertEquals("Alice", notes.observeNotesWithPeople().first().single().personName)
        assertEquals("2020-01-01", db.peopleDao().getPerson(1)!!.deathday)
    }

    @Test
    fun `пустая заметка без статуса удаляется`() = runTest {
        notes.save(TestData.bob, "текст", PersonalStatus.NONE)
        notes.save(TestData.bob, "   ", PersonalStatus.NONE)

        assertNull(notes.observeNote(TestData.bob.id).first())
        assertTrue(notes.notedPersonIds().isEmpty())
    }
}
