package com.example.comw.testutil

import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.model.HistoryEntry
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeHistoryRepository(
    private val clock: FakeTimeProvider = FakeTimeProvider()
) : HistoryRepository {

    val recorded = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observeHistory(limit: Int): Flow<List<HistoryEntry>> = recorded

    override suspend fun recordView(person: TvmPerson) {
        recorded.value = listOf(HistoryEntry(person, clock.now())) +
            recorded.value.filter { it.person.id != person.id }
    }

    override suspend fun clear() {
        recorded.value = emptyList()
    }
}
