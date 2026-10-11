package com.example.comw.data.repository

import com.example.comw.data.local.FavoriteDao
import com.example.comw.data.local.toFavoriteEntity
import com.example.comw.data.local.toFavoriteEntry
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.model.FavoriteEntry
import com.example.comw.model.TvmPerson
import com.example.comw.util.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
    private val timeProvider: TimeProvider
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<FavoriteEntry>> =
        dao.observeAll()
            .map { rows -> rows.map { it.toFavoriteEntry() } }
            .distinctUntilChanged()

    override fun observeIsFavorite(personId: Long): Flow<Boolean> =
        dao.observeIsFavorite(personId).distinctUntilChanged()

    override suspend fun add(person: TvmPerson): Boolean =
        dao.insert(person.toFavoriteEntity(addedAt = timeProvider.now())) != -1L

    override suspend fun remove(personId: Long) {
        dao.delete(personId)
    }

    override suspend fun toggle(person: TvmPerson): Boolean =
        if (dao.isFavorite(person.id)) {
            dao.delete(person.id)
            false
        } else {
            add(person)
            true
        }

    override suspend fun favoriteIds(): List<Long> = dao.allPersonIds()
}
