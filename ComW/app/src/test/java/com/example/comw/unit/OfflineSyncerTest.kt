package com.example.comw.unit

import com.example.comw.data.sync.OfflineSyncer
import com.example.comw.model.PersonalStatus
import com.example.comw.model.RefreshResult
import com.example.comw.testutil.FakeCollectionsRepository
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeNotesRepository
import com.example.comw.testutil.FakePeopleRepository
import com.example.comw.testutil.FakeSettingsRepository
import com.example.comw.testutil.FakeTimeProvider
import com.example.comw.testutil.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Логика фоновой синхронизации (то, что делает SyncWorker). */
class OfflineSyncerTest {

    private val clock = FakeTimeProvider(current = 777L)
    private val people = FakePeopleRepository()
    private val favorites = FakeFavoritesRepository(clock)
    private val collections = FakeCollectionsRepository()
    private val notes = FakeNotesRepository(clock)
    private val settings = FakeSettingsRepository()

    private val syncer = OfflineSyncer(people, favorites, collections, notes, settings, clock)

    @Test
    fun `предзагружаются люди из избранного, подборок и заметок - без повторов`() = runTest {
        favorites.add(TestData.alice)                          // id 1
        val collectionId = collections.create("Актёры").getOrThrow()
        collections.addPerson(collectionId, TestData.bob)      // id 2
        collections.addPerson(collectionId, TestData.alice)    // id 1 ещё раз
        notes.setStatus(TestData.carol.id, PersonalStatus.LIKED) // id 3

        val report = syncer.sync()

        assertEquals(listOf(1L, 2L, 3L), people.prefetchedIds.sorted())
        assertEquals(1, people.pruneCalls)
        assertFalse(report.networkProblem)
    }

    @Test
    fun `успешная синхронизация запоминает время в настройках`() = runTest {
        syncer.sync()
        assertEquals(777L, settings.state.value.lastSyncAt)
    }

    @Test
    fun `без сети - networkProblem и время синхронизации не обновляется`() = runTest {
        people.refreshMainListHandler = { RefreshResult.Failed(IOException("offline")) }

        val report = syncer.sync()

        assertTrue(report.networkProblem)
        assertNull(settings.state.value.lastSyncAt)
    }
}
