package com.example.comw.data.remote

import com.example.comw.data.remote.dto.TvmPersonDto
import com.example.comw.data.remote.dto.TvmSearchPeopleItemDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TvmApi {

    @GET("people")
    suspend fun getPeopleList(
        @Query("page") page: Int = 0
    ): List<TvmPersonDto>

    @GET("people/{id}")
    suspend fun getPersonDetail(
        @Path("id") id: Long
    ): TvmPersonDto

    @GET("search/people")
    suspend fun searchPeople(
        @Query("q") query: String
    ): List<TvmSearchPeopleItemDto>
}
