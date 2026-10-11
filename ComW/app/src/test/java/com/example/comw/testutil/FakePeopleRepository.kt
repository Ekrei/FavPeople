package com.example.comw.testutil

import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.model.CachedPerson
import com.example.comw.model.PrefetchReport
import com.example.comw.model.RefreshResult
import com.example.comw.model.SearchResult
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Фейковый offline-first репозиторий: "кэш" — это MutableStateFlow,
 * а поведение сети задаётся обработчиками (успех / нет сети / задержка).
 */
class FakePeopleRepository : PeopleRepository {

    /** "Room": содержимое главного списка в кэше. */
    val mainList = MutableStateFlow<List<TvmPerson>>(emptyList())
    val mainListUpdatedAt = MutableStateFlow<Long?>(null)
    val people = MutableStateFlow<Map<Long, CachedPerson>>(emptyMap())

    val refreshForceFlags = mutableListOf<Boolean>()
    var refreshMainListHandler: suspend (force: Boolean) -> RefreshResult = { RefreshResult.Updated }

    val refreshPersonCalls = mutableListOf<Pair<Long, Boolean>>()
    var refreshPersonHandler: suspend (id: Long, force: Boolean) -> RefreshResult =
        { _, _ -> RefreshResult.Updated }

    val searchQueries = mutableListOf<String>()
    var searchHandler: suspend (String) -> SearchResult = { SearchResult(emptyList(), fromCache = false) }

    val prefetchedIds = mutableListOf<Long>()
    var prefetchHandler: suspend (Collection<Long>) -> PrefetchReport =
        { ids -> PrefetchReport(updated = ids.size, skippedFresh = 0, failed = 0) }
    var pruneCalls = 0
        private set

    fun putPerson(detail: TvmPersonDetail, fetchedAt: Long = 0L) {
        people.value = people.value + (detail.id to CachedPerson(detail, fetchedAt))
    }

    override fun observeMainList(): Flow<List<TvmPerson>> = mainList
    override fun observeMainListUpdatedAt(): Flow<Long?> = mainListUpdatedAt

    override suspend fun refreshMainList(force: Boolean): RefreshResult {
        refreshForceFlags += force
        return refreshMainListHandler(force)
    }

    override fun observePerson(id: Long): Flow<CachedPerson?> =
        people.map { it[id] }.distinctUntilChanged()

    override suspend fun refreshPerson(id: Long, force: Boolean): RefreshResult {
        refreshPersonCalls += id to force
        return refreshPersonHandler(id, force)
    }

    override suspend fun search(query: String): SearchResult {
        searchQueries += query
        return searchHandler(query)
    }

    override suspend fun prefetchPeople(ids: Collection<Long>): PrefetchReport {
        prefetchedIds += ids
        return prefetchHandler(ids)
    }

    override suspend fun pruneCache(): Int {
        pruneCalls++
        return 0
    }

    override suspend fun clearCache() {
        mainList.value = emptyList()
    }

    override fun observeCachedCount(): Flow<Int> = people.map { it.size }
}
