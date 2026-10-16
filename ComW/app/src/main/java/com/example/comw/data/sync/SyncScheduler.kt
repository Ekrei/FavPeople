package com.example.comw.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.comw.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Планировщик синхронизации. Следит за настройками (Flow из DataStore) и
 * включает/выключает периодическую задачу, меняет ограничения (Wi-Fi / любая сеть).
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    fun start(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.settings
                .map { it.backgroundSyncEnabled to it.syncOnlyOnWifi }
                .distinctUntilChanged()
                .collect { (enabled, wifiOnly) ->
                    if (enabled) schedulePeriodic(wifiOnly) else cancelPeriodic()
                }
        }
    }

    /** Разовая синхронизация по кнопке в настройках. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints(wifiOnly = false))
            .build()
        workManager.enqueueUniqueWork(SYNC_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    private fun schedulePeriodic(wifiOnly: Boolean) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints(wifiOnly))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelPeriodic() {
        workManager.cancelUniqueWork(PERIODIC_SYNC)
    }

    private fun constraints(wifiOnly: Boolean): Constraints = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    companion object {
        const val PERIODIC_SYNC = "periodic_sync"
        const val SYNC_NOW = "sync_now"
    }
}
