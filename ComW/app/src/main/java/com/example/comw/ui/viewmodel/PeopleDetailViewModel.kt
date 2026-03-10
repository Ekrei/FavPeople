package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.data.favorites.FavoritesStore
import com.example.comw.domain.repository.TvmRepository
import com.example.comw.model.TvmPersonDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class PeopleDetailViewModel @Inject constructor(
    private val repository: TvmRepository,
    private val favoritesStore: FavoritesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<TvmPersonDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<TvmPersonDetail>> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val detail = repository.getPersonDetail(id)
                _uiState.value = UiState.Content(detail)
            } catch (e: IOException) {
                _uiState.value = UiState.Error("No network, check connection")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Can't load details id=$id")
            }
        }
    }

    fun addToFavorites(detail: TvmPersonDetail) {
        favoritesStore.add(detail.toTvmPerson())
    }

    fun removeFromFavorites(personId: Long) {
        favoritesStore.remove(personId)
    }

    fun isFavorite(personId: Long): Boolean = favoritesStore.isFavorite(personId)
}
