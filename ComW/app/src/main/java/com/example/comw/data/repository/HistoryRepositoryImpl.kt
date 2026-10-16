package com.example.comw.data.repository

import com.example.comw.data.local.HistoryDao
import com.example.comw.data.local.toHistoryEntity
import com.example.comw.data.local.toHistoryEntry
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.model.HistoryEntry
import com.example.comw.model.TvmPerson
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
    private val timeProvider: TimeProvider
) : HistoryRepository {

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> =
        dao.observeRecent(limit).map { rows -> rows.map { it.toHistoryEntry() } }

    override suspend fun recordView(person: TvmPerson) {
        dao.upsert(person.toHistoryEntity(viewedAt = timeProvider.now()))
    }

    override suspend fun clear() {
        dao.clear()
    }
}
