package com.example.comw.data.repository

import com.example.comw.data.remote.TvmApi
import com.example.comw.data.remote.toPersonDetailUiModel
import com.example.comw.data.remote.toPersonUiModel
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvmRepositoryImpl @Inject constructor(
    private val api: TvmApi
) : TvmRepository {

    override suspend fun getPeopleList(page: Int): List<TvmPerson> {
        return api.getPeopleList(page).mapNotNull { it.toPersonUiModel() }
    }

    override suspend fun getPersonDetail(id: Long): TvmPersonDetail {
        return api.getPersonDetail(id).toPersonDetailUiModel()
    }

    override suspend fun searchPeople(query: String): List<TvmPerson> {
        return api.searchPeople(query).mapNotNull { it.toPersonUiModel() }
    }
}
