package com.example.comw.testutil

import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.model.FavoriteEntry
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Избранное в памяти — ведёт себя как настоящее, но без Room. */
class FakeFavoritesRepository(
    private val clock: FakeTimeProvider = FakeTimeProvider()
) : FavoritesRepository {

    private val state = MutableStateFlow<List<FavoriteEntry>>(emptyList())

    override fun observeFavorites(): Flow<List<FavoriteEntry>> = state

    override fun observeIsFavorite(personId: Long): Flow<Boolean> =
        state.map { list -> list.any { it.person.id == personId } }.distinctUntilChanged()

    override suspend fun add(person: TvmPerson): Boolean {
        if (state.value.any { it.person.id == person.id }) return false
        state.value = (state.value + FavoriteEntry(person, clock.now()))
            .sortedByDescending { it.addedAtMillis }
        return true
    }

    override suspend fun remove(personId: Long) {
        state.value = state.value.filter { it.person.id != personId }
    }

    override suspend fun toggle(person: TvmPerson): Boolean =
        if (state.value.any { it.person.id == person.id }) {
            remove(person.id)
            false
        } else {
            add(person)
        }

    override suspend fun favoriteIds(): List<Long> = ids()

    fun ids(): List<Long> = state.value.map { it.person.id }
}
