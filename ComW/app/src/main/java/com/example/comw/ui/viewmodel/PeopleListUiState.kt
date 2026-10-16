package com.example.comw.ui.viewmodel

import com.example.comw.domain.logic.ListFilter
import com.example.comw.domain.logic.PersonRow
import com.example.comw.model.AppSettings
import com.example.comw.model.FavoriteEntry
import com.example.comw.model.PersonNote
import com.example.comw.model.TvmPerson

/** Состояние главного экрана. */
data class PeopleListUiState(
    val query: String = "",
    val filter: ListFilter = ListFilter.ALL,
    val content: UiState<List<PersonRow>> = UiState.Loading,
    val favorites: List<FavoriteEntry> = emptyList(),
    /** Плашка "нет сети — данные из кэша". null — всё в порядке. */
    val notice: String? = null,
    /** Когда главный список последний раз обновлялся из сети. */
    val lastUpdatedAt: Long? = null,
    /** Идёт фоновое обновление, но на экране уже есть данные из кэша. */
    val isRefreshing: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

/** Состояние поиска (внутреннее). */
sealed interface SearchState {
    data object Inactive : SearchState
    data object Loading : SearchState
    data class Result(val items: List<TvmPerson>, val fromCache: Boolean) : SearchState
    data class Failed(val message: String) : SearchState
}

/** Главный список из Room + статус его обновления. */
data class MainListState(
    val items: List<TvmPerson>,
    val updatedAt: Long?,
    val status: RefreshStatus
)

/** Личные данные пользователя, влияющие на список. */
data class PersonalState(
    val favorites: List<FavoriteEntry>,
    val notes: Map<Long, PersonNote>,
    val settings: AppSettings
)
