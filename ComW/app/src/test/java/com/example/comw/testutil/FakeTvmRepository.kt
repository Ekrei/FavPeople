package com.example.comw.testutil

import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail

/**
 * Фейковый репозиторий вместо настоящей сети.
 * Тест заранее кладёт в очередь "ответы" (успех, ошибка или ожидание),
 * а затем проверяет, сколько раз и с какими аргументами его вызывали.
 */
class FakeTvmRepository : TvmRepository {

    private val listAnswers = ArrayDeque<suspend () -> List<TvmPerson>>()
    private val detailAnswers = ArrayDeque<suspend (Long) -> TvmPersonDetail>()

    var listCalls = 0
        private set
    val requestedDetailIds = mutableListOf<Long>()

    /** Все строки, с которыми вызывался поиск, — по ним проверяем debounce и distinctUntilChanged. */
    val searchQueries = mutableListOf<String>()
    var searchHandler: suspend (String) -> List<TvmPerson> = { emptyList() }

    fun enqueueList(items: List<TvmPerson>) = listAnswers.addLast { items }
    fun enqueueListError(error: Exception) = listAnswers.addLast { throw error }
    fun enqueueListAnswer(answer: suspend () -> List<TvmPerson>) = listAnswers.addLast(answer)

    fun enqueueDetail(detail: TvmPersonDetail) = detailAnswers.addLast { detail }
    fun enqueueDetailError(error: Exception) = detailAnswers.addLast { throw error }

    override suspend fun getPeopleList(page: Int): List<TvmPerson> {
        listCalls++
        val answer = listAnswers.removeFirstOrNull() ?: error("Нет подготовленного ответа для списка")
        return answer()
    }

    override suspend fun getPersonDetail(id: Long): TvmPersonDetail {
        requestedDetailIds += id
        val answer = detailAnswers.removeFirstOrNull() ?: error("Нет подготовленного ответа для деталей")
        return answer(id)
    }

    override suspend fun searchPeople(query: String): List<TvmPerson> {
        searchQueries += query
        return searchHandler(query)
    }
}
