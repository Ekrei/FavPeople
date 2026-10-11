package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.model.HistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<List<HistoryEntry>>> = historyRepository.observeHistory()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Content(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun clear() {
        viewModelScope.launch { historyRepository.clear() }
    }
}
