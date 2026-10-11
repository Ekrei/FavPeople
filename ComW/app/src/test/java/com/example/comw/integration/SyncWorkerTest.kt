package com.example.comw.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.comw.data.sync.OfflineSyncer
import com.example.comw.data.sync.SyncWorker
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
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** Тест фоновой задачи WorkManager (TestListenableWorkerBuilder из work-testing). */
@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = FakeTimeProvider(current = 5_000L)
    private val people = FakePeopleRepository()
    private val favorites = FakeFavoritesRepository(clock)
    private val settings = FakeSettingsRepository()

    private fun buildWorker(): SyncWorker {
        val syncer = OfflineSyncer(
            people, favorites, FakeCollectionsRepository(), FakeNotesRepository(clock), settings, clock
        )
        // В приложении Worker создаёт HiltWorkerFactory, в тесте — наша фабрика с фейками.
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker = SyncWorker(appContext, workerParameters, syncer)
            })
            .build()
    }

    @Test
    fun `успешная синхронизация - success, избранное предзагружено, время сохранено`() = runTest {
        favorites.add(TestData.alice)

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(listOf(TestData.alice.id), people.prefetchedIds)
        assertEquals(5_000L, settings.state.value.lastSyncAt)
    }

    @Test
    fun `нет сети - retry, WorkManager повторит позже`() = runTest {
        people.refreshMainListHandler = { RefreshResult.Failed(IOException("offline")) }

        val result = buildWorker().doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
