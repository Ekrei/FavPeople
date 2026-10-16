package com.example.comw.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Фоновая задача WorkManager. Запускается периодически (раз в 6 часов) и по кнопке
 * "Синхронизировать сейчас". WorkManager сам дождётся сети (Constraints) и
 * переживёт перезапуск телефона.
 *
 * @HiltWorker + @AssistedInject: Context и WorkerParameters даёт WorkManager,
 * а OfflineSyncer — Hilt.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncer: OfflineSyncer
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val report = syncer.sync()
            Log.d(TAG, "sync finished: $report")
            if (report.networkProblem && runAttemptCount < MAX_ATTEMPTS) Result.retry()
            else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "sync failed", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val TAG = "SyncWorker"
        const val MAX_ATTEMPTS = 3
    }
}
