package com.example.comw.domain.repository

import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail

interface TvmRepository {
    suspend fun getPeopleList(page: Int = 0): List<TvmPerson>
    suspend fun getPersonDetail(id: Long): TvmPersonDetail
    suspend fun searchPeople(query: String): List<TvmPerson>
}
