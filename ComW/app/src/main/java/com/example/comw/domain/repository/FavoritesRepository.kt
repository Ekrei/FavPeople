package com.example.comw.domain.repository

import com.example.comw.model.FavoriteEntry
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow

/** Работа с избранным. UI/ViewModel знают только этот интерфейс, а не Room. */
interface FavoritesRepository {
    /** Избранное, отсортированное по времени добавления (сначала новые). */
    fun observeFavorites(): Flow<List<FavoriteEntry>>
    fun observeIsFavorite(personId: Long): Flow<Boolean>

    /** @return true, если человек добавлен; false, если он уже был в избранном. */
    suspend fun add(person: TvmPerson): Boolean
    suspend fun remove(personId: Long)

    /** Переключить состояние. @return новое состояние (true = теперь в избранном). */
    suspend fun toggle(person: TvmPerson): Boolean
}
