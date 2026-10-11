package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.logic.ListFilter
import com.example.comw.domain.logic.PeopleListComposer
import com.example.comw.domain.logic.PersonRow
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.domain.repository.SearchHistoryRepository
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.RefreshResult
import com.example.comw.model.TvmPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Главный экран (финальная, offline-first версия).
 *
 * Список берётся ИЗ ROOM (peopleRepository.observeMainList()), а сеть только обновляет Room.
 * Поэтому без интернета пользователь видит сохранённые данные и плашку "нет сети".
 *
 * Источники состояния:
 *  - query, filter                      — ввод пользователя (StateFlow)
 *  - refreshActions                     — поток нажатий "Обновить/Retry" (SharedFlow)
 *  - главный список + время обновления  — Room
 *  - избранное, заметки/статусы         — Room (личные данные)
 *  - настройки (сортировка, скрытые)    — DataStore
 *  - недавние запросы                   — Room
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val favoritesRepository: FavoritesRepository,
    notesRepository: NotesRepository,
    settingsRepository: SettingsRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filter = MutableStateFlow(ListFilter.ALL)

    private val refreshActions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    init {
        // Обновление главного списка: при старте (с учётом TTL) и по каждому "Обновить" (принудительно).
        viewModelScope.launch {
            refreshActions
                .filter { _query.value.isBlank() }
                .map { true }
                .onStart { emit(false) }
                .collectLatest { force ->
                    refreshStatus.value = RefreshStatus.Loading
                    refreshStatus.value = when (val result = peopleRepository.refreshMainList(force)) {
                        RefreshResult.Updated, RefreshResult.SkippedFresh -> RefreshStatus.Done
                        is RefreshResult.Failed -> RefreshStatus.Failed(result.error.toUserMessage())
                    }
                }
        }
    }

    /** Поиск: query -> debounce -> trim -> distinct -> (+Retry) -> flatMapLatest(search). */
    private val searchState: Flow<SearchState> =
        combine(
            _query
                .debounce { text -> if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
                .map { it.trim() }
                .distinctUntilChanged(),
            refreshActions.onStart { emit(Unit) }
        ) { text, _ -> text }
            .flatMapLatest { text ->
                if (text.isEmpty()) flowOf(SearchState.Inactive) else search(text)
            }

    private val mainState: Flow<MainListState> = combine(
        peopleRepository.observeMainList(),
        peopleRepository.observeMainListUpdatedAt(),
        refreshStatus
    ) { items, updatedAt, status -> MainListState(items, updatedAt, status) }

    private val personalState: Flow<PersonalState> = combine(
        favoritesRepository.observeFavorites(),
        notesRepository.observeAllNotes(),
        settingsRepository.settings
    ) { favorites, notes, settings ->
        PersonalState(favorites, notes.associateBy { it.personId }, settings)
    }

    val uiState: StateFlow<PeopleListUiState> = combine(
        _query,
        filter,
        mainState,
        searchState,
        personalState
    ) { text, selectedFilter, main, search, personal ->
        buildState(text, selectedFilter, main, search, personal)
    }
        .combine(searchHistoryRepository.observeRecent()) { state, recent ->
            state.copy(recentSearches = recent)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PeopleListUiState())

    // ---------- действия ----------
    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun onFilterChange(newFilter: ListFilter) {
        filter.value = newFilter
    }

    fun refresh() {
        refreshActions.tryEmit(Unit)
    }

    fun toggleFavorite(person: TvmPerson) {
        viewModelScope.launch { favoritesRepository.toggle(person) }
    }

    fun removeFavorite(personId: Long) {
        viewModelScope.launch { favoritesRepository.remove(personId) }
    }

    // ---------- внутреннее ----------
    private fun search(text: String): Flow<SearchState> = flow {
        emit(SearchState.Loading)
        val result = peopleRepository.search(text)
        if (!result.fromCache && result.items.isNotEmpty()) {
            searchHistoryRepository.record(text)
        }
        emit(SearchState.Result(result.items, result.fromCache))
    }.catch { e -> emit(SearchState.Failed(e.toUserMessage())) }

    private fun buildState(
        text: String,
        selectedFilter: ListFilter,
        main: MainListState,
        search: SearchState,
        personal: PersonalState
    ): PeopleListUiState {
        val favoriteIds = personal.favorites.map { it.person.id }.toSet()
        fun rowsOf(items: List<TvmPerson>) = PeopleListComposer.visibleRows(
            items = items,
            filter = selectedFilter,
            notes = personal.notes,
            favoriteIds = favoriteIds,
            settings = personal.settings
        )

        val base = PeopleListUiState(
            query = text,
            filter = selectedFilter,
            favorites = personal.favorites,
            lastUpdatedAt = main.updatedAt
        )

        return when (search) {
            SearchState.Inactive -> {
                val rows = rowsOf(main.items)
                val status = main.status
                val content: UiState<List<PersonRow>> = when {
                    rows.isNotEmpty() -> UiState.Content(rows)
                    main.items.isEmpty() && status is RefreshStatus.Failed -> UiState.Error(status.message)
                    main.items.isEmpty() &&
                        (status == RefreshStatus.Loading || status == RefreshStatus.Idle) -> UiState.Loading
                    else -> UiState.Empty
                }
                base.copy(
                    content = content,
                    notice = if (status is RefreshStatus.Failed && main.items.isNotEmpty()) OFFLINE_NOTICE else null,
                    isRefreshing = status == RefreshStatus.Loading && main.items.isNotEmpty()
                )
            }
            SearchState.Loading -> base.copy(content = UiState.Loading)
            is SearchState.Failed -> base.copy(content = UiState.Error(search.message))
            is SearchState.Result -> {
                val rows = rowsOf(search.items)
                base.copy(
                    content = if (rows.isEmpty()) UiState.Empty else UiState.Content(rows),
                    notice = if (search.fromCache) OFFLINE_SEARCH_NOTICE else null
                )
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
