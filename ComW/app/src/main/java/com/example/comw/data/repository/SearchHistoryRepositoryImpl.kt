package com.example.comw.data.repository

import com.example.comw.data.local.SearchHistoryDao
import com.example.comw.data.local.SearchQueryEntity
import com.example.comw.domain.repository.SearchHistoryRepository
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val dao: SearchHistoryDao,
    private val timeProvider: TimeProvider
) : SearchHistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<String>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.query } }

    override suspend fun record(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        dao.upsert(SearchQueryEntity(query = q.lowercase(), lastUsedAt = timeProvider.now()))
    }

    override suspend fun clear() {
        dao.clear()
    }
}
