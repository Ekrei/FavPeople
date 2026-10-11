package com.example.comw.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comw.data.local.AppDatabase
import com.example.comw.data.repository.HistoryRepositoryImpl
import com.example.comw.testutil.FakeTimeProvider
import com.example.comw.testutil.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryRoomTest {

    private val clock = FakeTimeProvider(current = 100L)
    private lateinit var db: AppDatabase
    private lateinit var repo: HistoryRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = HistoryRepositoryImpl(db.historyDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `повторный просмотр поднимает человека наверх без дубля`() = runTest {
        repo.recordView(TestData.alice)
        clock.advanceBy(10)
        repo.recordView(TestData.bob)
        clock.advanceBy(10)
        repo.recordView(TestData.alice) // снова открыли Alice

        val history = repo.observeHistory().first()
        assertEquals(listOf(1L, 2L), history.map { it.person.id })
        assertEquals(120L, history.first().viewedAtMillis)
        assertEquals(2, db.historyDao().count())
    }

    @Test
    fun `clear очищает историю`() = runTest {
        repo.recordView(TestData.alice)
        repo.clear()
        assertEquals(0, repo.observeHistory().first().size)
    }
}
