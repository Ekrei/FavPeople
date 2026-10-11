package com.example.comw.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.model.CollectionDetails
import com.example.comw.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionsRepository: CollectionsRepository
) : ViewModel() {

    val collectionId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.Collection.ARG_ID))

    val uiState: StateFlow<UiState<CollectionDetails>> =
        collectionsRepository.observeCollection(collectionId)
            .map { if (it == null) UiState.Empty else UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun update(name: String, description: String) {
        viewModelScope.launch {
            val error = collectionsRepository.update(collectionId, name, description).exceptionOrNull()
            _messages.tryEmit(error?.message ?: "Сохранено")
        }
    }

    fun removePerson(personId: Long) {
        viewModelScope.launch { collectionsRepository.removePerson(collectionId, personId) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            collectionsRepository.delete(collectionId)
            onDeleted()
        }
    }
}
