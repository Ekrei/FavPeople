package com.example.comw.testutil

import com.example.comw.data.remote.TvmApi
import com.example.comw.data.remote.dto.TvmPersonDto
import com.example.comw.data.remote.dto.TvmSearchPeopleItemDto
import java.io.IOException

/** Фейковый Retrofit-API: можно "выключить сеть" флагом offline. */
class FakeTvmApi : TvmApi {
    var offline = false
    var people: List<TvmPersonDto> = emptyList()
    var searchResults: Map<String, List<TvmPersonDto>> = emptyMap()

    var listCalls = 0
        private set
    val detailCalls = mutableListOf<Long>()

    override suspend fun getPeopleList(page: Int): List<TvmPersonDto> {
        listCalls++
        if (offline) throw IOException("offline")
        return people
    }

    override suspend fun getPersonDetail(id: Long): TvmPersonDto {
        detailCalls += id
        if (offline) throw IOException("offline")
        return people.firstOrNull { it.id == id } ?: dto(id, "Person $id")
    }

    override suspend fun searchPeople(query: String): List<TvmSearchPeopleItemDto> {
        if (offline) throw IOException("offline")
        return (searchResults[query] ?: emptyList()).map { TvmSearchPeopleItemDto(score = 1.0, person = it) }
    }

    companion object {
        fun dto(id: Long?, name: String, gender: String = "Male") = TvmPersonDto(
            id = id, url = "https://www.tvmaze.com/people/$id", name = name, country = null,
            birthday = "1990-01-01", deathday = null, gender = gender, image = null, updated = 1L
        )
    }
}
