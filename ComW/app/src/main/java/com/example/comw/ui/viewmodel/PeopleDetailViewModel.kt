package com.example.comw.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.domain.repository.FavoritesRepository
import com.example.comw.domain.repository.HistoryRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.model.CachedPerson
import com.example.comw.model.CollectionSummary
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.RefreshResult
import com.example.comw.model.TvmPerson
import com.example.comw.model.TvmPersonDetail
import com.example.comw.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Подборка + есть ли в ней этот человек (для чипов на экране деталей). */
data class CollectionChoice(
    val collection: CollectionSummary,
    val containsPerson: Boolean
)

data class DetailUiState(
    val content: UiState<TvmPersonDetail> = UiState.Loading,
    val fetchedAt: Long? = null,
    val notice: String? = null,
    val isRefreshing: Boolean = false,
    val isFavorite: Boolean = false,
    val note: PersonNote? = null,
    val collections: List<CollectionChoice> = emptyList()
)

/**
 * Экран деталей (offline-first): данные читаются из кэша Room, а сеть их обновляет.
 * Здесь же — личные данные: избранное, заметка + статус, подборки.
 */
@HiltViewModel
class PeopleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository,
    private val notesRepository: NotesRepository,
    private val collectionsRepository: CollectionsRepository
) : ViewModel() {

    val personId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.Detail.ARG_ID)) {
        "В аргументах навигации нет id"
    }

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private var refreshJob: Job? = null

    /** Короткие сообщения для Snackbar ("Заметка сохранена" и т.п.). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val cachedPerson: Flow<CachedPerson?> = peopleRepository.observePerson(personId)

    private val collectionChoices: Flow<List<CollectionChoice>> = combine(
        collectionsRepository.observeCollections(),
        collectionsRepository.observeCollectionIdsFor(personId)
    ) { all, mine -> all.map { CollectionChoice(it, it.id in mine) } }

    val uiState: StateFlow<DetailUiState> = combine(
        cachedPerson,
        refreshStatus,
        favoritesRepository.observeIsFavorite(personId),
        notesRepository.observeNote(personId),
        collectionChoices
    ) { cached, status, isFavorite, note, choices ->
        val content: UiState<TvmPersonDetail> = when {
            cached != null -> UiState.Content(cached.detail)
            status is RefreshStatus.Failed -> UiState.Error(status.message)
            status == RefreshStatus.Done -> UiState.Empty
            else -> UiState.Loading
        }
        DetailUiState(
            content = content,
            fetchedAt = cached?.fetchedAt,
            notice = if (cached != null && status is RefreshStatus.Failed) OFFLINE_NOTICE else null,
            isRefreshing = cached != null && status == RefreshStatus.Loading,
            isFavorite = isFavorite,
            note = note,
            collections = choices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        refresh(force = false)
        viewModelScope.launch {
            // В историю пишем, как только о человеке есть данные — из сети или из кэша (офлайн тоже).
            val person = cachedPerson.filterNotNull().first()
            historyRepository.recordView(person.detail.toTvmPerson())
        }
    }

    /** force = false — учитывать TTL; true — кнопка "Обновить"/"Retry". */
    fun refresh(force: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshStatus.value = RefreshStatus.Loading
            refreshStatus.value = when (val result = peopleRepository.refreshPerson(personId, force)) {
                RefreshResult.Updated, RefreshResult.SkippedFresh -> RefreshStatus.Done
                is RefreshResult.Failed -> RefreshStatus.Failed(result.error.toUserMessage())
            }
        }
    }

    fun toggleFavorite() = withPerson { person ->
        favoritesRepository.toggle(person)
    }

    fun saveNote(text: String, status: PersonalStatus) = withPerson { person ->
        notesRepository.save(person, text, status)
        _messages.tryEmit(
            if (text.isBlank() && status == PersonalStatus.NONE) "Заметка удалена" else "Заметка сохранена"
        )
    }

    fun toggleCollection(collectionId: Long, currentlyContains: Boolean) = withPerson { person ->
        if (currentlyContains) {
            collectionsRepository.removePerson(collectionId, person.id)
        } else {
            collectionsRepository.addPerson(collectionId, person)
        }
    }

    fun createCollectionAndAdd(name: String) = withPerson { person ->
        val result = collectionsRepository.create(name)
        val newId = result.getOrNull()
        if (newId != null) {
            collectionsRepository.addPerson(newId, person)
            _messages.tryEmit("Добавлено в «${name.trim()}»")
        } else {
            _messages.tryEmit(result.exceptionOrNull()?.message ?: "Не удалось создать подборку")
        }
    }

    /** Выполнить действие с человеком, если его данные уже есть (в сети или кэше). */
    private fun withPerson(action: suspend (TvmPerson) -> Unit) {
        viewModelScope.launch {
            val cached = peopleRepository.observePerson(personId).first() ?: return@launch
            action(cached.detail.toTvmPerson())
        }
    }
}
