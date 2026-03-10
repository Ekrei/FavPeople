package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.data.favorites.FavoriteEntry
import com.example.comw.data.favorites.FavoritesStore
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.TvmPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PeopleListViewModel @Inject constructor(
    private val repository: TvmRepository,
    val favoritesStore: FavoritesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<TvmPerson>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<TvmPerson>>> = _uiState.asStateFlow()

    val favorites: StateFlow<List<FavoriteEntry>> = favoritesStore.favorites

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val items = repository.getPeopleList(page = 0)
                _uiState.value = UiState.Content(items.take(20).ifEmpty { items })
            } catch (e: IOException) {
                _uiState.value = UiState.Error("Нет сети или проблема с подключением")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Не удалось загрузить список людей")
            }
        }
    }

    fun addFavorite(person: TvmPerson) {
        favoritesStore.add(person)
    }

    fun removeFavorite(personId: Long) {
        favoritesStore.remove(personId)
    }

    fun isFavorite(personId: Long): Boolean = favoritesStore.isFavorite(personId)
}
