package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.FavoriteEntry
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel главного экрана (ДЗ 6 — реактивная версия).
 *
 * Состояние экрана НЕ присваивается вручную — оно СОБИРАЕТСЯ из 4 независимых источников:
 *   1. query          — что пользователь вводит в строку поиска      (StateFlow, UI)
 *   2. filter         — выбранный фильтр по полу                     (StateFlow, UI)
 *   3. favorites      — избранное из Room                            (Flow, data layer)
 *   4. refreshActions — поток действий "Обновить / Retry"            (SharedFlow, события)
 *
 * Цепочка поиска:  query -> debounce -> map(trim) -> distinctUntilChanged
 *                  -> combine(refreshActions) -> flatMapLatest(загрузка)
 * Итог:            combine(query, filter, результат загрузки, favorites) -> stateIn -> UI
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repository: TvmRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    // --- источники ---
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val filter = MutableStateFlow(GenderFilter.ALL)

    /**
     * Поток действий пользователя. SharedFlow, а не StateFlow: у "нажатия" нет
     * "текущего значения", два одинаковых нажатия подряд — это два события,
     * и каждое должно перезапустить загрузку (StateFlow склеил бы одинаковые значения).
     */
    private val refreshActions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val favorites: Flow<List<FavoriteEntry>> = favoritesRepository.observeFavorites()

    // --- преобразования ---
    /** Запросы к сети: ждём паузу в наборе текста, обрезаем пробелы, отсекаем повторы. */
    private val searchRequests: Flow<String> = _query
        .debounce { text -> if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .map { it.trim() }
        .distinctUntilChanged()

    /**
     * Каждый новый запрос ИЛИ нажатие "Обновить" запускает загрузку.
     * flatMapLatest отменяет предыдущую загрузку, если пришёл новый запрос —
     * устаревший ответ никогда не попадёт на экран.
     */
    private val loadResults: Flow<LoadResult> =
        combine(searchRequests, refreshActions.onStart { emit(Unit) }) { text, _ -> text }
            .flatMapLatest { text -> load(text) }

    // --- итоговое состояние экрана ---
    val uiState: StateFlow<PeopleListUiState> = combine(
        _query,
        filter,
        loadResults,
        favorites
    ) { text, selectedFilter, result, favoriteList ->
        PeopleListUiState(
            query = text,
            filter = selectedFilter,
            content = result.toContent(selectedFilter, favoriteList),
            favorites = favoriteList
        )
    }.stateIn(
        scope = viewModelScope,
        // Eagerly: результат не теряется, пока открыт экран деталей,
        // и при возврате список не перезагружается.
        started = SharingStarted.Eagerly,
        initialValue = PeopleListUiState()
    )

    // --- действия пользователя ---
    fun onQueryChange(text: String) {
        _query.value = text
    }

    fun onFilterChange(newFilter: GenderFilter) {
        filter.value = newFilter
    }

    /** Retry / "Обновить": новое событие в потоке действий. */
    fun refresh() {
        refreshActions.tryEmit(Unit)
    }

    fun toggleFavorite(person: TvmPerson) {
        viewModelScope.launch { favoritesRepository.toggle(person) }
    }

    fun removeFavorite(personId: Long) {
        viewModelScope.launch { favoritesRepository.remove(personId) }
    }

    // --- вспомогательное ---
    private fun load(text: String): Flow<LoadResult> = flow {
        emit(LoadResult.Loading)
        val items = if (text.isEmpty()) {
            repository.getPeopleList(page = 0).take(PAGE_SIZE)
        } else {
            repository.searchPeople(text)
        }
        emit(LoadResult.Success(items))
    }.catch { e ->
        emit(
            LoadResult.Failure(
                if (e is IOException) "Нет сети или проблема с подключением"
                else "Не удалось загрузить список людей"
            )
        )
    }

    private fun LoadResult.toContent(
        selectedFilter: GenderFilter,
        favoriteList: List<FavoriteEntry>
    ): UiState<List<PersonRow>> = when (this) {
        LoadResult.Loading -> UiState.Loading
        is LoadResult.Failure -> UiState.Error(message)
        is LoadResult.Success -> {
            val favoriteIds = favoriteList.map { it.person.id }.toSet()
            val rows = items
                .filter { selectedFilter.matches(it) }
                .map { PersonRow(person = it, isFavorite = it.id in favoriteIds) }
            if (rows.isEmpty()) UiState.Empty else UiState.Content(rows)
        }
    }

    companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
