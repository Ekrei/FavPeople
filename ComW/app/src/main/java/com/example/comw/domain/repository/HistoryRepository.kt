package com.example.comw.domain.repository

import com.example.comw.model.HistoryEntry
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow

/** История просмотров экрана деталей. */
interface HistoryRepository {
    fun observeHistory(limit: Int = DEFAULT_LIMIT): Flow<List<HistoryEntry>>
    suspend fun recordView(person: TvmPerson)
    suspend fun clear()

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
