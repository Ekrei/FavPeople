package com.example.comw.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.TvmPersonDetail
import com.example.comw.navigation.Routes
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
 * ViewModel экрана деталей.
 * id человека берётся из SavedStateHandle — туда Navigation Compose
 * автоматически кладёт аргументы маршрута "people_detail/{id}".
 */
@HiltViewModel
class PeopleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TvmRepository,
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val personId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.Detail.ARG_ID)) {
        "В аргументах навигации нет id"
    }

    private val _uiState = MutableStateFlow<UiState<TvmPersonDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<TvmPersonDetail>> = _uiState.asStateFlow()

    /** Состояние кнопки "В избранное" — берётся прямо из Room и обновляется само. */
    val isFavorite: StateFlow<Boolean> = favoritesRepository.observeIsFavorite(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val detail = repository.getPersonDetail(personId)
                _uiState.value = UiState.Content(detail)
                // Успешно открыли детали -> пишем в историю просмотров (Room).
                historyRepository.recordView(detail.toTvmPerson())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = UiState.Error("Нет сети, проверьте подключение")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось загрузить детали id=$personId")
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value
        if (current !is UiState.Content) return
        viewModelScope.launch { favoritesRepository.toggle(current.data.toTvmPerson()) }
    }
}
