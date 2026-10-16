package com.example.comw.data.repository

import com.example.comw.data.local.PeopleDao
import com.example.comw.data.local.toCachedPerson
import com.example.comw.data.local.toTvmPerson
import com.example.comw.data.remote.TvmApi
import com.example.comw.data.remote.toPersonEntity
import com.example.comw.domain.logic.CachePolicy
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.CachedPerson
import com.example.comw.model.PrefetchReport
import com.example.comw.model.RefreshResult
import com.example.comw.model.SearchResult
import com.example.comw.model.TvmPerson
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first реализация: Room — единственный источник правды для UI,
 * Retrofit — только способ обновить Room.
 */
@Singleton
class OfflinePeopleRepository @Inject constructor(
    private val api: TvmApi,
    private val dao: PeopleDao,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) : PeopleRepository {

    override fun observeMainList(): Flow<List<TvmPerson>> =
        dao.observeMainList()
            .map { rows -> rows.map { it.toTvmPerson() } }
            .distinctUntilChanged()

    override fun observeMainListUpdatedAt(): Flow<Long?> =
        dao.observeMainListFetchedAt().distinctUntilChanged()

    override suspend fun refreshMainList(force: Boolean): RefreshResult {
        val now = timeProvider.now()
        if (!force && !CachePolicy.isStale(dao.mainListFetchedAt(), now, ttlMinutes())) {
            return RefreshResult.SkippedFresh
        }
        return runCatchingNetwork {
            val people = api.getPeopleList(page = 0)
                .mapNotNull { it.toPersonEntity(fetchedAt = now) }
                .take(MAIN_LIST_SIZE)
            dao.replaceMainList(people, fetchedAt = now)
        }
    }

    override fun observePerson(id: Long): Flow<CachedPerson?> =
        dao.observePerson(id)
            .map { it?.toCachedPerson() }
            .distinctUntilChanged()

    override suspend fun refreshPerson(id: Long, force: Boolean): RefreshResult {
        val now = timeProvider.now()
        val cached = dao.getPerson(id)
        if (!force && cached != null && !CachePolicy.isStale(cached.fetchedAt, now, ttlMinutes())) {
            return RefreshResult.SkippedFresh
        }
        return runCatchingNetwork {
            val entity = api.getPersonDetail(id).toPersonEntity(fetchedAt = now)
                ?: throw IllegalStateException("Сервер вернул человека без id")
            dao.upsertPerson(entity)
        }
    }

    override suspend fun search(query: String): SearchResult {
        val q = query.trim()
        return try {
            val now = timeProvider.now()
            val found = api.searchPeople(q).mapNotNull { it.person?.toPersonEntity(fetchedAt = now) }
            dao.upsertPeople(found) // найденное тоже попадает в кэш -> доступно офлайн
            SearchResult(items = found.map { it.toTvmPerson() }, fromCache = false)
        } catch (e: IOException) {
            // Нет сети -> ищем среди уже сохранённых людей.
            SearchResult(items = dao.searchLocal(q).map { it.toTvmPerson() }, fromCache = true)
        }
    }

    override suspend fun prefetchPeople(ids: Collection<Long>): PrefetchReport {
        var updated = 0
        var skipped = 0
        var failed = 0
        for (id in ids.distinct()) {
            when (val result = refreshPerson(id, force = false)) {
                RefreshResult.Updated -> updated++
                RefreshResult.SkippedFresh -> skipped++
                is RefreshResult.Failed -> {
                    failed++
                    // Сети нет — дальше пытаться бессмысленно, Worker повторит позже.
                    if (result.error is IOException) break
                }
            }
        }
        return PrefetchReport(updated = updated, skippedFresh = skipped, failed = failed)
    }

    override suspend fun pruneCache(): Int =
        dao.pruneUnreferenced(olderThan = timeProvider.now() - CachePolicy.PRUNE_AFTER_MILLIS)

    override suspend fun clearCache() {
        dao.clearMainList()
        dao.pruneUnreferenced(olderThan = Long.MAX_VALUE)
    }

    override fun observeCachedCount(): Flow<Int> = dao.observeCount()

    private suspend fun ttlMinutes(): Int = settingsRepository.settings.first().cacheTtlMinutes

    private suspend fun runCatchingNetwork(block: suspend () -> Unit): RefreshResult =
        try {
            block()
            RefreshResult.Updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RefreshResult.Failed(e)
        }

    companion object {
        const val MAIN_LIST_SIZE = 50
    }
}
