package com.example.comw.domain.repository

import com.example.comw.model.CollectionDetails
import com.example.comw.model.CollectionSummary
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow

interface CollectionsRepository {
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeCollection(id: Long): Flow<CollectionDetails?>
    fun observeCollectionIdsFor(personId: Long): Flow<Set<Long>>

    /** @return id новой подборки или ошибку валидации (пустое/длинное/повторяющееся имя). */
    suspend fun create(name: String, description: String = ""): Result<Long>
    suspend fun update(id: Long, name: String, description: String): Result<Unit>
    suspend fun delete(id: Long)

    /** @return false, если человек уже был в подборке. */
    suspend fun addPerson(collectionId: Long, person: TvmPerson): Boolean
    suspend fun removePerson(collectionId: Long, personId: Long)
    suspend fun allMemberIds(): List<Long>
}
