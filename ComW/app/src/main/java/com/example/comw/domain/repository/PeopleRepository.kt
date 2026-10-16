package com.example.comw.domain.repository

import com.example.comw.model.CachedPerson
import com.example.comw.model.PrefetchReport
import com.example.comw.model.RefreshResult
import com.example.comw.model.SearchResult
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first репозиторий людей.
 * Правило: UI всегда ЧИТАЕТ из Room (observe...), а сеть только ОБНОВЛЯЕТ Room (refresh...).
 * Поэтому без интернета экран показывает то, что уже сохранено.
 */
interface PeopleRepository {
    fun observeMainList(): Flow<List<TvmPerson>>
    fun observeMainListUpdatedAt(): Flow<Long?>

    /** Обновить главный список из сети, если кэш устарел (или force = true). */
    suspend fun refreshMainList(force: Boolean = false): RefreshResult

    fun observePerson(id: Long): Flow<CachedPerson?>
    suspend fun refreshPerson(id: Long, force: Boolean = false): RefreshResult

    /** Поиск: сначала сеть, при отсутствии сети — по локальному кэшу. */
    suspend fun search(query: String): SearchResult

    /** Предзагрузить детали для офлайна (используется фоновым Worker'ом). */
    suspend fun prefetchPeople(ids: Collection<Long>): PrefetchReport

    /** Удалить старые записи кэша, на которые ничто не ссылается. */
    suspend fun pruneCache(): Int
    suspend fun clearCache()
    fun observeCachedCount(): Flow<Int>
}
