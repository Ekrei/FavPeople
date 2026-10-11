package com.example.comw.integration

import com.example.comw.data.remote.TvmApi
import com.example.comw.data.remote.dto.TvmPersonDto
import com.example.comw.data.remote.dto.TvmSearchPeopleItemDto
import com.example.comw.data.repository.TvmRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** Связка "Repository + Fake API": проверяем, что репозиторий правильно обрабатывает ответы сервера. */
class TvmRepositoryFakeApiTest {

    private fun dto(id: Long?, name: String) = TvmPersonDto(
        id = id, url = null, name = name, country = null, birthday = null,
        deathday = null, gender = null, image = null, updated = null
    )

    private class FakeApi(
        private val list: List<TvmPersonDto> = emptyList(),
        private val fail: Boolean = false
    ) : TvmApi {
        var requestedPage: Int? = null
        override suspend fun getPeopleList(page: Int): List<TvmPersonDto> {
            requestedPage = page
            if (fail) throw IOException("offline")
            return list
        }
        override suspend fun getPersonDetail(id: Long): TvmPersonDto = throw IOException("offline")
        override suspend fun searchPeople(query: String): List<TvmSearchPeopleItemDto> = emptyList()
    }

    @Test
    fun `репозиторий запрашивает нужную страницу и отбрасывает записи без id`() = runTest {
        val api = FakeApi(list = listOf(dto(1, "A"), dto(null, "Broken"), dto(2, "B")))
        val repo = TvmRepositoryImpl(api)

        val result = repo.getPeopleList(page = 0)

        assertEquals(0, api.requestedPage)
        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test(expected = IOException::class)
    fun `ошибка сети пробрасывается наверх, чтобы ViewModel показала Error`() = runTest {
        TvmRepositoryImpl(FakeApi(fail = true)).getPeopleList()
    }
}
