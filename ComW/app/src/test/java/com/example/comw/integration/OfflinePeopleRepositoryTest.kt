package com.example.comw.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.repository.FavoritesRepositoryImpl
import com.example.comw.data.repository.OfflinePeopleRepository
import com.example.comw.domain.logic.CachePolicy
import com.example.comw.model.RefreshResult
import com.example.comw.testutil.FakeSettingsRepository
import com.example.comw.testutil.FakeTimeProvider
import com.example.comw.testutil.FakeTvmApi
import com.example.comw.testutil.FakeTvmApi.Companion.dto
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
import java.io.IOException

/**
 * Интеграция offline-first слоя: настоящий OfflinePeopleRepository + настоящая Room
 * + фейковый API, у которого можно "выключить сеть".
 */
@RunWith(AndroidJUnit4::class)
class OfflinePeopleRepositoryTest {

    private val minute = 60_000L
    private val clock = FakeTimeProvider(current = 1_000_000_000L)
    private val api = FakeTvmApi()
    private val settings = FakeSettingsRepository() // TTL по умолчанию 60 минут
    private lateinit var db: AppDatabase
    private lateinit var repo: OfflinePeopleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = OfflinePeopleRepository(api, db.peopleDao(), settings, clock)
        api.people = listOf(dto(1, "Alice", "Female"), dto(null, "Broken"), dto(2, "Bob"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `обновление сохраняет список в Room, а UI читает его оттуда`() = runTest {
        assertEquals(RefreshResult.Updated, repo.refreshMainList())

        assertEquals(listOf(1L, 2L), repo.observeMainList().first().map { it.id })
        assertEquals(clock.now(), repo.observeMainListUpdatedAt().first())
    }

    @Test
    fun `пока кэш свежий (TTL), в сеть не ходим`() = runTest {
        repo.refreshMainList()
        clock.advanceBy(10 * minute)

        assertEquals(RefreshResult.SkippedFresh, repo.refreshMainList())
        assertEquals(1, api.listCalls)
    }

    @Test
    fun `кэш устарел или нажали Обновить - идём в сеть`() = runTest {
        repo.refreshMainList()
        assertEquals(RefreshResult.Updated, repo.refreshMainList(force = true))

        clock.advanceBy(61 * minute)
        assertEquals(RefreshResult.Updated, repo.refreshMainList())

        settings.setCacheTtlMinutes(24 * 60) // настройка из DataStore влияет на политику
        clock.advanceBy(61 * minute)
        assertEquals(RefreshResult.SkippedFresh, repo.refreshMainList())

        assertEquals(3, api.listCalls)
    }

    @Test
    fun `без сети обновление падает, но сохранённый список остаётся доступен`() = runTest {
        repo.refreshMainList()
        api.offline = true

        val result = repo.refreshMainList(force = true)

        assertTrue(result is RefreshResult.Failed && result.error is IOException)
        assertEquals(listOf(1L, 2L), repo.observeMainList().first().map { it.id })
    }

    @Test
    fun `поиск онлайн кэширует результаты, а офлайн ищет по кэшу`() = runTest {
        api.searchResults = mapOf("Carol" to listOf(dto(3, "Carol", "Female")))

        val online = repo.search("Carol")
        assertFalse(online.fromCache)

        api.offline = true
        val offline = repo.search("car") // LIKE без учёта регистра

        assertTrue(offline.fromCache)
        assertEquals(listOf(3L), offline.items.map { it.id })
    }

    @Test
    fun `предзагрузка для офлайна - скачивает отсутствующих, пропускает свежих, останавливается без сети`() = runTest {
        val first = repo.prefetchPeople(listOf(7L, 8L))
        assertEquals(2, first.updated)
        assertNotNull(repo.observePerson(7).first())

        val second = repo.prefetchPeople(listOf(7L, 8L))
        assertEquals(2, second.skippedFresh)

        clock.advanceBy(2 * 60 * minute)
        api.offline = true
        val offline = repo.prefetchPeople(listOf(7L, 8L))
        assertEquals(1, offline.failed) // после первой сетевой ошибки дальше не пытаемся
        // 2 запроса в первый раз + 1 неудачный; во второй раз запросов не было — данные были свежие
        assertEquals(3, api.detailCalls.size)
    }

    @Test
    fun `чистка кэша не трогает людей из списка и избранного`() = runTest {
        repo.refreshMainList()                                     // 1, 2 в main_list
        api.searchResults = mapOf("x" to listOf(dto(3, "Carol"), dto(4, "Dave")))
        repo.search("x")                                           // 3, 4 просто в кэше
        FavoritesRepositoryImpl(db.favoriteDao(), clock).add(TestData.carol) // 3 в избранном

        clock.advanceBy(CachePolicy.PRUNE_AFTER_MILLIS + minute)
        val removed = repo.pruneCache()

        assertEquals(1, removed)
        assertNull(db.peopleDao().getPerson(4))
        assertNotNull(db.peopleDao().getPerson(3))
        assertNotNull(db.peopleDao().getPerson(1))
    }
}
