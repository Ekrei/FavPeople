package com.example.comw.integration

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.example.comw.data.settings.DataStoreSettingsRepository
import com.example.comw.model.AppSettings
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Настройки в настоящем DataStore (временный файл). */
class SettingsDataStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun createRepository(file: File = File(tmp.root, "settings.preferences_pb")) =
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
        )

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `по умолчанию - значения AppSettings`() = runTest {
        assertEquals(AppSettings(), createRepository().settings.first())
    }

    @Test
    fun `изменения сохраняются и приходят в Flow`() = runTest {
        val repo = createRepository()

        repo.settings.test {
            assertEquals(AppSettings(), awaitItem())

            repo.setCacheTtlMinutes(15)
            assertEquals(15, awaitItem().cacheTtlMinutes)

            repo.setShowHidden(true)
            assertEquals(true, awaitItem().showHidden)

            cancelAndIgnoreRemainingEvents()
        }

        repo.setSortOrder(SortOrder.NAME)
        repo.setThemeMode(ThemeMode.DARK)
        repo.setLastSyncAt(123L)
        val saved = repo.settings.first()
        assertEquals(SortOrder.NAME, saved.sortOrder)
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertEquals(123L, saved.lastSyncAt)
    }
}
