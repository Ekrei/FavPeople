package com.example.comw.ui.viewmodel

import com.example.comw.model.FavoriteEntry
import com.example.comw.model.TvmPerson

/** Фильтр по полу — выбирается чипами на главном экране. */
enum class GenderFilter(val label: String) {
    ALL("Все"),
    MALE("Мужчины"),
    FEMALE("Женщины");

    fun matches(person: TvmPerson): Boolean = when (this) {
        ALL -> true
        MALE -> person.gender.equals("Male", ignoreCase = true)
        FEMALE -> person.gender.equals("Female", ignoreCase = true)
    }
}

/** Строка списка: человек + отмечен ли он звёздочкой. */
data class PersonRow(
    val person: TvmPerson,
    val isFavorite: Boolean
)

/** Полное состояние главного экрана — собирается из нескольких потоков (см. PeopleListViewModel). */
data class PeopleListUiState(
    val query: String = "",
    val filter: GenderFilter = GenderFilter.ALL,
    val content: UiState<List<PersonRow>> = UiState.Loading,
    val favorites: List<FavoriteEntry> = emptyList()
)

/** Результат одного запроса в сеть (внутреннее состояние ViewModel). */
sealed interface LoadResult {
    data object Loading : LoadResult
    data class Success(val items: List<TvmPerson>) : LoadResult
    data class Failure(val message: String) : LoadResult
}
