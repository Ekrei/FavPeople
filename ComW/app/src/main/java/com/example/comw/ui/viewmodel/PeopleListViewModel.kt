package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.FavoriteEntry
import com.example.comw.model.TvmPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel главного экрана.
 * Зависимости (репозитории) приходят через конструктор — их создаёт и передаёт Hilt.
 */
@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repository: TvmRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<TvmPerson>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<TvmPerson>>> = _uiState.asStateFlow()

    /** Избранное из Room. Обновляется само, когда меняется таблица favorites. */
    val favorites: StateFlow<List<FavoriteEntry>> = favoritesRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        // Если предыдущая загрузка ещё идёт — отменяем её, чтобы старый ответ не попал в UI.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                val items = repository.getPeopleList(page = 0).take(PAGE_SIZE)
                if (items.isEmpty()) UiState.Empty else UiState.Content(items)
            } catch (e: CancellationException) {
                throw e // отмену корутины нельзя "глотать"
            } catch (e: IOException) {
                UiState.Error("Нет сети или проблема с подключением")
            } catch (e: Exception) {
                UiState.Error("Не удалось загрузить список людей")
            }
        }
    }

    fun toggleFavorite(person: TvmPerson) {
        viewModelScope.launch { favoritesRepository.toggle(person) }
    }

    fun removeFavorite(personId: Long) {
        viewModelScope.launch { favoritesRepository.remove(personId) }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
