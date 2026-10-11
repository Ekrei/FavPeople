package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.model.CollectionSummary
import com.example.comw.model.NoteWithPerson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Личный раздел "Моё": подборки и заметки пользователя (всё из Room, работает офлайн). */
@HiltViewModel
class MyViewModel @Inject constructor(
    private val collectionsRepository: CollectionsRepository,
    notesRepository: NotesRepository
) : ViewModel() {

    val collections: StateFlow<List<CollectionSummary>> = collectionsRepository.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<NoteWithPerson>> = notesRepository.observeNotesWithPeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            val result = collectionsRepository.create(name, description)
            _messages.tryEmit(
                result.exceptionOrNull()?.message ?: "Подборка «${name.trim()}» создана"
            )
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch { collectionsRepository.delete(id) }
    }
}
