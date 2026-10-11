package com.example.comw.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.repository.FavoritesRepositoryImpl
import com.example.comw.testutil.FakeTimeProvider
import com.example.comw.testutil.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционный тест data-слоя: настоящий FavoritesRepositoryImpl + настоящая Room
 * (база в памяти). Robolectric даёт Android-окружение прямо в JVM.
 */
@RunWith(AndroidJUnit4::class)
class FavoritesRepositoryRoomTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = FakeTimeProvider(current = 1_000L)
    private lateinit var db: AppDatabase
    private lateinit var repo: FavoritesRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = FavoritesRepositoryImpl(db.favoriteDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `повторное добавление не создаёт дубль и не меняет время добавления`() = runTest {
        assertTrue(repo.add(TestData.alice))
        clock.advanceBy(5_000)
        assertFalse(repo.add(TestData.alice))

        assertEquals(1, db.favoriteDao().count())
        assertEquals(1_000L, repo.observeFavorites().first().single().addedAtMillis)
    }

    @Test
    fun `избранное отсортировано по времени добавления - новые сверху`() = runTest {
        repo.add(TestData.alice)
        clock.advanceBy(10)
        repo.add(TestData.bob)
        clock.advanceBy(10)
        repo.add(TestData.carol)

        val ids = repo.observeFavorites().first().map { it.person.id }
        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun `Flow - Room сам присылает новые списки при каждом изменении`() = runTest {
        repo.observeFavorites().test {
            assertEquals(emptyList<Long>(), awaitItem().map { it.person.id })

            repo.add(TestData.alice)
            assertEquals(listOf(1L), awaitItem().map { it.person.id })

            clock.advanceBy(10)
            repo.add(TestData.bob)
            assertEquals(listOf(2L, 1L), awaitItem().map { it.person.id })

            repo.remove(TestData.bob.id)
            assertEquals(listOf(1L), awaitItem().map { it.person.id })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `данные переживают перезапуск - база закрыта и открыта заново`() = runTest {
        val name = "favorites-restart-test.db"
        context.deleteDatabase(name)

        val first = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries().build()
        FavoritesRepositoryImpl(first.favoriteDao(), clock).add(TestData.alice)
        first.close() // "закрыли приложение"

        val second = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .allowMainThreadQueries().build()
        val restored = FavoritesRepositoryImpl(second.favoriteDao(), clock).observeFavorites().first()
        second.close()
        context.deleteDatabase(name)

        assertEquals(TestData.alice, restored.single().person)
    }
}
