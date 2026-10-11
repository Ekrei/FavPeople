package com.example.comw.data.sync

import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.PrefetchReport
import com.example.comw.model.RefreshResult
import com.example.comw.util.TimeProvider
import java.io.IOException
import javax.inject.Inject

/** Итог одной синхронизации. */
data class SyncReport(
    val mainList: RefreshResult,
    val prefetch: PrefetchReport,
    val pruned: Int
) {
    /** Была ли проблема с сетью (тогда Worker попросит повторить позже). */
    val networkProblem: Boolean
        get() = (mainList as? RefreshResult.Failed)?.error is IOException ||
            (prefetch.failed > 0 && prefetch.updated == 0 && prefetch.skippedFresh == 0)
}

/**
 * Вся логика фоновой синхронизации (без зависимости от WorkManager — легко тестировать):
 *  1) обновить главный список, если он устарел (TTL из настроек);
 *  2) предзагрузить детали "личных" людей — из избранного, подборок и заметок,
 *     чтобы личный раздел полноценно работал без сети;
 *  3) почистить старый кэш, на который ничто не ссылается;
 *  4) запомнить время синхронизации (показывается в настройках).
 */
class OfflineSyncer @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val favoritesRepository: FavoritesRepository,
    private val collectionsRepository: CollectionsRepository,
    private val notesRepository: NotesRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) {
    suspend fun sync(): SyncReport {
        val mainList = peopleRepository.refreshMainList(force = false)

        val personalIds = (
            favoritesRepository.favoriteIds() +
                collectionsRepository.allMemberIds() +
                notesRepository.notedPersonIds()
            ).distinct()
        val prefetch = peopleRepository.prefetchPeople(personalIds)

        val pruned = peopleRepository.pruneCache()

        val report = SyncReport(mainList, prefetch, pruned)
        if (!report.networkProblem) {
            settingsRepository.setLastSyncAt(timeProvider.now())
        }
        return report
    }
}
