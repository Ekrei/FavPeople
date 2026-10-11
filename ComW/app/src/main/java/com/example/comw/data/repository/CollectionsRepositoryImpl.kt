package com.example.comw.data.repository

import com.example.comw.data.local.CollectionDao
import com.example.comw.data.local.CollectionEntity
import com.example.comw.data.local.CollectionItemEntity
import com.example.comw.data.local.PeopleDao
import com.example.comw.data.local.toPersonEntity
import com.example.comw.data.local.toSummary
import com.example.comw.data.local.toTvmPerson
import com.example.comw.domain.logic.CollectionNameValidator
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.model.CollectionDetails
import com.example.comw.model.CollectionSummary
import com.example.comw.model.TvmPerson
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionsRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val peopleDao: PeopleDao,
    private val timeProvider: TimeProvider
) : CollectionsRepository {

    override fun observeCollections(): Flow<List<CollectionSummary>> =
        collectionDao.observeSummaries()
            .map { rows -> rows.map { it.toSummary() } }
            .distinctUntilChanged()

    override fun observeCollection(id: Long): Flow<CollectionDetails?> =
        combine(
            collectionDao.observeCollection(id),
            collectionDao.observeMembers(id)
        ) { collection, members ->
            collection?.let {
                CollectionDetails(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    members = members.map { person -> person.toTvmPerson() }
                )
            }
        }.distinctUntilChanged()

    override fun observeCollectionIdsFor(personId: Long): Flow<Set<Long>> =
        collectionDao.observeCollectionIdsFor(personId).map { it.toSet() }.distinctUntilChanged()

    override suspend fun create(name: String, description: String): Result<Long> {
        val error = CollectionNameValidator.validate(name, collectionDao.namesExcept(excludeId = -1))
        if (error != null) return Result.failure(IllegalArgumentException(error))
        val id = collectionDao.insertCollection(
            CollectionEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = timeProvider.now()
            )
        )
        return Result.success(id)
    }

    override suspend fun update(id: Long, name: String, description: String): Result<Unit> {
        val error = CollectionNameValidator.validate(name, collectionDao.namesExcept(excludeId = id))
        if (error != null) return Result.failure(IllegalArgumentException(error))
        collectionDao.update(id, name.trim(), description.trim())
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long) {
        // элементы подборки удалятся сами (ForeignKey.CASCADE)
        collectionDao.deleteCollection(id)
    }

    override suspend fun addPerson(collectionId: Long, person: TvmPerson): Boolean {
        val now = timeProvider.now()
        peopleDao.insertIfAbsent(person.toPersonEntity(fetchedAt = now))
        return collectionDao.insertItem(
            CollectionItemEntity(collectionId = collectionId, personId = person.id, addedAt = now)
        ) != -1L
    }

    override suspend fun removePerson(collectionId: Long, personId: Long) {
        collectionDao.deleteItem(collectionId, personId)
    }

    override suspend fun allMemberIds(): List<Long> = collectionDao.allMemberIds()
}
