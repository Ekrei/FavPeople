package com.example.comw.data.favorites

import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesStore @Inject constructor() {

    private val _favorites = MutableStateFlow<List<FavoriteEntry>>(emptyList())
    val favorites: StateFlow<List<FavoriteEntry>> = _favorites.asStateFlow()

    fun add(person: TvmPerson) {
        _favorites.update { list ->
            if (list.any { it.person.id == person.id }) list
            else (list + FavoriteEntry(person, System.currentTimeMillis()))
                .sortedByDescending { it.addedAtMillis }
        }
    }

    fun remove(personId: Long) {
        _favorites.update { it.filter { e -> e.person.id != personId } }
    }

    fun isFavorite(personId: Long): Boolean =
        _favorites.value.any { it.person.id == personId }
}

data class FavoriteEntry(
    val person: TvmPerson,
    val addedAtMillis: Long
)
